package com.yourorg.observability.starter.metrics;

import com.yourorg.observability.contract.ObsMetricPolicy;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Metrics module — adds org-standard common tags and metric governance
 * on top of Spring Boot's built-in OTLP metrics auto-configuration.
 *
 * <p>
 * Spring Boot 3.x already auto-configures {@code OtlpMeterRegistry} via
 * {@code management.otlp.metrics.export.*} properties. This module adds:
 * </p>
 * <ul>
 * <li>Common tags ({@code service.name}, {@code env}) applied to all
 * meters</li>
 * <li>Metric governance via {@link ObsMetricPolicy} (deny-list
 * enforcement)</li>
 * <li>Feature toggle via {@code obs.metrics.enabled} (default: false /
 * opt-in)</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsMetricsProperties.class)
@ConditionalOnProperty(prefix = "obs.metrics", name = "enabled", havingValue = "true")
@ConditionalOnClass(MeterRegistry.class)
public class ObservabilityMetricsAutoConfiguration {

    /**
     * Adds org-standard common tags to every meter.
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

    /**
     * Enforces the org's metric governance policy ({@link ObsMetricPolicy}).
     *
     * <ul>
     * <li>Denies metrics with forbidden tag keys (userId, sessionId,
     * requestId)</li>
     * <li>Denies metrics whose names don't match allowed prefixes</li>
     * </ul>
     *
     * <p>
     * This is the app-layer first line of defense. The Collector provides
     * the second layer (defense in depth).
     * </p>
     */
    @Bean
    public MeterFilter obsMetricPolicyFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                // Deny metrics with forbidden tag keys
                for (Tag tag : id.getTags()) {
                    if (ObsMetricPolicy.isForbiddenTag(tag.getKey())) {
                        return MeterFilterReply.DENY;
                    }
                }

                // Deny metrics not matching allowed prefixes
                if (!ObsMetricPolicy.isAllowed(id.getName())) {
                    return MeterFilterReply.DENY;
                }

                return MeterFilterReply.NEUTRAL;
            }
        };
    }
}
