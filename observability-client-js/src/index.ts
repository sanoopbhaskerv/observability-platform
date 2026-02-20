
import { attachAxiosInterceptor } from './instrumentation/axios';
import { enableFetchInstrumentation } from './instrumentation/fetch';
import { generateTraceparent, getOrCreateCorrelationId } from './trace-context';

export type { TracingOptions, OtelBackendOptions, DynatraceBackendOptions } from './tracing/index';
export { initTracing } from './tracing/index';

export interface ObservabilityOptions {
    /**
     * List of allowed domains to send trace headers to.
     */
    allowList?: RegExp[];
    /**
     * Whether to instrument the global fetch API.
     * Default: true
     */
    instrumentFetch?: boolean;
    /**
     * Whether to inject a custom UUID-based `traceparent` header.
     * Set to `false` when also calling `initTracing({ backend: 'otel' | 'dynatrace' })`
     * so that OTel / Dynatrace owns the traceparent (real span-backed ID).
     * `initObservability` will still inject `X-Correlation-Id` — its unique responsibility.
     * @default true
     */
    injectTraceparent?: boolean;
}

/**
 * Initializes the observability SDK.
 * 
 * @param options Configuration options.
 */
export function initObservability(options: ObservabilityOptions = {}) {
    if (options.instrumentFetch !== false) {
        enableFetchInstrumentation({
            allowList: options.allowList,
            injectTraceparent: options.injectTraceparent,
        });
    }

    // Ensure correlation ID is generated/retrieved on startup
    getOrCreateCorrelationId();
}

export { attachAxiosInterceptor, enableFetchInstrumentation, generateTraceparent, getOrCreateCorrelationId };
