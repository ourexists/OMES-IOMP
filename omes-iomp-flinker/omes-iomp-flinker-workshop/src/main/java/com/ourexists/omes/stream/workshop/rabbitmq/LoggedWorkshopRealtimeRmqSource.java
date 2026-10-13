package com.ourexists.omes.stream.workshop.rabbitmq;

import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtime;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.rabbitmq.RMQSource;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

@Slf4j
public class LoggedWorkshopRealtimeRmqSource extends RMQSource<WorkshopRealtime> {

    public LoggedWorkshopRealtimeRmqSource(
            RMQConnectionConfig rmqConnectionConfig,
            String queueName,
            boolean usesCorrelationId,
            DeserializationSchema<WorkshopRealtime> deserializationSchema) {
        super(rmqConnectionConfig, queueName, usesCorrelationId, deserializationSchema);
    }

    @Override
    public void open(Configuration config) throws Exception {
        log.info("Workshop RMQSource opening...");
        try {
            super.open(config);
            log.info("Workshop RMQSource opened.");
        } catch (Exception e) {
            log.error("Workshop RMQSource open failed.", e);
            throw e;
        }
    }

    @Override
    public void run(SourceFunction.SourceContext<WorkshopRealtime> ctx) throws Exception {
        log.info("Workshop RMQSource run started.");
        try {
            super.run(ctx);
            log.warn("Workshop RMQSource run exited.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Workshop RMQSource run interrupted.");
            throw e;
        } catch (org.apache.flink.runtime.execution.CancelTaskException e) {
            log.info("Workshop RMQSource run cancelled.");
            throw e;
        } catch (Exception e) {
            log.error("Workshop RMQSource run failed.", e);
            throw e;
        }
    }

    @Override
    public void cancel() {
        log.warn("Workshop RMQSource cancel called.");
        super.cancel();
    }

    @Override
    public void close() throws Exception {
        log.warn("Workshop RMQSource closing.");
        super.close();
    }
}
