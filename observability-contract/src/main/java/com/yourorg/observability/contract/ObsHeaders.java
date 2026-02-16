package com.yourorg.observability.contract;

/**
 * Stable, org-wide header contract. Keep this vendor-neutral.
 */
public final class ObsHeaders {
    private ObsHeaders() {}

    public static final String TRACEPARENT = "traceparent"; // W3C Trace Context
    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String CLIENT_REQUEST_ID = "X-Client-Request-Id";
    public static final String SESSION_ID = "X-Session-Id"; // optional bridge (e.g., Glassbox)
}
