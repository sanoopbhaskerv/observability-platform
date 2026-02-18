
package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsHeaders;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient correlation propagation.
 * Separated from {@link ObservabilityHttpAutoConfiguration} to prevent class
 * loading issues
 * when spring-webflux is not present.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsHttpProperties.class)
@ConditionalOnProperty(prefix = "obs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(WebClient.class)
public class ObservabilityWebClientAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "obs.http", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OutboundCorrelationExchangeFilter observabilityWebClientFilter() {
        return new OutboundCorrelationExchangeFilter(ObsHeaders.CORRELATION_ID);
    }
}
