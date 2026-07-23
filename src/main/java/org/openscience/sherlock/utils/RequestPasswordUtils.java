package org.openscience.sherlock.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class RequestPasswordUtils {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private RequestPasswordUtils() {
    }

    public static String generatePassword() {
        return IdGenerator.generateId();
    }

    public static String hashPassword(final String requestPassword) {
        return PASSWORD_ENCODER.encode(requestPassword);
    }

    public static boolean matches(final String requestPassword, final String requestPasswordHash) {
        if (requestPassword == null || requestPassword.isBlank()) {
            return false;
        }
        if (requestPasswordHash == null || requestPasswordHash.isBlank()) {
            return false;
        }
        return PASSWORD_ENCODER.matches(requestPassword, requestPasswordHash);
    }
}