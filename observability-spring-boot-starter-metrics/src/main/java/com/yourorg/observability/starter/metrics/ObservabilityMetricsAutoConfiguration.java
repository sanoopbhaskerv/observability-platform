package com.yourorg.observability.starter.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Metrics export is deliberately gated: enable only when your org is ready
 * (cardinality + cost governance).
 * Endpoint and auth are typically injected via env vars / config maps.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsMetricsProperties.class)
@ConditionalOnProperty(prefix = "obs.metrics", name = "enabled", havingValue = "true")
@ConditionalOnClass(OtlpMeterRegistry.class)
public class ObservabilityMetricsAutoConfiguration {

    @Bean
    public OtlpConfig otlpConfig() {
        return new OtlpConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String url() {
                // Override via env var: MANAGEMENT_OTLP_METRICS_EXPORT_URL or OTLP url
                // properties
                // For template purposes, default to local collector; in prod set explicitly.
                return System.getProperty("obs.metrics.otlp.url", "http://localhost:4318/v1/metrics");
            }
        };
    }

    @Bean
    public OtlpMeterRegistry otlpMeterRegistry(OtlpConfig config) {
        return new OtlpMeterRegistry(config, Clock.SYSTEM);
    }
}
