package com.ourexists.omes.stream.workshop;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.concurrent.TimeUnit;

final class WorkshopRealtimeFlinkRmqConfig {

    private WorkshopRealtimeFlinkRmqConfig() {}

    static RMQConnectionConfig from(ParameterTool pt, int prefetch) {
        com.rabbitmq.client.ConnectionFactory rcf = new com.rabbitmq.client.ConnectionFactory();
        rcf.setHost(pt.get("RABBITMQ_HOST", "127.0.0.1"));
        rcf.setPort(pt.getInt("RABBITMQ_PORT", 5672));
        rcf.setUsername(pt.get("RABBITMQ_USERNAME", "admin"));
        rcf.setPassword(pt.get("RABBITMQ_PASSWORD", "TyY6Df3bZe"));
        String vhost = pt.get("RABBITMQ_VHOST", "/");
        try {
            rcf.setVirtualHost(vhost);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RABBITMQ_VHOST: " + vhost, e);
        }
        return fromRabbitClient(rcf, prefetch);
    }

    private static RMQConnectionConfig fromRabbitClient(com.rabbitmq.client.ConnectionFactory rcf, int prefetch) {
        String vhost = rcf.getVirtualHost();
        return new RMQConnectionConfig.Builder()
                .setHost(rcf.getHost())
                .setPort(rcf.getPort())
                .setVirtualHost(vhost != null ? vhost : "/")
                .setUserName(rcf.getUsername())
                .setPassword(rcf.getPassword())
                .setAutomaticRecovery(true)
                .setTopologyRecoveryEnabled(true)
                .setNetworkRecoveryInterval((int) TimeUnit.SECONDS.toMillis(5))
                .setPrefetchCount(prefetch)
                .build();
    }
}
