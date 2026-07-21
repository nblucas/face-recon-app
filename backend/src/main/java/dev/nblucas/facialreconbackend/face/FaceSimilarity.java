package dev.nblucas.facialreconbackend.face;

import org.springframework.stereotype.Component;

@Component
public class FaceSimilarity {

    private static final float MATCH_THRESHOLD = 0.60f;

    public float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    public boolean isMatch(float similarity) {
        return similarity >= MATCH_THRESHOLD;
    }
}
