
import { generateTraceparent, getOrCreateCorrelationId, CORRELATION_ID_HEADER, TRACEPARENT_HEADER, updateCorrelationId } from '../trace-context';

export interface FetchInstrumentationOptions {
    /**
     * List of allowed domains to send trace headers to.
     */
    allowList?: RegExp[];
    /**
     * Whether to inject a W3C `traceparent` header on outbound fetch calls.
     * Set to `false` when using initTracing({ backend: 'otel' | 'dynatrace' }),
     * since OTel FetchInstrumentation / Dynatrace OneAgent will own the traceparent
     * and generate a real span-backed ID. Injecting a custom UUID-based traceparent
     * on top would produce an orphaned, unresolvable trace ID in the backend.
     * @default true
     */
    injectTraceparent?: boolean;
}

function isAllowed(url: string | URL | Request, allowList: RegExp[] = []): boolean {
    if (allowList.length === 0) return true;

    let targetUrl: string;

    if (url instanceof Request) {
        targetUrl = url.url;
    } else if (url instanceof URL) {
        targetUrl = url.toString();
    } else {
        targetUrl = url;
    }

    try {
        const parsed = new URL(targetUrl, window.location.origin);
        if (parsed.origin === window.location.origin) return true;
        return allowList.some(regex => regex.test(parsed.hostname));
    } catch {
        return true;
    }
}

export function enableFetchInstrumentation(options: FetchInstrumentationOptions = {}): void {
    const originalFetch = window.fetch;

    window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
        if (!isAllowed(input, options.allowList)) {
            return originalFetch(input, init);
        }

        const headers = new Headers(init?.headers);

        // 1. Traceparent — skip if OTel/Dynatrace is owning traceparent (injectTraceparent: false)
        if (options.injectTraceparent !== false && !headers.has(TRACEPARENT_HEADER)) {
            headers.set(TRACEPARENT_HEADER, generateTraceparent());
        }

        // 2. Correlation ID
        const correlationId = getOrCreateCorrelationId();
        if (!headers.has(CORRELATION_ID_HEADER)) {
            headers.set(CORRELATION_ID_HEADER, correlationId);
        }

        const newInit: RequestInit = {
            ...init,
            headers
        };

        const response = await originalFetch(input, newInit);

        // Capture returned correlation ID
        const returnedId = response.headers.get(CORRELATION_ID_HEADER);
        if (returnedId) {
            updateCorrelationId(returnedId);
        }

        return response;
    };
}
