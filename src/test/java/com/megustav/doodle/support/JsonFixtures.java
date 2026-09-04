package com.megustav.doodle.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads a JSON request body from {@code src/test/resources} instead of keeping it inlined as a
 * text block in test code. Placeholders are plain {@code %s} - call {@link String#formatted} on
 * the result exactly as you would on an inline text block.
 */
public final class JsonFixtures {

    private JsonFixtures() {
    }

    public static String load(String path) {
        try (var in = JsonFixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No such JSON fixture on the test classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
