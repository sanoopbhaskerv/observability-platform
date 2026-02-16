package com.yourorg.observability.starter.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "obs.traces")
public class ObsTracingProperties {
    private boolean enabled = true;

    /**
     * Head-sampling ratio (0.0 - 1.0). Prefer tail-sampling at Collector for
     * production cost control.
     */
    private double sampleRate = 0.05;

    private final NoiseFilter noiseFilter = new NoiseFilter();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public NoiseFilter getNoiseFilter() {
        return noiseFilter;
    }

    public static class NoiseFilter {
        /**
         * URI path prefixes to exclude from tracing (reduces noise and cost).
         */
        private Set<String> excludedPaths = new LinkedHashSet<>(Set.of(
                "/actuator/health",
                "/actuator/info",
                "/ping"));

        public Set<String> getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(Set<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
    }
}
