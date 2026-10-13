package com.ourexists.omes.stream.equip.rabbitmq;

import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

@Slf4j
public class LoggedEquipRealtimeRmqSource extends RMQSource<EquipRealtime> {

    public LoggedEquipRealtimeRmqSource(
            RMQConnectionConfig rmqConnectionConfig,
            String queueName,
            boolean usesCorrelationId,
            DeserializationSchema<EquipRealtime> deserializationSchema) {
        super(rmqConnectionConfig, queueName, usesCorrelationId, deserializationSchema);
    }

    @Override
    public void open(Configuration config) throws Exception {
        log.info("RMQSource opening...");
        try {
            super.open(config);
            log.info("RMQSource opened.");
        } catch (Exception e) {
            log.error("RMQSource open failed.", e);
            throw e;
        }
    }

    @Override
    public void run(SourceFunction.SourceContext<EquipRealtime> ctx) throws Exception {
        log.info("RMQSource run started.");
        try {
            super.run(ctx);
            log.warn("RMQSource run exited.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("RMQSource run interrupted (usually cancel/restart path).");
            throw e;
        } catch (org.apache.flink.runtime.execution.CancelTaskException e) {
            log.info("RMQSource run cancelled by Flink task failover (buffer pool destroyed).");
            throw e;
        } catch (Exception e) {
            log.error("RMQSource run failed.", e);
            throw e;
        }
    }

    @Override
    public void cancel() {
        log.warn("RMQSource cancel called.");
        super.cancel();
    }

    @Override
    public void close() throws Exception {
        log.warn("RMQSource closing.");
        super.close();
    }
}
