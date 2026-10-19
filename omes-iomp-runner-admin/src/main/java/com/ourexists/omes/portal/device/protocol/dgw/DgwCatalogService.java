package com.ourexists.omes.portal.device.protocol.dgw;

import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.exceptions.EraCommonException;
import com.ourexists.era.framework.core.utils.RemoteHandleUtils;
import com.ourexists.omes.device.core.equip.protocol.ProtocolConnect;
import com.ourexists.omes.device.feign.GatewayFeign;
import com.ourexists.omes.device.model.GatewayDto;
import com.ourexists.omes.portal.device.gateway.GatewayRuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 按已保存的 DGW 网关连接拉取设备和点位，供绑定界面使用。
 */
@Service
public class DgwCatalogService {

    @Autowired
    private GatewayFeign gatewayFeign;

    @Autowired
    private DgwClient dgwClient;

    public List<DgwDeviceOption> devices(String gatewayId) {
        return dgwClient.listDevices(requireConnect(gatewayId));
    }

    public List<DgwPointOption> points(String gatewayId, String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            throw new BusinessException("请选择 DGW 设备");
        }
        return dgwClient.listPoints(requireConnect(gatewayId), deviceId.trim());
    }

    private ProtocolConnect requireConnect(String gatewayId) {
        if (!StringUtils.hasText(gatewayId)) {
            throw new BusinessException("网关不存在");
        }
        GatewayDto gateway;
        try {
            gateway = RemoteHandleUtils.getDataFormResponse(gatewayFeign.selectById(gatewayId));
        } catch (EraCommonException e) {
            throw new BusinessException(e.getMessage());
        }
        if (gateway == null) {
            throw new BusinessException("网关不存在");
        }
        if (gateway.getProtocol() == null || !"DGW".equalsIgnoreCase(gateway.getProtocol())) {
            throw new BusinessException("当前网关不是 DGW");
        }
        return GatewayRuntimeService.toConnect(gateway);
    }
}
