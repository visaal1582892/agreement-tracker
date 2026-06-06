package com.medplus.agreement_tracker_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/actuator") || uri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("HTTP {} {} failed before response (requestId={})", request.getMethod(), buildUri(request), requestId, ex);
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            logHttpOutcome(request, response, durationMs, requestId);
            MDC.remove("requestId");
        }
    }

    private void logHttpOutcome(HttpServletRequest request, HttpServletResponse response, long durationMs, String requestId) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = buildUri(request);
        String user = resolveUsername();

        if (status >= 500) {
            log.error("HTTP {} {} -> {} {}ms user={} requestId={}", method, uri, status, durationMs, user, requestId);
        } else if (status >= 400) {
            log.warn("HTTP {} {} -> {} {}ms user={} requestId={}", method, uri, status, durationMs, user, requestId);
        } else if (log.isDebugEnabled()) {
            log.debug("HTTP {} {} -> {} {}ms user={} requestId={}", method, uri, status, durationMs, user, requestId);
        } else {
            log.info("HTTP {} {} -> {} {}ms user={}", method, uri, status, durationMs, user);
        }
    }

    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            return uri + "?" + request.getQueryString();
        }
        return uri;
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        String name = authentication.getName();
        return StringUtils.hasText(name) ? name : "anonymous";
    }
}
