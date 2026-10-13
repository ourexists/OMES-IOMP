package com.ourexists.omes.stream.workshop;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.Map;

/**
 * 场景实时流：消费 {@code omes.workshop.realtime}，按周期输出采集快照至桥接队列。
 * <p>
 * {@link #main} 供 {@code flink run} / Web UI；Spring Boot 入口见 {@link WorkshopFlinkBootApplication}。
 * Configuration: Spring {@code application.yml}（最低）&lt; program args &lt; {@code -D} &lt; environment。
 */
@Slf4j
public final class WorkshopRealtimeFlinkJob {

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
        Thread.currentThread().setContextClassLoader(WorkshopRealtimeFlinkJob.class.getClassLoader());
        WorkshopRealtimeFlinkJobConfig cfg = WorkshopRealtimeFlinkJobProperties.from(pt);
        RMQConnectionConfig rmq = WorkshopRealtimeFlinkRmqConfig.from(pt, cfg.flinkRmqPrefetch());
        StreamExecutionEnvironment env = WorkshopRealtimeFlinkGraph.createExecutionEnvironment(cfg);
        WorkshopRealtimeFlinkGraph.configureExecutionEnvironment(env, cfg);
        WorkshopRealtimeFlinkGraph.buildAndExecute(env, rmq, cfg);
    }

    private WorkshopRealtimeFlinkJob() {}
}
