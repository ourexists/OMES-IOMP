package com.ourexists.omes.stream.equip.support;

import com.ourexists.omes.device.core.equip.cache.EquipRealtime;

import java.util.Date;

public final class EquipRealtimeEventTimeUtil {

    private EquipRealtimeEventTimeUtil() {
    }

    /**
     * 是否应将 candidate 视作不早于 baseline（用于去重与是否处理）。
     * 设备 {@code time} 为空或相等时，用 {@link EquipRealtime#getStreamIngressSeq()} 打破平局，避免误丢弃较新的在线态。
     */
    public static boolean isNewerOrSame(EquipRealtime candidate, EquipRealtime baseline) {
        if (baseline == null) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        long ct = extractEventTimestamp(candidate);
        long bt = extractEventTimestamp(baseline);
        if (ct != bt) {
            if (ct == 0L && bt != 0L) {
                return false;
            }
            if (ct != 0L && bt == 0L) {
                return true;
            }
            return ct >= bt;
        }
        return extractIngressSeq(candidate) >= extractIngressSeq(baseline);
    }

    /** 滑动窗口 reduce：在设备时间不可靠时仍能选出最后入站的一条。 */
    public static EquipRealtime pickLatestForWindowReduce(EquipRealtime left, EquipRealtime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (isNewerOrSame(right, left)) {
            return right;
        }
        return left;
    }

    public static long extractEventTimestamp(EquipRealtime realtime) {
        Date eventTime = realtime == null ? null : realtime.getTime();
        return eventTime == null ? 0L : eventTime.getTime();
    }

    private static long extractIngressSeq(EquipRealtime realtime) {
        Long seq = realtime == null ? null : realtime.getStreamIngressSeq();
        return seq == null ? 0L : seq;
    }
}
