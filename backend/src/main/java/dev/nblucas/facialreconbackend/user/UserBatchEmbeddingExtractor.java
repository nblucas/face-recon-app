package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.common.exceptions.InvalidFaceCountException;
import dev.nblucas.facialreconbackend.common.exceptions.InvalidPictureException;
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

    Map<String, float[]> extractAll(Map<String, MultipartFile> picturesByCpf) {
        Map<String, Future<float[]>> futuresByCpf = submitAll(picturesByCpf);
        return collectAll(futuresByCpf);
    }

    private Map<String, Future<float[]>> submitAll(Map<String, MultipartFile> picturesByCpf) {
        Map<String, Future<float[]>> futuresByCpf = new HashMap<>();
        for (Map.Entry<String, MultipartFile> entry : picturesByCpf.entrySet()) {
            MultipartFile picture = entry.getValue();
            futuresByCpf.put(entry.getKey(), executor.submit(() -> faceEmbeddingService.extractEmbedding(picture)));
        }
        return futuresByCpf;
    }

    private Map<String, float[]> collectAll(Map<String, Future<float[]>> futuresByCpf) {
        Map<String, float[]> embeddingsByCpf = new HashMap<>();
        for (Map.Entry<String, Future<float[]>> future : futuresByCpf.entrySet()) {
            embeddingsByCpf.put(future.getKey(), resolve(future.getKey(), future.getValue()));
        }
        return embeddingsByCpf;
    }

    private float[] resolve(String cpf, Future<float[]> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Batch face embedding extraction was interrupted.", e);
        } catch (ExecutionException e) {
            throw withCpf(cpf, e.getCause());
        }
    }

    private RuntimeException withCpf(String cpf, Throwable cause) {
        String message = "CPF " + cpf + ": " + cause.getMessage();
        if (cause instanceof InvalidFaceCountException) {
            return new InvalidFaceCountException(message);
        }
        if (cause instanceof InvalidPictureException) {
            return new InvalidPictureException(message);
        }
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException("Batch face embedding extraction failed.", cause);
    }
}
