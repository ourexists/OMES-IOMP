package com.ourexists.omes.stream.equip;

import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.model.EquipStateSnapshotDto;
import com.ourexists.omes.stream.equip.model.EquipRealtimeChangeEvent;
import com.ourexists.omes.stream.equip.model.EquipStateSnapshotEvent;
import com.ourexists.omes.stream.equip.process.EquipAttrFluctuationProcessFunction;
import com.ourexists.omes.stream.equip.process.EquipOfflineDetectProcessFunction;
import com.ourexists.omes.stream.equip.process.EquipRealtimeChangeDetectProcessFunction;
import com.ourexists.omes.stream.equip.process.EquipStateSnapshotProcessFunction;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 规模设备测试：离线、变化、状态快照三条算子按同一时间线回放，并核对波动比例因子。
 */
public class EquipScaleTestModeTest {

    private static final int DEVICES = 200;
    private static final long TIMEOUT_MS = 1_000L;
    private static final long SNAPSHOT_INTERVAL_MS = 60_000L;
    private static final long T0 = 1_000_000L;

    @Test
    public void scaleDevicesRecordStateAndFactors() throws Exception {
        List<EquipRealtime> online = new ArrayList<>();
        List<EquipRealtime> alarm = new ArrayList<>();
        List<EquipRealtime> stale = new ArrayList<>();
        List<EquipRealtime> revival = new ArrayList<>();
        List<EquipRealtime> recovery = new ArrayList<>();
        for (int i = 0; i < DEVICES; i++) {
            String sn = sn(i);
            online.add(sample(sn, 1, 1, 0, 5_000L, 1L));
            EquipRealtime alarmed = sample(sn, 1, 1, 1, 6_000L, 2L);
            alarmed.setOnlineChange(null);
            alarm.add(alarmed);
            stale.add(sample(sn, 1, 1, 0, 5_000L, 1L));
            revival.add(sample(sn, 1, 1, 0, 6_000L, 2L));
            recovery.add(sample(sn, 1, 0, 0, T0 + TIMEOUT_MS + 1_000L, 3L));
        }

        List<EquipRealtime> offlines = runOffline(online, alarm, stale);
        Map<String, EquipRealtime> offlineBySn = index(offlines);
        List<EquipRealtimeChangeEvent> changes = runChanges(online, alarm, stale, offlines, revival, recovery);
        List<EquipStateSnapshotEvent> snapshots = runSnapshots(online, alarm, stale, offlines, revival, recovery);

        int onlineOpen = 0;
        int alarmEdge = 0;
        int offlineEdge = 0;
        int recoveryEdge = 0;
        for (EquipRealtimeChangeEvent event : changes) {
            EquipRealtime target = event.getTarget();
            if (target.getRunState() == 1 && target.getAlarmState() == 0 && target.getOnlineState() == 1) {
                onlineOpen++;
            } else if (target.getAlarmState() == 1 && target.getOnlineState() == 1) {
                alarmEdge++;
                assertFalse(event.isRunChanged());
                assertFalse(event.isOnlineChanged());
            } else if (target.getOnlineState() == 0) {
                offlineEdge++;
                assertEquals(Integer.valueOf(-1), target.getRunState());
                assertEquals(Integer.valueOf(-1), target.getAlarmState());
                assertTrue(event.isOnlineChanged());
                assertTrue(event.isRunChanged());
                assertTrue(event.isAlarmChanged());
            } else if (target.getRunState() == 0 && target.getOnlineState() == 1) {
                recoveryEdge++;
                assertEquals(Integer.valueOf(0), target.getAlarmState());
            }
        }

        int offlineSnapshots = 0;
        for (EquipStateSnapshotEvent event : snapshots) {
            EquipStateSnapshotDto snapshot = event.getSnapshot();
            if (snapshot.getOnlineState() != null && snapshot.getOnlineState() == 0) {
                offlineSnapshots++;
                assertEquals(Integer.valueOf(-1), snapshot.getRunState());
                assertEquals(Integer.valueOf(-1), snapshot.getAlarmState());
                assertTrue(offlineBySn.containsKey(snapshot.getSn()));
            }
        }

        int legacyFalseAlarms = 0;
        int currentAlarms = 0;
        for (int i = 0; i < DEVICES; i++) {
            if (legacySpan(50D, 100D, 1D, 0.8D)) {
                legacyFalseAlarms++;
            }
            if (EquipAttrFluctuationProcessFunction.spanExceeded(50D, 100D, 1D, 0.8D)) {
                currentAlarms++;
            }
        }

        System.out.println("equip-scale devices=" + DEVICES
                + " changeEvents=" + changes.size()
                + " onlineOpen=" + onlineOpen
                + " alarmEdge=" + alarmEdge
                + " offlineEdge=" + offlineEdge
                + " recoveryEdge=" + recoveryEdge
                + " offlineSnapshots=" + offlineSnapshots
                + " factorLegacyFalseAlarms=" + legacyFalseAlarms
                + " factorCurrentAlarms=" + currentAlarms);

        assertEquals(DEVICES, offlines.size());
        assertEquals(DEVICES * 4, changes.size());
        assertEquals(DEVICES, onlineOpen);
        assertEquals(DEVICES, alarmEdge);
        assertEquals(DEVICES, offlineEdge);
        assertEquals(DEVICES, recoveryEdge);
        assertEquals(DEVICES, offlineSnapshots);
        assertEquals(DEVICES, legacyFalseAlarms);
        assertEquals(0, currentAlarms);
        assertTrue(EquipAttrFluctuationProcessFunction.spanExceeded(10D, 100D, 1D, 0.9D));
        assertFalse(EquipAttrFluctuationProcessFunction.spanExceeded(100D, 100.4D, 1D, 0.2D));
    }

