/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.portal.device.protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 一条网关连接最近一次连通结果，供列表展示。 */
final class ConnectLink {

    static final String UP = "up";
    static final String DOWN = "down";
    static final String PENDING = "pending";

    private final Map<String, String> state = new ConcurrentHashMap<>();
    private final Map<String, String> message = new ConcurrentHashMap<>();

    void pending(String id) {
        if (id == null) {
            return;
        }
        state.put(id, PENDING);
        message.remove(id);
    }

    void up(String id) {
        if (id == null) {
            return;
        }
        state.put(id, UP);
        message.remove(id);
    }

    void down(String id, String msg) {
        if (id == null) {
            return;
        }
        state.put(id, DOWN);
        putMessage(id, msg);
    }

    /** 没有拉起。状态为空，原因留给启动失败提示。 */
    void failed(String id, String msg) {
        if (id == null) {
            return;
        }
        state.remove(id);
        putMessage(id, msg);
    }

    void clear(String id) {
        if (id == null) {
            return;
        }
        state.remove(id);
        message.remove(id);
    }

    String state(String id) {
        return id == null ? null : state.get(id);
    }

    String message(String id) {
        return id == null ? null : message.get(id);
    }

    private void putMessage(String id, String msg) {
        if (msg == null || msg.isBlank()) {
            message.remove(id);
        } else {
            message.put(id, msg);
        }
    }
}
