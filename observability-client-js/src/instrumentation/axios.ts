
import { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { generateTraceparent, getOrCreateCorrelationId, CORRELATION_ID_HEADER, TRACEPARENT_HEADER, updateCorrelationId } from '../trace-context';

export interface AxiosInstrumentationOptions {
    /**
     * List of allowed domains to send trace headers to.
     * If empty, headers are sent to all domains (use with caution).
     */
    allowList?: RegExp[];
}

function isAllowed(url: string | undefined, allowList: RegExp[] = []): boolean {
    if (!url) return true; // Relative URLs are assumed safe/same-origin
    if (allowList.length === 0) return true;

    try {
        const parsed = new URL(url, window.location.origin);
        // If same origin, always allow
        if (parsed.origin === window.location.origin) return true;

        // Check allowList
        return allowList.some(regex => regex.test(parsed.hostname));
    } catch (e) {
        // If URL parsing fails, treat as relative and allow
        return true;
    }
}

export function attachAxiosInterceptor(axios: AxiosInstance, options: AxiosInstrumentationOptions = {}): void {
    axios.interceptors.request.use((config: InternalAxiosRequestConfig) => {
        if (!isAllowed(config.url, options.allowList)) {
            return config;
        }

        config.headers = config.headers || {};

        // 1. Traceparent (W3C)
        if (!config.headers[TRACEPARENT_HEADER]) {
            config.headers.set(TRACEPARENT_HEADER, generateTraceparent());
        }

        // 2. Correlation ID
        const correlationId = getOrCreateCorrelationId();
        if (!config.headers[CORRELATION_ID_HEADER]) {
            config.headers.set(CORRELATION_ID_HEADER, correlationId);
        }

        return config;
    });

    axios.interceptors.response.use((response: AxiosResponse) => {
        const returnedId = response.headers[CORRELATION_ID_HEADER.toLowerCase()] || response.headers[CORRELATION_ID_HEADER];
        if (returnedId) {
            updateCorrelationId(Array.isArray(returnedId) ? returnedId[0] : returnedId);
        }
        return response;
    });
}
