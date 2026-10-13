package com.ourexists.omes.stream.workshop;

import org.apache.flink.api.java.utils.ParameterTool;

final class WorkshopRealtimeFlinkJobProperties {

    private WorkshopRealtimeFlinkJobProperties() {}

    static WorkshopRealtimeFlinkJobConfig from(ParameterTool pt) {
        boolean flinkLocal = localMode(pt);
        return new WorkshopRealtimeFlinkJobConfig(
                str(pt, "omes.workshop.rabbitmq.workshop-realtime-queue", "OMES_WORKSHOP_REALTIME_QUEUE", "omes.workshop.realtime"),
                str(
                        pt,
                        "omes.workshop.rabbitmq.workshop-stream-persist-collect-queue",
                        "OMES_WORKSHOP_STREAM_PERSIST_COLLECT_QUEUE",
                        "omes.workshop.stream.persist.collect"),
                pt.getLong(
                        "omes.workshop.flink.checkpoint-interval-ms",
                        pt.getLong("OMES_FLINK_CHECKPOINT_MS", 10_000L)),
                pt.getBoolean(
                        "omes.workshop.flink.enable-checkpointing",
                        pt.getBoolean("OMES_FLINK_ENABLE_CHECKPOINTING", false)),
                pt.getLong(
                        "omes.workshop.flink.checkpoint-timeout-ms",
                        pt.getLong("OMES_FLINK_CHECKPOINT_TIMEOUT_MS", 120_000L)),
                pt.getBoolean(
                        "omes.workshop.flink.unaligned-checkpoint",
                        pt.getBoolean("OMES_FLINK_UNALIGNED_CHECKPOINT", true)),
                pt.getLong(
                        "omes.workshop.flink.aligned-checkpoint-timeout-ms",
                        pt.getLong("OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS", 30_000L)),
                pt.getInt("omes.workshop.flink.rmq-prefetch", pt.getInt("OMES_FLINK_RMQ_PREFETCH", 100)),
                pt.getLong("omes.workshop.snapshot-interval-ms", pt.getLong("OMES_WORKSHOP_SNAPSHOT_INTERVAL_MS", 30_000L)),
                ttlMin(
                        pt,
                        "omes.workshop.flink.state-ttl-minutes.collect-snapshot",
                        "OMES_FLINK_STATE_TTL_MINUTES_WORKSHOP_COLLECT_SNAPSHOT",
                        -1L),
                flinkLocal,
                localParallelism(pt, flinkLocal),
                pt.getBoolean(
                        "omes.workshop.flink.local.webui",
                        pt.getBoolean("OMES_FLINK_LOCAL_WEBUI", true)));
    }

    private static boolean localMode(ParameterTool pt) {
        return pt.getBoolean(
                "omes.workshop.flink.local",
                pt.getBoolean("OMES_FLINK_LOCAL", pt.getBoolean("local", false)));
    }

    private static int localParallelism(ParameterTool pt, boolean flinkLocal) {
        int p = pt.getInt(
                "omes.workshop.flink.local.parallelism",
                pt.getInt("OMES_FLINK_LOCAL_PARALLELISM", 1));
        if (flinkLocal && p <= 0) {
            throw new IllegalArgumentException(
                    "local parallelism must be positive when local mode is on, was: "
                            + p
                            + " (omes.workshop.flink.local.parallelism / OMES_FLINK_LOCAL_PARALLELISM)");
        }
        return p;
    }

    private static long ttlMin(ParameterTool pt, String dottedKey, String envKey, long defaultMinutes) {
        long v;
        if (pt.has(dottedKey)) {
            v = pt.getLong(dottedKey, defaultMinutes);
        } else if (pt.has(envKey)) {
            v = pt.getLong(envKey, defaultMinutes);
        } else {
            v = defaultMinutes;
        }
        if (v != -1L && v <= 0L) {
            throw new IllegalArgumentException(
                    "state TTL (minutes) must be -1 (disable) or positive, key " + dottedKey + " was: " + v);
        }
        return v;
    }

    private static String str(ParameterTool pt, String dottedKey, String envKey, String defaultVal) {
        if (pt.has(dottedKey)) {
            return pt.get(dottedKey);
        }
        if (pt.has(envKey)) {
            return pt.get(envKey);
        }
        return defaultVal;
    }
}
