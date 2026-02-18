package com.yourorg.observability.starter.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.yourorg.observability.contract.ObsLogFields;
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
        if (props.getFormat() != ObsLoggingProperties.LogFormat.JSON) {
            return; // Keep default text format for development
        }

        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

            // Create JSON encoder with org-standard field names
            LogstashEncoder encoder = new LogstashEncoder();
            encoder.setContext(context);

            // Map standard fields to match ObsLogFields contract
            LogstashFieldNames fieldNames = new LogstashFieldNames();
            fieldNames.setTimestamp(ObsLogFields.TIMESTAMP);
            fieldNames.setLevel(ObsLogFields.LEVEL);
            fieldNames.setLogger("logger");
            fieldNames.setThread("thread");
            fieldNames.setMessage("message");
            fieldNames.setStackTrace("stack_trace");
            encoder.setFieldNames(fieldNames);

            // Inject application metadata as custom fields using Jackson for safety
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
            node.put(ObsLogFields.SERVICE, serviceName);
            node.put(ObsLogFields.ENV, env);
            node.put(ObsLogFields.VERSION, version);
            encoder.setCustomFields(node.toString());

            // Include MDC fields (correlation_id, trace_id, span_id) automatically
            encoder.setIncludeMdcKeyNames(java.util.List.of(
                    ObsLogFields.CORRELATION_ID, ObsLogFields.TRACE_ID, ObsLogFields.SPAN_ID,
                    ObsLogFields.HTTP_METHOD, ObsLogFields.HTTP_ROUTE, ObsLogFields.HTTP_STATUS_CODE, ObsLogFields.DURATION_MS));

            encoder.start();

            // Replace root logger's appender with structured JSON
            ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setContext(context);
            appender.setEncoder(encoder);
            appender.setName("OBS_JSON_CONSOLE");
            appender.start();

            // Safety check: Only switch if the new appender is actually working
            if (appender.isStarted()) {
                rootLogger.detachAndStopAllAppenders();
                rootLogger.addAppender(appender);
            } else {
                System.err.println("CRITICAL: Failed to start JSON console appender. Keeping default logging.");
            }
        } catch (Exception e) {
            // Fallback: print error to stderr and don't touch existing functionality
            System.err.println("CRITICAL: Failed to configure structured logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
