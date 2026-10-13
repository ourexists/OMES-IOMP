package com.ourexists.omes.stream.workshop.sink.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public abstract class AbstractWorkshopStreamPersistRmqSink<T> extends RichSinkFunction<T> {

    protected final RMQConnectionConfig rmqConnectionConfig;
    protected final String queueName;

    protected transient ObjectMapper objectMapper;
    protected transient Connection connection;
    protected transient Channel channel;

    protected AbstractWorkshopStreamPersistRmqSink(RMQConnectionConfig rmqConnectionConfig, String queueName) {
        this.rmqConnectionConfig = rmqConnectionConfig;
        this.queueName = queueName;
    }

    public String newOutboundEventId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(queueName) || rmqConnectionConfig == null) {
            log.warn("{} disabled: queue empty or RMQ config null", getClass().getSimpleName());
            return;
        }
        try {
            com.rabbitmq.client.ConnectionFactory cf = rmqConnectionConfig.getConnectionFactory();
            connection = cf.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
        } catch (Exception e) {
            log.error("{}: RabbitMQ open failed", getClass().getSimpleName(), e);
            closeQuietly();
        }
    }

    protected void publish(byte[] body) {
        if (StringUtils.isBlank(queueName) || channel == null || !channel.isOpen()) {
            return;
        }
        try {
            channel.basicPublish("", queueName, null, body);
        } catch (IOException e) {
            log.error("{}: publish failed queue={}", getClass().getSimpleName(), queueName, e);
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
            log.warn("{}: channel close", getClass().getSimpleName(), e);
        }
        channel = null;
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("{}: connection close", getClass().getSimpleName(), e);
        }
        connection = null;
    }
}
