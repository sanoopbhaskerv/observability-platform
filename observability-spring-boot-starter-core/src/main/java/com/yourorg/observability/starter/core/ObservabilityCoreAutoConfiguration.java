package com.yourorg.observability.starter.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ObsCoreProperties.class)
@ConditionalOnProperty(prefix = "obs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
public class ObservabilityCoreAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "obs.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(ObsCoreProperties props) {
        FilterRegistrationBean<CorrelationIdFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CorrelationIdFilter(props.getCorrelation().getHeaderName()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
