package com.yourorg.observability.starter.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityCoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityCoreAutoConfiguration.class));

    @Test
    void filterBeanCreatedByDefault() {
        runner.run(context -> {
            assertThat(context).hasBean("correlationIdFilter");
            assertThat(context).hasSingleBean(FilterRegistrationBean.class);
        });
    }

    @Test
    void filterBeanDisabledWhenObsDisabled() {
        runner.withPropertyValues("obs.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("correlationIdFilter");
                });
    }

    @Test
    void filterBeanDisabledWhenCorrelationDisabled() {
        runner.withPropertyValues("obs.correlation.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("correlationIdFilter");
                });
    }
}
