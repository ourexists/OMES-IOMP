/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImSendDto {

    private String text;

    /** desk only: the user being replied to */
    private String userAccId;
}
