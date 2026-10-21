/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.ourexists.era.framework.core.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WuKongIM HTTP client. The browser never calls WuKongIM and never sees a token.
 * shortcut: history is the latest 50 messages of the person channel.
 */
@Slf4j
@Component
public class WuKongImClient {

    private static final int HISTORY_LIMIT = 50;

    private static final int WEB_DEVICE = 1;

    private static final int ALL_DEVICES = -1;

    private static final int TEXT_TYPE = 1;

    private static final long MILLIS_BOUND = 1_000_000_000_000L;

    private final WuKongImProperties properties;

    private final ObjectMapper objectMapper;

    private final RestTemplate http;

    private final ImLinkCount links = new ImLinkCount();

    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    public WuKongImClient(WuKongImProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = new RestTemplate(factory);
    }

    public void ensureUser(String userId, String nickname) {
        requireReady();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", userId);
        body.put("token", tokens.computeIfAbsent(userId, id -> UUID.randomUUID().toString()));
        body.put("device_flag", WEB_DEVICE);
        body.put("device_level", 1);
        post("/user/token", body, false);
    }

    public void sendText(String from, String to, String nickname, String text) {
        requireReady();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("header", Map.of("no_persist", 0, "red_dot", 1, "sync_once", 0));
        body.put("from_uid", from);
        body.put("channel_id", to);
        body.put("channel_type", 1);
        body.put("payload", payload(text));
        post("/message/send", body, false);
    }

    public List<ImRaw> searchSingle(String loginUid, String peerUid) {
        requireReady();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("login_uid", loginUid);
        body.put("channel_id", peerUid);
        body.put("channel_type", 1);
        body.put("start_message_seq", 0);
        body.put("end_message_seq", 0);
        body.put("limit", HISTORY_LIMIT);
        body.put("pull_mode", 1);
        return messagesOf(post("/channel/messagesync", body, false), objectMapper);
    }

    public List<ImHit> conversations(String serviceUserId) {
        requireReady();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("uid", serviceUserId);
        body.put("version", 0);
        body.put("last_msg_seqs", "");
        body.put("msg_count", 1);
        body.put("only_unread", 0);
        return hitsOf(post("/conversation/sync", body, false), serviceUserId, objectMapper);
    }

    public void connect(String userId) {
        links.acquire(userId);
    }

    public void disconnect(String userId) {
        if (links.release(userId)) {
            deviceQuit(userId, WEB_DEVICE);
        }
    }

    public void drop(String userId) {
        links.clear(userId);
        deviceQuit(userId, ALL_DEVICES);
    }

    private void deviceQuit(String userId, int deviceFlag) {
        requireReady();
        post("/user/device_quit", Map.of("uid", userId, "device_flag", deviceFlag), true);
    }

