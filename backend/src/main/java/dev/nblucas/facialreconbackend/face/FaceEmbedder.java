package dev.nblucas.facialreconbackend.face;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.ZooModel;
import dev.nblucas.facialreconbackend.face.models.BuffaloLModels;
import dev.nblucas.facialreconbackend.face.models.FaceModelLoader;
import jakarta.annotation.PreDestroy;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

@Component
class FaceEmbedder {

    private static final int INPUT_SIZE = 112;
    private static final float INPUT_MEAN = 127.5f;
    private static final float INPUT_STD = 127.5f;

    private ZooModel<NDList, NDList> model;

    // Releases the native ONNX Runtime resources when the Spring context shuts down.
    @PreDestroy
    void closeModel() {
        if (model != null) {
            model.close();
        }
    }

    // Triggers model loading ahead of the first real request. See FaceModelWarmup.
    void warmUp() {
        model();
    }

    // Loads the embedding model on first use only, so Spring context tests never trigger a network download.
    private synchronized ZooModel<NDList, NDList> model() {
        if (model == null) {
            try {
                model = FaceModelLoader.load(
                        BuffaloLModels.EMBEDDING_FILE, BuffaloLModels.EMBEDDING_URL, BuffaloLModels.EMBEDDING_SHA256);
            } catch (Exception e) {
                throw new RuntimeException("Could not load face embedding model.", e);
            }
        }
        return model;
    }

    // Runs ArcFace on the aligned face crop and returns its 512-dimensional embedding vector.
    float[] embed(Mat alignedFace) {
        try (NDManager manager = model().getNDManager().newSubManager();
             Predictor<NDList, NDList> predictor = model().newPredictor();
             UByteIndexer indexer = alignedFace.createIndexer()) {

            NDArray tensor = manager.create(toChw(indexer), new Shape(1, 3, INPUT_SIZE, INPUT_SIZE));
            NDList output = predictor.predict(new NDList(tensor));

            return output.get(0).toFloatArray();
        } catch (Exception e) {
            throw new RuntimeException("Face embedding extraction failed.", e);
        }
    }

    // Converts the BGR Mat into a normalized RGB NCHW tensor. Package-private so it can be tested
    // directly, without loading the real model.
    float[] toChw(UByteIndexer indexer) {
        float[] chw = new float[3 * INPUT_SIZE * INPUT_SIZE];
        int plane = INPUT_SIZE * INPUT_SIZE;

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int index = y * INPUT_SIZE + x;
                int blue = indexer.get(y, x, 0);
                int green = indexer.get(y, x, 1);
                int red = indexer.get(y, x, 2);

                chw[index] = (red - INPUT_MEAN) / INPUT_STD;
                chw[plane + index] = (green - INPUT_MEAN) / INPUT_STD;
                chw[plane * 2 + index] = (blue - INPUT_MEAN) / INPUT_STD;
            }
        }

        return chw;
    }
}
