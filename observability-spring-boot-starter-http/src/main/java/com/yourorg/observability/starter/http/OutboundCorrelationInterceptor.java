package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsMdcKeys;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * Adds X-Correlation-Id to outgoing HTTP requests (useful for legacy hops/log
 * search).
 * Tracing libraries handle traceparent propagation; this is a complementary
 * convenience header.
 */
public class OutboundCorrelationInterceptor implements ClientHttpRequestInterceptor {

    @NonNull
    private final String headerName;

    public OutboundCorrelationInterceptor(@NonNull String headerName) {
        this.headerName = headerName;
    }

    @Override
    @NonNull
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
            @NonNull ClientHttpRequestExecution execution) throws IOException {
        String cid = MDC.get(ObsMdcKeys.CORRELATION_ID);
        if (cid != null && !cid.isBlank() && !request.getHeaders().containsKey(headerName)) {
            // Linter might still complain about 'cid' being nullable despite the check, so
            // we cast it safely
            request.getHeaders().add(headerName, java.util.Objects.requireNonNull(cid));
        }
        return execution.execute(request, body);
    }
}
