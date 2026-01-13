package com.imdmanuel.book_library.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1) // Execute early in the filter chain
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Instant startTime = Instant.now();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = getClientIpAddress(request);

        // Log incoming request to console
        if (queryString == null) {
            logger.info("→ {} {} from {}", method, uri, clientIp);
        } else {
            logger.info("→ {} {}?{} from {}", method, uri, queryString, clientIp);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            Duration duration = Duration.between(startTime, Instant.now());
            int status = response.getStatus();
            logger.info("← {} {} - Status: {} - Duration: {}ms", method, uri, status, duration.toMillis());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Optionally exclude static resources from logging
        return path.startsWith("/actuator")
                || path.equals("/favicon.ico")
                || path.startsWith("/webjars");
    }
}
