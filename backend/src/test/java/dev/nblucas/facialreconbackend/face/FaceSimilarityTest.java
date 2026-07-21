package dev.nblucas.facialreconbackend.face;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class FaceSimilarityTest {

    private static final float MATCH_THRESHOLD = loadMatchThreshold();

    private final FaceSimilarity faceSimilarity = new FaceSimilarity(MATCH_THRESHOLD);

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
        assertThat(faceSimilarity.isMatch(MATCH_THRESHOLD + 0.01f)).isTrue();
    }

    @Test
    void shouldConsiderMatchWhenSimilarityEqualsThreshold() {
        assertThat(faceSimilarity.isMatch(MATCH_THRESHOLD)).isTrue();
    }

    @Test
    void shouldNotConsiderMatchWhenSimilarityBelowThreshold() {
        assertThat(faceSimilarity.isMatch(MATCH_THRESHOLD - 0.01f)).isFalse();
    }

    private static float loadMatchThreshold() {
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
            return Float.parseFloat(properties.getProperty("app.face.match-threshold"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
