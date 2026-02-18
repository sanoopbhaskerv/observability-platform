package com.yourorg.observability.starter.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ObsLoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObsLoggingAutoConfiguration.class));

    @Test
    void autoConfigurationRegistersWhenEnabled() {
        contextRunner
                .withPropertyValues("obs.logging.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(ObsLoggingAutoConfiguration.class));
    }

    @Test
    void autoConfigurationDisabledByProperty() {
        contextRunner
                .withPropertyValues("obs.logging.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(ObsLoggingAutoConfiguration.class));
    }

    @Test
    void autoConfigurationEnabledByDefault() {
        contextRunner
                .run(context -> assertThat(context)
                        .hasSingleBean(ObsLoggingAutoConfiguration.class));
    }
}
