export type { TracingOptions, OtelBackendOptions, DynatraceBackendOptions } from './types';
import type { TracingOptions } from './types';

/**
 * Initialises frontend tracing with the specified backend.
 *
 * IMPORTANT: Call this BEFORE your framework renders (before React.render /
 * Angular bootstrapApplication) so that window.fetch is instrumented before
 * any component issues a network request.
 *
 * Works alongside `initObservability()`:
 *  - `initTracing`       → telemetry pipeline (spans → backend)
 *  - `initObservability` → X-Correlation-Id session linking (log correlation)
 *
 * @example
 * // OTel (default, open-source)
 * await initTracing({ backend: 'otel', otlpEndpoint: 'http://localhost:4318/v1/traces' });
 *
 * // Dynatrace (swap by changing one line)
 * await initTracing({ backend: 'dynatrace', scriptUrl: 'https://<tenant>.live.dynatrace.com/...' });
 */
export async function initTracing(options: TracingOptions): Promise<void> {
    switch (options.backend) {
        case 'otel': {
            // Lazy-import: OTel packages only loaded when OTel is the active backend.
            const { initOtelBackend } = await import('./otel');
            initOtelBackend(options);
            break;
        }
        case 'dynatrace': {
            // Lazy-import: no OTel packages needed for Dynatrace-only apps.
            const { initDynatraceBackend } = await import('./dynatrace');
            initDynatraceBackend(options);
            break;
        }
        default: {
            // Exhaustiveness check — TypeScript will catch unhandled backends at compile time.
            const _exhaustive: never = options;
            console.error('[observability] Unknown tracing backend:', (_exhaustive as any));
        }
    }
}
