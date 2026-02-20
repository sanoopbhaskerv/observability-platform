import type { DynatraceBackendOptions } from './types';

/**
 * Activates the Dynatrace RUM tracing backend by injecting the
 * Dynatrace OneAgent JavaScript snippet into the page.
 *
 * Dynatrace OneAgent handles:
 *  - Session replay & user journeys
 *  - W3C traceparent header injection (linking browser → backend)
 *  - Core Web Vitals & performance timing
 *  - Automatic Davis AI correlation
 *
 * You do NOT need the OTel packages installed to use this backend.
 * @internal — call via initTracing({ backend: 'dynatrace', ... })
 */
export function initDynatraceBackend(options: DynatraceBackendOptions): void {
    const { scriptUrl, rumAttributes = {} } = options;

    if (!scriptUrl) {
        console.error('[observability] Dynatrace backend requires a scriptUrl (OneAgent RUM script URL).');
        return;
    }

    const script = document.createElement('script');
    script.src = scriptUrl;
    script.async = true;
    script.crossOrigin = 'anonymous';

    // Inject optional RUM custom attributes (e.g. user ID, tenant) that will
    // appear on every Dynatrace session and action.
    if (Object.keys(rumAttributes).length > 0) {
        script.onload = () => {
            const dt = (window as any).dtrum;
            if (dt?.addActionProperties) {
                Object.entries(rumAttributes).forEach(([key, value]) => {
                    dt.addActionProperties([{ key, value }]);
                });
            }
        };
    }

    document.head.appendChild(script);
    console.info('[observability] Dynatrace RUM script injected.');
}
