package dev.nblucas.facialreconbackend.facialrecognition;

record DetectedFace(float x1, float y1, float x2, float y2, float score, float[][] landmarks) {
}
