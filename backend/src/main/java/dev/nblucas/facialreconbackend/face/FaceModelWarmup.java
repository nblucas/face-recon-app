package dev.nblucas.facialreconbackend.face;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Loads the detection and embedding models as soon as the application starts, so the first real
// registration/identification/verification request doesn't pay the download-and-load cost.
// Disabled under the "test" profile so Spring context tests never trigger a network download.
//
// Loaded sequentially, not in parallel: DJL's OnnxRuntime engine registration (triggered by the
// first Criteria.loadModel() call) is not thread-safe, and loading both models concurrently races
// on that first-time registration, intermittently failing with "ModelZoo doesn't support specified
// engine: OnnxRuntime".
@Component
@Profile("!test")
class FaceModelWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FaceModelWarmup.class);

    private final FaceDetector faceDetector;
    private final FaceEmbedder faceEmbedder;

    FaceModelWarmup(FaceDetector faceDetector, FaceEmbedder faceEmbedder) {
        this.faceDetector = faceDetector;
        this.faceEmbedder = faceEmbedder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Warming up face detection and embedding models...");
        long start = System.currentTimeMillis();

        faceDetector.warmUp();
        faceEmbedder.warmUp();

        log.info("Face models warmed up in {}ms.", System.currentTimeMillis() - start);
    }
}
