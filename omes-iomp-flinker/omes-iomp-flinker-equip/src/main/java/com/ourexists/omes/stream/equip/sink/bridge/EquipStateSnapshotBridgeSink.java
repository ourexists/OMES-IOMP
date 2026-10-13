package com.ourexists.omes.stream.equip.sink.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourexists.omes.stream.equip.model.EquipStateSnapshotEvent;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

/** 状态周期快照 → MQ，门户 {@code EquipStateSnapshotFeign#add}。 */
public class EquipStateSnapshotBridgeSink extends AbstractEquipStreamPersistRmqSink<EquipStateSnapshotEvent> {

    public EquipStateSnapshotBridgeSink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        super(rmqConnectionConfig, queueName);
    }

    @Override
    public void invoke(EquipStateSnapshotEvent event, SinkFunction.Context context) throws Exception {
        if (event == null || event.getSnapshot() == null || objectMapper == null) {
            return;
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set("snapshot", objectMapper.valueToTree(event.getSnapshot()));
        root.put("eventId", newOutboundEventId());
        publish(objectMapper.writeValueAsBytes(root));
    }
}
