package com.yourorg.observability.contract;

/**
 * Stable MDC keys for log correlation.
 */
public final class ObsMdcKeys {
    private ObsMdcKeys() {}

    public static final String CORRELATION_ID = "correlation_id";
    // trace_id/span_id are commonly injected by tracing libs/agents; keep keys stable.
    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
}
