package dev.nblucas.facialreconbackend.facialrecognition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FaceModelLoaderTest {

    @Test
    void shouldComputeKnownSha256OfFileContent() throws Exception {
        Path file = Files.createTempFile("face-model-loader-test", ".bin");
        Files.writeString(file, "hello world");

        String sha256 = FaceModelLoader.sha256Of(file);

        assertThat(sha256).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }
}
