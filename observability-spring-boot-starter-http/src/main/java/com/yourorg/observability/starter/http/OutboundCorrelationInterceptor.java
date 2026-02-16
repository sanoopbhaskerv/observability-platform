package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsMdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Adds X-Correlation-Id to outgoing HTTP requests (useful for legacy hops/log
 * search).
 * Tracing libraries handle traceparent propagation; this is a complementary
 * convenience header.
 */
public class OutboundCorrelationInterceptor implements ClientHttpRequestInterceptor {

    private final String headerName;

    public OutboundCorrelationInterceptor(String headerName) {
        this.headerName = headerName;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String cid = MDC.get(ObsMdcKeys.CORRELATION_ID);
        if (cid != null && !cid.isBlank() && !request.getHeaders().containsKey(headerName)) {
            request.getHeaders().add(headerName, cid);
        }
        return execution.execute(request, body);
    }
}
