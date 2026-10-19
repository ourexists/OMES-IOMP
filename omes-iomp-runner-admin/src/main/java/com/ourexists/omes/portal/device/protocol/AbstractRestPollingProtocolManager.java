/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.portal.device.protocol;

import com.alibaba.fastjson2.JSONObject;
import com.ourexists.era.framework.core.user.UserContext;
import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
import com.ourexists.omes.device.core.equip.protocol.ProtocolManager;
import com.ourexists.omes.device.enums.ConnectValidTypeEnum;
import com.ourexists.omes.portal.device.collect.JSONEquipDataParser;
import com.ourexists.omes.portal.mq.EquipRealtimeStreamOutbound;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.IdleConnectionEvictor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * REST 定时轮询协议管理器：
 * - start：为指定 connect 建立 cron 定时任务
 * - stop：移除并取消该 connect 的定时任务
 *
 * 注意：该类内部单独维护 scheduler/taskMap，不依赖全局任务管理器。
 * HTTP 采用连接池复用：基于 Apache HttpClient5 PoolingHttpClientConnectionManager，多请求复用同一 host 的 TCP 连接。
 * 连接超时/断开重建：setValidateAfterInactivity 复用前校验空闲连接；IdleConnectionEvictor 定期回收过期/空闲连接。
 */
@Slf4j
public abstract class AbstractRestPollingProtocolManager implements ProtocolManager {

    private static final int DEFAULT_POOL_SIZE = 4;
    private static final int CONNECT_TIMEOUT_SEC = 10;
    /** 连接空闲超过此时间后，复用前需校验有效性（服务端可能已关闭） */
    private static final int VALIDATE_AFTER_INACTIVITY_SEC = 2;
    /** 空闲连接回收：检查间隔与最大空闲时间 */
    private static final int IDLE_EVICT_SLEEP_SEC = 10;
    private static final int IDLE_EVICT_MAX_IDLE_SEC = 30;

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final Map<String, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final Map<String, ProtocolConnect> connectSnapshotMap = new ConcurrentHashMap<>();
    private final ConnectLink link = new ConnectLink();

    private RestTemplate restTemplate;
    private CloseableHttpClient httpClient;
    private IdleConnectionEvictor idleConnectionEvictor;

    @Autowired
    protected EquipRealtimeStreamOutbound equipRealtimeStreamOutbound;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        scheduler.setPoolSize(DEFAULT_POOL_SIZE);
        scheduler.setThreadNamePrefix("rest-polling-");
        scheduler.setDaemon(true);
        scheduler.initialize();

        // 信任所有证书（HTTPS 自签名等可用；对 HTTP 无影响）
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (chain, authType) -> true)
                .build();
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        connectionManager.setValidateAfterInactivity(TimeValue.ofSeconds(VALIDATE_AFTER_INACTIVITY_SEC));
        idleConnectionEvictor = new IdleConnectionEvictor(
                connectionManager,
                TimeValue.ofSeconds(IDLE_EVICT_SLEEP_SEC),
                TimeValue.ofSeconds(IDLE_EVICT_MAX_IDLE_SEC));
        idleConnectionEvictor.start();

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SEC));
        restTemplate = new RestTemplate(factory);
    }

    @PreDestroy
    public void destroy() {
        stopAll();
        try {
            scheduler.shutdown();
        } catch (Exception ignored) {
        }
        if (idleConnectionEvictor != null && idleConnectionEvictor.isRunning()) {
            idleConnectionEvictor.shutdown();
        }
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                log.debug("REST polling HttpClient close error: {}", e.getMessage());
            }
        }
    }

    @Override
    public synchronized boolean start(ProtocolConnect connect) {
        if (connect == null || !StringUtils.hasText(connect.getId())) {
            return false;
        }
        String connectId = connect.getId();
        if (taskMap.containsKey(connectId)) {
            log.debug("REST polling task already exists, connectId={}", connectId);
            return true;
        }
        if (!StringUtils.hasText(connect.getUri())) {
            log.warn("REST polling skipped: uri is empty, connectId={}", connectId);
            link.failed(connectId, "连接地址为空");
            return false;
        }
        if (!StringUtils.hasText(connect.getCollectCron())) {
            log.warn("REST polling skipped: collectCron is empty, connectId={}", connectId);
            link.failed(connectId, "采集 Cron 为空");
            return false;
        }

        Runnable job = () -> {
            UserContext.defaultTenant();
            String payload;
            try {
                payload = doRequest(connect);
                link.up(connectId);
            } catch (Exception e) {
                link.down(connectId, e.getMessage());
                log.error("REST polling error, connectId={}, uri={}", connect.getId(), connect.getUri(), e);
                return;
            }
            try {
                if (StringUtils.hasText(payload)) {
                    respHandle(connect, payload);
                }
            } catch (Exception e) {
                log.error("REST polling parse error, connectId={}, uri={}", connect.getId(), connect.getUri(), e);
            }
        };

        link.pending(connectId);
        ScheduledFuture<?> future;
        try {
            future = scheduler.schedule(job, new CronTrigger(connect.getCollectCron()));
        } catch (IllegalArgumentException e) {
            link.failed(connectId, "采集 Cron 不正确");
            return false;
        }
        taskMap.put(connectId, future);
        connectSnapshotMap.put(connectId, connect);
        try {
            scheduler.execute(job);
        } catch (Exception e) {
            log.debug("REST first probe skipped, connectId={}", connectId);
        }
        log.info("REST polling started, connectId={}, cron={}, uri={}", connectId, connect.getCollectCron(), connect.getUri());
        return true;
    }

    protected abstract Object requestBody(ProtocolConnect connect);

    protected abstract void respHandle(ProtocolConnect connect, String payload);

    protected abstract String path();

    @Override
    public synchronized boolean stop(String connectId) {
        if (!StringUtils.hasText(connectId)) {
            return false;
        }
        ScheduledFuture<?> future = taskMap.remove(connectId);
        connectSnapshotMap.remove(connectId);
        link.clear(connectId);
        if (future != null) {
            future.cancel(true);
            log.info("REST polling stopped, connectId={}", connectId);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void stopAll() {
        List<String> keys = new ArrayList<>(taskMap.keySet());
        for (String key : keys) {
            stop(key);
        }
    }

    private String doRequest(ProtocolConnect connect) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (ConnectValidTypeEnum.BASIC.getCode().equals(connect.getValidType())
                && StringUtils.hasText(connect.getUsername())) {
            String auth = connect.getUsername() + ":" + (connect.getPassword() != null ? connect.getPassword() : "");
            byte[] encoded = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + new String(encoded));
        }

        if (StringUtils.hasText(connect.getParams())) {
            try {
                JSONObject jo = JSONObject.parseObject(connect.getParams());
                if (jo != null) {
                    JSONObject h = jo.getJSONObject("headers");
                    Map<String, String> headerMap = null;
                    if (h != null && !h.isEmpty()) {
                        headerMap = h.toJavaObject(Map.class);
                    }
                    if (headerMap != null && !headerMap.isEmpty()) {
                        for (Map.Entry<String, String> e : headerMap.entrySet()) {
                            headers.set(e.getKey(), e.getValue());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Object body = requestBody(connect);
        HttpEntity<?> entity = (body == null) ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
        String url = StringUtils.hasText(path()) ? connect.getUri() + "/" + path() : connect.getUri();
        return restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
    }

    @Override
    public String linkState(String connectId) {
        return link.state(connectId);
    }

    @Override
    public String linkMessage(String connectId) {
        return link.message(connectId);
    }
}

