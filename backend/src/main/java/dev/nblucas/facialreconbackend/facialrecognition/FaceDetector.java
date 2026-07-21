package dev.nblucas.facialreconbackend.facialrecognition;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Component
class FaceDetector {

    private static final int INPUT_SIZE = 640;
    private static final float INPUT_MEAN = 127.5f;
    private static final float INPUT_STD = 128.0f;
    private static final int[] STRIDES = {8, 16, 32};
    private static final int NUM_ANCHORS = 2;
    private static final float SCORE_THRESHOLD = 0.5f;
    private static final float NMS_IOU_THRESHOLD = 0.4f;

    private ZooModel<NDList, NDList> model;

    // Releases the native ONNX Runtime resources when the Spring context shuts down.
    @PreDestroy
    void closeModel() {
        if (model != null) {
            model.close();
        }
    }

    // Loads the detection model on first use only, so Spring context tests never trigger a network download.
    private synchronized ZooModel<NDList, NDList> model() {
        if (model == null) {
            try {
                model = FaceModelLoader.load(
                        BuffaloLModels.DETECTION_FILE, BuffaloLModels.DETECTION_URL, BuffaloLModels.DETECTION_SHA256);
            } catch (Exception e) {
                throw new RuntimeException("Could not load face detection model.", e);
            }
        }
        return model;
    }

    // Runs SCRFD end to end: prepares the input tensor, performs inference, and decodes the raw output into faces.
    List<DetectedFace> detect(BufferedImage image) {
        try (NDManager manager = model().getNDManager().newSubManager();
             Predictor<NDList, NDList> predictor = model().newPredictor()) {

            LetterboxedImage letterboxed = letterbox(image, manager);
            NDList output = predictor.predict(new NDList(letterboxed.tensor()));

            return decode(output, letterboxed.scale());
        } catch (Exception e) {
            throw new RuntimeException("Face detection failed.", e);
        }
    }

    // Converts the original image into the normalized 640x640 tensor the model expects.
    private LetterboxedImage letterbox(BufferedImage image, NDManager manager) {
        ScaledCanvas canvas = resizeKeepingAspectRatio(image);
        float[] chw = toNormalizedChwTensor(canvas.image());

        NDArray tensor = manager.create(chw, new Shape(1, 3, INPUT_SIZE, INPUT_SIZE));
        return new LetterboxedImage(tensor, canvas.scale());
    }

    // Fits the image into a 640x640 canvas without distorting its aspect ratio, keeping the scale factor used.
    private ScaledCanvas resizeKeepingAspectRatio(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        float scale = Math.min((float) INPUT_SIZE / originalWidth, (float) INPUT_SIZE / originalHeight);
        int resizedWidth = Math.round(originalWidth * scale);
        int resizedHeight = Math.round(originalHeight * scale);

        BufferedImage canvas = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.drawImage(image, 0, 0, resizedWidth, resizedHeight, null);
        g.dispose();

        return new ScaledCanvas(canvas, scale);
    }

