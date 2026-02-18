package com.yourorg.observability.starter.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obs.metrics")
public class ObsMetricsProperties {
    /**
     * Enable/disable metric export.
     */
    private boolean enabled = false;

    /**
     * Additional metric name prefixes to allow, extending the org-standard
     * deny-list in {@link com.yourorg.observability.contract.ObsMetricPolicy}.
     */
    private java.util.List<String> additionalAllowedPrefixes = java.util.Collections.emptyList();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public java.util.List<String> getAdditionalAllowedPrefixes() {
        return additionalAllowedPrefixes;
    }

    public void setAdditionalAllowedPrefixes(java.util.List<String> additionalAllowedPrefixes) {
        this.additionalAllowedPrefixes = additionalAllowedPrefixes;
    }
}
