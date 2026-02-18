package com.yourorg.observability.starter.core;

import com.yourorg.observability.contract.ObsMdcKeys;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Hooks;

/**
 * Automatically registers MDC keys with Micrometer's ContextRegistry and
 * enables Reactor's automatic context propagation. This ensures that
 * correlation_id (and other MDC values) are transparently carried across
 * Reactor scheduler boundaries without any manual setup in consuming apps.
 *
 * <p>
 * Activates only when both {@code obs.enabled=true} (default) and
 * {@code reactor-core} is on the classpath.
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "obs", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "reactor.core.publisher.Hooks")
public class MdcContextPropagationAutoConfiguration {

    @PostConstruct
    public void enableContextPropagation() {
        // Enable Reactor ↔ ThreadLocal bridging
        Hooks.enableAutomaticContextPropagation();

        // Register correlation_id for automatic capture/restore across threads
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                ObsMdcKeys.CORRELATION_ID,
                () -> MDC.get(ObsMdcKeys.CORRELATION_ID),
                value -> MDC.put(ObsMdcKeys.CORRELATION_ID, value),
                () -> MDC.remove(ObsMdcKeys.CORRELATION_ID));
    }
}
