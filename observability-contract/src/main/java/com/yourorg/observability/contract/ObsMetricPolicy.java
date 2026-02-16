package com.yourorg.observability.contract;

import java.util.List;
import java.util.Set;

/**
 * Org-wide metric governance policy.
 *
 * <p>
 * Enforced at two layers:
 * </p>
 * <ol>
 * <li><strong>App-side:</strong> {@code starter-metrics} applies a
 * {@code MeterFilter} using these rules</li>
 * <li><strong>Collector-side:</strong> OTel Collector config scrubs again
 * (defense in depth)</li>
 * </ol>
 *
 * <p>
 * Only metrics matching {@link #ALLOWED_PREFIXES} pass through.
 * Metrics with tags in {@link #FORBIDDEN_TAG_KEYS} are denied to prevent
 * high-cardinality explosion.
 * </p>
 */
public final class ObsMetricPolicy {
    private ObsMetricPolicy() {
    }

    /**
     * Metric name prefixes that are allowed through.
     * Any metric not starting with one of these prefixes is denied.
     */
    public static final List<String> ALLOWED_PREFIXES = List.of(
            "http.server.requests",
            "jvm.",
            "db.pool.",
            "custom.business.");

    /**
     * Tag keys that are FORBIDDEN on any metric (high-cardinality risk).
     */
    public static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
            "userId",
            "sessionId",
            "requestId");

    /**
     * Check if a metric name is allowed by the policy.
     */
    public static boolean isAllowed(String metricName) {
        if (metricName == null)
            return false;
        return ALLOWED_PREFIXES.stream().anyMatch(metricName::startsWith);
    }

    /**
     * Check if a tag key is forbidden by the policy.
     */
    public static boolean isForbiddenTag(String tagKey) {
        return FORBIDDEN_TAG_KEYS.contains(tagKey);
    }
}
