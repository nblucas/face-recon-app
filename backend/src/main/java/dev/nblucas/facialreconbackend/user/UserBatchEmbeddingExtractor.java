package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.face.FaceEmbeddingService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
class UserBatchEmbeddingExtractor {

    private final FaceEmbeddingService faceEmbeddingService;
    private final ExecutorService executor;

    @Autowired
    UserBatchEmbeddingExtractor(FaceEmbeddingService faceEmbeddingService) {
        this.faceEmbeddingService = faceEmbeddingService;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    Map<String, float[]> extractAll(Map<String, MultipartFile> picturesByClientId) {
        Map<String, Future<float[]>> futuresByClientId = submitAll(picturesByClientId);
        return collectAll(futuresByClientId);
    }

    private Map<String, Future<float[]>> submitAll(Map<String, MultipartFile> picturesByClientId) {
        Map<String, Future<float[]>> futuresByClientId = new HashMap<>();
        for (Map.Entry<String, MultipartFile> entry : picturesByClientId.entrySet()) {
            MultipartFile picture = entry.getValue();
            futuresByClientId.put(entry.getKey(), executor.submit(() -> faceEmbeddingService.extractEmbedding(picture)));
        }
        return futuresByClientId;
    }

    private Map<String, float[]> collectAll(Map<String, Future<float[]>> futuresByClientId) {
        Map<String, float[]> embeddingsByClientId = new HashMap<>();
        for (Map.Entry<String, Future<float[]>> future : futuresByClientId.entrySet()) {
            embeddingsByClientId.put(future.getKey(), resolve(future.getValue()));
        }
        return embeddingsByClientId;
    }

    private float[] resolve(Future<float[]> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Batch face embedding extraction was interrupted.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Batch face embedding extraction failed.", e.getCause());
        }
    }
}
