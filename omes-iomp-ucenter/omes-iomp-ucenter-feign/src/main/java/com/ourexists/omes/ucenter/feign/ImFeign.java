/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.feign;

import com.ourexists.era.framework.core.model.vo.JsonResponseEntity;
import com.ourexists.omes.ucenter.im.ImDeskSessionVo;
import com.ourexists.omes.ucenter.im.ImMessageVo;
import com.ourexists.omes.ucenter.im.ImSendDto;
import com.ourexists.omes.ucenter.im.ImSessionVo;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface ImFeign {

    JsonResponseEntity<ImSessionVo> openSession();

    JsonResponseEntity<Boolean> closeSession();

    JsonResponseEntity<List<ImMessageVo>> messages();

    JsonResponseEntity<Boolean> send(@RequestBody ImSendDto dto);

    JsonResponseEntity<List<ImDeskSessionVo>> deskSessions();

    JsonResponseEntity<List<ImMessageVo>> deskMessages(@RequestParam String userAccId);

    JsonResponseEntity<Boolean> deskSend(@RequestBody ImSendDto dto);
}
