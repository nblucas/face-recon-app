package dev.nblucas.facialreconbackend.facialrecognition;

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

public final class ModelLoadingCheck {

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

    private ModelLoadingCheck() {
    }

    public static void main(String[] args) throws Exception {
        checkModelLoads("Detection (SCRFD)", "det_10g.onnx",
                "https://huggingface.co/public-data/insightface/resolve/main/models/buffalo_l/det_10g.onnx",
                "5838f7fe053675b1c7a08b633df49e7af5495cee0493c7dcf6697200b85b5b91");
        checkModelLoads("Embedding (ArcFace)", "w600k_r50.onnx",
                "https://huggingface.co/public-data/insightface/resolve/main/models/buffalo_l/w600k_r50.onnx",
                "4c06341c33c2ca1f86781dab0e829f88ad5b64be9fba56e56bc9ebdefc619e43");
        System.out.println("Both models loaded and verified successfully.");
    }

    private static void checkModelLoads(String label, String modelFileName, String downloadUrl, String expectedSha256)
            throws Exception {
        Path modelFile = downloadAndVerify(modelFileName, downloadUrl, expectedSha256);

        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class)
                .optModelPath(modelFile)
                .optEngine("OnnxRuntime")
                .optTranslator(new NoopTranslator())
                .build();

        try (ZooModel<NDList, NDList> model = criteria.loadModel()) {
            System.out.println(label + " loaded and verified: " + model.getName());
        }
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

    private static String sha256Of(Path file) throws Exception {
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
