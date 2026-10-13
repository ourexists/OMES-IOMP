package com.ourexists.omes.stream.equip.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.device.core.equip.cache.EquipRealtimeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class EquipRealtimeAmqpDeserializationSchema implements DeserializationSchema<EquipRealtime> {

    private static final ObjectMapper MAPPER = buildMapper();

    /** 跨并行度子任务全局单调，保证同一 JVM 内入站顺序可比较 */
    private static final AtomicLong STREAM_INGRESS_SEQ = new AtomicLong();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        mapper.setDateFormat(dateFormat);
        return mapper;
    }

    @Override
    public EquipRealtime deserialize(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            return null;
        }
        try {
            EquipRealtime realtime = MAPPER.readValue(message, EquipRealtime.class);
            normalizeForFlinkSerialization(realtime);
            realtime.setStreamIngressSeq(STREAM_INGRESS_SEQ.incrementAndGet());
            return realtime;
        } catch (Exception e) {
            log.warn("Drop malformed equip realtime message: {}", new String(message, StandardCharsets.UTF_8), e);
            return null;
        }
    }

    private static void normalizeForFlinkSerialization(EquipRealtime realtime) {
        if (realtime == null) {
            return;
        }
        realtime.setAlarmTexts(copyList(realtime.getAlarmTexts()));
        realtime.setEquipAttrRealtimes(copyList(realtime.getEquipAttrRealtimes()));
        realtime.setEquipControlRealtimes(copyList(realtime.getEquipControlRealtimes()));
        EquipRealtimeConfig cfg = realtime.getEquipRealtimeConfig();
        if (cfg != null) {
            cfg.setAttrs(copyList(cfg.getAttrs()));
            cfg.setAlarms(copyList(cfg.getAlarms()));
            cfg.setControls(copyList(cfg.getControls()));
        }
    }

    private static <T> List<T> copyList(List<T> src) {
        return src == null ? null : new ArrayList<>(src);
    }

    @Override
    public boolean isEndOfStream(EquipRealtime nextElement) {
        return false;
    }

    @Override
    public TypeInformation<EquipRealtime> getProducedType() {
        return TypeInformation.of(EquipRealtime.class);
    }
}
