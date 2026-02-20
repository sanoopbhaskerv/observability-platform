# Current Observability Architecture (Today)

## Overview

Today's observability is **log-centric** — applications write logs to disk, a Sumo Logic Collector agent ships them to Sumo Logic cloud, and teams query/dashboard from there. Frontend session replay is handled separately by **Glassbox**.

---

## Architecture Diagram

```mermaid
graph TD
    subgraph Browser["User Browser"]
        User["User Session"]
        Glassbox["Glassbox Agent\n(JS snippet injected)"]
    end

    subgraph Servers["App Servers  (la* / sa*)"]
        Apache["Apache HTTPd\n(custom-access.log)\n⬅ ALL browser requests land here"]
        Tomcat["Apache Tomcat\n(app logs)"]
        SumoAgent["Sumo Logic Collector\n(agent on each server)"]
    end

    subgraph SumoLogic["Sumo Logic (SaaS — Cloud)"]
        Ingest["Log Ingest & Indexing\n✅ Every browser request is here"]
        Search["Search Engine\n(log queries)"]
        Dashboards["Dashboards\n(e.g. Components vs Version)"]
        Alerts["Alerts & Monitoring"]
        SIEM["Cloud SIEM\n(security events)"]
    end

    subgraph GlassboxCloud["Glassbox (SaaS — Cloud)"]
        SessionReplay["Session Replay"]
        UserJourneys["User Journey Analytics"]
        HeatMaps["Heatmaps & Click Tracking"]
    end

    User -->|"Every HTTP request\n(page loads, API calls)"| Apache
    User -->|"Click / interaction data\n(parallel, direct to Glassbox)"| Glassbox
    Glassbox -->|"Session data (HTTPS)"| GlassboxCloud

    Apache -->|"Logs every request to disk\n/logs/httpd/ear00/custom-access.log"| SumoAgent
    Apache -->|"Forwards app requests"| Tomcat
    Tomcat -->|"Writes app logs to disk"| SumoAgent
    SumoAgent -->|"Ship logs (HTTPS)"| Ingest

    Ingest --> Search
    Search --> Dashboards
    Search --> Alerts
    Search --> SIEM

    style User fill:#2d6a4f,color:#fff
    style Glassbox fill:#52b788,color:#fff
    style Apache fill:#1d3557,color:#fff
    style Tomcat fill:#1d3557,color:#fff
    style SumoAgent fill:#457b9d,color:#fff
    style Ingest fill:#e07b39,color:#fff
    style Search fill:#e07b39,color:#fff
    style Dashboards fill:#e07b39,color:#fff
    style Alerts fill:#e07b39,color:#fff
    style SIEM fill:#e07b39,color:#fff
    style SessionReplay fill:#6a5acd,color:#fff
    style UserJourneys fill:#6a5acd,color:#fff
    style HeatMaps fill:#6a5acd,color:#fff
```

---

## What Exists Today

| Signal | Tool | How it gets there |
|---|---|---|
| **Application Logs** | Sumo Logic | Apache/Tomcat write to disk → Sumo Collector agent ships to cloud |
| **Frontend Session Replay** | Glassbox | JS snippet injected into browser → streams directly to Glassbox SaaS |
| **Log Search & Dashboards** | Sumo Logic | Teams write SPL queries (like `parse field=_raw` you see in log-search) |
| **Security Events** | Sumo Cloud SIEM | Derived from the same log stream |
| **Distributed Tracing** | ❌ None | Not available today |
| **Metrics Pipeline** | ❌ None | Inferred from log queries only |
| **Correlation across services** | ❌ None | No trace/correlation IDs flowing between services |

---

## Key Gaps vs the New Platform

```mermaid
graph LR
    subgraph Today["Today ❌"]
        L["Logs only\n(Sumo Logic)"]
        S["Session Replay\n(Glassbox — isolated)"]
    end

    subgraph New["New Platform ✅"]
        NL["Logs + Traces + Metrics\n(structured, correlated)"]
        NT["Distributed Tracing\n(X-Ray / Dynatrace)"]
        NM["Metrics\n(AMP / Dynatrace Native)"]
        NS["RUM / Session Replay\n(Dynatrace OneAgent)"]
        NC["Correlation IDs\n(end-to-end: Browser → DB)"]
    end

    Today -.->|"Migrate to"| New

    style Today fill:#e63946,color:#fff
    style New fill:#2d6a4f,color:#fff
```
