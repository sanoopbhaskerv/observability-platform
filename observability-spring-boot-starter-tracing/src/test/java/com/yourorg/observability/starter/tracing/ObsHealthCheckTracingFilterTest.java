package com.yourorg.observability.starter.tracing;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ObsHealthCheckTracingFilterTest {

    @Test
    void excludesHealthCheckPaths() {
        ObsHealthCheckTracingFilter filter = new ObsHealthCheckTracingFilter(
                Set.of("/actuator/health", "/actuator/info", "/ping"));

        assertThat(filter.isExcluded("/actuator/health")).isTrue();
        assertThat(filter.isExcluded("/actuator/health/liveness")).isTrue();
        assertThat(filter.isExcluded("/actuator/info")).isTrue();
        assertThat(filter.isExcluded("/ping")).isTrue();
    }

    @Test
    void allowsNonExcludedPaths() {
        ObsHealthCheckTracingFilter filter = new ObsHealthCheckTracingFilter(
                Set.of("/actuator/health", "/ping"));

        assertThat(filter.isExcluded("/api/users")).isFalse();
        assertThat(filter.isExcluded("/hello")).isFalse();
    }

    @Test
    void handlesNullPath() {
        ObsHealthCheckTracingFilter filter = new ObsHealthCheckTracingFilter(
                Set.of("/actuator/health"));

        assertThat(filter.isExcluded(null)).isFalse();
    }
}
