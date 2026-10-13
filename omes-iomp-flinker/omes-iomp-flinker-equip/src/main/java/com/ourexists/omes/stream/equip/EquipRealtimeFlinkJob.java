package com.ourexists.omes.stream.equip;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.Map;

/**
 * Flink 实时管道。{@link #main} 供 {@code flink run} / Web UI；Spring Boot 入口见 {@link EquipFlinkBootApplication}。
 * <p>
 * Configuration: Flink {@code ParameterTool} merges Spring {@code application.yml}（最低优先级）、program args、{@code -D}
 * 与 environment（见 {@code README.md}）。
 * <p>
 * messaging 仍为 Flink 连接器 + RabbitMQ Java 客户端；与门户侧通过共用队列名、vhost 等对齐。
 */
@Slf4j
public final class EquipRealtimeFlinkJob {

    /**
     * Merged keys (lowest precedence first): {@code springDefaults} &lt; program args &lt; JVM system properties &lt;
     * OS environment variables.
     */
    public static ParameterTool parameterTool(String[] args, Map<String, String> springDefaults) {
        Map<String, String> base = springDefaults != null ? springDefaults : Map.of();
        return ParameterTool.fromMap(base)
                .mergeWith(ParameterTool.fromArgs(args))
                .mergeWith(ParameterTool.fromSystemProperties())
                .mergeWith(ParameterTool.fromMap(System.getenv()));
    }

    public static void main(String[] args) throws Exception {
        run(parameterTool(args, Map.of()));
    }

    public static void run(ParameterTool pt) throws Exception {
        Thread.currentThread().setContextClassLoader(EquipRealtimeFlinkJob.class.getClassLoader());
        EquipRealtimeFlinkJobConfig cfg = EquipRealtimeFlinkJobProperties.from(pt);
        RMQConnectionConfig rmq = EquipRealtimeFlinkRmqConfig.from(pt, cfg.flinkRmqPrefetch());
        StreamExecutionEnvironment env = EquipRealtimeFlinkGraph.createExecutionEnvironment(cfg);
        EquipRealtimeFlinkGraph.configureExecutionEnvironment(env, cfg);
        EquipRealtimeFlinkGraph.buildAndExecute(env, rmq, cfg);
    }

    private EquipRealtimeFlinkJob() {
    }
}
