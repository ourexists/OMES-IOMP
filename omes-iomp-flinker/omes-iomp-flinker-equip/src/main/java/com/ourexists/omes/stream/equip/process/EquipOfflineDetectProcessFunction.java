package com.ourexists.omes.stream.equip.process;

import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.stream.equip.support.EquipStreamStateTtl;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 工业级设备离线检测（Processing Time）
 *
 * 核心机制：
 * 1. 仅记录 lastSeenProcessingTime
 * 2. 每次有效数据 → 重新注册 timer(lastSeen + timeout)
 * 3. timer 触发时只做“是否仍为最新 deadline”判断
 * 4. 不允许旧数据刷新在线状态（防“续命”）
 */
public class EquipOfflineDetectProcessFunction
        extends KeyedProcessFunction<String, EquipRealtime, EquipRealtime> {

    private final long timeoutMs;
    private final long stateTtlMinutes;

    private transient ValueState<Long> activeTimer;

    private transient ValueState<EquipRealtime> latestState;

    public EquipOfflineDetectProcessFunction(long timeoutMs, long stateTtlMinutes) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        EquipStreamStateTtl.validateMinutesOption(stateTtlMinutes);
        this.timeoutMs = timeoutMs;
        this.stateTtlMinutes = stateTtlMinutes;
    }

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<EquipRealtime> latestDesc =
                new ValueStateDescriptor<>("latest-state", EquipRealtime.class);
        EquipStreamStateTtl.enableIfConfigured(latestDesc, stateTtlMinutes);

        ValueStateDescriptor<Long> activeDesc =
                new ValueStateDescriptor<>("active-time", Long.class);

        latestState = getRuntimeContext().getState(latestDesc);
        activeTimer = getRuntimeContext().getState(activeDesc);
    }

    @Override
    public void processElement(EquipRealtime value,
                               Context ctx,
                               Collector<EquipRealtime> out) throws Exception {

        long now = ctx.timerService().currentProcessingTime();

        Long oldTimer = activeTimer.value();

        // ❗ 删除旧 timer（必须成功依赖 state）
        if (oldTimer != null) {
            ctx.timerService().deleteProcessingTimeTimer(oldTimer);
        }

        long newTimer = now + timeoutMs;

        ctx.timerService().registerProcessingTimeTimer(newTimer);

        activeTimer.update(newTimer);

        latestState.update(value);
    }

    @Override
    public void onTimer(long timestamp,
                        OnTimerContext ctx,
                        Collector<EquipRealtime> out) throws Exception {
        Long t = activeTimer.value();

        // ❗ 非当前 timer 直接忽略
        if (t == null || t != timestamp) {
            return;
        }

        EquipRealtime latest = latestState.value();
        if (latest == null) {
            return;
        }

        EquipRealtime offline = copy(latest);
        offline.offline();
        offline.setTime(new Date(ctx.timerService().currentProcessingTime()));

        out.collect(offline);
    }

    /**
     * 深拷贝，避免状态污染
     */
    private static EquipRealtime copy(EquipRealtime src) {
        EquipRealtime dst = new EquipRealtime();

        dst.setId(src.getId());
        dst.setName(src.getName());
        dst.setSelfCode(src.getSelfCode());

        dst.setEquipRealtimeConfig(src.getEquipRealtimeConfig());
        dst.setOnlineState(src.getOnlineState());
        dst.setRunState(src.getRunState());
        dst.setAlarmState(src.getAlarmState());

        dst.setEquipAttrRealtimes(copyList(src.getEquipAttrRealtimes()));
        dst.setEquipControlRealtimes(copyList(src.getEquipControlRealtimes()));
        dst.setAlarmTexts(copyList(src.getAlarmTexts()));

        dst.setAlarmLevel(src.getAlarmLevel());
        dst.setTenantId(src.getTenantId());
        dst.setWorkshopCode(src.getWorkshopCode());

        dst.setTime(src.getTime());
        dst.setOnlineChangeTime(src.getOnlineChangeTime());
        dst.setRunChangeTime(src.getRunChangeTime());
        dst.setAlarmChangeTime(src.getAlarmChangeTime());

        return dst;
    }

    private static <T> List<T> copyList(List<T> src) {
        return src == null ? null : new ArrayList<>(src);
    }
}