# Observability Platform — Architecture Walkthrough

## 1. High-Level Module Structure

```mermaid
graph TB
    subgraph "Parent POM"
        direction TB
        parent["observability-platform-parent<br/><i>pom packaging · version mgmt</i>"]
    end

    subgraph "Foundation Layer"
        contract["observability-contract<br/><i>ObsHeaders · ObsMdcKeys · CorrelationId</i>"]
    end

    subgraph "Starter Modules"
        core["starter-core<br/><i>CorrelationIdFilter · MDC · context-propagation</i>"]
        http["starter-http<br/><i>RestTemplateCustomizer</i>"]
        tracing["starter-tracing<br/><i>Micrometer → OTel bridge</i>"]
        metrics["starter-metrics<br/><i>OTLP MeterRegistry</i>"]
    end

    umbrella["starter - Umbrella<br/><i>Pulls in all starters</i>"]

    subgraph "Example App"
        demo["spring-boot-demo-service<br/><i>/hello · /hello-async · /hello-reactor</i>"]
    end

    parent -.->|manages| contract
    parent -.->|manages| core
    parent -.->|manages| http
    parent -.->|manages| tracing
    parent -.->|manages| metrics
    parent -.->|manages| umbrella
    parent -.->|manages| demo

    contract --> core
    contract --> http
    core --> umbrella
    http --> umbrella
    tracing --> umbrella
    metrics --> umbrella
    umbrella --> demo
```

**How to read this:** Solid arrows = compile dependency. Dotted arrows = parent POM management. The parent controls versions but modules are independently deployable JARs.

---

## 2. Module Dependency Graph

```mermaid
graph LR
    contract["observability-contract"]
    core["starter-core"]
    http["starter-http"]
    tracing["starter-tracing"]
    metrics["starter-metrics"]
    umbrella["starter<br/>(Umbrella)"]
    demo["demo-service"]

    contract --> core
    contract --> http
    core --> umbrella
    http --> umbrella
    tracing --> umbrella
    metrics --> umbrella
    umbrella --> demo

    SB["spring-boot-autoconfigure"] -.-> core
    SB -.-> http
    SB -.-> tracing
    SB -.-> metrics
    MW["spring-boot-starter-web"] -.-> core
    MW -.-> http
    ACT["spring-boot-starter-actuator"] -.-> tracing
    ACT -.-> metrics
    MT["micrometer-tracing + bridge-otel"] -.-> tracing
    OTLP["micrometer-registry-otlp"] -.-> metrics
    OTEL["opentelemetry-exporter-otlp"] -.-> tracing
    CP["context-propagation"] -.-> core
```

> [!IMPORTANT]
> `context-propagation` is pulled in by `starter-core`, making MDC bridging available to every service that uses any starter.

---

## 3. Request Data Flow

```mermaid
sequenceDiagram
    participant Client
    participant Filter as CorrelationIdFilter<br/>(starter-core)
    participant MDC as SLF4J MDC
    participant Controller as Your Controller
    participant Interceptor as OutboundCorrelationInterceptor<br/>(starter-http)
    participant Downstream as Downstream Service
    participant Tracing as Micrometer Tracer<br/>(starter-tracing)
    participant Metrics as OTLP MeterRegistry<br/>(starter-metrics)
    participant Collector as OTel Collector

    Client->>Filter: HTTP Request<br/>X-Correlation-Id: abc-123
    Filter->>MDC: MDC.put("correlation_id", id)
    Filter->>Filter: Echo header in response
    Filter->>Controller: doFilter(request, response)
    Controller->>Controller: log.info("…") — MDC auto-appends correlation_id
    
    Controller->>Interceptor: RestTemplate call
    Interceptor->>MDC: MDC.get("correlation_id")
    Interceptor->>Downstream: Adds X-Correlation-Id header
    
    Note over Tracing: Parallel: Micrometer auto-instruments spans
    Tracing->>Collector: OTLP traces export

    Note over Metrics: Parallel: MeterRegistry records metrics
    Metrics->>Collector: OTLP metrics export (default)
    Metrics-->>Prometheus: Scrape /actuator/prometheus (optional)
    Metrics-->>Dynatrace: Native API export (optional)

    Filter->>MDC: MDC.remove("correlation_id")
    Filter->>Client: Response + X-Correlation-Id header
```

