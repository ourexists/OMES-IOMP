/*
 * Copyright (c) 2025. created by ourexists.https://gitee.com/ourexists
 */
package com.ourexists.omes.sas.gateway;

import com.ourexists.era.oauth2.core.gateway.GatewayIdentityHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Proxies non-SAS HTTP requests to OMES-ADMIN so SAS acts as unified API gateway entry.
 * Always injects the internal service key so Admin can trust the hop.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class AdminUpstreamProxyFilter extends OncePerRequestFilter {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private static final Set<String> STRIPPED_ON_PROXY = Set.of(
            "authorization",
            GatewayIdentityHeaders.INTERNAL_SERVICE_KEY.toLowerCase()
    );

    /** 由 SAS 网关统一输出 CORS，避免与 Admin 上游重复导致浏览器拒绝 */
    private static final String CORS_HEADER_PREFIX = "access-control-";

    private final OmesGatewayProperties properties;
    private final GatewayLocalPathMatcher localPathMatcher;
    private final String internalServiceKey;
    private final HttpClient httpClient;

    public AdminUpstreamProxyFilter(OmesGatewayProperties properties,
                                    GatewayLocalPathMatcher localPathMatcher,
                                    @Value("${omes.internal.service-key:d41ab5d5775f90e753d2cba0b3099e6523c285864e64a2ac6c46cbdcdb216091}") String internalServiceKey) {
        this.properties = properties;
        this.localPathMatcher = localPathMatcher;
        this.internalServiceKey = internalServiceKey == null ? "" : internalServiceKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || localPathMatcher.isLocalPath(request, properties)) {
            filterChain.doFilter(request, response);
            return;
        }
        proxy(request, response);
    }

    private void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String adminBase = properties.getAdminUrl().replaceAll("/+$", "");
        String targetUri = adminBase + request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            targetUri = targetUri + "?" + request.getQueryString();
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(targetUri))
                .timeout(Duration.ofSeconds(120))
                .method(request.getMethod(), bodyPublisher(request));

        copyRequestHeaders(request, builder);
        if (StringUtils.hasText(internalServiceKey)) {
            // 网关出口统一盖章，避免依赖上游身份包装链路偶然丢头
            builder.setHeader(GatewayIdentityHeaders.INTERNAL_SERVICE_KEY, internalServiceKey);
        }

        HttpResponse<InputStream> upstream;
        try {
            upstream = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "upstream interrupted");
            return;
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "upstream unavailable");
            return;
        }

        response.setStatus(upstream.statusCode());
        upstream.headers().map().forEach((name, values) -> {
            if (shouldSkipResponseHeader(name)) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
            }
        });

        try (InputStream in = upstream.body(); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    private static HttpRequest.BodyPublisher bodyPublisher(HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readAllBytes();
        if (body.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private static void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (shouldSkipRequestHeader(name)) {
                continue;
            }
            List<String> values = Collections.list(request.getHeaders(name));
            for (String value : values) {
                builder.header(name, value);
            }
        }
    }

    private static boolean shouldSkipRequestHeader(String name) {
        if (name == null) {
            return true;
        }
        String lower = name.toLowerCase();
        return HOP_BY_HOP.contains(lower) || STRIPPED_ON_PROXY.contains(lower);
    }

    private static boolean shouldSkipResponseHeader(String name) {
        if (name == null) {
            return true;
        }
        String lower = name.toLowerCase();
        if (HOP_BY_HOP.contains(lower) || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name)) {
            return true;
        }
        return lower.startsWith(CORS_HEADER_PREFIX);
    }
}
