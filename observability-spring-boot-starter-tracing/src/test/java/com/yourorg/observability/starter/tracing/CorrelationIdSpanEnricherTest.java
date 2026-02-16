package com.yourorg.observability.starter.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CorrelationIdSpanEnricherTest {

    private final CorrelationIdSpanEnricher enricher = new CorrelationIdSpanEnricher();

    @Test
    void onStartAddsCorrelationIdFromMdc() {
        ReadWriteSpan span = mock(ReadWriteSpan.class);
        MDC.put("correlation_id", "test-corr-id");

        try {
            enricher.onStart(Context.current(), span);

            verify(span).setAttribute("correlation_id", "test-corr-id");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void onStartDoesNothingIfMdcEmpty() {
        ReadWriteSpan span = mock(ReadWriteSpan.class);
        MDC.clear();

        enricher.onStart(Context.current(), span);

        verifyNoInteractions(span);
    }
}
