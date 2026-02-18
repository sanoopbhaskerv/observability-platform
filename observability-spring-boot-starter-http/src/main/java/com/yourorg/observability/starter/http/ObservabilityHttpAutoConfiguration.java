package com.yourorg.observability.starter.http;

import com.yourorg.observability.contract.ObsHeaders;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Outbound correlation propagation for both RestTemplate and WebClient.
 *
 * <ul>
 * <li>RestTemplate — always available (spring-web)</li>
 * <li>WebClient — activates only when spring-webflux is on the classpath</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsHttpProperties.class)
@ConditionalOnProperty(prefix = "obs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(RestTemplate.class)
public class ObservabilityHttpAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "obs.http", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RestTemplateCustomizer observabilityRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors()
                .add(new OutboundCorrelationInterceptor(ObsHeaders.CORRELATION_ID));
    }
}
