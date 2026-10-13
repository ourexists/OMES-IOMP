package com.ourexists.omes.stream.equip;

/** Configuration for the equip realtime Flink job (Spring Boot–hosted embedded mini-cluster). */
public final class EquipRealtimeFlinkJobConfig {
    private final String equipRealtimeRabbitQueue;
    private final String equipNotifyCreateQueue;
    private final String equipStreamPersistChangeQueue;
    private final String equipStreamPersistStateQueue;
    private final String equipStreamPersistCollectQueue;
    private final long flinkCheckpointIntervalMs;
    private final boolean flinkEnableCheckpointing;
    private final long flinkCheckpointTimeoutMs;
    /** When true, disables barrier alignment blocking under load (saves checkpoint latency during backpressure). */
    private final boolean flinkUnalignedCheckpoint;
    /**
     * When positive and {@link #flinkUnalignedCheckpoint} is true: try aligned barriers first, then switch to unaligned
     * after this duration (reduces alignment stalls under backpressure; cheaper checkpoints when the pipeline is idle).
     */
    private final long flinkAlignedCheckpointTimeoutMs;
    private final int flinkRmqPrefetch;
    private final long offlineTimeoutMs;
    private final long snapshotIntervalMs;
    private final long attrFluctuationWindowMs;
    private final long attrFluctuationSlideMs;
    /**
     * When true, change-detect ingests a processing-time sliding-window reduced stream (limits upstream rate; latency up
     * to roughly one slide). When false (default), ingests every filtered realtime record plus offline timer emits.
     */
    private final boolean changeDetectIngressWindowed;
    /** Sliding window length for {@link #changeDetectIngressWindowed}; must be positive when windowed. */
    private final long changeDetectIngressWindowMs;
    /** Slide interval for {@link #changeDetectIngressWindowed}; must be positive and not greater than window length. */
    private final long changeDetectIngressSlideMs;
    /**
     * Keyed state idle TTL in minutes per operator; {@code -1} means disable Flink State TTL (default). Positive enables
     * expiry.
     */
    private final long stateTtlMinutesOfflineDetect;
    private final long stateTtlMinutesChangeDetect;
    private final long stateTtlMinutesAttrFluctuation;
    private final long stateTtlMinutesStateSnapshot;
    private final long stateTtlMinutesCollectSnapshot;
    /** Embedded mini-cluster default parallelism; must be positive. Ignored when {@link #flinkLocalMode} is false. */
    private final int flinkParallelism;
    /** Embedded mini-cluster. False submits via {@code flink run} / the Web UI. */
    private final boolean flinkLocalMode;

