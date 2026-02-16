package com.yourorg.observability.starter.core;

import com.yourorg.observability.contract.CorrelationId;
import com.yourorg.observability.contract.ObsMdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures every request has a correlation id (even if tracing is disabled).
 * Also echoes it back in the response header for client-side reuse.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {
    private final String headerName;

    public CorrelationIdFilter(String headerName) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = CorrelationId.fromHeaderOrNew(request.getHeader(headerName));
        MDC.put(ObsMdcKeys.CORRELATION_ID, correlationId);
        response.setHeader(headerName, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ObsMdcKeys.CORRELATION_ID);
        }
    }
}
