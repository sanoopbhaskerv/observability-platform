package com.yourorg.observability.demo;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        // Enable automatic context propagation across Reactor/async boundaries
        Hooks.enableAutomaticContextPropagation();

        // Register MDC keys with Micrometer's ContextRegistry so they are
        // automatically captured from ThreadLocal (MDC) and restored when
        // crossing thread boundaries (CompletableFuture, Reactor schedulers).
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                "correlation_id", // key used in Reactor Context
                () -> MDC.get("correlation_id"), // reader: get from MDC
                value -> MDC.put("correlation_id", value), // writer: set into MDC
                () -> MDC.remove("correlation_id") // cleaner: remove from MDC
        );

        SpringApplication.run(DemoApplication.class, args);
    }
}
