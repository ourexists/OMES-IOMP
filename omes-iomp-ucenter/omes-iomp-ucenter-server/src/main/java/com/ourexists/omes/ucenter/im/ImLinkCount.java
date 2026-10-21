/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import java.util.HashMap;
import java.util.Map;

/**
 * How many open chat windows still hold this user's WuKongIM sign-in.
 */
final class ImLinkCount {

    private final Map<String, Integer> refs = new HashMap<>();

    synchronized boolean acquire(String userId) {
        int next = refs.getOrDefault(userId, 0) + 1;
        refs.put(userId, next);
        return next == 1;
    }

    synchronized boolean release(String userId) {
        Integer current = refs.get(userId);
        if (current == null) {
            return false;
        }
        if (current <= 1) {
            refs.remove(userId);
            return true;
        }
        refs.put(userId, current - 1);
        return false;
    }

    synchronized void clear(String userId) {
        refs.remove(userId);
    }
}
