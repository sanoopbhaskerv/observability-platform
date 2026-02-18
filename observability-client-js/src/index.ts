
import { attachAxiosInterceptor } from './instrumentation/axios';
import { enableFetchInstrumentation } from './instrumentation/fetch';
import { generateTraceparent, getOrCreateCorrelationId } from './trace-context';

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
}

/**
 * Initializes the observability SDK.
 * 
 * @param options Configuration options.
 */
export function initObservability(options: ObservabilityOptions = {}) {
    if (options.instrumentFetch !== false) {
        enableFetchInstrumentation({ allowList: options.allowList });
    }

    // Ensure correlation ID is generated/retrieved on startup
    getOrCreateCorrelationId();
}

export { attachAxiosInterceptor, enableFetchInstrumentation, generateTraceparent, getOrCreateCorrelationId };
