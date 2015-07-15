package com.github.rbuck.dash.common;

import java.util.Random;
import java.util.UUID;

/**
 * Basic utilities to synthesize data.
 */
public class SyntheticData {

    private static final Random random;

    static {
        random = new Random();
    }

    public static String genRandString(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public static String genRandUuid() {
        return "" + UUID.randomUUID();
    }

}