    // Rearranges interleaved RGB pixels into the NCHW float layout (3 separate channel planes) the model expects.
    private float[] toNormalizedChwTensor(BufferedImage canvas) {
        float[] chw = new float[3 * INPUT_SIZE * INPUT_SIZE];
        int plane = INPUT_SIZE * INPUT_SIZE;
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int rgb = canvas.getRGB(x, y);
                int index = y * INPUT_SIZE + x;
                chw[index] = ((rgb >> 16 & 0xFF) - INPUT_MEAN) / INPUT_STD;
                chw[plane + index] = ((rgb >> 8 & 0xFF) - INPUT_MEAN) / INPUT_STD;
                chw[plane * 2 + index] = ((rgb & 0xFF) - INPUT_MEAN) / INPUT_STD;
            }
        }
        return chw;
    }

    // Turns the model's raw per-stride output into face candidates, then removes overlapping duplicates.
    // Package-private so it can be exercised directly in tests, without loading the real model.
    List<DetectedFace> decode(NDList output, float scale) {
        List<DetectedFace> candidates = new ArrayList<>();
        for (int strideIndex = 0; strideIndex < STRIDES.length; strideIndex++) {
            candidates.addAll(decodeStride(output, strideIndex, scale));
        }
        return nonMaxSuppression(candidates);
    }

    // Decodes every anchor position of a single stride's grid into face candidates above the score threshold.
    private List<DetectedFace> decodeStride(NDList output, int strideIndex, float scale) {
        int stride = STRIDES[strideIndex];
        float[] scores = output.get(strideIndex).toFloatArray();
        float[] bboxDeltas = output.get(strideIndex + STRIDES.length).toFloatArray();
        float[] kpsDeltas = output.get(strideIndex + STRIDES.length * 2).toFloatArray();
        int gridSize = gridSizeFromScoreCount(scores.length);

        List<DetectedFace> strideCandidates = new ArrayList<>();
        int position = 0;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                for (int anchor = 0; anchor < NUM_ANCHORS; anchor++) {
                    float score = scores[position];
                    if (score >= SCORE_THRESHOLD) {
                        float centerX = x * stride;
                        float centerY = y * stride;
                        strideCandidates.add(
                                decodeDetection(centerX, centerY, stride, scale, score, bboxDeltas, kpsDeltas, position));
                    }
                    position++;
                }
            }
        }
        return strideCandidates;
    }

    // Derives the (square) grid side from the actual score array size instead of assuming a fixed INPUT_SIZE/stride,
    // so this stays correct if INPUT_SIZE ever changes and so tests can exercise it with small synthetic tensors.
    private int gridSizeFromScoreCount(int scoreCount) {
        return (int) Math.round(Math.sqrt(scoreCount / (double) NUM_ANCHORS));
    }

    // Decodes one anchor position into a single DetectedFace (bounding box + landmarks + score).
    private DetectedFace decodeDetection(float centerX, float centerY, int stride, float scale, float score,
                                          float[] bboxDeltas, float[] kpsDeltas, int position) {
        float[] bbox = decodeBoundingBox(centerX, centerY, stride, scale, bboxDeltas, position * 4);
        float[][] landmarks = decodeLandmarks(centerX, centerY, stride, scale, kpsDeltas, position * 10);
        return new DetectedFace(bbox[0], bbox[1], bbox[2], bbox[3], score, landmarks);
    }

    // Converts the model's predicted (left, top, right, bottom) distances from the anchor center into an absolute
    // bounding box, scaled back to the original image's coordinate space.
    private float[] decodeBoundingBox(float centerX, float centerY, int stride, float scale,
                                       float[] bboxDeltas, int offset) {
        float x1 = (centerX - bboxDeltas[offset] * stride) / scale;
        float y1 = (centerY - bboxDeltas[offset + 1] * stride) / scale;
        float x2 = (centerX + bboxDeltas[offset + 2] * stride) / scale;
        float y2 = (centerY + bboxDeltas[offset + 3] * stride) / scale;
        return new float[] {x1, y1, x2, y2};
    }

    // Converts the model's predicted (x, y) offsets from the anchor center for each of the 5 landmark points into
    // absolute positions, scaled back to the original image's coordinate space.
    private float[][] decodeLandmarks(float centerX, float centerY, int stride, float scale,
                                       float[] kpsDeltas, int offset) {
        float[][] landmarks = new float[5][2];
        for (int point = 0; point < 5; point++) {
            landmarks[point][0] = (centerX + kpsDeltas[offset + point * 2] * stride) / scale;
            landmarks[point][1] = (centerY + kpsDeltas[offset + point * 2 + 1] * stride) / scale;
        }
        return landmarks;
    }

    // Keeps the highest-confidence detection of each face, discarding lower-confidence ones that overlap it too much.
    private List<DetectedFace> nonMaxSuppression(List<DetectedFace> candidates) {
        List<DetectedFace> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Float.compare(b.score(), a.score()));

        List<DetectedFace> kept = new ArrayList<>();
        for (DetectedFace candidate : sorted) {
            boolean overlaps = kept.stream().anyMatch(k -> iou(k, candidate) > NMS_IOU_THRESHOLD);
            if (!overlaps) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    // Computes Intersection over Union: how much two bounding boxes overlap, from 0 (no overlap) to 1 (identical).
    private float iou(DetectedFace a, DetectedFace b) {
        float x1 = Math.max(a.x1(), b.x1());
        float y1 = Math.max(a.y1(), b.y1());
        float x2 = Math.min(a.x2(), b.x2());
        float y2 = Math.min(a.y2(), b.y2());

        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = (a.x2() - a.x1()) * (a.y2() - a.y1());
        float areaB = (b.x2() - b.x1()) * (b.y2() - b.y1());

        return intersection / (areaA + areaB - intersection);
    }

    // Holds the letterboxed input tensor alongside the scale factor needed to map coordinates back to the original image.
    private record LetterboxedImage(NDArray tensor, float scale) {
    }

    // Holds the resized-into-canvas image alongside the scale factor that was applied to it.
    private record ScaledCanvas(BufferedImage image, float scale) {
    }
}
