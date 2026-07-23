package org.openscience.sherlock.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {

    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 16;

    private IdGenerator() {
    }

    public static String generateId() {
        final StringBuilder id = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            id.append(BASE62.charAt(ThreadLocalRandom.current().nextInt(BASE62.length())));
        }
        return id.toString();
    }
}
