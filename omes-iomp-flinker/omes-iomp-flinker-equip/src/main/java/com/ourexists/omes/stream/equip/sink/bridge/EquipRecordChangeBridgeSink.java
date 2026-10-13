package com.ourexists.omes.stream.equip.sink.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.model.EquipRecordAlarmDto;
import com.ourexists.omes.device.model.EquipRecordOnlineDto;
import com.ourexists.omes.device.model.EquipRecordRunDto;
import com.ourexists.omes.stream.equip.model.EquipRealtimeChangeEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.Date;
import java.util.stream.Collectors;

/**
 * 设备运行/报警/在线记录变更 → MQ，由门户调 {@code EquipRecord*Feign#add} 入库。
 */
public class EquipRecordChangeBridgeSink extends AbstractEquipStreamPersistRmqSink<EquipRealtimeChangeEvent> {

    public EquipRecordChangeBridgeSink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        super(rmqConnectionConfig, queueName);
    }

    @Override
    public void invoke(EquipRealtimeChangeEvent event, SinkFunction.Context context) throws Exception {
        if (event == null || objectMapper == null) {
            return;
        }
        EquipRealtime target = event.getTarget();
        if (target == null) {
            return;
        }
        if (!event.isAlarmChanged() && !event.isRunChanged() && !event.isOnlineChanged()) {
            return;
        }
        ObjectNode root = objectMapper.createObjectNode();
        if (event.isAlarmChanged()) {
            String reason = CollectionUtils.isEmpty(target.getAlarmTexts())
                    ? null
                    : target.getAlarmTexts().stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(","));
            target.setAlarmChangeTime(target.getTime());
            EquipRecordAlarmDto alarm = new EquipRecordAlarmDto()
                    .setSn(target.getSelfCode())
                    .setState(target.getAlarmState())
                    .setStartTime(target.getTime())
                    .setTenantId(target.getTenantId())
                    .setReason(reason)
                    .setLevel(target.getAlarmLevel())
                    .setEventId(event.getAlarmSegmentEventId())
                    .setPrevEventId(event.getAlarmPrevSegmentEventId());
            root.set("alarm", objectMapper.valueToTree(alarm));
        }
        if (event.isRunChanged()) {
            target.setRunChangeTime(target.getTime());
            EquipRecordRunDto run = new EquipRecordRunDto()
                    .setSn(target.getSelfCode())
                    .setState(target.getRunState())
                    .setStartTime(target.getTime())
                    .setTenantId(target.getTenantId())
                    .setEventId(event.getRunSegmentEventId())
                    .setPrevEventId(event.getRunPrevSegmentEventId());
            root.set("run", objectMapper.valueToTree(run));
        }
        if (event.isOnlineChanged()) {
            target.setOnlineChangeTime(new Date());
            EquipRecordOnlineDto online = new EquipRecordOnlineDto()
                    .setSn(target.getSelfCode())
                    .setState(target.getOnlineState())
                    .setStartTime(target.getOnlineChangeTime())
                    .setTenantId(target.getTenantId())
                    .setEventId(event.getOnlineSegmentEventId())
                    .setPrevEventId(event.getOnlinePrevSegmentEventId());
            root.set("online", objectMapper.valueToTree(online));
        }
        // 供门户 equipStreamPersistChange 队列经 Aggregator 写 Redis（与门户 DEquipRealtimeManager 一致）
        root.set("realtime", objectMapper.valueToTree(target));
        root.put("eventId", newOutboundEventId());
        publish(objectMapper.writeValueAsBytes(root));
    }
}
