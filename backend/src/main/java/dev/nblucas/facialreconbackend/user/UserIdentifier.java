package dev.nblucas.facialreconbackend.user;

import dev.nblucas.facialreconbackend.face.FaceSimilarity;
import dev.nblucas.facialreconbackend.jooq.tables.records.TbUsersRecord;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
class UserIdentifier {

    static final int BATCH_SIZE = 1000;

    private final UserRepository userRepository;
    private final FaceSimilarity faceSimilarity;
    private final ExecutorService executor;

    @Autowired
    UserIdentifier(UserRepository userRepository, FaceSimilarity faceSimilarity) {
        this.userRepository = userRepository;
        this.faceSimilarity = faceSimilarity;
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    Optional<TbUsersRecord> findBestMatch(float[] queryEmbedding) {
        ScoredUser best = null;

        int offset = 0;
        List<TbUsersRecord> page;
        while (isPageNotEmpty(page = fetchPage(offset))) {
            ScoredUser pageBest = bestInPage(queryEmbedding, page);
            if (isBetter(pageBest, best)) {
                best = pageBest;
            }

            if (isLastPage(page)) {
                break;
            }
            offset += BATCH_SIZE;
        }

        return toMatchResult(best);
    }

    private List<TbUsersRecord> fetchPage(int offset) {
        return userRepository.findAll(offset, BATCH_SIZE);
    }

    private boolean isPageNotEmpty(List<TbUsersRecord> page) {
        return !page.isEmpty();
    }

    private boolean isLastPage(List<TbUsersRecord> page) {
        return page.size() < BATCH_SIZE;
    }

    private Optional<TbUsersRecord> toMatchResult(ScoredUser best) {
        return best != null && faceSimilarity.isMatch(best.similarity())
                ? Optional.of(best.user())
                : Optional.empty();
    }

    private ScoredUser bestInPage(float[] queryEmbedding, List<TbUsersRecord> page) {
        List<Callable<ScoredUser>> tasks = new ArrayList<>();
        for (List<TbUsersRecord> chunk : partitionIntoChunks(page)) {
            tasks.add(() -> bestInChunk(queryEmbedding, chunk));
        }

        try {
            return reduceToBest(executor.invokeAll(tasks));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Face identification was interrupted.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Face identification failed.", e.getCause());
        }
    }

    private List<List<TbUsersRecord>> partitionIntoChunks(List<TbUsersRecord> page) {
        int chunkCount = Math.min(page.size(), Runtime.getRuntime().availableProcessors());
        int chunkSize = (int) Math.ceil(page.size() / (double) chunkCount);

        List<List<TbUsersRecord>> chunks = new ArrayList<>();
        for (int start = 0; start < page.size(); start += chunkSize) {
            chunks.add(page.subList(start, Math.min(start + chunkSize, page.size())));
        }
        return chunks;
    }

    private ScoredUser reduceToBest(List<Future<ScoredUser>> futures) throws InterruptedException, ExecutionException {
        ScoredUser best = null;
        for (Future<ScoredUser> future : futures) {
            ScoredUser chunkBest = future.get();
            if (isBetter(chunkBest, best)) {
                best = chunkBest;
            }
        }
        return best;
    }

    private ScoredUser bestInChunk(float[] queryEmbedding, List<TbUsersRecord> chunk) {
        ScoredUser best = null;

        for (TbUsersRecord candidate : chunk) {
            float similarity = faceSimilarity.cosineSimilarity(queryEmbedding, unbox(candidate.getEmbedding()));
            ScoredUser scored = new ScoredUser(candidate, similarity);
            if (isBetter(scored, best)) {
                best = scored;
            }
        }

        return best;
    }

    private boolean isBetter(ScoredUser candidate, ScoredUser current) {
        return candidate != null && (current == null || candidate.similarity() > current.similarity());
    }

    private static float[] unbox(Float[] values) {
        float[] unboxed = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            unboxed[i] = values[i];
        }
        return unboxed;
    }

    private record ScoredUser(TbUsersRecord user, float similarity) {
    }
}
