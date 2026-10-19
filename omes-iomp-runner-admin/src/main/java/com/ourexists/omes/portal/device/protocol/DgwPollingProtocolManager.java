/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.portal.device.protocol;

import com.alibaba.fastjson2.JSONObject;
import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.exceptions.EraCommonException;
import com.ourexists.era.framework.core.user.UserContext;
import com.ourexists.era.framework.core.utils.RemoteHandleUtils;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtimeManager;
import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
import com.ourexists.omes.device.core.equip.protocol.ProtocolManager;
import com.ourexists.omes.device.feign.GatewayFeign;
import com.ourexists.omes.device.model.GatewayDto;
import com.ourexists.omes.portal.device.collect.DgwEquipDataParser;
import com.ourexists.omes.portal.device.gateway.GatewayRuntimeService;
import com.ourexists.omes.portal.device.protocol.dgw.DgwClient;
import com.ourexists.omes.portal.device.protocol.dgw.DgwPointAddress;
import com.ourexists.omes.portal.mq.EquipRealtimeStreamOutbound;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * DGW 轮询：按采集 Cron 拉取实时点位，并按设备绑定把工程值写入属性、报警与控制。
 * 控制写入走 DGW {@code /device/points/write}，点位编码对应型号映射。
 */
@Slf4j
@Component
public class DgwPollingProtocolManager implements ProtocolManager {

    private static final int DEFAULT_POOL_SIZE = 4;

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final Map<String, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final Map<String, ProtocolConnect> connectSnapshotMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> deviceIdIndex = new ConcurrentHashMap<>();
    private final ConnectLink link = new ConnectLink();

    @Autowired
    private DgwClient dgwClient;

    @Autowired
    private DgwEquipDataParser equipDataParser;

    @Autowired
    private EquipRealtimeManager equipRealtimeManager;

    @Autowired
    private EquipRealtimeStreamOutbound equipRealtimeStreamOutbound;

    @Autowired
    private GatewayFeign gatewayFeign;

    @PostConstruct
    public void init() {
        scheduler.setPoolSize(DEFAULT_POOL_SIZE);
        scheduler.setThreadNamePrefix("dgw-polling-");
        scheduler.setDaemon(true);
        scheduler.initialize();
    }

    @PreDestroy
    public void destroy() {
        stopAll();
        try {
            scheduler.shutdown();
        } catch (Exception ignored) {
        }
    }

    @Override
    public String protocol() {
        return "DGW";
    }

    @Override
    public synchronized boolean start(ProtocolConnect connect) {
        if (connect == null || !StringUtils.hasText(connect.getId())) {
            return false;
        }
        String connectId = connect.getId();
        if (taskMap.containsKey(connectId)) {
            log.debug("DGW polling task already exists, connectId={}", connectId);
            return true;
        }
        if (!StringUtils.hasText(connect.getUri())) {
            log.warn("DGW polling skipped: uri is empty, connectId={}", connectId);
            link.failed(connectId, "连接地址为空");
            return false;
        }
        if (!StringUtils.hasText(connect.getCollectCron())) {
            log.warn("DGW polling skipped: collectCron is empty, connectId={}", connectId);
            link.failed(connectId, "采集 Cron 为空");
            return false;
        }
        if (!StringUtils.hasText(connect.getUsername()) || !StringUtils.hasText(connect.getPassword())) {
            log.warn("DGW polling skipped: username or password is empty, connectId={}", connectId);
            link.failed(connectId, "账号或密码为空");
            return false;
        }
        dgwClient.invalidate(connect);
        Runnable job = () -> {
            UserContext.defaultTenant();
            try {
                poll(connect);
            } catch (Exception e) {
                log.error("DGW polling error, connectId={}, uri={}", connect.getId(), connect.getUri(), e);
            }
        };
        link.pending(connectId);
        ScheduledFuture<?> future;
        try {
            future = scheduler.schedule(job, new CronTrigger(connect.getCollectCron()));
        } catch (IllegalArgumentException e) {
            log.warn("DGW polling skipped: invalid cron {}, connectId={}", connect.getCollectCron(), connectId);
            link.failed(connectId, "采集 Cron 不正确");
            return false;
        }
        taskMap.put(connectId, future);
        connectSnapshotMap.put(connectId, connect);
        try {
            scheduler.execute(job);
        } catch (Exception e) {
            log.debug("DGW first probe skipped, connectId={}", connectId);
        }
        log.info("DGW polling started, connectId={}, cron={}, uri={}", connectId, connect.getCollectCron(), connect.getUri());
        return true;
    }

