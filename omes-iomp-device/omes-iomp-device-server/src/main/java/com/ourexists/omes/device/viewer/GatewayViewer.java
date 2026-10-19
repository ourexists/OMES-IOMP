/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.device.viewer;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ourexists.era.framework.core.exceptions.BusinessException;
import com.ourexists.era.framework.core.model.dto.IdsDto;
import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.era.framework.orm.mybatisplus.OrmUtils;
import com.ourexists.omes.device.feign.GatewayFeign;
import com.ourexists.omes.device.model.GatewayDto;
import com.ourexists.omes.device.model.GatewayPageQuery;
import com.ourexists.omes.device.pojo.Gateway;
import com.ourexists.omes.device.service.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
public class GatewayViewer implements GatewayFeign {

    @Autowired
    private GatewayService gatewayService;


    @Override
    @PostMapping("selectByPage")
    public JsonResponseEntity<List<GatewayDto>> selectByPage(@RequestBody GatewayPageQuery query) {
        Page<Gateway> page = gatewayService.selectByPage(query);
        return JsonResponseEntity.success(Gateway.covert(page.getRecords()), OrmUtils.extraPagination(page));
    }

    @Override
    public JsonResponseEntity<List<GatewayDto>> selectConnectByProtocol(@RequestParam String protocol) {
        List<Gateway> gateways = gatewayService.getConnectByProtocol(protocol);
        return JsonResponseEntity.success(Gateway.covert(gateways));
    }

    @Override
    public JsonResponseEntity<GatewayDto> selectConnectByName(@RequestParam String serverName) {
        Gateway gateway = gatewayService.getConnect(serverName);
        return JsonResponseEntity.success(Gateway.covert(gateway));
    }

    @Override
    @GetMapping("selectById")
    public JsonResponseEntity<GatewayDto> selectById(@RequestParam String id) {
        return JsonResponseEntity.success(Gateway.covert(gatewayService.getById(id)));
    }

    @Override
    @PostMapping("addOrUpdate")
    public JsonResponseEntity<Boolean> addOrUpdate(@Validated @RequestBody GatewayDto dto) {
        if (dto.getId() != null) {
            Gateway existing = gatewayService.getById(dto.getId());
            if (existing != null && Boolean.TRUE.equals(existing.getEnabled())) {
                throw new BusinessException("请先停用连接");
            }
        }
        gatewayService.saveOrUpdate(Gateway.wrap(dto));
        return JsonResponseEntity.success(true);
    }

    @Override
    @PostMapping("delete")
    public JsonResponseEntity<Boolean> delete(@Validated @RequestBody IdsDto idsDto) {
        if (idsDto.getIds() != null) {
            for (String id : idsDto.getIds()) {
                Gateway existing = gatewayService.getById(id);
                if (existing != null && Boolean.TRUE.equals(existing.getEnabled())) {
                    throw new BusinessException("请先停用连接");
                }
            }
        }
        gatewayService.removeByIds(idsDto.getIds());
        return JsonResponseEntity.success(true);
    }

    @Override
    @GetMapping("start")
    public JsonResponseEntity<Boolean> start(@RequestParam String id) {
        gatewayService.start(id);
        return JsonResponseEntity.success(true);
    }

    @Override
    @GetMapping("stop")
    public JsonResponseEntity<Boolean> stop(@RequestParam String id) {
        gatewayService.stop(id);
        return JsonResponseEntity.success(true);
    }
}
