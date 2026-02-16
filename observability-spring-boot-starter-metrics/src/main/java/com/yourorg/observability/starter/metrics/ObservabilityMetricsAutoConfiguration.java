package com.yourorg.observability.starter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Metrics module — adds org-standard common tags on top of Spring Boot's
 * built-in OTLP metrics auto-configuration.
 *
 * <p>
 * Spring Boot 3.x already auto-configures {@code OtlpMeterRegistry} via
 * {@code management.otlp.metrics.export.*} properties. This module adds:
 * </p>
 * <ul>
 * <li>Common tags ({@code service.name}, {@code env}) applied to all
 * meters</li>
 * <li>Feature toggle via {@code obs.metrics.enabled} (default: false /
 * opt-in)</li>
 * </ul>
 *
 * <p>
 * Configure the OTLP endpoint via Spring Boot's native property:
 * {@code management.otlp.metrics.export.url=http://collector:4318/v1/metrics}
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsMetricsProperties.class)
@ConditionalOnProperty(prefix = "obs.metrics", name = "enabled", havingValue = "true")
@ConditionalOnClass(MeterRegistry.class)
public class ObservabilityMetricsAutoConfiguration {

    /**
     * Adds org-standard common tags to every meter. These tags enable consistent
     * filtering/grouping across services in Grafana dashboards.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> obsCommonTagsCustomizer(
            @Value("${spring.application.name:unknown}") String appName,
            @Value("${obs.metrics.env:dev}") String env) {
        return registry -> registry.config()
                .commonTags(
                        "service.name", appName,
                        "env", env);
    }
}
