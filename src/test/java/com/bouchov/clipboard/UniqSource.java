package com.bouchov.clipboard;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Alexandre Y. Bouchov
 * Date: 21.07.2021
 * Time: 16:52
 * Copyright 2014 ConnectiveGames LLC. All rights reserved.
 */
public class UniqSource {
    private static final AtomicLong number = new AtomicLong();

    public static String uniqueString(String prefix) {
        return prefix + number.incrementAndGet();
    }

    public static Long uniqueLong() {
        return uniqueLong(0L);
    }

    public static Long uniqueLong(long start) {
        return number.incrementAndGet() + start;
    }

    public static UUID uuid() {
        return UUID.randomUUID();
    }
}
