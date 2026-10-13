package com.ourexists.omes.stream.workshop;

import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtime;
import com.ourexists.omes.stream.workshop.model.WorkshopCollectSnapshotEvent;
import com.ourexists.omes.stream.workshop.process.WorkshopCollectSnapshotProcessFunction;
import com.ourexists.omes.stream.workshop.rabbitmq.LoggedWorkshopRealtimeRmqSource;
import com.ourexists.omes.stream.workshop.rabbitmq.WorkshopRealtimeAmqpDeserializationSchema;
import com.ourexists.omes.stream.workshop.sink.bridge.WorkshopCollectSnapshotBridgeSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.core.execution.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public final class WorkshopRealtimeFlinkGraph {

    private WorkshopRealtimeFlinkGraph() {}

    public static StreamExecutionEnvironment createExecutionEnvironment(WorkshopRealtimeFlinkJobConfig cfg) {
        if (!cfg.flinkLocalMode()) {
            Configuration cluster = new Configuration();
            cluster.setString("classloader.resolve-order", "parent-first");
            return StreamExecutionEnvironment.getExecutionEnvironment(cluster);
        }
        Configuration flinkConfig = new Configuration();
        /* Fat-jar 下子 ClassLoader 才有 BOOT-INF/lib；child-first 便于解析 Flink 与用户类。 */
        flinkConfig.setString("classloader.resolve-order", "child-first");
        flinkConfig.set(CoreOptions.ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL, List.of("com.rabbitmq."));
        ClassLoader appLoader = WorkshopRealtimeFlinkGraph.class.getClassLoader();
        if (appLoader instanceof URLClassLoader urlClassLoader) {
            flinkConfig.set(
                    PipelineOptions.CLASSPATHS,
                    Arrays.stream(urlClassLoader.getURLs())
                            .map(u -> u.toExternalForm())
                            .collect(Collectors.toList()));
        }
        int p = cfg.flinkLocalParallelism();
        StreamExecutionEnvironment env;
        if (cfg.flinkLocalWebUI()) {
            env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(flinkConfig);
            env.setParallelism(p);
        } else {
            env = StreamExecutionEnvironment.createLocalEnvironment(p, flinkConfig);
        }
        log.info(
                "Workshop Flink local debug mode: parallelism={} webUI={} (checkpointing follows enable-checkpointing; "
                        + "leave false for typical local runs)",
                p,
                cfg.flinkLocalWebUI());
        return env;
    }

    public static void configureExecutionEnvironment(StreamExecutionEnvironment env, WorkshopRealtimeFlinkJobConfig cfg) {
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(10, Duration.ofSeconds(5)));
        if (cfg.flinkEnableCheckpointing()) {
            env.enableCheckpointing(cfg.flinkCheckpointIntervalMs(), CheckpointingMode.AT_LEAST_ONCE);
            env.getCheckpointConfig().setCheckpointTimeout(cfg.flinkCheckpointTimeoutMs());
            env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500L);
            env.getCheckpointConfig().setTolerableCheckpointFailureNumber(5);
            if (cfg.flinkUnalignedCheckpoint()) {
                env.getCheckpointConfig().enableUnalignedCheckpoints();
                if (cfg.flinkAlignedCheckpointTimeoutMs() > 0L) {
                    env.getCheckpointConfig()
                            .setAlignedCheckpointTimeout(Duration.ofMillis(cfg.flinkAlignedCheckpointTimeoutMs()));
                }
            }
            log.info(
                    "Workshop Flink checkpoint ON: interval={}ms timeout={}ms unaligned={}",
                    cfg.flinkCheckpointIntervalMs(),
                    cfg.flinkCheckpointTimeoutMs(),
                    cfg.flinkUnalignedCheckpoint());
        } else {
            log.warn(
                    "Workshop Flink checkpoint OFF for RMQSource — set omes.workshop.flink.enable-checkpointing=true in cluster.");
        }
    }

    public static void buildAndExecute(StreamExecutionEnvironment env, RMQConnectionConfig rmq, WorkshopRealtimeFlinkJobConfig cfg)
            throws Exception {
        if (StringUtils.isBlank(cfg.workshopRealtimeRabbitQueue())) {
            log.error("omes.workshop.rabbitmq.workshop-realtime-queue is empty; workshop Flink job not started");
            return;
        }
        if (StringUtils.isBlank(cfg.workshopStreamPersistCollectQueue())) {
            log.warn("workshop stream persist collect queue is empty; collect snapshot sink disabled");
        }

        LoggedWorkshopRealtimeRmqSource rmqSource = new LoggedWorkshopRealtimeRmqSource(
                rmq,
                cfg.workshopRealtimeRabbitQueue(),
                false,
                new WorkshopRealtimeAmqpDeserializationSchema());

        DataStream<WorkshopRealtime> source = env.addSource(rmqSource).name("workshop-realtime-rmq-source");
        DataStream<WorkshopRealtime> validStream = source
                .filter(w -> w != null && StringUtils.isNotBlank(w.getId()))
                .name("workshop-realtime-filter");

        DataStream<WorkshopCollectSnapshotEvent> collectSnapshotStream = validStream
                .keyBy(WorkshopRealtime::getId)
                .process(new WorkshopCollectSnapshotProcessFunction(
                        cfg.snapshotIntervalMs(), cfg.stateTtlMinutesCollectSnapshot()))
                .name("workshop-collect-snapshot-timed");

        collectSnapshotStream
                .addSink(new WorkshopCollectSnapshotBridgeSink(rmq, cfg.workshopStreamPersistCollectQueue()))
                .name("workshop-collect-snapshot-persist-bridge-rmq-sink");

        log.info(
                "Submitting Flink job workshop-realtime-job; queue={} persistCollect={}",
                cfg.workshopRealtimeRabbitQueue(),
                cfg.workshopStreamPersistCollectQueue());
        env.execute("workshop-realtime-job");
    }
}
