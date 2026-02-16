package com.yourorg.observability.starter.tracing;

import com.yourorg.observability.contract.ObsMdcKeys;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.slf4j.MDC;

/**
 * Enriches every span with the org's {@code correlation_id} from MDC.
 *
 * <p>
 * This bridges the gap between log correlation (MDC-based) and trace
 * correlation (span attributes), enabling queries like
 * "find the trace for correlation_id = abc-123" in Grafana/Tempo.
 * </p>
 */
public class CorrelationIdSpanEnricher implements SpanProcessor {

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String cid = MDC.get(ObsMdcKeys.CORRELATION_ID);
        if (cid != null && !cid.isBlank()) {
            span.setAttribute("correlation_id", cid);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // no-op
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
