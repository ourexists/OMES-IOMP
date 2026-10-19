/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.portal.device.controller;

import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.exceptions.EraCommonException;
import com.ourexists.era.framework.core.model.dto.IdsDto;
import com.ourexists.era.framework.core.model.dto.MapDto;
import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.era.framework.core.utils.CollectionUtil;
import com.ourexists.era.framework.core.utils.RemoteHandleUtils;
import com.ourexists.omes.device.core.equip.protocol.ProtocolManager;
import com.ourexists.omes.device.feign.GatewayFeign;
import com.ourexists.omes.device.model.GatewayDto;
import com.ourexists.omes.device.model.GatewayPageQuery;
import com.ourexists.omes.portal.device.gateway.GatewayRuntimeService;
import com.ourexists.omes.portal.device.protocol.dgw.DgwCatalogService;
import com.ourexists.omes.portal.device.protocol.dgw.DgwDeviceOption;
import com.ourexists.omes.portal.device.protocol.dgw.DgwPointOption;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Tag(name = "连接管理")
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Autowired
    private GatewayFeign gatewayFeign;

    @Autowired
    private List<ProtocolManager> protocolManagers;

    @Autowired
    private GatewayRuntimeService gatewayRuntimeService;

    @Autowired
    private DgwCatalogService dgwCatalogService;

    @Operation(summary = "分页查询", description = "连接分页列表")
    @PostMapping("selectByPage")
    public JsonResponseEntity<List<GatewayDto>> selectByPage(@RequestBody GatewayPageQuery query) {
        JsonResponseEntity<List<GatewayDto>> response = gatewayFeign.selectByPage(query);
        if (response != null && response.getData() != null) {
            for (GatewayDto item : response.getData()) {
                gatewayRuntimeService.fillLink(item);
            }
        }
        return response;
    }

    @Operation(summary = "按ID查询", description = "单条连接详情")
    @GetMapping("selectById")
    public JsonResponseEntity<GatewayDto> selectById(@RequestParam String id) {
        return gatewayFeign.selectById(id);
    }

    @Operation(summary = "新增或修改", description = "已启用的连接拒绝修改")
    @PostMapping("addOrUpdate")
    public JsonResponseEntity<Boolean> addOrUpdate(@Validated @RequestBody GatewayDto dto) {
        return gatewayFeign.addOrUpdate(dto);
    }

    @Operation(summary = "删除", description = "已启用的连接拒绝删除")
    @PostMapping("delete")
    public JsonResponseEntity<Boolean> delete(@Validated @RequestBody IdsDto idsDto) {
        return gatewayFeign.delete(idsDto);
    }

    @Operation(summary = "DGW 设备列表", description = "用该网关保存的地址和账号查询 DGW 设备，供绑定")
    @GetMapping("dgw/devices")
    public JsonResponseEntity<List<DgwDeviceOption>> dgwDevices(@RequestParam String id) {
        return JsonResponseEntity.success(dgwCatalogService.devices(id));
    }

    @Operation(summary = "DGW 点位列表", description = "查询指定 DGW 设备的读取参数与控制点位")
    @GetMapping("dgw/points")
    public JsonResponseEntity<List<DgwPointOption>> dgwPoints(@RequestParam String id,
                                                              @RequestParam String deviceId) {
        return JsonResponseEntity.success(dgwCatalogService.points(id, deviceId));
    }

    @Operation(summary = "协议枚举列表", description = "返回所有支持的协议类型，用于下拉/配置")
    @GetMapping("protocols")
    public JsonResponseEntity<List<MapDto>> getProtocols() {
        List<MapDto> list = new ArrayList<>();
        for (ProtocolManager protocolManager : protocolManagers) {
            list.add(new MapDto().setId(protocolManager.protocol()).setName(protocolManager.protocol()));
        }
        return JsonResponseEntity.success(list);
    }

    @Operation(summary = "按协议查询连接列表", description = "返回指定协议下的所有连接，protocol 取 protocols 接口的 id")
    @GetMapping("getByProtocol")
    public JsonResponseEntity<List<GatewayDto>> getByProtocol(@RequestParam String protocol) {
        try {
            List<GatewayDto> connects = RemoteHandleUtils.getDataFormResponse(
                    gatewayFeign.selectConnectByProtocol(protocol));
            return JsonResponseEntity.success(connects != null ? connects : new ArrayList<>());
        } catch (EraCommonException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Operation(summary = "OPC UA 连接列表（兼容）", description = "返回 OPC UA 协议下的连接，用于下拉选择服务名")
    @GetMapping("getAll")
    public JsonResponseEntity<List<MapDto>> getAll() {
        List<GatewayDto> connects;
        try {
            connects = RemoteHandleUtils.getDataFormResponse(gatewayFeign.selectConnectByProtocol("opc_ua"));
        } catch (EraCommonException e) {
            throw new BusinessException(e.getMessage());
        }
        List<MapDto> r = new ArrayList<>();
        if (CollectionUtil.isNotBlank(connects)) {
            for (GatewayDto connect : connects) {
                r.add(new MapDto().setId(connect.getServerName()).setName(connect.getServerName()));
            }
        }
        return JsonResponseEntity.success(r);
    }

    @Operation(summary = "启用", description = "启用")
    @GetMapping("start")
    public JsonResponseEntity<Boolean> start(@RequestParam String id) {
        JsonResponseEntity<Boolean> response = gatewayFeign.start(id);
        GatewayDto gateway;
        try {
            gateway = RemoteHandleUtils.getDataFormResponse(gatewayFeign.selectById(id));
        } catch (EraCommonException e) {
            throw new BusinessException(e.getMessage());
        }
        if (!gatewayRuntimeService.start(gateway)) {
            gatewayFeign.stop(id);
            throw new BusinessException(startFailure(gateway));
        }
        return response;
    }

    private String startFailure(GatewayDto gateway) {
        String reason = gatewayRuntimeService.lastLinkMessage(gateway);
        return StringUtils.hasText(reason) ? reason : "网关连接未建立";
    }

    @Operation(summary = "停用", description = "停用")
    @GetMapping("stop")
    public JsonResponseEntity<Boolean> stop(@RequestParam String id) {
        try {
            GatewayDto gateway = RemoteHandleUtils.getDataFormResponse(gatewayFeign.selectById(id));
            gatewayRuntimeService.stop(gateway);
        } catch (EraCommonException e) {
            throw new BusinessException(e.getMessage());
        }
        return gatewayFeign.stop(id);
    }
}
