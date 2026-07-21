package dev.nblucas.facialreconbackend.facialrecognition;

import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.ZooModel;

public final class ModelLoadingCheck {

    private ModelLoadingCheck() {
    }

    public static void main(String[] args) throws Exception {
        try (ZooModel<NDList, NDList> detector = FaceModelLoader.load(
                BuffaloLModels.DETECTION_FILE, BuffaloLModels.DETECTION_URL, BuffaloLModels.DETECTION_SHA256)) {
            System.out.println("Detection (SCRFD) loaded and verified: " + detector.getName());
        }

        try (ZooModel<NDList, NDList> embedder = FaceModelLoader.load(
                BuffaloLModels.EMBEDDING_FILE, BuffaloLModels.EMBEDDING_URL, BuffaloLModels.EMBEDDING_SHA256)) {
            System.out.println("Embedding (ArcFace) loaded and verified: " + embedder.getName());
        }

        System.out.println("Both models loaded and verified successfully.");
    }
}
