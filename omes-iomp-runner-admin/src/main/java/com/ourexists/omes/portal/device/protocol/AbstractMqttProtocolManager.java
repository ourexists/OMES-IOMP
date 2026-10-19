/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.portal.device.protocol;

import com.ourexists.era.framework.core.user.UserContext;
import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
import com.ourexists.omes.device.core.equip.protocol.ProtocolManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一 MQTT 订阅管理器：根据网关配置动态创建/销毁订阅连接，支持启停控制。
 * 使用 Eclipse Paho 原生 MqttClient，避免 Spring Integration 适配器导致的“一连接/一收消息就断”问题。
 */
@Slf4j
public abstract class AbstractMqttProtocolManager implements ProtocolManager {

    private static final int CONNECTION_TIMEOUT = 30;
    private static final int KEEP_ALIVE_INTERVAL = 30;


    /** key: gatewayId，value: 该网关的 MqttClient */
    private final Map<String, MqttClient> connectionMap = new ConcurrentHashMap<>();

    private final ConnectLink link = new ConnectLink();

    /**
     * 本机机器码后缀（首块可用网卡 MAC 十六进制；失败则用主机名哈希），用于 MQTT clientId 区分多实例。
     */
    private static volatile String machineCodeSuffix;

    private static String resolveMachineCodeSuffix() {
        if (machineCodeSuffix != null) {
            return machineCodeSuffix;
        }
        synchronized (AbstractMqttProtocolManager.class) {
            if (machineCodeSuffix != null) {
                return machineCodeSuffix;
            }
            machineCodeSuffix = computeMachineCodeSuffix();
            return machineCodeSuffix;
        }
    }

    private static String computeMachineCodeSuffix() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                try {
                    if (ni.isLoopback() || !ni.isUp()) {
                        continue;
                    }
                } catch (SocketException e) {
                    continue;
                }
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02x", b));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.warn("MQTT clientId machineCode: failed to enumerate network interfaces: {}", e.getMessage());
        }
        try {
            return Integer.toHexString(InetAddress.getLocalHost().getHostName().hashCode());
        } catch (Exception e) {
            return "na";
        }
    }

    @Override
    public synchronized void stopAll() {
        List<String> keys = new ArrayList<>(connectionMap.keySet());
        for (String key : keys) {
            stop(key);
        }
    }

    @Override
    public synchronized boolean start(ProtocolConnect gw) {
        String key = gw.getId();
        if (connectionMap.containsKey(key)) {
            log.debug("Connection already exists for gateway: {}", key);
            return true;
        }
        try {
            UserContext.defaultTenant();
            MqttClient client = createAndConnect(gw);
            connectionMap.put(key, client);
            log.info("MQTT subscription started for gateway: {} topic: {}", gw.getServerName(), gw.getTopic());
            return true;
        } catch (Exception e) {
            log.error("Failed to start MQTT for gateway {}: {}", gw.getServerName(), e.getMessage(), e);
            link.failed(key, e.getMessage() != null ? e.getMessage() : "连接未建立");
            return false;
        }
    }

    public synchronized boolean stop(String gatewayId) {
        if (gatewayId == null || gatewayId.isEmpty()) {
            return false;
        }
        link.clear(gatewayId);
        MqttClient client = connectionMap.remove(gatewayId);
        if (client == null) {
            log.debug("No connection found for gateway: {}", gatewayId);
            return false;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            log.info("MQTT subscription stopped for gateway: {}", gatewayId);
            return true;
        } catch (MqttException e) {
            log.error("Error stopping MQTT client {}: {}", gatewayId, e.getMessage(), e);
            return true;
        }
    }

    private MqttClient createAndConnect(ProtocolConnect gw) throws MqttException {
        String baseId = gw.getId();
        String machineCode = resolveMachineCodeSuffix();
        String clientId = "omes_mqtt_" + baseId + "_" + machineCode;

        MqttClient client = new MqttClient(gw.getUri(), clientId, null);

        String topic = gw.getTopic();
        if (!StringUtils.hasText(topic)) {
            client.close();
            throw new MqttException(new IllegalArgumentException("MQTT gateway topic is required, gateway: " + gw.getServerName()));
        }
        topic = topic.trim();
        IMqttMessageListener listener = getListener(gw);

        // 重连后 Paho 不会自动重新订阅，必须在 connectComplete(reconnect=true) 里手动 re-subscribe
        String finalTopic = topic;
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                link.up(gw.getId());
                if (!reconnect) {
                    return;
                }
                try {
                    client.subscribe(finalTopic, 1, listener);
                    log.info("MQTT re-subscribed after reconnect clientId: [{}] topic: [{}]", client.getClientId(), finalTopic);
                } catch (MqttException e) {
                    log.error("MQTT re-subscribe failed clientId: [{}] topic: [{}]: {}", client.getClientId(), finalTopic, e.getMessage(), e);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                String reason = cause != null && cause.getMessage() != null ? cause.getMessage() : "连接已断开";
                link.down(gw.getId(), reason);
                log.warn("MQTT connection lost clientId: [{}]: {}", client.getClientId(), reason);
            }

            @Override
            public void messageArrived(String topicFilter, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                // 由 subscribe 时注册的 IMqttMessageListener 处理
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        if (StringUtils.hasText(gw.getUsername()) && StringUtils.hasText(gw.getPassword())) {
            options.setUserName(gw.getUsername());
            options.setPassword(gw.getPassword().toCharArray());
        }
        options.setConnectionTimeout(CONNECTION_TIMEOUT);
        options.setKeepAliveInterval(KEEP_ALIVE_INTERVAL);
        options.setAutomaticReconnect(true);
        options.setMaxReconnectDelay(10_000);
        options.setCleanSession(true);
        client.connect(options);

        try {
            log.info("MQTT subscribing gateway: {} topic: [{}]", gw.getServerName(), topic);
            client.subscribe(topic, 1, listener);
            log.info("MQTT subscribed successfully clientId: [{}] topic: [{}]", clientId, topic);
        } catch (Exception e) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (MqttException ex) {
                log.warn("Error closing client after subscribe failure: {}", ex.getMessage());
            }
            log.error("MQTT subscribe failed for gateway: {} topic: [{}], error: {}", gw.getServerName(), topic, e.getMessage(), e);
            throw e instanceof MqttException ? (MqttException) e : new MqttException(e);
        }
        return client;
    }

    @Override
    public String linkState(String connectId) {
        MqttClient client = connectionMap.get(connectId);
        if (client == null) {
            return null;
        }
        try {
            return client.isConnected() ? ConnectLink.UP : ConnectLink.DOWN;
        } catch (Exception e) {
            return ConnectLink.DOWN;
        }
    }

    @Override
    public String linkMessage(String connectId) {
        if (ConnectLink.UP.equals(linkState(connectId))) {
            return null;
        }
        String msg = link.message(connectId);
        if (ConnectLink.DOWN.equals(linkState(connectId))) {
            return msg != null ? msg : "连接已断开";
        }
        return msg;
    }

    protected abstract IMqttMessageListener getListener(ProtocolConnect gw);

    public static String decodePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return null;
        }
        try {
            return new String(payload, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(payload, StandardCharsets.ISO_8859_1);
        }
    }
}
