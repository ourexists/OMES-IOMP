package com.ourexists.omes.stream.equip.process;

import com.ourexists.omes.device.core.equip.cache.EquipAttrRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.stream.equip.model.EquipRealtimeChangeEvent;
import com.ourexists.omes.stream.equip.support.EquipRealtimeEventTimeUtil;
import com.ourexists.omes.stream.equip.support.EquipStreamStateTtl;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.RichProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.*;

/**
 * 滑动窗口内按属性计算波动是否超阈值，并在同一算子内维护“连续超阈值窗口数”，达标时输出 {@link EquipRealtimeChangeEvent}，
 * 供 {@link com.ourexists.omes.stream.equip.sink.bridge.EquipRecordChangeBridgeSink} / {@link com.ourexists.omes.stream.equip.sink.EquipAlarmNotifySink} 直写 MQ。
 */
public class EquipAttrFluctuationProcessFunction
        extends RichProcessWindowFunction<EquipRealtime, EquipRealtimeChangeEvent, String, TimeWindow> {

    private static final double MIN_BASELINE = 1e-6D;

    private final long stateTtlMinutes;

    private transient MapState<String, Integer> consecutiveByAttr;

    public EquipAttrFluctuationProcessFunction(long stateTtlMinutes) {
        EquipStreamStateTtl.validateMinutesOption(stateTtlMinutes);
        this.stateTtlMinutes = stateTtlMinutes;
    }

    @Override
    public void open(Configuration parameters) {
        MapStateDescriptor<String, Integer> desc =
                new MapStateDescriptor<>("equip-attr-fluctuation-consecutive-by-attr", String.class, Integer.class);
        EquipStreamStateTtl.enableIfConfigured(desc, stateTtlMinutes);
        consecutiveByAttr = getRuntimeContext().getMapState(desc);
    }

    @Override
    public void process(String selfCode, Context context, Iterable<EquipRealtime> elements, Collector<EquipRealtimeChangeEvent> out)
            throws Exception {
        EquipRealtime latest = null;
        Map<String, Double> minValues = new HashMap<>();
        Map<String, Double> maxValues = new HashMap<>();
        for (EquipRealtime realtime : elements) {
            if (realtime == null) {
                continue;
            }
            if (latest == null || EquipRealtimeEventTimeUtil.isNewerOrSame(realtime, latest)) {
                latest = realtime;
            }
            if (realtime.getEquipAttrRealtimes() == null) {
                continue;
            }
            for (EquipAttrRealtime attr : realtime.getEquipAttrRealtimes()) {
                if (attr == null || StringUtils.isBlank(attr.getName()) || StringUtils.isBlank(attr.getValue())) {
                    continue;
                }
                if (Boolean.FALSE.equals(attr.getFluctuationEnabled())) {
                    continue;
                }
                Double numeric = tryParseDouble(attr.getValue());
                if (numeric == null) {
                    continue;
                }
                minValues.merge(attr.getName(), numeric, Math::min);
                maxValues.merge(attr.getName(), numeric, Math::max);
            }
        }
        if (latest == null || latest.getEquipAttrRealtimes() == null) {
            return;
        }
        for (EquipAttrRealtime attr : latest.getEquipAttrRealtimes()) {
            if (attr == null || StringUtils.isBlank(attr.getName())) {
                continue;
            }
            if (Boolean.FALSE.equals(attr.getFluctuationEnabled())) {
                continue;
            }
            Double min = minValues.get(attr.getName());
            Double max = maxValues.get(attr.getName());
            boolean exceeded = false;
            Double thresholdRatio = attr.getFluctuationThresholdRatio();
            Double minDelta = attr.getFluctuationMinDelta();
            Integer requiredWindowsRaw = attr.getFluctuationConsecutiveWindows();
            if (thresholdRatio == null || minDelta == null || requiredWindowsRaw == null) {
                continue;
            }
            int requiredWindows = Math.max(1, requiredWindowsRaw);
            if (min != null && max != null) {
                double delta = Math.abs(max - min);
                if (delta >= minDelta) {
                    double baseline = Math.max(Math.abs(min), MIN_BASELINE);
                    double ratio = delta / baseline;
                    exceeded = ratio >= thresholdRatio;
                }
            }
            String attrName = attr.getName();
            Integer current = consecutiveByAttr.get(attrName);
            int next = exceeded ? (current == null ? 1 : current + 1) : 0;
            consecutiveByAttr.put(attrName, next);
            if (!exceeded || next != requiredWindows) {
                continue;
            }
            EquipRealtime sourceSnapshot = new EquipRealtime();
            BeanUtils.copyProperties(sourceSnapshot, latest);
            EquipRealtime alarmRealtime = new EquipRealtime();
            BeanUtils.copyProperties(alarmRealtime, latest);
            List<String> mergedAlarmTexts = new ArrayList<>();
            if (!CollectionUtils.isEmpty(latest.getAlarmTexts())) {
                mergedAlarmTexts.addAll(latest.getAlarmTexts());
            }
            mergedAlarmTexts.add("【" + attrName + "】数据波动异常，请及时排查");
            alarmRealtime.setAlarmTexts(mergedAlarmTexts);
            alarmRealtime.setAlarmState(1);
            String alarmSegmentId = UUID.randomUUID().toString();
            out.collect(
                    new EquipRealtimeChangeEvent(
                            sourceSnapshot,
                            alarmRealtime,
                            true,
                            false,
                            false,
                            null,
                            alarmSegmentId,
                            null,
                            null,
                            null,
                            null));
        }
    }

    private static Double tryParseDouble(String raw) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception ex) {
            return null;
        }
    }
}
