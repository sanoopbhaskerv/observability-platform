package com.yourorg.observability.starter.tracing;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Set;

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
 * <li>Span enrichment with {@code correlation_id} from MDC</li>
 * <li>Noise filtering for health check endpoints</li>
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
     * Suppresses tracing observations for noise endpoints
     * (health checks, ping, static assets).
     */
    @Bean
    @ConditionalOnClass(ObservationPredicate.class)
    public ObservationPredicate obsNoiseFilterPredicate(ObsHealthCheckTracingFilter filter) {
        return (name, context) -> {
            if (context instanceof io.micrometer.observation.Observation.Context) {
                try {
                    String uri = context.getLowCardinalityKeyValue("uri").getValue();
                    return !filter.isExcluded(uri);
                } catch (Exception e) {
                    // KeyValue not found (e.g. non-HTTP observation) or other issue
                    return true;
                }
            }
            return true;
        };
    }

    /**
     * Exposes the noise filter so other modules can query excluded paths.
     */
    @Bean
    public ObsHealthCheckTracingFilter obsHealthCheckTracingFilter(ObsTracingProperties props) {
        return new ObsHealthCheckTracingFilter(props.getNoiseFilter().getExcludedPaths());
    }
}
