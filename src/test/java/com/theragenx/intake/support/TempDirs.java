package com.theragenx.intake.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TempDirs {

    private TempDirs() {
    }

    /** A temporary folder that exists before the Spring context starts, for {@code @DynamicPropertySource}. */
    public static Path create(String prefix) {
        try {
            Path dir = Files.createTempDirectory(prefix);
            dir.toFile().deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
