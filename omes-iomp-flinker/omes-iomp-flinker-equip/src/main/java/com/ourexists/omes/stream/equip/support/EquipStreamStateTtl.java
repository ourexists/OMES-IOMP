package com.ourexists.omes.stream.equip.support;

import org.apache.flink.api.common.state.StateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;

/**
 * Keyed state TTL: {@link #DISABLED_TTL_MINUTES} turns Flink State TTL off; otherwise retention is in minutes.
 */
public final class EquipStreamStateTtl {

    /** Default and sentinel: do not enable {@code enableTimeToLive} (state does not expire by TTL). */
    public static final long DISABLED_TTL_MINUTES = -1L;

    private EquipStreamStateTtl() {}

    /** @throws IllegalArgumentException unless {@code minutes} is {@link #DISABLED_TTL_MINUTES} or positive */
    public static void validateMinutesOption(long minutes) {
        if (minutes == DISABLED_TTL_MINUTES) {
            return;
        }
        if (minutes <= 0L) {
            throw new IllegalArgumentException(
                    "state TTL minutes must be -1 (disable) or positive, was: " + minutes);
        }
    }

    /**
     * Enables TTL when {@code stateTtlMinutes} is positive; {@link #DISABLED_TTL_MINUTES} leaves state without TTL.
     */
    public static void enableIfConfigured(StateDescriptor<?, ?> descriptor, long stateTtlMinutes) {
        validateMinutesOption(stateTtlMinutes);
        if (stateTtlMinutes == DISABLED_TTL_MINUTES) {
            return;
        }
        descriptor.enableTimeToLive(keyedStateTtlMinutes(stateTtlMinutes));
    }

    /**
     * @param minutes strictly positive
     */
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
