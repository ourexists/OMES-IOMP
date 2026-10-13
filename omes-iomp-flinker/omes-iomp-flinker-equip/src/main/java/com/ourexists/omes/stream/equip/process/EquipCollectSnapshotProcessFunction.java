package com.ourexists.omes.stream.equip.process;

import com.ourexists.omes.device.core.equip.cache.EquipAttrRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.model.EquipCollectDto;
import com.ourexists.omes.stream.equip.model.EquipCollectSnapshotEvent;
import com.ourexists.omes.stream.equip.support.EquipRealtimeEventTimeUtil;
import com.ourexists.omes.stream.equip.support.EquipStreamStateTtl;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipCollectSnapshotProcessFunction extends KeyedProcessFunction<String, EquipRealtime, EquipCollectSnapshotEvent> {

    private final long snapshotIntervalMs;
    private final long stateTtlMinutes;
    private transient ValueState<EquipRealtime> latestState;
    private transient ValueState<Long> nextSnapshotTimerState;

    public EquipCollectSnapshotProcessFunction(long snapshotIntervalMs, long stateTtlMinutes) {
        EquipStreamStateTtl.validateMinutesOption(stateTtlMinutes);
        this.snapshotIntervalMs = snapshotIntervalMs;
        this.stateTtlMinutes = stateTtlMinutes;
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<EquipRealtime> latestDesc =
                new ValueStateDescriptor<>("collect-snapshot-latest-state", EquipRealtime.class);
        EquipStreamStateTtl.enableIfConfigured(latestDesc, stateTtlMinutes);
        ValueStateDescriptor<Long> timerDesc = new ValueStateDescriptor<>("collect-snapshot-next-timer", Long.class);
        EquipStreamStateTtl.enableIfConfigured(timerDesc, stateTtlMinutes);
        latestState = getRuntimeContext().getState(latestDesc);
        nextSnapshotTimerState = getRuntimeContext().getState(timerDesc);
    }

    @Override
    public void processElement(EquipRealtime value, Context ctx, Collector<EquipCollectSnapshotEvent> out) throws Exception {
        EquipRealtime current = latestState.value();
        if (value != null && (current == null || EquipRealtimeEventTimeUtil.isNewerOrSame(value, current))) {
            latestState.update(value);
        }
        Long timer = nextSnapshotTimerState.value();
        if (timer == null) {
            long firstTs = ctx.timerService().currentProcessingTime() + snapshotIntervalMs;
            ctx.timerService().registerProcessingTimeTimer(firstTs);
            nextSnapshotTimerState.update(firstTs);
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<EquipCollectSnapshotEvent> out) throws Exception {
        EquipRealtime latest = latestState.value();
        if (latest == null) {
            ctx.timerService().deleteProcessingTimeTimer(timestamp);
            nextSnapshotTimerState.clear();
            return;
        }
        Map<String, String> data = extractCollectData(latest);
        if (!MapUtils.isEmpty(data)) {
            EquipCollectDto dto = new EquipCollectDto();
            dto.setSn(latest.getSelfCode());
            dto.setTime(new Date(timestamp));
            dto.setData(data);
            dto.setTenantId(latest.getTenantId());
            out.collect(new EquipCollectSnapshotEvent(dto));
        }

        long nextTs = timestamp + snapshotIntervalMs;
        ctx.timerService().registerProcessingTimeTimer(nextTs);
        nextSnapshotTimerState.update(nextTs);
    }

    private Map<String, String> extractCollectData(EquipRealtime realtime) {
        List<EquipAttrRealtime> attrs = realtime == null ? null : realtime.getEquipAttrRealtimes();
        if (CollectionUtils.isEmpty(attrs)) {
            return Collections.emptyMap();
        }
        Map<String, String> data = new HashMap<>();
        for (EquipAttrRealtime attr : attrs) {
            if (attr.getNeedCollect() == null || !attr.getNeedCollect()) {
                continue;
            }
            data.put(attr.getName(), attr.getValue());
        }
        return data;
    }
}
