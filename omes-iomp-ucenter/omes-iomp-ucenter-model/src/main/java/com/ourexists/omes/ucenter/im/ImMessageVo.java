/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImMessageVo {

    private String sendId;

    private String senderName;

    private String text;

    private long sendTime;

    /** true when the service account sent it */
    private boolean fromService;
}
