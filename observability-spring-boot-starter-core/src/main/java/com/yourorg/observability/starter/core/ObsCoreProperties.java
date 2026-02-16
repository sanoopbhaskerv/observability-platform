package com.yourorg.observability.starter.core;

import com.yourorg.observability.contract.ObsHeaders;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obs")
public class ObsCoreProperties {
    /**
     * Master switch for the platform starter.
     */
    private boolean enabled = true;

    private final Correlation correlation = new Correlation();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Correlation getCorrelation() { return correlation; }

    public static class Correlation {
        private boolean enabled = true;
        private String headerName = ObsHeaders.CORRELATION_ID;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }
    }
}
