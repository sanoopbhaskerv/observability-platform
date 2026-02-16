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

    /**
     * Enriches every OTel span with {@code correlation_id} from MDC,
     * enabling cross-signal queries (logs ↔ traces by correlation_id).
     */
    @Bean
    @ConditionalOnClass(name = "io.opentelemetry.sdk.trace.SpanProcessor")
    public CorrelationIdSpanEnricher correlationIdSpanEnricher() {
        return new CorrelationIdSpanEnricher();
    }

    /**
     * Suppresses tracing observations for noise endpoints
     * (health checks, ping, static assets).
     */
    @Bean
    @ConditionalOnClass(ObservationPredicate.class)
    public ObservationPredicate obsNoiseFilterPredicate(ObsTracingProperties props) {
        Set<String> excluded = props.getNoiseFilter().getExcludedPaths();
        return (name, context) -> {
            if (context instanceof io.micrometer.observation.Observation.Context) {
                try {
                    String uri = context.getLowCardinalityKeyValue("uri").getValue();
                    return excluded.stream().noneMatch(uri::startsWith);
                } catch (Exception e) {
                    // KeyValue not found — not an HTTP observation, allow it
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
