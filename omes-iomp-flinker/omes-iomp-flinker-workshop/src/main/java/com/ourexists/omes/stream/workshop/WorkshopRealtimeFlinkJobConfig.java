package com.ourexists.omes.stream.workshop;

public final class WorkshopRealtimeFlinkJobConfig {
    private final String workshopRealtimeRabbitQueue;
    private final String workshopStreamPersistCollectQueue;
    private final long flinkCheckpointIntervalMs;
    private final boolean flinkEnableCheckpointing;
    private final long flinkCheckpointTimeoutMs;
    private final boolean flinkUnalignedCheckpoint;
    private final long flinkAlignedCheckpointTimeoutMs;
    private final int flinkRmqPrefetch;
    private final long snapshotIntervalMs;
    private final long stateTtlMinutesCollectSnapshot;
    /** Embedded local mini-cluster (IDE / {@code java -jar}), not remote {@code flink run}. */
    private final boolean flinkLocalMode;
    /** Operator parallelism when {@link #flinkLocalMode} is true; must be positive. */
    private final int flinkLocalParallelism;
    /** When local mode: start Flink Web UI for debugging. */
    private final boolean flinkLocalWebUI;

    public WorkshopRealtimeFlinkJobConfig(
            String workshopRealtimeRabbitQueue,
            String workshopStreamPersistCollectQueue,
            long flinkCheckpointIntervalMs,
            boolean flinkEnableCheckpointing,
            long flinkCheckpointTimeoutMs,
            boolean flinkUnalignedCheckpoint,
            long flinkAlignedCheckpointTimeoutMs,
            int flinkRmqPrefetch,
            long snapshotIntervalMs,
            long stateTtlMinutesCollectSnapshot,
            boolean flinkLocalMode,
            int flinkLocalParallelism,
            boolean flinkLocalWebUI) {
        this.workshopRealtimeRabbitQueue = workshopRealtimeRabbitQueue;
        this.workshopStreamPersistCollectQueue = workshopStreamPersistCollectQueue;
        this.flinkCheckpointIntervalMs = flinkCheckpointIntervalMs;
        this.flinkEnableCheckpointing = flinkEnableCheckpointing;
        this.flinkCheckpointTimeoutMs = flinkCheckpointTimeoutMs;
        this.flinkUnalignedCheckpoint = flinkUnalignedCheckpoint;
        this.flinkAlignedCheckpointTimeoutMs = flinkAlignedCheckpointTimeoutMs;
        this.flinkRmqPrefetch = flinkRmqPrefetch;
        this.snapshotIntervalMs = snapshotIntervalMs;
        this.stateTtlMinutesCollectSnapshot = stateTtlMinutesCollectSnapshot;
        this.flinkLocalMode = flinkLocalMode;
        this.flinkLocalParallelism = flinkLocalParallelism;
        this.flinkLocalWebUI = flinkLocalWebUI;
    }

    public String workshopRealtimeRabbitQueue() {
        return workshopRealtimeRabbitQueue;
    }

    public String workshopStreamPersistCollectQueue() {
        return workshopStreamPersistCollectQueue;
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

    public long snapshotIntervalMs() {
        return snapshotIntervalMs;
    }

    public long stateTtlMinutesCollectSnapshot() {
        return stateTtlMinutesCollectSnapshot;
    }

    public boolean flinkLocalMode() {
        return flinkLocalMode;
    }

    public int flinkLocalParallelism() {
        return flinkLocalParallelism;
    }

    public boolean flinkLocalWebUI() {
        return flinkLocalWebUI;
    }
}
