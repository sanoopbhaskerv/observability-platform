package com.yourorg.observability.starter.tracing;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Tracing module — adds org-standard customizations on top of Spring Boot's
 * built-in Micrometer + OTel auto-configuration.
 *
 * <p>
 * Spring Boot already provides the {@link Tracer} and export pipeline.
 * This module adds:
 * </p>
 * <ul>
 * <li>Configurable head-sampling via {@code obs.traces.sample-rate} (default
 * 0.05)</li>
 * <li>Feature toggle via {@code obs.traces.enabled}</li>
 * </ul>
 *
 * <p>
 * For production, prefer tail-sampling at the OTel Collector level.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsTracingProperties.class)
@ConditionalOnProperty(prefix = "obs.traces", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(Tracer.class)
public class ObservabilityTracingAutoConfiguration {

    /**
     * Sets the head-sampling probability via
     * {@code management.tracing.sampling.probability}.
     * This bridges the org-level config key ({@code obs.traces.sample-rate}) to
     * Spring Boot's
     * native tracing property, keeping a single config surface for consumers.
     */
    @Bean
    public ObsTracingSamplingInitializer obsTracingSamplingInitializer(ObsTracingProperties props) {
        return new ObsTracingSamplingInitializer(props.getSampleRate());
    }
}
