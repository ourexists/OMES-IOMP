package com.ourexists.omes.portal.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtimeManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * 设备实时数据经 {@link StreamBridge} 发往 Spring Cloud Stream 出站 binding（Rabbit binder 等由配置决定）。
 * <p>
 * 同步写入门户 {@link EquipRealtimeManager}（Redis，主缓存不过期）：Flink 变更持久化仅在「检测到有变更」时才刷新缓存，
 * 因此每次出站即刷新实时态，保证界面看到最新上报时间与点位值。
 */
@Slf4j
@Component
public class EquipRealtimeStreamOutbound {

    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final EquipRealtimeManager equipRealtimeManager;

    public static final String EQUIP_REALTIME_OUT_BINDING = "equipRealtime-out-0";

    public EquipRealtimeStreamOutbound(
            StreamBridge streamBridge,
            ObjectMapper objectMapper,
            EquipRealtimeManager equipRealtimeManager) {
        this.streamBridge = streamBridge;
        this.objectMapper = objectMapper;
        this.equipRealtimeManager = equipRealtimeManager;
    }

    public void send(EquipRealtime realtime) {
        if (realtime == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(realtime);
            streamBridge.send(EQUIP_REALTIME_OUT_BINDING, json);
        } catch (JsonProcessingException e) {
            log.error("Serialize equip realtime for stream outbound failed, equip={}", realtime.getSelfCode(), e);
            throw new IllegalStateException("equip realtime JSON serialization failed", e);
        }
        try {
            equipRealtimeManager.addOrUpdate(realtime);
        } catch (Exception e) {
            log.warn(
                    "Equip realtime Redis refresh after stream send failed (telemetry still in MQ), selfCode={}",
                    realtime.getSelfCode(),
                    e);
        }
    }
}
