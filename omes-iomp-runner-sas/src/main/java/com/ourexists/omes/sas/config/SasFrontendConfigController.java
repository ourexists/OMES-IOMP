/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.sas.config;

import com.ourexists.era.oauth2.core.gateway.GatewayIdentityHeaders;
import com.ourexists.omes.sas.gateway.OmesGatewayProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Same-origin gateway entry: OAuth2/captcha use relative paths (empty sasBaseUrl).
 * Baidu map AK is loaded from Admin system config when available.
 */
@RestController
@RequestMapping("/open/frontend-config")
public class SasFrontendConfigController {

    private static final Pattern BAIDU_AK_PATTERN = Pattern.compile("\"baiduMapAk\"\\s*:\\s*\"([^\"]*)\"");

    private final OmesGatewayProperties gatewayProperties;
    private final String internalServiceKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public SasFrontendConfigController(OmesGatewayProperties gatewayProperties,
                                       @Value("${omes.internal.service-key:d41ab5d5775f90e753d2cba0b3099e6523c285864e64a2ac6c46cbdcdb216091}") String internalServiceKey) {
        this.gatewayProperties = gatewayProperties;
        this.internalServiceKey = internalServiceKey == null ? "" : internalServiceKey.trim();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> config() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sasBaseUrl", "");
        result.put("gatewayPort", 9400);
        String ak = fetchBaiduMapAk();
        if (StringUtils.hasText(ak)) {
            result.put("baiduMapAk", ak);
        }
        return result;
    }

    private String fetchBaiduMapAk() {
        try {
            String adminBase = gatewayProperties.getAdminUrl().replaceAll("/+$", "");
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(adminBase + "/expose/frontendConfig"))
                    .timeout(Duration.ofSeconds(5))
                    .GET();
            if (StringUtils.hasText(internalServiceKey)) {
                builder.header(GatewayIdentityHeaders.INTERNAL_SERVICE_KEY, internalServiceKey);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            Matcher matcher = BAIDU_AK_PATTERN.matcher(response.body() == null ? "" : response.body());
            if (matcher.find()) {
                String ak = matcher.group(1);
                return StringUtils.hasText(ak) ? ak.trim() : null;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Admin 未启动或配置表为空时不影响网关启动
        }
        return null;
    }
}
