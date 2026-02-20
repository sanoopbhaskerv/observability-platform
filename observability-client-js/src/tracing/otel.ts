import {
    WebTracerProvider,
    BatchSpanProcessor,
} from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import type { OtelBackendOptions } from './types';

/**
 * Activates the OTel tracing backend.
 * Uses WebTracerProvider + FetchInstrumentation + OTLP/HTTP exporter.
 * @internal — call via initTracing({ backend: 'otel', ... })
 */
export function initOtelBackend(options: OtelBackendOptions): void {
    const {
        otlpEndpoint = 'http://localhost:4318/v1/traces',
        exportHeaders = {},
        propagateTraceHeaderCorsUrls = [/localhost/, /127\.0\.0\.1/],
    } = options;

    const exporter = new OTLPTraceExporter({ url: otlpEndpoint, headers: exportHeaders });

    // OTel v2.x: spanProcessors passed in constructor config
    const provider = new WebTracerProvider({
        spanProcessors: [new BatchSpanProcessor(exporter)],
    });

    provider.register();

    registerInstrumentations({
        instrumentations: [
            new FetchInstrumentation({ propagateTraceHeaderCorsUrls }),
        ],
    });

    console.info(`[observability] OTel tracing → ${otlpEndpoint}`);
}