    private static List<EquipRealtime> runOffline(
            List<EquipRealtime> online, List<EquipRealtime> alarm, List<EquipRealtime> stale) throws Exception {
        try (KeyedOneInputStreamOperatorTestHarness<String, EquipRealtime, EquipRealtime> harness = harness(
                new EquipOfflineDetectProcessFunction(TIMEOUT_MS, -1L))) {
            harness.open();
            harness.setProcessingTime(T0);
            push(harness, online);
            harness.setProcessingTime(T0 + 100);
            push(harness, alarm);
            harness.setProcessingTime(T0 + 200);
            push(harness, stale);
            assertTrue(harness.extractOutputValues().isEmpty());
            harness.setProcessingTime(T0 + 100 + TIMEOUT_MS);
            return new ArrayList<>(harness.extractOutputValues());
        }
    }

    private static List<EquipRealtimeChangeEvent> runChanges(
            List<EquipRealtime> online,
            List<EquipRealtime> alarm,
            List<EquipRealtime> stale,
            List<EquipRealtime> offlines,
            List<EquipRealtime> revival,
            List<EquipRealtime> recovery) throws Exception {
        try (KeyedOneInputStreamOperatorTestHarness<String, EquipRealtime, EquipRealtimeChangeEvent> harness = harness(
                new EquipRealtimeChangeDetectProcessFunction(-1L))) {
            harness.open();
            push(harness, online);
            push(harness, alarm);
            push(harness, stale);
            push(harness, offlines);
            push(harness, revival);
            push(harness, recovery);
            return new ArrayList<>(harness.extractOutputValues());
        }
    }

    private static List<EquipStateSnapshotEvent> runSnapshots(
            List<EquipRealtime> online,
            List<EquipRealtime> alarm,
            List<EquipRealtime> stale,
            List<EquipRealtime> offlines,
            List<EquipRealtime> revival,
            List<EquipRealtime> recovery) throws Exception {
        try (KeyedOneInputStreamOperatorTestHarness<String, EquipRealtime, EquipStateSnapshotEvent> harness = harness(
                new EquipStateSnapshotProcessFunction(SNAPSHOT_INTERVAL_MS, -1L))) {
            harness.open();
            harness.setProcessingTime(T0);
            push(harness, online);
            push(harness, alarm);
            push(harness, stale);
            push(harness, offlines);
            push(harness, revival);
            push(harness, recovery);
            return new ArrayList<>(harness.extractOutputValues());
        }
    }

    private static <OUT> KeyedOneInputStreamOperatorTestHarness<String, EquipRealtime, OUT> harness(
            org.apache.flink.streaming.api.functions.KeyedProcessFunction<String, EquipRealtime, OUT> function)
            throws Exception {
        return new KeyedOneInputStreamOperatorTestHarness<>(
                new KeyedProcessOperator<>(function),
                EquipRealtime::getSelfCode,
                BasicTypeInfo.STRING_TYPE_INFO);
    }

    private static <OUT> void push(
            KeyedOneInputStreamOperatorTestHarness<String, EquipRealtime, OUT> harness,
            List<EquipRealtime> events) throws Exception {
        for (EquipRealtime event : events) {
            harness.processElement(event, 0L);
        }
    }

    private static Map<String, EquipRealtime> index(List<EquipRealtime> events) {
        Map<String, EquipRealtime> bySn = new LinkedHashMap<>();
        for (EquipRealtime event : events) {
            bySn.put(event.getSelfCode(), event);
        }
        return bySn;
    }

    private static EquipRealtime sample(String sn, int online, int run, int alarm, long eventTime, long seq) {
        EquipRealtime realtime = new EquipRealtime();
        realtime.setSelfCode(sn);
        realtime.setOnlineState(online);
        realtime.setRunState(run);
        realtime.setAlarmState(alarm);
        realtime.setTime(new Date(eventTime));
        realtime.setStreamIngressSeq(seq);
        realtime.setTenantId("scale");
        return realtime;
    }

    private static String sn(int index) {
        return "EQ-" + index;
    }

    /** 修复前的比例：分母用最小值，50→100 在阈值 0.8 下会误报。 */
    private static boolean legacySpan(double min, double max, double minDelta, double thresholdRatio) {
        double delta = Math.abs(max - min);
        if (delta < minDelta) {
            return false;
        }
        double baseline = Math.max(Math.abs(min), 1e-6D);
        return delta / baseline >= thresholdRatio;
    }
}
