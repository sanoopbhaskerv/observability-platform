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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Only set default if not already set by use
        if (environment.containsProperty("management.tracing.sampling.probability")) {
            return;
        }

        // Bridge obs.traces.sample-rate (default 0.05) to Spring Boot's property
        Double sampleRate = environment.getProperty("obs.traces.sample-rate", Double.class, 0.05);

        environment.getPropertySources().addLast(
                new MapPropertySource("obs-tracing-defaults",
                        Map.of("management.tracing.sampling.probability", sampleRate)));
    }
}
