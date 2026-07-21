package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.face.FaceSimilarity;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserIdentifierTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FaceSimilarity faceSimilarity;

    private UserIdentifier userIdentifier;

    private static final float[] QUERY_EMBEDDING = new float[512];

    @BeforeEach
    void setUp() {
        userIdentifier = new UserIdentifier(userRepository, faceSimilarity);
    }

    @Test
    void shouldReturnEmptyWhenNoUsersAreRegistered() {
        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE)).thenReturn(List.of());

        Optional<TbUsersRecord> result = userIdentifier.findBestMatch(QUERY_EMBEDDING);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenBestSimilarityIsBelowThreshold() {
        TbUsersRecord user = userWithEmbedding(1L);
        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE)).thenReturn(List.of(user));
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), any())).thenReturn(0.1f);
        when(faceSimilarity.isMatch(0.1f)).thenReturn(false);

        Optional<TbUsersRecord> result = userIdentifier.findBestMatch(QUERY_EMBEDDING);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnUserWhenSimilarityIsAtOrAboveThreshold() {
        TbUsersRecord user = userWithEmbedding(1L);
        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE)).thenReturn(List.of(user));
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), any())).thenReturn(0.9f);
        when(faceSimilarity.isMatch(0.9f)).thenReturn(true);

        Optional<TbUsersRecord> result = userIdentifier.findBestMatch(QUERY_EMBEDDING);

        assertThat(result).contains(user);
    }

    @Test
    void shouldPickHighestSimilarityAmongMultipleUsersInTheSamePage() {
        TbUsersRecord lowSimilarityUser = userWithEmbedding(1L);
        TbUsersRecord highSimilarityUser = userWithEmbedding(2L);
        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE))
                .thenReturn(List.of(lowSimilarityUser, highSimilarityUser));
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), eq(unbox(lowSimilarityUser.getEmbedding()))))
                .thenReturn(0.5f);
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), eq(unbox(highSimilarityUser.getEmbedding()))))
                .thenReturn(0.9f);
        when(faceSimilarity.isMatch(0.9f)).thenReturn(true);

        Optional<TbUsersRecord> result = userIdentifier.findBestMatch(QUERY_EMBEDDING);

        assertThat(result).contains(highSimilarityUser);
    }

    @Test
    void shouldFetchNextPageWhenFirstPageIsFull() {
        List<TbUsersRecord> fullPage = new ArrayList<>();
        for (long id = 1; id <= UserIdentifier.BATCH_SIZE; id++) {
            fullPage.add(userWithEmbedding(id));
        }
        TbUsersRecord secondPageUser = userWithEmbedding(-1L);

        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE)).thenReturn(fullPage);
        when(userRepository.findAll(UserIdentifier.BATCH_SIZE, UserIdentifier.BATCH_SIZE))
                .thenReturn(List.of(secondPageUser));
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), any())).thenReturn(0.1f);
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), eq(unbox(secondPageUser.getEmbedding()))))
                .thenReturn(0.9f);
        when(faceSimilarity.isMatch(0.9f)).thenReturn(true);

        Optional<TbUsersRecord> result = userIdentifier.findBestMatch(QUERY_EMBEDDING);

        assertThat(result).contains(secondPageUser);
        verify(userRepository).findAll(UserIdentifier.BATCH_SIZE, UserIdentifier.BATCH_SIZE);
    }

    @Test
    void shouldNotFetchNextPageWhenPageIsShorterThanBatchSize() {
        TbUsersRecord user = userWithEmbedding(1L);
        when(userRepository.findAll(0, UserIdentifier.BATCH_SIZE)).thenReturn(List.of(user));
        when(faceSimilarity.cosineSimilarity(eq(QUERY_EMBEDDING), any())).thenReturn(0.9f);
        when(faceSimilarity.isMatch(0.9f)).thenReturn(true);

        userIdentifier.findBestMatch(QUERY_EMBEDDING);

        verify(userRepository, never()).findAll(UserIdentifier.BATCH_SIZE, UserIdentifier.BATCH_SIZE);
    }

    private TbUsersRecord userWithEmbedding(long id) {
        Float[] embedding = new Float[512];
        Arrays.fill(embedding, (float) id);
        return new TbUsersRecord(
                id, "User " + id, "%011d".formatted(id), "picture.png",
                OffsetDateTime.now(), OffsetDateTime.now(), embedding);
    }

    private float[] unbox(Float[] values) {
        float[] unboxed = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            unboxed[i] = values[i];
        }
        return unboxed;
    }
}
