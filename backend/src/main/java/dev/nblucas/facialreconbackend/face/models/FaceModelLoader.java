package dev.nblucas.facialreconbackend.face.models;

import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.NoopTranslator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

public final class FaceModelLoader {

    // Official host (storage.insightface.ai) is down — see
    // https://github.com/deepinsight/insightface/issues/1896.
    //
    // Criteria.optModelUrls only auto-downloads/extracts a URL whose last path
    // segment ends in .zip/.tar/.tgz (ai.djl.repository.RepositoryFactoryImpl);
    // anything else (a mirror URL ending in "/download", or a direct .onnx URL)
    // silently falls through to DJL's RpcRepository, meant for remote inference
    // servers, whose download() is a no-op. So the files are downloaded and
    // SHA-256-verified here directly (individual files from the buffalo_l bundle,
    // hosted on Hugging Face: https://huggingface.co/public-data/insightface),
    // then handed to DJL as an already-local, already-verified file via
    // Criteria.optModelPath, which bypasses that URL-extension heuristic entirely.
    private static final Path CACHE_DIR =
            Path.of(System.getProperty("user.home"), ".djl.ai", "facial-recon-models");

    private FaceModelLoader() {
    }

    public static ZooModel<NDList, NDList> load(String fileName, String downloadUrl, String expectedSha256) throws Exception {
        Path modelFile = downloadAndVerify(fileName, downloadUrl, expectedSha256);

        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class)
                .optModelPath(modelFile)
                .optEngine("OnnxRuntime")
                .optTranslator(new NoopTranslator())
                .build();

        return criteria.loadModel();
    }

    private static Path downloadAndVerify(String fileName, String downloadUrl, String expectedSha256) throws Exception {
        Files.createDirectories(CACHE_DIR);
        Path target = CACHE_DIR.resolve(fileName);

        if (Files.exists(target) && expectedSha256.equalsIgnoreCase(sha256Of(target))) {
            return target;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl)).GET().build();
        Path tempFile = Files.createTempFile(CACHE_DIR, fileName, ".part");

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(tempFile);
            throw new IllegalStateException("Failed to download " + fileName + ": HTTP " + response.statusCode());
        }

        String actualSha256 = sha256Of(tempFile);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            Files.deleteIfExists(tempFile);
            throw new IllegalStateException(
                    fileName + ": SHA-256 mismatch. Expected " + expectedSha256 + " but got " + actualSha256);
        }

        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    static String sha256Of(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
