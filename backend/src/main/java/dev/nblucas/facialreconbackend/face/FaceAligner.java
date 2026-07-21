package dev.nblucas.facialreconbackend.face;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point2f;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

import static org.bytedeco.opencv.global.opencv_calib3d.estimateAffinePartial2D;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_imgproc.warpAffine;

@Component
class FaceAligner {

    private static final int OUTPUT_SIZE = 112;

    // Standard ArcFace reference template for the 112x112 crop (left eye, right eye, nose,
    // left mouth corner, right mouth corner).
    private static final float[][] REFERENCE_LANDMARKS = {
            {38.2946f, 51.6963f},
            {73.5318f, 51.5014f},
            {56.0252f, 71.7366f},
            {41.5493f, 92.3655f},
            {70.7299f, 92.2041f}
    };

    // Estimates the rotation/scale/translation that best maps the detected landmarks onto the
    // reference template, then applies it to produce a 112x112 aligned face crop.
    Mat align(BufferedImage image, float[][] landmarks) {
        try (Mat source = toMat(image);
             Mat sourcePoints = toPointsMat(landmarks);
             Mat referencePoints = toPointsMat(REFERENCE_LANDMARKS);
             Mat affineMatrix = estimateAffinePartial2D(sourcePoints, referencePoints)) {

            Mat aligned = new Mat();
            warpAffine(source, aligned, affineMatrix, new Size(OUTPUT_SIZE, OUTPUT_SIZE));
            return aligned;
        }
    }

    // Converts a Java BufferedImage into an OpenCV Mat, writing channels in OpenCV's BGR convention.
    private Mat toMat(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Mat mat = new Mat(height, width, CV_8UC3);

        try (UByteIndexer indexer = mat.createIndexer()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    indexer.put(y, x, 0, rgb & 0xFF);
                    indexer.put(y, x, 1, (rgb >> 8) & 0xFF);
                    indexer.put(y, x, 2, (rgb >> 16) & 0xFF);
                }
            }
        }

        return mat;
    }

    // Packs an array of (x, y) points into the Mat format estimateAffinePartial2D expects.
    private Mat toPointsMat(float[][] points) {
        try (Point2f buffer = new Point2f(points.length)) {
            for (int i = 0; i < points.length; i++) {
                buffer.position(i).x(points[i][0]).y(points[i][1]);
            }
            buffer.position(0);
            return new Mat(buffer);
        }
    }
}
