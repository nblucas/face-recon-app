package dev.nblucas.facialreconbackend.common.utils;

public final class EmbeddingCodec {

    private EmbeddingCodec() {
    }

    public static Float[] box(float[] values) {
        Float[] boxed = new Float[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        return boxed;
    }

    public static float[] unbox(Float[] values) {
        float[] unboxed = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            unboxed[i] = values[i];
        }
        return unboxed;
    }
}
