package com.yourorg.observability.starter.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obs.logging")
public class ObsLoggingProperties {

    /**
     * Enable/disable structured logging auto-configuration.
     */
    private boolean enabled = true;

    public enum LogFormat {
        JSON, TEXT
    }

    /**
     * Log output format: "json" for structured JSON (production), "text" for
     * human-readable console output (development).
     */
    private LogFormat format = LogFormat.JSON;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LogFormat getFormat() { return format; }
    public void setFormat(LogFormat format) { this.format = format; }
}
