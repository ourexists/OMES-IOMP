package com.ourexists.omes.stream.workshop.support;

import org.apache.flink.api.common.state.StateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;

public final class WorkshopStreamStateTtl {

    public static final long DISABLED_TTL_MINUTES = -1L;

    private WorkshopStreamStateTtl() {}

    public static void validateMinutesOption(long minutes) {
        if (minutes == DISABLED_TTL_MINUTES) {
            return;
        }
        if (minutes <= 0L) {
            throw new IllegalArgumentException(
                    "state TTL minutes must be -1 (disable) or positive, was: " + minutes);
        }
    }

    public static void enableIfConfigured(StateDescriptor<?, ?> descriptor, long stateTtlMinutes) {
        validateMinutesOption(stateTtlMinutes);
        if (stateTtlMinutes == DISABLED_TTL_MINUTES) {
            return;
        }
        descriptor.enableTimeToLive(keyedStateTtlMinutes(stateTtlMinutes));
    }

    public static StateTtlConfig keyedStateTtlMinutes(long minutes) {
        if (minutes <= 0L) {
            throw new IllegalArgumentException("state TTL minutes must be positive, was: " + minutes);
        }
        return StateTtlConfig.newBuilder(Time.minutes(minutes))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
    }
}