    public EquipRealtimeFlinkJobConfig(
            String equipRealtimeRabbitQueue,
            String equipNotifyCreateQueue,
            String equipStreamPersistChangeQueue,
            String equipStreamPersistStateQueue,
            String equipStreamPersistCollectQueue,
            long flinkCheckpointIntervalMs,
            boolean flinkEnableCheckpointing,
            long flinkCheckpointTimeoutMs,
            boolean flinkUnalignedCheckpoint,
            long flinkAlignedCheckpointTimeoutMs,
            int flinkRmqPrefetch,
            long offlineTimeoutMs,
            long snapshotIntervalMs,
            long attrFluctuationWindowMs,
            long attrFluctuationSlideMs,
            boolean changeDetectIngressWindowed,
            long changeDetectIngressWindowMs,
            long changeDetectIngressSlideMs,
            long stateTtlMinutesOfflineDetect,
            long stateTtlMinutesChangeDetect,
            long stateTtlMinutesAttrFluctuation,
            long stateTtlMinutesStateSnapshot,
            long stateTtlMinutesCollectSnapshot,
            int flinkParallelism,
            boolean flinkLocalMode) {
        this.equipRealtimeRabbitQueue = equipRealtimeRabbitQueue;
        this.equipNotifyCreateQueue = equipNotifyCreateQueue;
        this.equipStreamPersistChangeQueue = equipStreamPersistChangeQueue;
        this.equipStreamPersistStateQueue = equipStreamPersistStateQueue;
        this.equipStreamPersistCollectQueue = equipStreamPersistCollectQueue;
        this.flinkCheckpointIntervalMs = flinkCheckpointIntervalMs;
        this.flinkEnableCheckpointing = flinkEnableCheckpointing;
        this.flinkCheckpointTimeoutMs = flinkCheckpointTimeoutMs;
        this.flinkUnalignedCheckpoint = flinkUnalignedCheckpoint;
        this.flinkAlignedCheckpointTimeoutMs = flinkAlignedCheckpointTimeoutMs;
        this.flinkRmqPrefetch = flinkRmqPrefetch;
        this.offlineTimeoutMs = offlineTimeoutMs;
        this.snapshotIntervalMs = snapshotIntervalMs;
        this.attrFluctuationWindowMs = attrFluctuationWindowMs;
        this.attrFluctuationSlideMs = attrFluctuationSlideMs;
        this.changeDetectIngressWindowed = changeDetectIngressWindowed;
        this.changeDetectIngressWindowMs = changeDetectIngressWindowMs;
        this.changeDetectIngressSlideMs = changeDetectIngressSlideMs;
        this.stateTtlMinutesOfflineDetect = stateTtlMinutesOfflineDetect;
        this.stateTtlMinutesChangeDetect = stateTtlMinutesChangeDetect;
        this.stateTtlMinutesAttrFluctuation = stateTtlMinutesAttrFluctuation;
        this.stateTtlMinutesStateSnapshot = stateTtlMinutesStateSnapshot;
        this.stateTtlMinutesCollectSnapshot = stateTtlMinutesCollectSnapshot;
        this.flinkParallelism = flinkParallelism;
        this.flinkLocalMode = flinkLocalMode;
    }

    public String equipRealtimeRabbitQueue() {
        return equipRealtimeRabbitQueue;
    }

    public String equipNotifyCreateQueue() {
        return equipNotifyCreateQueue;
    }

    public String equipStreamPersistChangeQueue() {
        return equipStreamPersistChangeQueue;
    }

    public String equipStreamPersistStateQueue() {
        return equipStreamPersistStateQueue;
    }

    public String equipStreamPersistCollectQueue() {
        return equipStreamPersistCollectQueue;
    }

    public long flinkCheckpointIntervalMs() {
        return flinkCheckpointIntervalMs;
    }

    public boolean flinkEnableCheckpointing() {
        return flinkEnableCheckpointing;
    }

    public long flinkCheckpointTimeoutMs() {
        return flinkCheckpointTimeoutMs;
    }

    public boolean flinkUnalignedCheckpoint() {
        return flinkUnalignedCheckpoint;
    }

    public long flinkAlignedCheckpointTimeoutMs() {
        return flinkAlignedCheckpointTimeoutMs;
    }

    public int flinkRmqPrefetch() {
        return flinkRmqPrefetch;
    }

    public long offlineTimeoutMs() {
        return offlineTimeoutMs;
    }

    public long snapshotIntervalMs() {
        return snapshotIntervalMs;
    }

    public long attrFluctuationWindowMs() {
        return attrFluctuationWindowMs;
    }

    public long attrFluctuationSlideMs() {
        return attrFluctuationSlideMs;
    }

    public boolean changeDetectIngressWindowed() {
        return changeDetectIngressWindowed;
    }

    public long changeDetectIngressWindowMs() {
        return changeDetectIngressWindowMs;
    }

    public long changeDetectIngressSlideMs() {
        return changeDetectIngressSlideMs;
    }

    public long stateTtlMinutesOfflineDetect() {
        return stateTtlMinutesOfflineDetect;
    }

    public long stateTtlMinutesChangeDetect() {
        return stateTtlMinutesChangeDetect;
    }

    public long stateTtlMinutesAttrFluctuation() {
        return stateTtlMinutesAttrFluctuation;
    }

    public long stateTtlMinutesStateSnapshot() {
        return stateTtlMinutesStateSnapshot;
    }

    public long stateTtlMinutesCollectSnapshot() {
        return stateTtlMinutesCollectSnapshot;
    }

    public int flinkParallelism() {
        return flinkParallelism;
    }

    public boolean flinkLocalMode() {
        return flinkLocalMode;
    }
}
