/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ourexists.era.framework.orm.mybatisplus.MainEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
@TableName("t_ucenter_im_service")
public class ImServiceAccount extends MainEntity {

    private String tenantId;

    private String imUserId;

    private Integer syncStatus;

    private String lastError;

    private Date lastSyncTime;
}
