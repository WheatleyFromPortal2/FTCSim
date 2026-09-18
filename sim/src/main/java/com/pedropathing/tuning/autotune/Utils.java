package com.pedropathing.tuning.autotune;

import java.security.SecureRandom;

public final class Utils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private Utils() {}
    public static String nanoid() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
