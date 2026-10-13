package com.ourexists.omes.stream.equip.model;

import com.ourexists.omes.device.core.equip.cache.EquipRealtime;

public class EquipRealtimeChangeEvent {
    private final EquipRealtime source;
    private final EquipRealtime target;
    private final boolean alarmChanged;
    private final boolean runChanged;
    private final boolean onlineChanged;
    private final String alarmPrevSegmentEventId;
    private final String alarmSegmentEventId;
    private final String runPrevSegmentEventId;
    private final String runSegmentEventId;
    private final String onlinePrevSegmentEventId;
    private final String onlineSegmentEventId;

    public EquipRealtimeChangeEvent(
            EquipRealtime source,
            EquipRealtime target,
            boolean alarmChanged,
            boolean runChanged,
            boolean onlineChanged,
            String alarmPrevSegmentEventId,
            String alarmSegmentEventId,
            String runPrevSegmentEventId,
            String runSegmentEventId,
            String onlinePrevSegmentEventId,
            String onlineSegmentEventId) {
        this.source = source;
        this.target = target;
        this.alarmChanged = alarmChanged;
        this.runChanged = runChanged;
        this.onlineChanged = onlineChanged;
        this.alarmPrevSegmentEventId = alarmPrevSegmentEventId;
        this.alarmSegmentEventId = alarmSegmentEventId;
        this.runPrevSegmentEventId = runPrevSegmentEventId;
        this.runSegmentEventId = runSegmentEventId;
        this.onlinePrevSegmentEventId = onlinePrevSegmentEventId;
        this.onlineSegmentEventId = onlineSegmentEventId;
    }

    public EquipRealtime getSource() {
        return source;
    }

    public EquipRealtime getTarget() {
        return target;
    }

    public boolean isAlarmChanged() {
        return alarmChanged;
    }

    public boolean isRunChanged() {
        return runChanged;
    }

    public boolean isOnlineChanged() {
        return onlineChanged;
    }

    public String getAlarmPrevSegmentEventId() {
        return alarmPrevSegmentEventId;
    }

    public String getAlarmSegmentEventId() {
        return alarmSegmentEventId;
    }

    public String getRunPrevSegmentEventId() {
        return runPrevSegmentEventId;
    }

    public String getRunSegmentEventId() {
        return runSegmentEventId;
    }

    public String getOnlinePrevSegmentEventId() {
        return onlinePrevSegmentEventId;
    }

    public String getOnlineSegmentEventId() {
        return onlineSegmentEventId;
    }
}
