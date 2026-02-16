
import { v4 as uuidv4 } from 'uuid';

export const CORRELATION_ID_HEADER = 'X-Correlation-Id';
export const TRACEPARENT_HEADER = 'traceparent';
export const OBS_CORRELATION_ID_KEY = 'obs.correlationId';

/**
 * Generates a W3C traceparent header value.
 * Format: 00-traceId-parentId-flags
 * 
 * @param sampled Whether the trace should be sampled (01) or not (00). Defaults to true (01).
 * @returns The generated traceparent string.
 */
export function generateTraceparent(sampled: boolean = true): string {
    const version = "00";
    const flags = sampled ? "01" : "00";

    // traceId: 16 bytes (32 hex chars)
    const traceId = uuidv4().replace(/-/g, '');

    // parentId: 8 bytes (16 hex chars)
    // We use the first 16 chars of a UUID for simplicity, or generate random bytes
    const parentId = uuidv4().replace(/-/g, '').substring(0, 16);

    return `${version}-${traceId}-${parentId}-${flags}`;
}

/**
 * Retrieves the existing correlation ID from session storage or generates a new one.
 * Persists the ID in session storage to maintain context across page reloads/navigations.
 * 
 * @returns The correlation ID string.
 */
export function getOrCreateCorrelationId(): string {
    let id = '';
    try {
        id = sessionStorage.getItem(OBS_CORRELATION_ID_KEY) || '';
    } catch (e) {
        // Accessing sessionStorage might fail in some contexts (e.g. sandboxed iframes)
        console.warn('Failed to access sessionStorage for correlation ID', e);
    }

    if (!id) {
        id = uuidv4();
        try {
            sessionStorage.setItem(OBS_CORRELATION_ID_KEY, id);
        } catch (e) {
            console.warn('Failed to save correlation ID to sessionStorage', e);
        }
    }
    return id;
}

/**
 * Updates the stored correlation ID if the backend returns a new one (optional behavior).
 * 
 * @param id The new correlation ID from the backend response.
 */
export function updateCorrelationId(id: string): void {
    if (id) {
        try {
            sessionStorage.setItem(OBS_CORRELATION_ID_KEY, id);
        } catch (e) {
            console.warn('Failed to update correlation ID in sessionStorage', e);
        }
    }
}
