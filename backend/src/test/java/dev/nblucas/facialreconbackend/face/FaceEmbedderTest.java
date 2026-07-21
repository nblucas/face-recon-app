package dev.nblucas.facialreconbackend.face;

import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;

class FaceEmbedderTest {

    private final FaceEmbedder embedder = new FaceEmbedder();

    @Test
    void shouldNormalizeAndSwapChannelsFromBgrMatToRgbChwTensor() {
        try (Mat mat = new Mat(112, 112, CV_8UC3);
             UByteIndexer indexer = mat.createIndexer()) {

            indexer.put(0, 0, 0, 0);
            indexer.put(0, 0, 1, 127);
            indexer.put(0, 0, 2, 255);

            float[] chw = embedder.toChw(indexer);

            int plane = 112 * 112;
            assertThat(chw[0]).isCloseTo((255 - 127.5f) / 127.5f, org.assertj.core.data.Offset.offset(0.01f));
            assertThat(chw[plane]).isCloseTo((127 - 127.5f) / 127.5f, org.assertj.core.data.Offset.offset(0.01f));
            assertThat(chw[plane * 2]).isCloseTo((0 - 127.5f) / 127.5f, org.assertj.core.data.Offset.offset(0.01f));
        }
    }
}
