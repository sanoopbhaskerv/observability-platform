package com.yourorg.observability.starter.tracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Bridges {@code obs.traces.sample-rate} →
 * {@code management.tracing.sampling.probability}
 * so that the org-wide config key controls Spring Boot's native sampling.
 *
 * <p>
 * Registered as a bean by {@link ObservabilityTracingAutoConfiguration}.
 * </p>
 */
public class ObsTracingSamplingInitializer implements EnvironmentPostProcessor {

    private final double sampleRate;

    public ObsTracingSamplingInitializer(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.containsProperty("management.tracing.sampling.probability")) {
            environment.getPropertySources().addLast(
                    new MapPropertySource("obs-tracing-defaults",
                            Map.of("management.tracing.sampling.probability", sampleRate)));
        }
    }
}
