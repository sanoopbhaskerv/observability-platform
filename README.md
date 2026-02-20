# 🔭 Observability Platform — Maven Multi-Module Template

> Enterprise-grade observability starter packs for **Spring Boot 3/4** with OpenTelemetry, Micrometer, and structured correlation — ready to drop into any microservice fleet.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Modules](#modules)
- [Quick Start](#quick-start)
- [Integration Guide](#integration-guide)
- [Configuration Reference](#configuration-reference)
- [How It Works](#how-it-works)
- [Production Deployment](#production-deployment)
- [Extending the Platform](#extending-the-platform)
- [Tech Stack](#tech-stack)
- [Contributing](#contributing)

---

## Overview

This platform provides a **plug-and-play observability layer** for Spring Boot microservices. Add a single dependency and every service in your fleet gets:

- ✅ **Automatic request correlation** — inbound ID extraction, MDC logging, response header echo
- ✅ **Outbound propagation** — correlation headers forwarded on RestTemplate calls
- ✅ **Distributed tracing** — Micrometer → OpenTelemetry bridge with OTLP export
- ✅ **Metrics export** — OTLP MeterRegistry for Prometheus/Grafana/CloudWatch
- ✅ **Feature toggles** — every capability can be independently enabled/disabled at runtime

### Design Principles

| Principle | Implementation |
|---|---|
| **Zero code changes** | Auto-configuration via Spring Boot starters |
| **Vendor neutral** | W3C Trace Context, OpenTelemetry, standard headers |
| **Incremental adoption** | Each module is independent; use the umbrella or pick individual starters |
| **Org-wide contract** | Shared header names and MDC keys across all services |

---

## Architecture

```
observability-platform-parent (pom)
├── observability-contract              ← Shared constants (headers, MDC keys)
├── observability-spring-boot-starter-core    ← Correlation filter + MDC
├── observability-spring-boot-starter-http    ← Outbound header propagation
├── observability-spring-boot-starter-tracing ← Micrometer + OTel tracing
├── observability-spring-boot-starter-metrics ← OTLP metrics registry
├── observability-spring-boot-starter         ← Umbrella (pulls all above)
└── examples/
    └── spring-boot-demo-service              ← Working demo app
```

### Module Dependency Graph

```
                ┌──────────────────────┐
                │  observability-contract  │
                │  (ObsHeaders, ObsMdcKeys,│
                │   CorrelationId)         │
                └──────┬───────┬───────────┘
                       │       │
              ┌────────▼──┐  ┌─▼───────────┐
              │starter-core│  │ starter-http │
              │(Filter+MDC)│  │(RestTemplate)│
              └─────┬──────┘  └──────┬──────┘
                    │                │
     ┌──────────────┼────────────────┼──────────────┐
     │              │                │              │
     │     ┌────────▼──┐    ┌────────▼──┐           │
     │     │           │    │           │           │
     │  ┌──▼──────────────────────────────────┐     │
     │  │     starter (Umbrella)              │     │
     │  │  aggregates: core + http +          │     │
     │  │  tracing + metrics                  │     │
     │  └──────────────┬──────────────────────┘     │
     │                 │                            │
     │        ┌────────▼────────┐                   │
     │        │ demo-service    │                   │
     │        │ (example app)   │                   │
     │        └─────────────────┘                   │
     │                                              │
  ┌──▼──────────┐                      ┌────────────▼──┐
  │starter-tracing│                    │ starter-metrics │
  │(Micrometer    │                    │ (OTLP          │
  │ + OTel bridge)│                    │  MeterRegistry) │
  └───────────────┘                    └────────────────┘
```

---

## Modules

### `observability-contract`

The **foundation module** — zero Spring dependencies. Defines the org-wide observability contract that all services share.

| Class | Purpose |
|---|---|
| `ObsHeaders` | HTTP header constants: `X-Correlation-Id`, `traceparent`, `X-Client-Request-Id`, `X-Session-Id` |
| `ObsMdcKeys` | SLF4J MDC key constants: `correlation_id`, `trace_id`, `span_id` |
| `CorrelationId` | Utility to extract correlation ID from a header value or generate a new UUID |

### `observability-spring-boot-starter-core`

Automatic **inbound request correlation**. Registers a highest-precedence servlet filter that:
1. Extracts `X-Correlation-Id` from the incoming request (or generates a new UUID)
2. Places it into the SLF4J MDC so every log statement includes it
3. Echoes it back in the response header for client-side reuse
4. Cleans up MDC after the request completes

| Class | Purpose |
|---|---|
| `ObservabilityCoreAutoConfiguration` | Registers `CorrelationIdFilter` as a `FilterRegistrationBean` |
| `CorrelationIdFilter` | `OncePerRequestFilter` — extracts/generates ID → MDC → response |
| `ObsCoreProperties` | Configures `obs.enabled`, `obs.correlation.enabled`, `obs.correlation.header-name` |

### `observability-spring-boot-starter-http`

**Outbound correlation propagation** for service-to-service calls. Adds a `ClientHttpRequestInterceptor` to `RestTemplateBuilder` that copies the correlation ID from MDC to outgoing HTTP headers.

| Class | Purpose |
|---|---|
| `ObservabilityHttpAutoConfiguration` | Adds `OutboundCorrelationInterceptor` to `RestTemplateBuilder` |
| `OutboundCorrelationInterceptor` | Reads `correlation_id` from MDC, adds `X-Correlation-Id` header to outbound requests |
| `ObsHttpProperties` | Configures `obs.http.enabled`, `obs.http.propagate-correlation-id` |

### `observability-spring-boot-starter-tracing`

**Distributed tracing** via Micrometer Tracing bridged to OpenTelemetry. This module's presence on the classpath enables trace context propagation and span export.

| Class | Purpose |
|---|---|
| `ObservabilityTracingAutoConfiguration` | Presence-based toggle for the tracing subsystem |
| `ObsTracingProperties` | Configures `obs.traces.enabled` |

**Key dependencies:** `micrometer-tracing`, `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`

### `observability-spring-boot-starter-metrics`

**Metrics export** via Micrometer. Supports multiple export strategies:
- **OTLP** (Push, Default) — `micrometer-registry-otlp`
- **Prometheus** (Pull) — `micrometer-registry-prometheus`
- **Dynatrace** (Native, Optional) — `micrometer-registry-dynatrace`

| Class | Purpose |
|---|---|
| `ObservabilityMetricsAutoConfiguration` | Configures MeterRegistries based on enabled properties |
| `ObsMetricsProperties` | Configures `obs.metrics.enabled` (default: **false**) |

### `observability-spring-boot-starter` (Umbrella)

A convenience module that transitively pulls in **all four starters** (core, http, tracing, metrics). Add this single dependency to get everything.

### `examples/spring-boot-demo-service`

A working Spring Boot application demonstrating the starter in action with three endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /hello` | Basic correlation ID extraction and MDC logging |
| `GET /hello-async` | Manual MDC propagation across `CompletableFuture` threads |
| `GET /hello-reactor` | Automatic MDC propagation via Reactor + context-propagation |

---

## Quick Start

### Prerequisites

- **Java 17+**
- **Maven 3.8+**

### Build

```bash
mvn clean install
```

### Run the Demo Service

```bash
mvn -pl examples/spring-boot-demo-service spring-boot:run
```

### Test Correlation

```bash
# Without a correlation ID (one will be generated)
curl -i http://localhost:8080/hello

# With an explicit correlation ID
curl -i -H "X-Correlation-Id: my-trace-123" http://localhost:8080/hello
```

**Expected response headers:**

```
HTTP/1.1 200
X-Correlation-Id: my-trace-123
```

**Expected log output:**

```
INFO  [correlation_id=my-trace-123] c.y.o.demo.HelloController : Hello endpoint hit. incomingCorrelationId=my-trace-123
```

### Test Async & Reactor MDC Propagation

```bash
# CompletableFuture — manual MDC propagation
curl -s -H "X-Correlation-Id: test-async" http://localhost:8080/hello-async | python3 -m json.tool

# Reactor — automatic MDC propagation
curl -s -H "X-Correlation-Id: test-reactor" http://localhost:8080/hello-reactor | python3 -m json.tool
```

**Expected server logs (both endpoints):**

```
INFO [nio-8080-exec-1] [request-thread] correlation_id=test-async
INFO [onPool-worker-1] [async-thread]   correlation_id=test-async      ← manual propagation

INFO [nio-8080-exec-2] [request-thread] correlation_id=test-reactor
INFO [oundedElastic-1] [reactor-thread] correlation_id=test-reactor    ← automatic propagation
```

---

## Integration Guide

### Option 1: Umbrella Starter (Recommended)

Add a single dependency to your service's `pom.xml`:

```xml
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-spring-boot-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

This pulls in **all** observability capabilities: correlation, HTTP propagation, tracing, and metrics.

### Option 2: Individual Starters

Pick only what you need:

```xml
<!-- Correlation filter + MDC only -->
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-spring-boot-starter-core</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Add outbound correlation propagation -->
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-spring-boot-starter-http</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Add distributed tracing -->
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-spring-boot-starter-tracing</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Add OTLP metrics export -->
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-spring-boot-starter-metrics</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Option 3: Contract Only

For non-Spring services or shared libraries that just need the header/MDC constants:

```xml
<dependency>
  <groupId>com.yourorg.observability</groupId>
  <artifactId>observability-contract</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## Configuration Reference

Add these properties to your `application.yml` or `application.properties`:

```yaml
obs:
  # Master kill-switch for the entire observability platform
  enabled: true                          # default: true

  correlation:
    # Enable/disable the CorrelationIdFilter
    enabled: true                        # default: true
    # Custom header name (if your org uses a different one)
    header-name: X-Correlation-Id        # default: X-Correlation-Id

  http:
    # Enable/disable the RestTemplate interceptor bean
    enabled: true                        # default: true
    # Propagate correlation ID on outbound HTTP calls
    propagate-correlation-id: true       # default: true

  traces:
    # Enable/disable the Micrometer → OTel tracing bridge
    enabled: true                        # default: true

  metrics:
    # Enable/disable OTLP metrics export (opt-in for cost governance)
    enabled: false                       # default: false

# Spring Boot Actuator & Exporters
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  
  # --- Metrics Exporters ---
  otlp:
    metrics:
      export:
        enabled: true                     # Push to Collector (default)
        url: http://localhost:4318/v1/metrics
  
  prometheus:
    metrics:
      export:
        enabled: true                     # Expose /actuator/prometheus (default)

  # dynatrace:                            # Uncomment for Native Export
  #   metrics:
  #     export:
  #       enabled: true
  #       uri: https://{env}.live.dynatrace.com
  #       api-token: {token}

  tracing:
    enabled: true

```

### Feature Toggle Summary

| Property | Default | Effect |
|---|---|---|
| `obs.enabled` | `true` | Master kill-switch — disables all observability |
| `obs.correlation.enabled` | `true` | Inbound correlation filter + MDC enrichment |
| `obs.correlation.header-name` | `X-Correlation-Id` | HTTP header name for correlation |
| `obs.http.enabled` | `true` | RestTemplate interceptor bean registration |
| `obs.http.propagate-correlation-id` | `true` | Attach correlation header on outbound calls |
| `obs.traces.enabled` | `true` | Micrometer tracing auto-configuration |
| `obs.metrics.enabled` | `false` | OTLP MeterRegistry (opt-in) |

### Environment Variables (Tracing & Metrics Export)

| Variable | Purpose | Example |
|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTel Collector endpoint | `http://localhost:4317` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | Export protocol | `grpc` or `http/protobuf` |
| `OTEL_SERVICE_NAME` | Service name in traces | `my-payment-service` |
| `MANAGEMENT_OTLP_METRICS_EXPORT_URL` | Metrics OTLP endpoint | `http://localhost:4318/v1/metrics` |

---

## How It Works

### Request Lifecycle

```
Client Request
    │
    ▼
┌─────────────────────────────────────────────┐
│  CorrelationIdFilter (Highest Precedence)   │
│                                              │
│  1. Extract X-Correlation-Id from header     │
│     (or generate UUID if missing)            │
│  2. MDC.put("correlation_id", id)            │
│  3. Set response header                      │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  Your Controller / Service Logic            │
│                                              │
│  • log.info("...") automatically includes   │
│    correlation_id from MDC                   │
│  • Micrometer auto-instruments spans        │
└──────────────────┬──────────────────────────┘
                   │
          ┌────────┴────────┐
          ▼                 ▼
┌──────────────────┐  ┌──────────────────────┐
│ RestTemplate call│  │ Metrics recorded     │
│                  │  │ by MeterRegistry     │
│ Interceptor adds │  └──────────┬───────────┘
│ X-Correlation-Id │             │
│ from MDC         │             ▼
└────────┬─────────┘  ┌──────────────────────┐
         │            │ OTLP Export          │
         ▼            │ → OTel Collector     │
┌──────────────────┐  └──────────────────────┘
│ Downstream       │
│ Service          │
│ (receives same   │
│  correlation ID) │
└──────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│  CorrelationIdFilter (finally block)        │
│  MDC.remove("correlation_id")               │
└─────────────────────────────────────────────┘
                   │
                   ▼
             Client Response
         (includes X-Correlation-Id)
```

### Auto-Configuration Registration

Each starter registers itself via Spring Boot 3.x's `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` file — no `spring.factories` needed.

### ⚠️ Async & Virtual Threads — MDC Propagation Gotcha

MDC is backed by a `ThreadLocal`, so it works perfectly for standard servlet requests (one thread per request). However, **MDC values do not automatically propagate** when you spawn child threads or use virtual threads:

| Scenario | MDC Propagates? | Solution |
|---|---|---|
| Normal servlet request | ✅ Automatic | — |
| Reactor (`Mono`/`Flux`) | ✅ Automatic | `ContextRegistry` + `Hooks.enableAutomaticContextPropagation()` |
| `CompletableFuture.runAsync(...)` | ❌ Manual | `MDC.getCopyOfContextMap()` → `MDC.setContextMap()` |
| `@Async` methods | ❌ Manual | `TaskDecorator` wrapping |
| Virtual threads (Project Loom) | ❌ Manual | Same as `CompletableFuture` |

> [!TIP]
> The demo service includes working examples of both patterns — try `/hello-async` and `/hello-reactor`.

#### Reactor: Automatic Propagation (Recommended)

This platform ships with `context-propagation` in `starter-core`. To enable automatic MDC bridging for Reactor, register your MDC keys with `ContextRegistry` at application startup:

```java
import io.micrometer.context.ContextRegistry;
import reactor.core.publisher.Hooks;

public class MyApplication {
    public static void main(String[] args) {
        // Enable Reactor ↔ ThreadLocal bridging
        Hooks.enableAutomaticContextPropagation();

        // Register correlation_id for automatic capture/restore
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            "correlation_id",
            () -> MDC.get("correlation_id"),
            value -> MDC.put("correlation_id", value),
            () -> MDC.remove("correlation_id")
        );

        SpringApplication.run(MyApplication.class, args);
    }
}
```

Now any `Mono`/`Flux` that switches schedulers will automatically carry the correlation ID:

```java
@GetMapping("/data")
public Mono<Data> getData() {
    // correlation_id is in MDC here (set by CorrelationIdFilter)
    return dataService.fetch()
        .subscribeOn(Schedulers.boundedElastic());  // MDC propagated automatically
}
```

#### CompletableFuture: Manual Propagation

`CompletableFuture` uses `ForkJoinPool` which is not managed by Reactor, so context-propagation cannot auto-bridge MDC. Copy the MDC snapshot manually:

```java
Map<String, String> mdcContext = MDC.getCopyOfContextMap();

CompletableFuture.runAsync(() -> {
    try {
        if (mdcContext != null) MDC.setContextMap(mdcContext);
        log.info("correlation_id is available here");
    } finally {
        MDC.clear();
    }
});
```

#### TaskDecorator for `@Async`

Register a `TaskDecorator` to automatically copy MDC for all `@Async` calls:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
```

---

## Production Deployment

### Recommended Topology

```
┌───────────────────────────────────────────┐
│  Pod / ECS Task                           │
│                                           │
│  ┌─────────────────────┐                  │
│  │  Your Spring Boot   │──── OTLP ────►   │
│  │  Application        │  (localhost)  ┌──┴──────────┐
│  │  + obs-starter      │              │ OTel         │
│  └─────────┬───────────┘              │ Collector    │
│            │ stdout (JSON)            │ (sidecar)    │
│            ▼                          └──┬──────┬────┘
│  ┌─────────────────────┐                 │      │
│  │ Log Agent           │                 │      │
│  │ (FluentBit/Filebeat)│                 │      │
│  └─────────┬───────────┘                 │      │
└────────────┼─────────────────────────────┼──────┼─┘
             │                             │      │
             ▼                             ▼      ▼
        ┌─────────┐               ┌──────────┐ ┌──────────┐
        │  Loki / │               │  Tempo / │ │Prometheus│
        │  ELK /  │               │  Jaeger /│ │  / CW    │
        │  CW Logs│               │  X-Ray   │ │  Metrics │
        └────┬────┘               └─────┬────┘ └─────┬────┘
             │                          │             │
             └──────────┬───────────────┘             │
                        ▼                             │
                  ┌──────────┐                        │
                  │ Grafana  │◄───────────────────────┘
                  │Dashboard │
                  └──────────┘
```

### Best Practices

1. **Export OTLP to a local Collector** — use a K8s DaemonSet/gateway or ECS sidecar; never send directly to a backend from the application.
2. **Structured JSON logs** — configure `logback-spring.xml` to output JSON to stdout; let your log agent tail them.
3. **Cardinality governance** — keep `obs.metrics.enabled=false` by default; enable per-service only after reviewing metric labels.
4. **Environment-based config** — use env vars (`OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_SERVICE_NAME`) rather than hard-coded YAML for portability across environments.
5. **CI enforcement** — add build checks for log schema consistency, PII redaction, and metric cardinality budgets.

### Docker / Kubernetes Example

```yaml
# docker-compose.yml (local development)
services:
  demo-service:
    build: ./examples/spring-boot-demo-service
    ports:
      - "8080:8080"
    environment:
      - OTEL_SERVICE_NAME=demo-service
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
      - OBS_METRICS_ENABLED=true

  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    ports:
      - "4317:4317"   # gRPC
      - "4318:4318"   # HTTP
```

---

## Extending the Platform

### Adding WebClient / RestClient Support

Extend `starter-http` to support reactive and new `RestClient`:

```java
@Bean
@ConditionalOnClass(WebClient.class)
public WebClientCustomizer correlationWebClientCustomizer() {
    return builder -> builder.filter((request, next) -> {
        String cid = MDC.get(ObsMdcKeys.CORRELATION_ID);
        if (cid != null) {
            request.headers().add(ObsHeaders.CORRELATION_ID, cid);
        }
        return next.exchange(request);
    });
}
```

### Adding Custom Span Attributes

Extend `starter-tracing` with an `ObservationHandler`:

```java
@Bean
public ObservationHandler<Observation.Context> customSpanHandler() {
    return new ObservationHandler<>() {
        @Override
        public void onStart(Observation.Context context) {
            context.put("tenant.id", TenantContext.getCurrentTenant());
        }
        // ...
    };
}
```

### Adding a New Starter Module

1. Create a new Maven module: `observability-spring-boot-starter-<name>`
2. Add parent reference to `observability-platform-parent`
3. Create your `AutoConfiguration` class with `@ConditionalOnProperty`
4. Register it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
5. Add the module to the parent POM's `<modules>` section
6. Optionally add it as a dependency in the umbrella starter

---

## Tech Stack

| Component | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.4.1 |
| Metrics | Micrometer Core | 1.14.3 |
| Tracing | Micrometer Tracing | 1.4.3 |
| Telemetry | OpenTelemetry API | 1.44.1 |
| Build | Maven | 3.8+ |
| Java | JDK | 17+ |

---

## Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/my-feature`)
3. **Build** and test (`mvn clean install`)
4. **Submit** a pull request

### Code Conventions

- Follow existing package naming: `com.yourorg.observability.starter.<module>`
- Every auto-configuration must be gated with `@ConditionalOnProperty`
- Use `observability-contract` constants — never hard-code header names or MDC keys
- Register auto-configurations via `AutoConfiguration.imports` (not `spring.factories`)

---

## License

This is a template project. Apply your organization's license as appropriate.
