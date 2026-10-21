/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.viewer;

import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.omes.ucenter.feign.ImFeign;
import com.ourexists.omes.ucenter.im.ImChatService;
import com.ourexists.omes.ucenter.im.ImDeskSessionVo;
import com.ourexists.omes.ucenter.im.ImMessageVo;
import com.ourexists.omes.ucenter.im.ImSendDto;
import com.ourexists.omes.ucenter.im.ImSessionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Component
public class ImViewer implements ImFeign {

    @Autowired
    private ImChatService imChatService;

    @Override
    public JsonResponseEntity<ImSessionVo> openSession() {
        return JsonResponseEntity.success(imChatService.openSession());
    }

    @Override
    public JsonResponseEntity<Boolean> closeSession() {
        imChatService.closeSession();
        return JsonResponseEntity.success(true);
    }

    @Override
    public JsonResponseEntity<List<ImMessageVo>> messages() {
        return JsonResponseEntity.success(imChatService.messages());
    }

    @Override
    public JsonResponseEntity<Boolean> send(@RequestBody ImSendDto dto) {
        imChatService.send(dto == null ? null : dto.getText());
        return JsonResponseEntity.success(true);
    }

    @Override
    public JsonResponseEntity<List<ImDeskSessionVo>> deskSessions() {
        return JsonResponseEntity.success(imChatService.deskSessions());
    }

    @Override
    public JsonResponseEntity<List<ImMessageVo>> deskMessages(@RequestParam String userAccId) {
        return JsonResponseEntity.success(imChatService.deskMessages(userAccId));
    }

    @Override
    public JsonResponseEntity<Boolean> deskSend(@RequestBody ImSendDto dto) {
        imChatService.deskSend(dto == null ? null : dto.getUserAccId(), dto == null ? null : dto.getText());
        return JsonResponseEntity.success(true);
    }
}
