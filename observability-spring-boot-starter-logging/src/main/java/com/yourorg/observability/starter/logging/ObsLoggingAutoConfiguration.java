package com.yourorg.observability.starter.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import jakarta.annotation.PostConstruct;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Structured logging auto-configuration.
 *
 * <p>
 * When {@code obs.logging.format=json} (default), this reconfigures Logback's
 * root logger to use a {@link LogstashEncoder} that outputs structured JSON
 * matching the org's log schema contract ({@code ObsLogFields}).
 * </p>
 *
 * <p>
 * MDC fields ({@code correlation_id}, {@code trace_id}, {@code span_id})
 * are automatically included by the encoder. Application metadata
 * ({@code service}, {@code env}, {@code version}) is injected as custom fields.
 * </p>
 *
 * <p>
 * <strong>Zero OTLP dependency for logs.</strong> Logs go to stdout as JSON →
 * picked up by FluentBit/Filebeat at the infrastructure layer.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObsLoggingProperties.class)
@ConditionalOnProperty(prefix = "obs.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(LogstashEncoder.class)
public class ObsLoggingAutoConfiguration {

    private final ObsLoggingProperties props;
    private final String serviceName;
    private final String env;
    private final String version;

    public ObsLoggingAutoConfiguration(
            ObsLoggingProperties props,
            @Value("${spring.application.name:unknown}") String serviceName,
            @Value("${obs.logging.env:${obs.metrics.env:dev}}") String env,
            @Value("${spring.application.version:${obs.logging.version:unknown}}") String version) {
        this.props = props;
        this.serviceName = serviceName;
        this.env = env;
        this.version = version;
    }

    @PostConstruct
    public void configureStructuredLogging() {
        if (!"json".equalsIgnoreCase(props.getFormat())) {
            return; // Keep default text format for development
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // Create JSON encoder with org-standard field names
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);

        // Map standard fields to match ObsLogFields contract
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setTimestamp("timestamp");
        fieldNames.setLevel("level");
        fieldNames.setLogger("logger");
        fieldNames.setThread("thread");
        fieldNames.setMessage("message");
        fieldNames.setStackTrace("stack_trace");
        encoder.setFieldNames(fieldNames);

        // Inject application metadata as custom fields
        encoder.setCustomFields(String.format(
                "{\"service\":\"%s\",\"env\":\"%s\",\"version\":\"%s\"}",
                serviceName, env, version));

        // Include MDC fields (correlation_id, trace_id, span_id) automatically
        encoder.setIncludeMdcKeyNames(java.util.List.of(
                "correlation_id", "trace_id", "span_id",
                "http.method", "http.route", "http.status_code", "duration_ms"));

        encoder.start();

        // Replace root logger's appender with structured JSON
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setName("OBS_JSON_CONSOLE");
        appender.start();

        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(appender);
    }
}
