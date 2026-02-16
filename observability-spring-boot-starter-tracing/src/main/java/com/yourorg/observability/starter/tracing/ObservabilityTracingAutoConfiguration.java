package com.yourorg.observability.starter.tracing;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * This module intentionally keeps code-light:
 * - Boot + Micrometer auto-config provides the Tracer.
 * - Export is controlled via OTEL_* env vars and/or spring
 * management.tracing.*.
 * Your policy: treat tracing as a capability toggle, not vendor binding.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsTracingProperties.class)
@ConditionalOnProperty(prefix = "obs.traces", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(Tracer.class)
public class ObservabilityTracingAutoConfiguration {
    // Intentionally empty; existence of this module is the adoption “knob”.
    // You can add advanced customization here later (span naming, attribute
    // allowlists, etc.).
}
