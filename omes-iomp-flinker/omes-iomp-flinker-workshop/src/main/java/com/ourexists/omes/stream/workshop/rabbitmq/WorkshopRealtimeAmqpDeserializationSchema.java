package com.ourexists.omes.stream.workshop.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtime;
import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtimeCollect;
import com.ourexists.omes.device.core.workshop.cache.WorkshopRealtimeConfig;
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
public class WorkshopRealtimeAmqpDeserializationSchema implements DeserializationSchema<WorkshopRealtime> {

    private static final ObjectMapper MAPPER = buildMapper();
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
    public WorkshopRealtime deserialize(byte[] message) throws IOException {
        if (message == null || message.length == 0) {
            return null;
        }
        try {
            WorkshopRealtime realtime = MAPPER.readValue(message, WorkshopRealtime.class);
            normalizeForFlinkSerialization(realtime);
            realtime.setStreamIngressSeq(STREAM_INGRESS_SEQ.incrementAndGet());
            return realtime;
        } catch (Exception e) {
            log.warn("Drop malformed workshop realtime message: {}", new String(message, StandardCharsets.UTF_8), e);
            return null;
        }
    }

    private static void normalizeForFlinkSerialization(WorkshopRealtime realtime) {
        if (realtime == null) {
            return;
        }
        realtime.setAttrsRealtime(copyList(realtime.getAttrsRealtime()));
        WorkshopRealtimeConfig cfg = realtime.getConfig();
        if (cfg != null) {
            cfg.setAttrs(copyList(cfg.getAttrs()));
        }
    }

    private static <T> List<T> copyList(List<T> src) {
        return src == null ? null : new ArrayList<>(src);
    }

    @Override
    public boolean isEndOfStream(WorkshopRealtime nextElement) {
        return false;
    }

    @Override
    public TypeInformation<WorkshopRealtime> getProducedType() {
        return TypeInformation.of(WorkshopRealtime.class);
    }
}
