package com.yourorg.observability.starter.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "obs.http")
public class ObsHttpProperties {
    private boolean enabled = true;

    /**
     * When true, propagate correlation header on outbound HTTP calls (RestClient/RestTemplate).
     */
    private boolean propagateCorrelationId = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isPropagateCorrelationId() { return propagateCorrelationId; }
    public void setPropagateCorrelationId(boolean propagateCorrelationId) { this.propagateCorrelationId = propagateCorrelationId; }
}
