/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImLinkCountTest {

    @Test
    void lastCloseDropsTheSocketAndAStrayCloseDoesNot() {
        ImLinkCount links = new ImLinkCount();
        assertTrue(links.acquire("u1"));
        assertFalse(links.acquire("u1"));
        assertFalse(links.release("u1"));
        assertTrue(links.release("u1"));
        assertFalse(links.release("u1"));
    }

    @Test
    void freezeClearsEveryWindow() {
        ImLinkCount links = new ImLinkCount();
        links.acquire("u1");
        links.acquire("u1");
        links.clear("u1");
        assertTrue(links.acquire("u1"));
    }
}
