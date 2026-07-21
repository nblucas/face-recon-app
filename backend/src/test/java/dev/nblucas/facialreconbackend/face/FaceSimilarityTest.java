package dev.nblucas.facialreconbackend.face;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class FaceSimilarityTest {

    private final FaceSimilarity faceSimilarity = new FaceSimilarity();

    @Test
    void shouldReturnOneForIdenticalVectors() {
        float[] vector = {1f, 2f, 3f};

        float similarity = faceSimilarity.cosineSimilarity(vector, vector);

        assertThat(similarity).isCloseTo(1f, offset(0.0001f));
    }

    @Test
    void shouldReturnZeroForOrthogonalVectors() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};

        float similarity = faceSimilarity.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(0f, offset(0.0001f));
    }

    @Test
    void shouldReturnMinusOneForOppositeVectors() {
        float[] a = {1f, 2f, 3f};
        float[] b = {-1f, -2f, -3f};

        float similarity = faceSimilarity.cosineSimilarity(a, b);

        assertThat(similarity).isCloseTo(-1f, offset(0.0001f));
    }

    @Test
    void shouldConsiderMatchWhenSimilarityAboveThreshold() {
        assertThat(faceSimilarity.isMatch(0.41f)).isTrue();
    }

    @Test
    void shouldConsiderMatchWhenSimilarityEqualsThreshold() {
        assertThat(faceSimilarity.isMatch(0.40f)).isTrue();
    }

    @Test
    void shouldNotConsiderMatchWhenSimilarityBelowThreshold() {
        assertThat(faceSimilarity.isMatch(0.39f)).isFalse();
    }
}
