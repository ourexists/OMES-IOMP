/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.portal.device.gateway;

import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
import com.ourexists.omes.device.core.equip.protocol.ProtocolExecutor;
import com.ourexists.omes.device.core.equip.protocol.ProtocolManager;
import com.ourexists.omes.device.model.GatewayDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Device gateway protocol lifecycle (MQTT / PLC polling, etc.).
 * Runs in Admin where {@link ProtocolManager} implementations are registered.
 */
@Service
public class GatewayRuntimeService {

    @Autowired
    private ProtocolExecutor protocolExecutor;

    public boolean start(GatewayDto gateway) {
        if (gateway == null) {
            return false;
        }
        ProtocolConnect connect = toConnect(gateway);
        return protocolExecutor.start(connect);
    }

    public boolean stop(GatewayDto gateway) {
        if (gateway == null || gateway.getId() == null) {
            return false;
        }
        return protocolExecutor.stop(gateway.getProtocol(), gateway.getId());
    }

    public ProtocolManager protocolManager(String protocol) {
        return protocolExecutor.protocolManager(protocol);
    }

    /** 把本进程的连通结果写到列表项上。未启用的连接不填。 */
    public void fillLink(GatewayDto gateway) {
        if (gateway == null || !Boolean.TRUE.equals(gateway.getEnabled()) || !StringUtils.hasText(gateway.getId())) {
            return;
        }
        ProtocolManager manager = protocolManager(gateway.getProtocol());
        if (manager == null) {
            gateway.setLinkState("down");
            gateway.setLinkMessage("协议未加载");
            return;
        }
        String state = manager.linkState(gateway.getId());
        if (!StringUtils.hasText(state)) {
            gateway.setLinkState("down");
            String msg = manager.linkMessage(gateway.getId());
            gateway.setLinkMessage(StringUtils.hasText(msg) ? msg : "未拉起");
            return;
        }
        gateway.setLinkState(state);
        gateway.setLinkMessage(manager.linkMessage(gateway.getId()));
    }

    public String lastLinkMessage(GatewayDto gateway) {
        if (gateway == null || !StringUtils.hasText(gateway.getId())) {
            return null;
        }
        ProtocolManager manager = protocolManager(gateway.getProtocol());
        return manager == null ? null : manager.linkMessage(gateway.getId());
    }

    public static ProtocolConnect toConnect(GatewayDto gateway) {
        ProtocolConnect connect = new ProtocolConnect();
        BeanUtils.copyProperties(gateway, connect);
        return connect;
    }
}