---

## 4. MDC Thread Propagation

MDC is `ThreadLocal`-based. This diagram shows how context crosses thread boundaries:

```mermaid
graph TD
    subgraph "Request Thread (nio-8080-exec-1)"
        A["CorrelationIdFilter<br/>MDC.put('correlation_id', 'abc')"]
        B["Controller method runs"]
    end

    subgraph "CompletableFuture (ForkJoinPool-worker-1)"
        C["MDC is EMPTY by default"]
        D["Manual: MDC.setContextMap(copy)"]
        E["MDC.get('correlation_id') = 'abc' ✅"]
    end

    subgraph "Reactor (boundedElastic-1)"
        F["ContextRegistry auto-restores MDC"]
        G["MDC.get('correlation_id') = 'abc' ✅"]
    end

    A --> B
    B -->|"MDC.getCopyOfContextMap()"| C
    C --> D --> E
    B -->|"Hooks.enableAutomaticContextPropagation()"| F
    F --> G

    style C fill:#e63946,color:#fff
    style E fill:#2d6a4f,color:#fff
    style G fill:#2d6a4f,color:#fff
```

| Scenario | Propagation | Mechanism |
|---|---|---|
| Same servlet thread | ✅ Automatic | ThreadLocal |
| Reactor schedulers | ✅ Automatic | `ContextRegistry` + `Hooks.enableAutomaticContextPropagation()` |
| `CompletableFuture` | ❌ Manual | `MDC.getCopyOfContextMap()` → `MDC.setContextMap()` |
| `@Async` | ❌ Manual | `TaskDecorator` wrapping |

---

## 5. Feature Toggle Tree

```mermaid
graph TD
    master["obs.enabled = true<br/><i>(master kill-switch)</i>"]

    master --> corr["obs.correlation.enabled<br/><i>CorrelationIdFilter + MDC</i>"]
    master --> httpmod["obs.http.enabled<br/><i>RestTemplateCustomizer bean</i>"]
    master --> traces["obs.traces.enabled<br/><i>Tracing auto-config</i>"]
    master --> metricsmod["obs.metrics.enabled<br/><i>OTLP MeterRegistry</i>"]

    httpmod --> propag["obs.http.propagate-correlation-id<br/><i>Attach X-Correlation-Id outbound</i>"]

    style master fill:#2d6a4f,color:#fff
    style corr fill:#40916c,color:#fff
    style httpmod fill:#40916c,color:#fff
    style traces fill:#40916c,color:#fff
    style metricsmod fill:#e63946,color:#fff
    style propag fill:#52b788,color:#fff
```

> [!NOTE]
> `obs.metrics.enabled` defaults to **false** (red) — opt-in for cost governance. All others default to **true**.

---

## 6. Production Deployment Topology

```mermaid
graph LR
    subgraph "Your Microservice (Pod / ECS Task)"
        App["Spring Boot App<br/>+ observability-spring-boot-starter"]
        Sidecar["OTel Collector<br/>(sidecar / gateway)"]
    end

    App -->|OTLP gRPC<br/>localhost:4317| Sidecar
    App -->|Structured JSON logs<br/>stdout| LogPipeline["Log Pipeline<br/>(FluentBit / Filebeat)"]

    Sidecar -->|Traces| Tempo["Tempo / Jaeger / X-Ray"]
    Sidecar -->|Metrics| Prometheus["Prometheus / CloudWatch"]
    LogPipeline -->|Logs| Loki["Loki / CloudWatch Logs / ELK"]

    Tempo --> Grafana["Grafana Dashboard"]
    Prometheus --> Grafana
    Loki --> Grafana

    style App fill:#1d3557,color:#fff
    style Sidecar fill:#457b9d,color:#fff
    style Grafana fill:#e63946,color:#fff

### Alternative: Direct / Native Export
For environments where a sidecar is not possible, or when using vendor-specific agents (e.g., Dynatrace OneAgent):

```mermaid
graph LR
    App["Spring Boot App"]
    
    App -->|Scrape| Prometheus
    App -.->|Native API| Dynatrace
