package com.yourorg.observability.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class HelloController {
    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    /**
     * Basic endpoint — demonstrates correlation ID extraction and MDC logging.
     */
    @GetMapping("/hello")
    public String hello(@RequestHeader(value = "X-Correlation-Id", required = false) String cid) {
        log.info("Hello endpoint hit. incomingCorrelationId={}", cid);
        return "hello";
    }

    /**
     * Demonstrates MANUAL MDC propagation for CompletableFuture.
     *
     * CompletableFuture uses ForkJoinPool which is NOT managed by Reactor,
     * so context-propagation does not auto-bridge MDC. You must copy MDC
     * manually before going async.
     */
    @GetMapping("/hello-async")
    public Map<String, String> helloAsync() {
        String correlationId = MDC.get("correlation_id");
        log.info("[request-thread] correlation_id={}", correlationId);

        // Capture MDC snapshot BEFORE crossing the thread boundary
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        CompletableFuture.runAsync(() -> {
            try {
                if (mdcContext != null)
                    MDC.setContextMap(mdcContext);
                log.info("[async-thread] correlation_id={}", MDC.get("correlation_id"));
            } finally {
                MDC.clear();
            }
        });

        return Map.of(
                "message", "Check server logs — correlation_id should appear in both threads",
                "correlation_id", correlationId != null ? correlationId : "none");
    }

    /**
     * Demonstrates AUTOMATIC MDC propagation via Reactor + context-propagation.
     *
     * With Hooks.enableAutomaticContextPropagation() and ContextRegistry
     * registration (see DemoApplication.java), MDC values are automatically
     * captured from the request thread and restored when Reactor switches
     * schedulers (e.g., boundedElastic).
     */
    @GetMapping("/hello-reactor")
    public Mono<Map<String, String>> helloReactor() {
        String correlationId = MDC.get("correlation_id");
        log.info("[request-thread] correlation_id={}", correlationId);

        return Mono.fromCallable(() -> {
            String reactorCid = MDC.get("correlation_id");
            log.info("[reactor-thread] correlation_id={}", reactorCid);
            return Map.of(
                    "message", "Check server logs — correlation_id auto-propagated to reactor thread",
                    "correlation_id", reactorCid != null ? reactorCid : "none");
        })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
