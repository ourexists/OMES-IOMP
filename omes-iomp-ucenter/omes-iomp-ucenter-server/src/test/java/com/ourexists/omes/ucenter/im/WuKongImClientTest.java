/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WuKongImClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void personChannelIdBecomesTheOtherUser() {
        assertEquals("cs_t1", WuKongImClient.peerOf("u1", "u1@cs_t1"));
        assertEquals("u1", WuKongImClient.peerOf("cs_t1", "cs_t1@u1"));
        assertEquals("u1", WuKongImClient.peerOf("cs_t1", "u1"));
        assertEquals("", WuKongImClient.peerOf("cs_t1", "a@b"));
    }

    @Test
    void textPayloadKeepsTypeOne() throws Exception {
        assertEquals("你好", WuKongImClient.textOf(encode("{\"type\":1,\"content\":\"你好\"}"), mapper));
        assertEquals("", WuKongImClient.textOf(encode("{\"type\":2,\"content\":\"img\"}"), mapper));
        assertEquals("", WuKongImClient.textOf("not-json", mapper));
    }

    @Test
    void secondsBecomeMillis() {
        assertEquals(1_700_000_000_000L, WuKongImClient.millis(1_700_000_000L));
        assertEquals(1_700_000_000_000L, WuKongImClient.millis(1_700_000_000_000L));
        assertEquals(0L, WuKongImClient.millis(0L));
    }

    @Test
    void historyReadsWrappedAndBareMessages() throws Exception {
        String payload = encode("{\"type\":1,\"content\":\"hi\"}");
        String wrapped = """
                {"messages":[{"from_uid":"u1","channel_id":"cs_t1","timestamp":1700000000,"payload":"%s"}]}
                """.formatted(payload);
        List<WuKongImClient.ImRaw> rows = WuKongImClient.messagesOf(mapper.readTree(wrapped), mapper);
        assertEquals(1, rows.size());
        assertEquals("u1", rows.get(0).sendId());
        assertEquals("cs_t1", rows.get(0).recvId());
        assertEquals("hi", rows.get(0).text());
        assertEquals(1_700_000_000_000L, rows.get(0).sendTime());

        String bare = """
                [{"from_uid":"cs_t1","channel_id":"u1","timestamp":1700000001,"payload":"%s"}]
                """.formatted(payload);
        assertEquals(1, WuKongImClient.messagesOf(mapper.readTree(bare), mapper).size());
    }

    @Test
    void deskReadsFakeChannelAndLastMessage() throws Exception {
        String payload = encode("{\"type\":1,\"content\":\"hello\"}");
        String sync = """
                [{"channel_id":"cs_t1@u1","channel_type":1,"timestamp":1700000000,"recents":[{"from_uid":"u1","timestamp":1700000000,"payload":"%s"}]},
                 {"channel_id":"g1","channel_type":2,"timestamp":1700000000,"recents":[]}]
                """.formatted(payload);
        List<WuKongImClient.ImHit> hits = WuKongImClient.hitsOf(mapper.readTree(sync), "cs_t1", mapper);
        assertEquals(1, hits.size());
        assertEquals("u1", hits.get(0).userId());
        assertEquals("hello", hits.get(0).preview());
        assertEquals(1_700_000_000_000L, hits.get(0).sendTime());

        String listed = """
                {"conversations":[{"channel_id":"u2","channel_type":1,"last_message":{"payload":"%s","server_timestamp_ms":1700000000000}}]}
                """.formatted(payload);
        List<WuKongImClient.ImHit> page = WuKongImClient.hitsOf(mapper.readTree(listed), "cs_t1", mapper);
        assertEquals("u2", page.get(0).userId());
        assertTrue(page.get(0).sendTime() > 0);
    }

    private static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
