package com.yourorg.observability.contract;

/**
 * Required log schema fields for structured JSON logging.
 * Every log line emitted by org services MUST include these fields.
 *
 * <p>
 * Fields like {@code trace_id}, {@code span_id}, and {@code correlation_id}
 * are injected via MDC. Framework fields ({@code timestamp}, {@code level})
 * are provided by the logging encoder. Application-level fields
 * ({@code service}, {@code env}, {@code version}) are configured via
 * Spring properties.
 * </p>
 */
public final class ObsLogFields {
    private ObsLogFields() {
    }

    // --- Framework-provided (by Logback encoder) ---
    public static final String TIMESTAMP = "timestamp";
    public static final String LEVEL = "level";

    // --- Application-provided (via Spring properties → MDC or encoder config) ---
    public static final String SERVICE = "service";
    public static final String ENV = "env";
    public static final String VERSION = "version";

    // --- MDC-injected (by correlation filter + tracing library) ---
    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    public static final String CORRELATION_ID = "correlation_id";

    // --- HTTP context (populated per-request via servlet filter/interceptor) ---
    public static final String HTTP_METHOD = "http.method";
    public static final String HTTP_ROUTE = "http.route";
    public static final String HTTP_STATUS_CODE = "http.status_code";
    public static final String DURATION_MS = "duration_ms";
}
