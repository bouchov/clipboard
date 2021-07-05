package com.bouchov.clipboard.entities;

import java.security.SecureRandom;

/**
 * Alexandre Y. Bouchov
 * Date: 05.02.2021
 * Time: 15:25
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class IdGenerator {
    private static final String DIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@";
    private static final long MASK = 0x3FL;

    private static class Holder {
        static final SecureRandom numberGenerator = new SecureRandom();
    }

    public static String generate() {
        long v = Holder.numberGenerator.nextLong();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(code(v, i));
        }
        return sb.toString();
    }

    private static char code(long val, int idx) {
        int n = (int) ((val >> (6 * idx)) & MASK);
        return DIC.charAt(n);
    }
}
