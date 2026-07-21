package dev.nblucas.facialreconbackend.facialrecognition;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class FaceAlignerTest {

    private final FaceAligner aligner = new FaceAligner();

    @Test
    void shouldProduceA112x112AlignedCrop() {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        float[][] landmarks = {
                {70f, 70f}, {130f, 70f}, {100f, 100f}, {75f, 130f}, {125f, 130f}
        };

        try (Mat aligned = aligner.align(image, landmarks)) {
            assertThat(aligned.rows()).isEqualTo(112);
            assertThat(aligned.cols()).isEqualTo(112);
        }
    }

    @Test
    void shouldPreserveColorAtLandmarkWhenIdentityAligned() {
        BufferedImage image = new BufferedImage(112, 112, BufferedImage.TYPE_INT_RGB);
        int leftEyeX = 38;
        int leftEyeY = 52;
        image.setRGB(leftEyeX, leftEyeY, new Color(200, 100, 50).getRGB());

        float[][] identityLandmarks = {
                {38.2946f, 51.6963f}, {73.5318f, 51.5014f}, {56.0252f, 71.7366f},
                {41.5493f, 92.3655f}, {70.7299f, 92.2041f}
        };

        try (Mat aligned = aligner.align(image, identityLandmarks);
             UByteIndexer indexer = aligned.createIndexer()) {

            int blue = indexer.get(leftEyeY, leftEyeX, 0);
            int green = indexer.get(leftEyeY, leftEyeX, 1);
            int red = indexer.get(leftEyeY, leftEyeX, 2);

            assertThat(red).isCloseTo(200, org.assertj.core.data.Offset.offset(5));
            assertThat(green).isCloseTo(100, org.assertj.core.data.Offset.offset(5));
            assertThat(blue).isCloseTo(50, org.assertj.core.data.Offset.offset(5));
        }
    }
}
