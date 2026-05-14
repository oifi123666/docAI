package com.javaee.aiservice.aiops;

import com.javaee.aiservice.aiops.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class AIOpsMetricsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AIOpsMetricsFilter.class);

    @Autowired
    private MonitoringService monitoringService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (!uri.startsWith("/api/ai/") || uri.startsWith("/api/ai/aiops/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        boolean hasError = false;

        try {
            filterChain.doFilter(request, response);

            int status = response.getStatus();
            if (status >= 400) {
                hasError = true;
            }
        } catch (Exception e) {
            hasError = true;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            monitoringService.incrementCounter("ai.requests");
            monitoringService.recordTimer("ai.request", duration);

            if (hasError) {
                monitoringService.incrementCounter("ai.errors");
            }

            log.debug("AI请求指标: uri={}, method={}, duration={}ms, error={}",
                    uri, method, duration, hasError);
        }
    }
}