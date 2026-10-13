/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */

package com.ourexists.omes.sas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.PropertySource;

/**
 * OMES 认证服务器（Security Authentication Server）。
 * 提供 OAuth2 令牌签发、验证码登录、账户注册与密码变更等能力。
 */
@SpringBootApplication(scanBasePackages = "com.ourexists.omes.sas")
@EnableFeignClients(basePackages = "com.ourexists.omes.sas.remote")
@PropertySource(value = {"file:config/sas.properties"})
public class SasApp {

    public static void main(String[] args) {
        SpringApplication.run(SasApp.class, args);
    }
}
