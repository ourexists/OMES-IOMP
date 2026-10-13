package com.ourexists.omes.stream.equip.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourexists.omes.device.core.equip.cache.EquipRealtime;
import com.ourexists.omes.message.enums.MessageSourceEnum;
import com.ourexists.omes.message.enums.MessageTypeEnum;
import com.ourexists.omes.message.model.NotifyDto;
import com.ourexists.omes.stream.equip.model.EquipRealtimeChangeEvent;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class EquipAlarmNotifySink extends RichSinkFunction<EquipRealtimeChangeEvent> {

    private final RMQConnectionConfig rmqConnectionConfig;
    private final String queueName;

    private transient ObjectMapper objectMapper;
    private transient Connection connection;
    private transient Channel channel;

    public EquipAlarmNotifySink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        this.rmqConnectionConfig = rmqConnectionConfig;
        this.queueName = queueName;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(queueName) || rmqConnectionConfig == null) {
            log.warn("EquipAlarmNotifySink disabled: queueName empty or rmq config null");
            return;
        }
        try {
            ConnectionFactory cf = rmqConnectionConfig.getConnectionFactory();
            connection = cf.newConnection();
            channel = connection.createChannel();
            // 与门户 {@code new Queue(name, true)} 一致，幂等声明
            channel.queueDeclare(queueName, true, false, false, null);
        } catch (Exception e) {
            log.error("EquipAlarmNotifySink: RabbitMQ open failed", e);
            closeQuietly();
        }
    }

    @Override
    public void invoke(EquipRealtimeChangeEvent event, Context context) {
        if (StringUtils.isBlank(queueName) || channel == null || !channel.isOpen()) {
            return;
        }
        EquipRealtime source = event == null ? null : event.getSource();
        EquipRealtime target = event == null ? null : event.getTarget();
        if (source == null || target == null) {
            return;
        }
        if (!event.isAlarmChanged() || target.getAlarmState() == null || target.getAlarmState() != 1) {
            return;
        }
        try {
            List<String> platforms = new ArrayList<>();
            platforms.add("mes-app");
            platforms.add("mes-edge");
            StringBuilder message = new StringBuilder();
            if (!CollectionUtils.isEmpty(target.getAlarmTexts())) {
                for (String alarmText : target.getAlarmTexts()) {
                    message.append(alarmText).append("\r\n");
                }
            } else {
                message.append("设备报警");
            }
            NotifyDto notifyDto = new NotifyDto()
                    .setStep(0)
                    .setContext(message.toString())
                    .setTitle("【" + target.getName() + "】异常报警")
                    .setSource(MessageSourceEnum.Equip.name())
                    .setSourceId(source.getId())
                    .setPlatforms(platforms)
                    .setType(MessageTypeEnum.ALARM.getCode())
                    .setEventId(UUID.randomUUID().toString());
            byte[] body = objectMapper.writeValueAsBytes(notifyDto);
            channel.basicPublish("", queueName, null, body);
        } catch (Exception e) {
            log.error("EquipAlarmNotifySink: publish failed queue={}", queueName, e);
        }
    }

    @Override
    public void close() throws Exception {
        closeQuietly();
        super.close();
    }

    private void closeQuietly() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception e) {
            log.warn("EquipAlarmNotifySink: channel close", e);
        }
        channel = null;
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("EquipAlarmNotifySink: connection close", e);
        }
        connection = null;
    }
}
