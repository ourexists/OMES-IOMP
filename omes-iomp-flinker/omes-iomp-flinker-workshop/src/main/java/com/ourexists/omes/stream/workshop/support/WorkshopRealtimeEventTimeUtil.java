package com.ourexists.omes.stream.workshop.support;

import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtime;

import java.util.Date;

public final class WorkshopRealtimeEventTimeUtil {

    private WorkshopRealtimeEventTimeUtil() {}

    public static boolean isNewerOrSame(WorkshopRealtime candidate, WorkshopRealtime baseline) {
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

    public static long extractEventTimestamp(WorkshopRealtime realtime) {
        Date eventTime = realtime == null ? null : realtime.getTime();
        return eventTime == null ? 0L : eventTime.getTime();
    }

    private static long extractIngressSeq(WorkshopRealtime realtime) {
        Long seq = realtime == null ? null : realtime.getStreamIngressSeq();
        return seq == null ? 0L : seq;
    }
}
