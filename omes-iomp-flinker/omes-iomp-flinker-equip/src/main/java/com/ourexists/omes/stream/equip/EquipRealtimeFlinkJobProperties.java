package com.ourexists.omes.stream.equip;

import org.apache.flink.api.java.utils.ParameterTool;

final class EquipRealtimeFlinkJobProperties {

    private EquipRealtimeFlinkJobProperties() {
    }

    static EquipRealtimeFlinkJobConfig from(ParameterTool pt) {
        return new EquipRealtimeFlinkJobConfig(
                str(pt, "omes.device.rabbitmq.equip-realtime-queue", "OMES_EQUIP_REALTIME_QUEUE", "omes.equip.realtime"),
                str(pt, "omes.device.rabbitmq.equip-notify-create-queue", "OMES_EQUIP_NOTIFY_CREATE_QUEUE", "omes.notify.create"),
                str(
                        pt,
                        "omes.device.rabbitmq.equip-stream-persist-change-queue",
                        "OMES_EQUIP_STREAM_PERSIST_CHANGE_QUEUE",
                        "omes.equip.stream.persist.change"),
                str(
                        pt,
                        "omes.device.rabbitmq.equip-stream-persist-state-queue",
                        "OMES_EQUIP_STREAM_PERSIST_STATE_QUEUE",
                        "omes.equip.stream.persist.state"),
                str(
                        pt,
                        "omes.device.rabbitmq.equip-stream-persist-collect-queue",
                        "OMES_EQUIP_STREAM_PERSIST_COLLECT_QUEUE",
                        "omes.equip.stream.persist.collect"),
                pt.getLong(
                        "omes.device.flink.checkpoint-interval-ms",
                        pt.getLong("OMES_FLINK_CHECKPOINT_MS", 10_000L)),
                pt.getBoolean(
                        "omes.device.flink.enable-checkpointing",
                        pt.getBoolean("OMES_FLINK_ENABLE_CHECKPOINTING", false)),
                pt.getLong(
                        "omes.device.flink.checkpoint-timeout-ms",
                        pt.getLong("OMES_FLINK_CHECKPOINT_TIMEOUT_MS", 120_000L)),
                pt.getBoolean(
                        "omes.device.flink.unaligned-checkpoint",
                        pt.getBoolean("OMES_FLINK_UNALIGNED_CHECKPOINT", true)),
                pt.getLong(
                        "omes.device.flink.aligned-checkpoint-timeout-ms",
                        pt.getLong("OMES_FLINK_ALIGNED_CHECKPOINT_TIMEOUT_MS", 30_000L)),
                pt.getInt("omes.device.flink.rmq-prefetch", pt.getInt("OMES_FLINK_RMQ_PREFETCH", 100)),
                pt.getLong("omes.device.offline-timeout-ms", pt.getLong("OMES_EQUIP_OFFLINE_TIMEOUT_MS", 120_000L)),
                pt.getLong("omes.device.snapshot-interval-ms", pt.getLong("OMES_EQUIP_SNAPSHOT_INTERVAL_MS", 30_000L)),
                pt.getLong("omes.device.attr-fluctuation-window-ms", pt.getLong("OMES_EQUIP_ATTR_FLUCTUATION_WINDOW_MS", 90_000L)),
                pt.getLong("omes.device.attr-fluctuation-slide-ms", pt.getLong("OMES_EQUIP_ATTR_FLUCTUATION_SLIDE_MS", 5_000L)),
                pt.getBoolean(
                        "omes.device.flink.change-detect-ingress-windowed",
                        pt.getBoolean("OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOWED", false)),
                pt.getLong(
                        "omes.device.flink.change-detect-ingress-window-ms",
                        pt.getLong("OMES_FLINK_CHANGE_DETECT_INGRESS_WINDOW_MS", 60_000L)),
                pt.getLong(
                        "omes.device.flink.change-detect-ingress-slide-ms",
                        pt.getLong("OMES_FLINK_CHANGE_DETECT_INGRESS_SLIDE_MS", 60_000L)),
                ttlMin(
                        pt,
                        "omes.device.flink.state-ttl-minutes.offline-detect",
                        "OMES_FLINK_STATE_TTL_MINUTES_OFFLINE_DETECT",
                        -1L),
                ttlMin(
                        pt,
                        "omes.device.flink.state-ttl-minutes.change-detect",
                        "OMES_FLINK_STATE_TTL_MINUTES_CHANGE_DETECT",
                        -1L),
                ttlMin(
                        pt,
                        "omes.device.flink.state-ttl-minutes.attr-fluctuation",
                        "OMES_FLINK_STATE_TTL_MINUTES_ATTR_FLUCTUATION",
                        -1L),
                ttlMin(
                        pt,
                        "omes.device.flink.state-ttl-minutes.state-snapshot",
                        "OMES_FLINK_STATE_TTL_MINUTES_STATE_SNAPSHOT",
                        -1L),
                ttlMin(
                        pt,
                        "omes.device.flink.state-ttl-minutes.collect-snapshot",
                        "OMES_FLINK_STATE_TTL_MINUTES_COLLECT_SNAPSHOT",
                        -1L),
                flinkParallelism(pt),
                localMode(pt));
    }

    private static boolean localMode(ParameterTool pt) {
        return pt.getBoolean(
                "omes.device.flink.local",
                pt.getBoolean("OMES_FLINK_LOCAL", pt.getBoolean("local", false)));
    }

    private static int flinkParallelism(ParameterTool pt) {
        int p = pt.getInt("omes.device.flink.parallelism", pt.getInt("OMES_FLINK_PARALLELISM", 1));
        if (p <= 0) {
            throw new IllegalArgumentException(
                    "Flink parallelism must be positive, was: "
                            + p
                            + " (omes.device.flink.parallelism / OMES_FLINK_PARALLELISM)");
        }
        return p;
    }

    /**
     * {@code -1} = disable keyed state TTL; positive = retention in minutes (idle expire).
     */
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
