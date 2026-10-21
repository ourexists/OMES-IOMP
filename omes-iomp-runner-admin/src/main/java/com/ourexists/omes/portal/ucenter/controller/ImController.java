/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.portal.ucenter.controller;

import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.omes.ucenter.feign.ImFeign;
import com.ourexists.omes.ucenter.im.ImDeskSessionVo;
import com.ourexists.omes.ucenter.im.ImMessageVo;
import com.ourexists.omes.ucenter.im.ImSendDto;
import com.ourexists.omes.ucenter.im.ImSessionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "在线客服")
@RestController
@RequestMapping("/im")
public class ImController {

    @Autowired
    private ImFeign imFeign;

    @Operation(summary = "打开与本租户客服的会话")
    @PostMapping("/session")
    public JsonResponseEntity<ImSessionVo> openSession() {
        return imFeign.openSession();
    }

    @Operation(summary = "关闭窗口并断开 IM")
    @PostMapping("/disconnect")
    public JsonResponseEntity<Boolean> closeSession() {
        return imFeign.closeSession();
    }

    @Operation(summary = "当前用户与客服的消息")
    @GetMapping("/messages")
    public JsonResponseEntity<List<ImMessageVo>> messages() {
        return imFeign.messages();
    }

    @Operation(summary = "发给客服")
    @PostMapping("/send")
    public JsonResponseEntity<Boolean> send(@RequestBody ImSendDto dto) {
        return imFeign.send(dto);
    }

    @Operation(summary = "客服工作台会话")
    @GetMapping("/desk/sessions")
    public JsonResponseEntity<List<ImDeskSessionVo>> deskSessions() {
        return imFeign.deskSessions();
    }

    @Operation(summary = "客服工作台消息")
    @GetMapping("/desk/messages")
    public JsonResponseEntity<List<ImMessageVo>> deskMessages(@RequestParam String userAccId) {
        return imFeign.deskMessages(userAccId);
    }

    @Operation(summary = "客服代发")
    @PostMapping("/desk/send")
    public JsonResponseEntity<Boolean> deskSend(@RequestBody ImSendDto dto) {
        return imFeign.deskSend(dto);
    }
}
