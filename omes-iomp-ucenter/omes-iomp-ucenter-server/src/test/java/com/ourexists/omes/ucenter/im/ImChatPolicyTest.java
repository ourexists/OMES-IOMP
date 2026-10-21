/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImChatPolicyTest {

    @Test
    void onlyTheTenantServiceAccountIsAPeer() {
        assertEquals("cs_t1", ImChatPolicy.serviceUserId("t1"));
        assertEquals("u1", ImChatPolicy.humanPeer("u1", "cs_t1", "t1"));
        assertEquals("u1", ImChatPolicy.humanPeer("cs_t1", "u1", "t1"));
        assertNull(ImChatPolicy.humanPeer("u1", "u2", "t1"));
        assertNull(ImChatPolicy.humanPeer("cs_t2", "u1", "t1"));
        assertNull(ImChatPolicy.humanPeer("cs_t1", "cs_t1", "t1"));
        assertNull(ImChatPolicy.humanPeer(null, "cs_t1", "t1"));
    }
}
