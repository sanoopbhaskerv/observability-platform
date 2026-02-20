/**
 * Options for the OTel tracing backend.
 */
export interface OtelBackendOptions {
    /**
     * OTLP/HTTP endpoint to export spans to.
     * - Local dev (OTel Collector):  'http://localhost:4318/v1/traces'
     * - Dynatrace OTLP ingest:       'https://<tenant>.live.dynatrace.com/api/v2/otlp/v1/traces'
     * @default 'http://localhost:4318/v1/traces'
     */
    otlpEndpoint?: string;
    /**
     * HTTP headers added to every OTLP export request.
     * Use for auth: e.g. { Authorization: 'Api-Token <token>' } for Dynatrace.
     */
    exportHeaders?: Record<string, string>;
    /**
     * URL patterns (RegExp) that are allowed to receive the W3C `traceparent`
     * CORS header. Requests to other origins will not have trace headers injected.
     * @default [/localhost/, /127\.0\.0\.1/]
     */
    propagateTraceHeaderCorsUrls?: RegExp[];
}

/**
 * Options for the Dynatrace OneAgent RUM backend.
 */
export interface DynatraceBackendOptions {
    /**
     * URL of the Dynatrace OneAgent JavaScript snippet.
     * Found in Dynatrace → Deploy Dynatrace → Set up RUM.
     * Example: 'https://<tenant>.live.dynatrace.com/jstag/managed/<app-id>/ruxitagent.js'
     */
    scriptUrl: string;
    /**
     * Optional custom key-value attributes attached to every Dynatrace RUM
     * session and action (e.g. tenant ID, app version, user role).
     */
    rumAttributes?: Record<string, string>;
}

/**
 * Union type for all supported tracing backend configurations.
 * Discriminated by the `backend` field.
 *
 * @example
 * initTracing({ backend: 'otel', otlpEndpoint: '...' });
 * initTracing({ backend: 'dynatrace', scriptUrl: '...' });
 */
export type TracingOptions =
    | ({ backend: 'otel' } & OtelBackendOptions)
    | ({ backend: 'dynatrace' } & DynatraceBackendOptions);
