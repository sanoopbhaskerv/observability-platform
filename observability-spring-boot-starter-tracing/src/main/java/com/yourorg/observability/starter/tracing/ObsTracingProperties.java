package com.yourorg.observability.starter.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obs.traces")
public class ObsTracingProperties {
    private boolean enabled = true;

    /**
     * Head-sampling ratio (0.0 - 1.0). Prefer tail-sampling at Collector for production cost control.
     */
    private double sampleRate = 0.05;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public double getSampleRate() { return sampleRate; }
    public void setSampleRate(double sampleRate) { this.sampleRate = sampleRate; }
}
