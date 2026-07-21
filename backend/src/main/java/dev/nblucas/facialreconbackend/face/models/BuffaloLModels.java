package dev.nblucas.facialreconbackend.face.models;

public final class BuffaloLModels {

    public static final String DETECTION_FILE = "det_10g.onnx";
    public static final String DETECTION_URL =
            "https://huggingface.co/public-data/insightface/resolve/main/models/buffalo_l/det_10g.onnx";
    public static final String DETECTION_SHA256 =
            "5838f7fe053675b1c7a08b633df49e7af5495cee0493c7dcf6697200b85b5b91";

    public static final String EMBEDDING_FILE = "w600k_r50.onnx";
    public static final String EMBEDDING_URL =
            "https://huggingface.co/public-data/insightface/resolve/main/models/buffalo_l/w600k_r50.onnx";
    public static final String EMBEDDING_SHA256 =
            "4c06341c33c2ca1f86781dab0e829f88ad5b64be9fba56e56bc9ebdefc619e43";

    private BuffaloLModels() {
    }
}
