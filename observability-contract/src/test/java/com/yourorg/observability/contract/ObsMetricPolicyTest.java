package com.yourorg.observability.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObsMetricPolicyTest {

    @Test
    void allowedPrefixesPassThrough() {
        assertThat(ObsMetricPolicy.isAllowed("http.server.requests")).isTrue();
        assertThat(ObsMetricPolicy.isAllowed("jvm.memory.used")).isTrue();
        assertThat(ObsMetricPolicy.isAllowed("db.pool.active")).isTrue();
        assertThat(ObsMetricPolicy.isAllowed("custom.business.orders")).isTrue();
    }

    @Test
    void unallowedPrefixesDenied() {
        assertThat(ObsMetricPolicy.isAllowed("system.cpu.usage")).isFalse();
        assertThat(ObsMetricPolicy.isAllowed("random.metric")).isFalse();
        assertThat(ObsMetricPolicy.isAllowed(null)).isFalse();
    }

    @Test
    void forbiddenTagKeysDetected() {
        assertThat(ObsMetricPolicy.isForbiddenTag("userId")).isTrue();
        assertThat(ObsMetricPolicy.isForbiddenTag("sessionId")).isTrue();
        assertThat(ObsMetricPolicy.isForbiddenTag("requestId")).isTrue();
    }

    @Test
    void allowedTagKeysPass() {
        assertThat(ObsMetricPolicy.isForbiddenTag("env")).isFalse();
        assertThat(ObsMetricPolicy.isForbiddenTag("service.name")).isFalse();
    }
}
