package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsMdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboundCorrelationInterceptorTest {

    private final OutboundCorrelationInterceptor interceptor = new OutboundCorrelationInterceptor("X-Correlation-Id");

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void addsCorrelationHeaderFromMdc() throws Exception {
        MDC.put(ObsMdcKeys.CORRELATION_ID, "outbound-123");

        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(java.util.Objects.requireNonNull(URI.create("http://downstream/api")));
        request.setMethod(java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.GET));
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, body, java.util.Objects.requireNonNull(execution));

        assertThat(request.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("outbound-123");
        verify(execution).execute(request, body);
    }

    @Test
    void handlesEmptyMdcGracefully() throws Exception {
        // No MDC set — should not crash
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(java.util.Objects.requireNonNull(URI.create("http://downstream/api")));
        request.setMethod(java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.GET));
        byte[] body = new byte[0];
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, body, java.util.Objects.requireNonNull(execution));

        // Header should not be set when MDC is empty
        assertThat(request.getHeaders().get("X-Correlation-Id")).isNull();
        verify(execution).execute(request, body);
    }
}
