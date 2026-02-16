package com.yourorg.observability.starter.tracing;

import java.util.Set;

/**
 * Holds the set of URI path prefixes that should be excluded from tracing.
 *
 * <p>
 * Used by the {@code ObservationPredicate} registered in
 * {@link ObservabilityTracingAutoConfiguration} to suppress span creation
 * for noise endpoints (health checks, ping, static assets).
 * </p>
 *
 * <p>
 * Configurable via {@code obs.traces.noise-filter.excluded-paths}.
 * Defaults: {@code /actuator/health}, {@code /actuator/info}, {@code /ping}.
 * </p>
 */
public class ObsHealthCheckTracingFilter {

    private final Set<String> excludedPaths;

    public ObsHealthCheckTracingFilter(Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    /**
     * Returns true if the given URI path should be excluded from tracing.
     */
    public boolean isExcluded(String path) {
        if (path == null)
            return false;
        return excludedPaths.stream().anyMatch(path::startsWith);
    }

    public Set<String> getExcludedPaths() {
        return excludedPaths;
    }
}
