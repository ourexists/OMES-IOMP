package com.ourexists.omes.stream.workshop.sink.bridge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ourexists.omes.stream.workshop.model.WorkshopCollectSnapshotEvent;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

/** 场景采集周期快照 → MQ，门户攒批后 {@code WorkshopCollectFeign#addBatch}。 */
public class WorkshopCollectSnapshotBridgeSink extends AbstractWorkshopStreamPersistRmqSink<WorkshopCollectSnapshotEvent> {

    public WorkshopCollectSnapshotBridgeSink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        super(rmqConnectionConfig, queueName);
    }

    @Override
    public void invoke(WorkshopCollectSnapshotEvent event, SinkFunction.Context context) throws Exception {
        if (event == null || event.getCollect() == null || objectMapper == null) {
            return;
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set("collect", objectMapper.valueToTree(event.getCollect()));
        root.put("eventId", newOutboundEventId());
        publish(objectMapper.writeValueAsBytes(root));
    }
}
