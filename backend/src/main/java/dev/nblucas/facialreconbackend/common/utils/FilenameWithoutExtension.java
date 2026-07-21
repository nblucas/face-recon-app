package dev.nblucas.facialreconbackend.common.utils;

import java.util.Optional;

public final class FilenameWithoutExtension {

    private FilenameWithoutExtension() {
    }

    public static Optional<String> strip(String filename) {
        if (filename == null) {
            return Optional.empty();
        }

        int dotIndex = filename.lastIndexOf('.');
        String name = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        return name.isEmpty() ? Optional.empty() : Optional.of(name);
    }
}
