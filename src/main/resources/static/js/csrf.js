// Global CSRF helper for same-origin requests.
// Source of truth: GET /api/csrf (server session token).
// We keep a cached token in memory and also support the default header/param names:
// - header: X-XSRF-TOKEN
// - request param: _csrf

let csrfState = {
  token: null,
  headerName: 'X-XSRF-TOKEN',
  parameterName: '_csrf',
  inFlight: null
};

function dispatchCsrfInitialized() {
  try {
    document.dispatchEvent(new CustomEvent('csrf:initialized', { detail: { ...csrfState } }));
  } catch {
    // ignore
  }
}

/**
 * Blocks the main thread briefly until a token exists or timeout.
 * HTMX configRequest handlers are synchronous; async ensure() alone can race the first request.
 */
function waitForTokenSync(maxMs) {
  const deadline = Date.now() + (maxMs || 400);
  if (csrfState.token) {
    return csrfState.token;
  }
  if (!csrfState.inFlight) {
    ensureCsrf();
  }
  while (Date.now() < deadline) {
    if (csrfState.token) {
      return csrfState.token;
    }
    if (!csrfState.inFlight && !csrfState.token) {
      break;
    }
    const spin = Date.now();
    while (Date.now() - spin < 2) {
      /* cooperative busy-wait slice */
    }
  }
  return csrfState.token || null;
}

async function ensureCsrf() {
  if (csrfState.token) {
    return csrfState;
  }
  if (csrfState.inFlight) return csrfState.inFlight;

  csrfState.inFlight = fetch('/api/csrf', { credentials: 'same-origin' })
    .then(r => (r.ok ? r.json() : Promise.reject(new Error('CSRF bootstrap failed'))))
    .then(data => {
      csrfState.token = data.token;
      csrfState.headerName = data.headerName || csrfState.headerName;
      csrfState.parameterName = data.parameterName || csrfState.parameterName;
      csrfState.inFlight = null;
      dispatchCsrfInitialized();
      return csrfState;
    })
    .catch(() => {
      csrfState.inFlight = null;
      dispatchCsrfInitialized();
      return csrfState;
    });

  return csrfState.inFlight;
}

function isUnsafeMethod(method) {
  const m = (method || 'GET').toUpperCase();
  return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(m);
}

// Patch fetch to automatically attach CSRF header for unsafe same-origin requests.
(() => {
  if (!window.fetch) return;
  const originalFetch = window.fetch.bind(window);

  window.fetch = async (input, init) => {
    const options = init ? { ...init } : {};
    const method = options.method || (typeof input === 'object' && input.method) || 'GET';

    if (isUnsafeMethod(method)) {
      const { token, headerName } = await ensureCsrf();
      if (token) {
        const existingHeaders = options.headers || {};
        const headers = existingHeaders instanceof Headers ? existingHeaders : new Headers(existingHeaders);
        if (!headers.has(headerName)) {
          headers.set(headerName, token);
        }
        options.headers = headers;
      }
    }

    return originalFetch(input, options);
  };
})();

// Expose helper for dynamic form posts (e.g. LiqPay init).
window.__csrf = {
  ensure: ensureCsrf,
  token: () => csrfState.token,
  waitForTokenSync,
  async addHiddenInput(form) {
    if (!form) return;
    const { token, parameterName } = await ensureCsrf();
    if (!token) return;
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = parameterName || '_csrf';
    input.value = token;
    form.appendChild(input);
  }
};

