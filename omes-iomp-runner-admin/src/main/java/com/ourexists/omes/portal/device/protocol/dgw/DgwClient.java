package com.ourexists.omes.portal.device.protocol.dgw;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用 DGW HTTP 接口：登录、实时快照、设备/点位目录、控制写入。
 */
@Slf4j
@Component
public class DgwClient {

    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES = 20;

    private final RestTemplate restTemplate;
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();
    private final Object tokenLock = new Object();

    public DgwClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restTemplate = new RestTemplate(factory);
    }

    public void invalidate(ProtocolConnect connect) {
        if (connect == null) {
            return;
        }
        tokens.remove(tokenKey(connect));
    }

    public List<JSONObject> listRealtime(ProtocolConnect connect) {
        JSONObject root = parseOk(exchange(connect, HttpMethod.GET, "/device/realtime/list", null, true));
        JSONArray data = root.getJSONArray("data");
        List<JSONObject> list = new ArrayList<>();
        if (data == null) {
            return list;
        }
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    public List<DgwDeviceOption> listDevices(ProtocolConnect connect) {
        List<DgwDeviceOption> all = new ArrayList<>();
        long total = Long.MAX_VALUE;
        for (int page = 1; page <= MAX_PAGES && all.size() < total; page++) {
            Map<String, Object> body = new HashMap<>();
            body.put("page", page);
            body.put("size", PAGE_SIZE);
            JSONObject root = parseOk(exchange(connect, HttpMethod.POST, "/device/devices/page", body, true));
            JSONObject data = root.getJSONObject("data");
            if (data == null) {
                break;
            }
            total = data.getLongValue("total");
            JSONArray records = data.getJSONArray("records");
            if (records == null || records.isEmpty()) {
                break;
            }
            for (int i = 0; i < records.size(); i++) {
                JSONObject item = records.getJSONObject(i);
                if (item == null || !StringUtils.hasText(item.getString("id"))) {
                    continue;
                }
                all.add(new DgwDeviceOption()
                        .setId(item.getString("id"))
                        .setCode(item.getString("code"))
                        .setName(item.getString("name"))
                        .setProtocol(item.getString("protocol"))
                        .setEnabled(item.getBoolean("enabled")));
            }
            if (records.size() < PAGE_SIZE) {
                break;
            }
        }
        return all;
    }

    public List<DgwPointOption> listPoints(ProtocolConnect connect, String deviceId) {
        String path = UriComponentsBuilder.fromPath("/device/points/list")
                .queryParam("deviceId", deviceId)
                .build()
                .encode()
                .toUriString();
        JSONObject root = parseOk(exchange(connect, HttpMethod.GET, path, null, true));
        JSONArray data = root.getJSONArray("data");
        List<DgwPointOption> list = new ArrayList<>();
        if (data == null) {
            return list;
        }
        for (int i = 0; i < data.size(); i++) {
            JSONObject item = data.getJSONObject(i);
            if (item == null || !StringUtils.hasText(item.getString("code"))) {
                continue;
            }
            String access = item.getString("accessType");
            boolean readable = "READ_ONLY".equals(access) || "READ_WRITE".equals(access);
            boolean writable = "WRITE_ONLY".equals(access) || "READ_WRITE".equals(access);
            list.add(new DgwPointOption()
                    .setCode(item.getString("code"))
                    .setName(item.getString("name"))
                    .setAccessType(access)
                    .setUnit(item.getString("engUnit"))
                    .setReadable(readable)
                    .setWritable(writable));
        }
        return list;
    }

    public String resolveDeviceId(ProtocolConnect connect, String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return null;
        }
        return deviceIdOf(detail(connect, "deviceCode", deviceCode));
    }

    public boolean writePoint(ProtocolConnect connect, String deviceId, String pointCode, Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", "omes-" + connect.getId() + "-" + System.currentTimeMillis());
        body.put("deviceId", deviceId);
        body.put("code", pointCode);
        body.put("value", coerceValue(value));
        parseOk(exchange(connect, HttpMethod.POST, "/device/points/write", body, true));
        return true;
    }

    private JSONObject detail(ProtocolConnect connect, String param, String value) {
        String path = UriComponentsBuilder.fromPath("/device/realtime/detail")
                .queryParam(param, value)
                .build()
                .encode()
                .toUriString();
        try {
            JSONObject root = parseOk(exchange(connect, HttpMethod.GET, path, null, true));
            return root.getJSONObject("data");
        } catch (BusinessException e) {
            log.debug("DGW detail lookup failed, {}={}: {}", param, value, e.getMessage());
            return null;
        }
    }

    private static String deviceIdOf(JSONObject detail) {
        if (detail == null) {
            return null;
        }
        return detail.getString("deviceId");
    }

    private String exchange(ProtocolConnect connect, HttpMethod method, String path, Object body, boolean retryAuth) {
        requireConnect(connect);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (!"/auth/login".equals(path)) {
            headers.setBearerAuth(ensureToken(connect));
        }
        HttpEntity<?> entity = body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        String url = join(connect.getUri(), path);
        try {
            return restTemplate.exchange(url, method, entity, String.class).getBody();
        } catch (HttpStatusCodeException e) {
            if (retryAuth && e.getStatusCode().value() == 401 && !"/auth/login".equals(path)) {
                tokens.remove(tokenKey(connect));
                return exchange(connect, method, path, body, false);
            }
            throw new BusinessException(extractMessage(e));
        } catch (RestClientException e) {
            throw new BusinessException("连接 DGW 失败: " + e.getMessage());
        }
    }

    private String ensureToken(ProtocolConnect connect) {
        String key = tokenKey(connect);
        Token token = tokens.get(key);
        long now = System.currentTimeMillis();
        if (token != null && token.expireAtMs > now + 30_000) {
            return token.value;
        }
        synchronized (tokenLock) {
            token = tokens.get(key);
            if (token != null && token.expireAtMs > now + 30_000) {
                return token.value;
            }
            return login(connect);
        }
    }

    private String login(ProtocolConnect connect) {
        if (!StringUtils.hasText(connect.getUsername()) || !StringUtils.hasText(connect.getPassword())) {
            throw new BusinessException("请配置 DGW 账号和密码");
        }
        Map<String, String> body = new HashMap<>();
        body.put("username", connect.getUsername());
        body.put("password", connect.getPassword());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String url = join(connect.getUri(), "/auth/login");
        String raw;
        try {
            raw = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new BusinessException(extractMessage(e));
        } catch (RestClientException e) {
            throw new BusinessException("DGW 登录失败: " + e.getMessage());
        }
        JSONObject root = parseOk(raw);
        JSONObject data = root.getJSONObject("data");
        if (data == null || !StringUtils.hasText(data.getString("token"))) {
            throw new BusinessException("DGW 登录未返回令牌");
        }
        long expiresInMs = data.getLongValue("expiresInMs");
        if (expiresInMs <= 0) {
            expiresInMs = 30 * 60 * 1000L;
        }
        Token token = new Token(data.getString("token"), System.currentTimeMillis() + expiresInMs);
        tokens.put(tokenKey(connect), token);
        return token.value;
    }

    private static JSONObject parseOk(String body) {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException("DGW 响应为空");
        }
        JSONObject root;
        try {
            root = JSON.parseObject(body);
        } catch (Exception e) {
            throw new BusinessException("DGW 响应无法解析");
        }
        if (root == null) {
            throw new BusinessException("DGW 响应无法解析");
        }
        if (root.getIntValue("code") != 0) {
            String message = root.getString("message");
            throw new BusinessException(StringUtils.hasText(message) ? message : "DGW 请求失败");
        }
        return root;
    }

    private static String extractMessage(HttpStatusCodeException e) {
        String body = e.getResponseBodyAsString();
        if (StringUtils.hasText(body)) {
            try {
                JSONObject root = JSON.parseObject(body);
                if (root != null && StringUtils.hasText(root.getString("message"))) {
                    return root.getString("message");
                }
            } catch (Exception ignored) {
            }
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return "DGW 认证失败，请检查账号密码及写入权限";
        }
        return "DGW 请求失败: " + e.getStatusCode().value();
    }

    private static void requireConnect(ProtocolConnect connect) {
        if (connect == null || !StringUtils.hasText(connect.getUri())) {
            throw new BusinessException("DGW 连接地址为空");
        }
    }

    private static String tokenKey(ProtocolConnect connect) {
        return connect.getUri() + "|" + (connect.getUsername() == null ? "" : connect.getUsername());
    }

    private static String join(String base, String path) {
        String normalized = base.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return normalized + path;
    }

    static Object coerceValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        String text = value.toString().trim();
        if (!StringUtils.hasText(text)) {
            return text;
        }
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.parseBoolean(text);
        }
        try {
            if (text.indexOf('.') >= 0) {
                return new BigDecimal(text);
            }
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text;
        }
    }

    private static final class Token {
        private final String value;
        private final long expireAtMs;

        private Token(String value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }
}
