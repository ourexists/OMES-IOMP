package com.ourexists.omes.stream.workshop.process;

import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtime;
import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtimeCollect;
import com.ourexists.omes.device.model.WorkshopCollectDto;
import com.ourexists.omes.stream.workshop.model.WorkshopCollectSnapshotEvent;
import com.ourexists.omes.stream.workshop.support.WorkshopRealtimeEventTimeUtil;
import com.ourexists.omes.stream.workshop.support.WorkshopStreamStateTtl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkshopCollectSnapshotProcessFunction
        extends KeyedProcessFunction<String, WorkshopRealtime, WorkshopCollectSnapshotEvent> {

    private final long snapshotIntervalMs;
    private final long stateTtlMinutes;
    private transient ValueState<WorkshopRealtime> latestState;
    private transient ValueState<Long> nextSnapshotTimerState;

    public WorkshopCollectSnapshotProcessFunction(long snapshotIntervalMs, long stateTtlMinutes) {
        WorkshopStreamStateTtl.validateMinutesOption(stateTtlMinutes);
        this.snapshotIntervalMs = snapshotIntervalMs;
        this.stateTtlMinutes = stateTtlMinutes;
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<WorkshopRealtime> latestDesc =
                new ValueStateDescriptor<>("workshop-collect-snapshot-latest-state", WorkshopRealtime.class);
        WorkshopStreamStateTtl.enableIfConfigured(latestDesc, stateTtlMinutes);
        ValueStateDescriptor<Long> timerDesc = new ValueStateDescriptor<>("workshop-collect-snapshot-next-timer", Long.class);
        WorkshopStreamStateTtl.enableIfConfigured(timerDesc, stateTtlMinutes);
        latestState = getRuntimeContext().getState(latestDesc);
        nextSnapshotTimerState = getRuntimeContext().getState(timerDesc);
    }

    @Override
    public void processElement(WorkshopRealtime value, Context ctx, Collector<WorkshopCollectSnapshotEvent> out) throws Exception {
        WorkshopRealtime current = latestState.value();
        if (value != null && (current == null || WorkshopRealtimeEventTimeUtil.isNewerOrSame(value, current))) {
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
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<WorkshopCollectSnapshotEvent> out) throws Exception {
        WorkshopRealtime latest = latestState.value();
        if (latest == null) {
            ctx.timerService().deleteProcessingTimeTimer(timestamp);
            nextSnapshotTimerState.clear();
            return;
        }
        Map<String, String> data = extractCollectData(latest);
        if (!MapUtils.isEmpty(data)) {
            WorkshopCollectDto dto = new WorkshopCollectDto();
            dto.setWorkshopId(latest.getId());
            dto.setTime(new Date(timestamp));
            dto.setData(data);
            dto.setTenantId(latest.getTenantId());
            out.collect(new WorkshopCollectSnapshotEvent(dto));
        }

        long nextTs = timestamp + snapshotIntervalMs;
        ctx.timerService().registerProcessingTimeTimer(nextTs);
        nextSnapshotTimerState.update(nextTs);
    }

    private static Map<String, String> extractCollectData(WorkshopRealtime realtime) {
        List<WorkshopRealtimeCollect> attrs = resolveAttrs(realtime);
        if (CollectionUtils.isEmpty(attrs)) {
            return Collections.emptyMap();
        }
        Map<String, String> data = new HashMap<>();
        for (WorkshopRealtimeCollect attr : attrs) {
            if (attr.getNeedCollect() == null || !attr.getNeedCollect()) {
                continue;
            }
            data.put(attr.getName(), attr.getValue());
        }
        return data;
    }

    private static List<WorkshopRealtimeCollect> resolveAttrs(WorkshopRealtime rt) {
        if (rt == null) {
            return null;
        }
        if (!CollectionUtils.isEmpty(rt.getAttrsRealtime())) {
            return rt.getAttrsRealtime();
        }
        if (rt.getConfig() != null) {
            return rt.getConfig().getAttrs();
        }
        return null;
    }
}
