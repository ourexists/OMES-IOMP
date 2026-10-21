/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.ucenter.im;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class WuKongImProperties {

    @Value("${omes.im.enabled:false}")
    private boolean enabled;

    @Value("${omes.im.api-url:http://127.0.0.1:5001}")
    private String apiUrl;

    /** WuKongIM managerToken. Sent as the HTTP token header. */
    @Value("${omes.im.secret:}")
    private String secret;
}