```
```

**Key architectural decision:** Application exports OTLP to a **local** Collector sidecar — never directly to backends. This decouples the app from backend choices and allows buffering, retries, and sampling at the Collector layer.

---

## 7. Class-Level Detail by Module

### [observability-contract](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-contract)
| Class | Purpose |
|---|---|
| [ObsHeaders](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-contract/src/main/java/com/yourorg/observability/contract/ObsHeaders.java) | HTTP header constants: `X-Correlation-Id`, `traceparent`, `X-Client-Request-Id`, `X-Session-Id` |
| [ObsMdcKeys](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-contract/src/main/java/com/yourorg/observability/contract/ObsMdcKeys.java) | MDC key constants: `correlation_id`, `trace_id`, `span_id` |
| [CorrelationId](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-contract/src/main/java/com/yourorg/observability/contract/CorrelationId.java) | Extract correlation ID from header or generate UUID |

### [starter-core](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-core)
| Class | Purpose |
|---|---|
| [ObservabilityCoreAutoConfiguration](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-core/src/main/java/com/yourorg/observability/starter/core/ObservabilityCoreAutoConfiguration.java) | Registers `CorrelationIdFilter` as highest-precedence servlet filter |
| [CorrelationIdFilter](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-core/src/main/java/com/yourorg/observability/starter/core/CorrelationIdFilter.java) | Extracts/generates correlation ID → MDC + response header |
| `ObsCoreProperties` | Config: `obs.correlation.header-name` (default: `X-Correlation-Id`) |

**Dependency:** Ships `context-propagation` for MDC bridging across threads.

### [starter-http](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-http)
| Class | Purpose |
|---|---|
| [ObservabilityHttpAutoConfiguration](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-http/src/main/java/com/yourorg/observability/starter/http/ObservabilityHttpAutoConfiguration.java) | Registers `RestTemplateCustomizer` that adds the interceptor |
| [OutboundCorrelationInterceptor](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-http/src/main/java/com/yourorg/observability/starter/http/OutboundCorrelationInterceptor.java) | Copies correlation ID from MDC to outgoing HTTP headers |

### [starter-tracing](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-tracing)
| Class | Purpose |
|---|---|
| [ObservabilityTracingAutoConfiguration](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-tracing/src/main/java/com/yourorg/observability/starter/tracing/ObservabilityTracingAutoConfiguration.java) | Presence-based toggle; actual tracing via Micrometer + OTel bridge |

### [starter-metrics](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-metrics)
| Class | Purpose |
|---|---|
| [ObservabilityMetricsAutoConfiguration](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/observability-spring-boot-starter-metrics/src/main/java/com/yourorg/observability/starter/metrics/ObservabilityMetricsAutoConfiguration.java) | Creates `OtlpConfig` + `OtlpMeterRegistry` beans |

### [demo-service](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/examples/spring-boot-demo-service)
| Class | Purpose |
|---|---|
| [DemoApplication](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/examples/spring-boot-demo-service/src/main/java/com/yourorg/observability/demo/DemoApplication.java) | Enables `Hooks.enableAutomaticContextPropagation()` + registers MDC with `ContextRegistry` |
| [HelloController](file:///Users/sanoopbhaskerv/workspace/observability_platform/observability-platform-maven-template/examples/spring-boot-demo-service/src/main/java/com/yourorg/observability/demo/HelloController.java) | Three demo endpoints: `/hello`, `/hello-async`, `/hello-reactor` |

---

## 8. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.4.1 |
| Metrics | Micrometer Core | 1.14.3 |
| Tracing | Micrometer Tracing | 1.4.3 |
| Telemetry | OpenTelemetry API | 1.44.1 |
| Context | Micrometer Context Propagation | (managed by Spring BOM) |
| Reactor | Project Reactor | (managed by Spring BOM) |
| Java | JDK | 17+ |

---

## Source

GitHub: [sanoopbhaskerv/observability-platform](https://github.com/sanoopbhaskerv/observability-platform)