    private String payload(String text) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(Map.of("type", TEXT_TYPE, "content", text));
            return Base64.getEncoder().encodeToString(json);
        } catch (Exception ex) {
            throw new BusinessException("在线客服暂时不可用");
        }
    }

    private JsonNode post(String path, Object body, boolean missingIsOk) {
        String base = properties.getApiUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", properties.getSecret());
        try {
            String raw = http.exchange(base + path, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class).getBody();
            return read(raw);
        } catch (HttpStatusCodeException ex) {
            if (missingIsOk && ex.getStatusCode().is4xxClientError()) {
                return objectMapper.createObjectNode();
            }
            log.warn("wukong {} status={}", path, ex.getStatusCode().value());
            throw new BusinessException("在线客服暂时不可用");
        } catch (RestClientException ex) {
            log.warn("wukong {} unreachable", path);
            throw new BusinessException("在线客服暂时不可用");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("wukong {} response unreadable", path);
            throw new BusinessException("在线客服暂时不可用");
        }
    }

    private JsonNode read(String raw) throws Exception {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createObjectNode();
        }
        JsonNode node = objectMapper.readTree(raw);
        if (node.isObject() && node.has("status")) {
            int status = node.path("status").asInt(200);
            if (status != 200 && status != 0) {
                log.warn("wukong status={} msg={}", status, node.path("msg").asText(""));
                throw new BusinessException("在线客服暂时不可用");
            }
        }
        return node;
    }

    private void requireReady() {
        if (!properties.isEnabled()) {
            throw new BusinessException("在线客服未启用");
        }
        if (!StringUtils.hasText(properties.getApiUrl()) || !StringUtils.hasText(properties.getSecret())) {
            throw new BusinessException("在线客服暂时不可用");
        }
    }

    static List<ImRaw> messagesOf(JsonNode root, ObjectMapper mapper) {
        JsonNode messages = messageNodes(root);
        List<ImRaw> rows = new ArrayList<>();
        if (!messages.isArray()) {
            return rows;
        }
        for (JsonNode item : messages) {
            String text = textOf(item.path("payload").asText(""), mapper);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            rows.add(new ImRaw(
                    item.path("from_uid").asText(""),
                    item.path("channel_id").asText(""),
                    text,
                    millis(item.path("timestamp").asLong(0)),
                    TEXT_TYPE));
        }
        return rows;
    }

    static List<ImHit> hitsOf(JsonNode root, String loginUid, ObjectMapper mapper) {
        JsonNode conversations = conversationNodes(root);
        List<ImHit> hits = new ArrayList<>();
        if (!conversations.isArray()) {
            return hits;
        }
        for (JsonNode item : conversations) {
            if (item.has("channel_type") && item.path("channel_type").asInt() != 1) {
                continue;
            }
            String peer = peerOf(loginUid, item.path("channel_id").asText(""));
            if (!StringUtils.hasText(peer) || peer.equals(loginUid)) {
                continue;
            }
            JsonNode last = lastMessage(item);
            long time = item.path("timestamp").asLong(0);
            if (time == 0) {
                time = last.path("timestamp").asLong(last.path("server_timestamp_ms").asLong(0));
            }
            hits.add(new ImHit(peer, textOf(last.path("payload").asText(""), mapper), millis(time)));
        }
        return hits;
    }

    private static JsonNode messageNodes(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return MissingNode.getInstance();
        }
        if (root.isArray()) {
            return root;
        }
        if (root.path("messages").isArray()) {
            return root.path("messages");
        }
        JsonNode data = root.path("data");
        if (data.path("messages").isArray()) {
            return data.path("messages");
        }
        if (data.isArray()) {
            return data;
        }
        return MissingNode.getInstance();
    }

    private static JsonNode conversationNodes(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return MissingNode.getInstance();
        }
        if (root.isArray()) {
            return root;
        }
        if (root.path("conversations").isArray()) {
            return root.path("conversations");
        }
        if (root.path("data").isArray()) {
            return root.path("data");
        }
        return MissingNode.getInstance();
    }

    private static JsonNode lastMessage(JsonNode conversation) {
        JsonNode recents = conversation.path("recents");
        if (recents.isArray() && !recents.isEmpty()) {
            return recents.get(0);
        }
        JsonNode last = conversation.path("last_message");
        return last.isObject() ? last : MissingNode.getInstance();
    }

    static String peerOf(String loginUid, String channelId) {
        if (!StringUtils.hasText(channelId)) {
            return "";
        }
        int at = channelId.indexOf('@');
        if (at < 0) {
            return channelId;
        }
        String left = channelId.substring(0, at);
        String right = channelId.substring(at + 1);
        if (loginUid != null && loginUid.equals(left)) {
            return right;
        }
        if (loginUid != null && loginUid.equals(right)) {
            return left;
        }
        return "";
    }

    static String textOf(String payload, ObjectMapper mapper) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            raw = payload.getBytes(StandardCharsets.UTF_8);
        }
        try {
            JsonNode node = mapper.readTree(raw);
            if (node.path("type").asInt() != TEXT_TYPE) {
                return "";
            }
            return node.path("content").asText("");
        } catch (Exception ex) {
            return "";
        }
    }

    static long millis(long timestamp) {
        if (timestamp > 0 && timestamp < MILLIS_BOUND) {
            return timestamp * 1000L;
        }
        return timestamp;
    }

    public record ImRaw(String sendId, String recvId, String text, long sendTime, int contentType) {
    }

    public record ImHit(String userId, String preview, long sendTime) {
    }
}