    @Override
    public synchronized boolean stop(String connectId) {
        if (!StringUtils.hasText(connectId)) {
            return false;
        }
        ScheduledFuture<?> future = taskMap.remove(connectId);
        ProtocolConnect connect = connectSnapshotMap.remove(connectId);
        deviceIdIndex.remove(connectId);
        link.clear(connectId);
        if (connect != null) {
            dgwClient.invalidate(connect);
        }
        if (future != null) {
            future.cancel(true);
            log.info("DGW polling stopped, connectId={}", connectId);
            return true;
        }
        return false;
    }

    @Override
    public String linkState(String connectId) {
        return link.state(connectId);
    }

    @Override
    public String linkMessage(String connectId) {
        return link.message(connectId);
    }

    @Override
    public synchronized void stopAll() {
        for (String key : List.copyOf(taskMap.keySet())) {
            stop(key);
        }
    }

    @Override
    public boolean write(String connectId, String address, Object value) {
        String[] parts = DgwPointAddress.decode(address);
        String deviceKey = parts[0];
        String pointCode = parts[1];
        if (!StringUtils.hasText(pointCode)) {
            log.warn("DGW write skipped: point code empty, connectId={}", connectId);
            return false;
        }
        ProtocolConnect connect = connectOf(connectId);
        if (connect == null) {
            throw new BusinessException("DGW 网关不存在或未配置");
        }
        String deviceId = resolveDeviceId(connectId, connect, deviceKey);
        if (!StringUtils.hasText(deviceId)) {
            throw new BusinessException("未找到 DGW 设备");
        }
        return dgwClient.writePoint(connect, deviceId, pointCode.trim(), value);
    }

    private void poll(ProtocolConnect connect) {
        List<JSONObject> devices;
        try {
            devices = dgwClient.listRealtime(connect);
        } catch (RuntimeException e) {
            link.down(connect.getId(), e.getMessage());
            throw e;
        }
        link.up(connect.getId());
        List<EquipRealtime> equips = equipRealtimeManager.listByGwId(connect.getId());
        if (CollectionUtils.isEmpty(devices) || CollectionUtils.isEmpty(equips)) {
            return;
        }
        for (JSONObject device : devices) {
            String deviceId = device.getString("deviceId");
            String deviceCode = device.getString("deviceCode");
            rememberDevice(connect.getId(), deviceId, deviceCode);
            EquipRealtime equip = match(equips, deviceCode);
            if (equip == null) {
                continue;
            }
            EquipRealtime parsed = equipDataParser.parse(equip, device.getBooleanValue("online"), pointValues(device));
            if (parsed != null) {
                equipRealtimeStreamOutbound.send(parsed);
            }
        }
    }

    private EquipRealtime match(List<EquipRealtime> equips, String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return null;
        }
        for (EquipRealtime equip : equips) {
            if (equip != null && deviceCode.equals(equip.getSelfCode())) {
                return equip;
            }
        }
        return null;
    }

    private static Map<String, Object> pointValues(JSONObject device) {
        Map<String, Object> values = new HashMap<>();
        JSONObject points = device.getJSONObject("points");
        if (points == null) {
            return values;
        }
        for (String code : points.keySet()) {
            JSONObject point = points.getJSONObject(code);
            if (point == null || StringUtils.hasText(point.getString("error"))) {
                continue;
            }
            Object value = point.get("value");
            if (value != null) {
                values.put(code, value);
            }
        }
        return values;
    }

    private void rememberDevice(String connectId, String deviceId, String deviceCode) {
        if (!StringUtils.hasText(deviceId) || !StringUtils.hasText(deviceCode)) {
            return;
        }
        Map<String, String> index = deviceIdIndex.computeIfAbsent(connectId, key -> new ConcurrentHashMap<>());
        index.put(deviceCode, deviceId);
    }

    private String resolveDeviceId(String connectId, ProtocolConnect connect, String deviceKey) {
        if (!StringUtils.hasText(deviceKey)) {
            return null;
        }
        Map<String, String> index = deviceIdIndex.get(connectId);
        if (index != null && StringUtils.hasText(index.get(deviceKey))) {
            return index.get(deviceKey);
        }
        String deviceId = dgwClient.resolveDeviceId(connect, deviceKey);
        if (StringUtils.hasText(deviceId)) {
            rememberDevice(connectId, deviceId, deviceKey);
        }
        return deviceId;
    }

    private ProtocolConnect connectOf(String connectId) {
        ProtocolConnect cached = connectSnapshotMap.get(connectId);
        if (cached != null) {
            return cached;
        }
        try {
            GatewayDto gateway = RemoteHandleUtils.getDataFormResponse(gatewayFeign.selectById(connectId));
            if (gateway == null) {
                return null;
            }
            return GatewayRuntimeService.toConnect(gateway);
        } catch (EraCommonException e) {
            log.warn("Load DGW gateway failed, connectId={}: {}", connectId, e.getMessage());
            return null;
        }
    }
}
