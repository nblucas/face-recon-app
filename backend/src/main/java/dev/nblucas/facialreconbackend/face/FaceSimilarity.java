package dev.nblucas.facialreconbackend.face;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FaceSimilarity {

    private final float matchThreshold;

    @Autowired
    public FaceSimilarity(@Value("${app.face.match-threshold}") float matchThreshold) {
        this.matchThreshold = matchThreshold;
    }

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
        return similarity >= matchThreshold;
    }
}
