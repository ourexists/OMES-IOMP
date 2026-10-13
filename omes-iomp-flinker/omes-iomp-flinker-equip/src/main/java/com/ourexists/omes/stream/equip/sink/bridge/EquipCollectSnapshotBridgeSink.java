package com.ourexists.omes.stream.equip.sink.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourexists.omes.stream.equip.model.EquipCollectSnapshotEvent;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

/** 采集周期快照 → MQ，门户 {@code EquipCollectFeign#save}。 */
public class EquipCollectSnapshotBridgeSink extends AbstractEquipStreamPersistRmqSink<EquipCollectSnapshotEvent> {

    public EquipCollectSnapshotBridgeSink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        super(rmqConnectionConfig, queueName);
    }

    @Override
    public void invoke(EquipCollectSnapshotEvent event, SinkFunction.Context context) throws Exception {
        if (event == null || event.getCollect() == null || objectMapper == null) {
            return;
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set("collect", objectMapper.valueToTree(event.getCollect()));
        root.put("eventId", newOutboundEventId());
        publish(objectMapper.writeValueAsBytes(root));
    }
}
