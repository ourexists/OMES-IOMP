/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.device.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
public class GatewayDto {

    protected String id;

    protected String createdBy;

    protected String createdId;

    protected Date createdTime;

    protected String updatedBy;

    protected String updatedId;

    protected Date updatedTime;
    /**
     * 服务名
     */
    private String serverName;

    /**
     * ip
     */
    private String uri;

    private String topic;

    private String protocol;

    private String params;

    private Boolean enabled;

    /** 采集 cron 表达式 */
    private String collectCron;

    private Integer validType;

    private String username;

    private String password;

    private String tenantId;

    /** 运行时连通：up / down / pending。仅 Admin 列表填充，不入库。 */
    private String linkState;

    /** 连通异常说明。仅 Admin 列表填充，不入库。 */
    private String linkMessage;
}
