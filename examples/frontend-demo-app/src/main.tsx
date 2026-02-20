// ⚠️ Tracing MUST be initialised before React mounts so window.fetch is patched
// before any component issues a network request.
import { initTracing, initObservability } from 'observability-client-js';
void initTracing({ backend: 'otel' }); // swap to 'dynatrace' with a single config change
// injectTraceparent: false → OTel owns traceparent; observability-client-js only injects X-Correlation-Id
initObservability({ injectTraceparent: false });

import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
