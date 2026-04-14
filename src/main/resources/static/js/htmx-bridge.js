// HTMX bridge: attach CSRF header for unsafe requests.
// fetch() is already patched in csrf.js; HTMX uses XHR and needs this hook.
// Uses synchronous wait (csrf.waitForTokenSync) so the first hx-trigger=load cannot outrun async ensure().

(() => {
  function isUnsafe(method) {
    const m = String(method || 'GET').toUpperCase();
    return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(m);
  }

  function statusMessage(status) {
    switch (status) {
      case 400:
        return 'Некоректний запит';
      case 401:
        return 'Потрібна авторизація';
      case 403:
        return 'Доступ заборонено (перевірте сесію або CSRF)';
      case 404:
        return 'Ресурс не знайдено';
      case 409:
        return 'Конфлікт даних';
      case 422:
        return 'Дані не пройшли перевірку';
      case 429:
        return 'Забагато запитів. Спробуйте пізніше';
      default:
        if (status >= 500) return 'Помилка сервера. Спробуйте пізніше';
        if (status > 0) return `Помилка ${status}`;
        return 'Помилка запиту';
    }
  }

  function messageFromXhr(xhr) {
    if (!xhr) return statusMessage(0);
    const status = xhr.status || 0;
    const raw = xhr.responseText;
    if (raw && typeof raw === 'string') {
      const trimmed = raw.trim();
      if (trimmed && !trimmed.startsWith('<') && trimmed.length <= 2000) {
        try {
          const j = JSON.parse(trimmed);
          const m =
            j.message ||
            j.error ||
            (typeof j.detail === 'string' ? j.detail : null) ||
            (Array.isArray(j.errors) && j.errors[0]?.defaultMessage);
          if (m && String(m).length <= 500) return String(m);
        } catch {
          if (trimmed.length <= 280) return trimmed;
        }
      }
    }
    return statusMessage(status);
  }

  function toastError(text) {
    if (typeof showToast !== 'function') return;
    showToast(`\u26A0\uFE0F ${text}`, 'error');
  }

  function findToastSource(startEl) {
    let el = startEl;
    for (let i = 0; i < 6 && el; i++) {
      if (el.getAttribute) {
        const hasAny =
          el.getAttribute('data-toast-pending') ||
          el.getAttribute('data-toast-success') ||
          el.getAttribute('data-toast-success-type');
        if (hasAny) return el;
      }
      el = el.parentElement;
    }
    return startEl;
  }

  function attachCsrfHeader(evt) {
    const method = evt?.detail?.verb;
    if (!isUnsafe(method)) return;

    let token = null;
    if (typeof window.__csrf?.waitForTokenSync === 'function') {
      token = window.__csrf.waitForTokenSync(450);
    }
    if (!token && typeof window.__csrf?.token === 'function') {
      token = window.__csrf.token();
    }
    if (!token && typeof window.__csrf?.ensure === 'function') {
      window.__csrf.ensure();
      if (typeof window.__csrf.waitForTokenSync === 'function') {
        token = window.__csrf.waitForTokenSync(200);
      }
    }

    if (token) {
      evt.detail.headers = evt.detail.headers || {};
      evt.detail.headers['X-XSRF-TOKEN'] = token;
    }
  }

  document.body?.addEventListener('htmx:configRequest', (evt) => {
    try {
      attachCsrfHeader(evt);
    } catch {
      // ignore
    }
  });

  // Opt-in UX toasts for HTMX actions via data attributes:
  // - data-toast-pending: message shown on request start
  // - data-toast-success: message shown after 2xx response
  // - data-toast-success-type: success|info|error (default success)
  function maybeShowPendingToast(evt) {
    try {
      const elt = findToastSource(evt?.detail?.elt);
      if (!elt || typeof showToast !== 'function') return;
      const msg = elt.getAttribute?.('data-toast-pending');
      if (!msg) return;
      showToast(msg, 'info');
    } catch {
      // ignore
    }
  }

  function maybeShowSuccessToast(evt) {
    try {
      const elt = findToastSource(evt?.detail?.elt);
      const xhr = evt?.detail?.xhr;
      if (!elt || !xhr || typeof showToast !== 'function') return;
      if (xhr.status < 200 || xhr.status >= 300) return;
      const msg = elt.getAttribute?.('data-toast-success');
      if (!msg) return;
      const t = elt.getAttribute?.('data-toast-success-type') || 'success';
      showToast(msg, t);
    } catch {
      // ignore
    }
  }

  document.body?.addEventListener('htmx:beforeRequest', maybeShowPendingToast);
  document.body?.addEventListener('htmx:afterRequest', maybeShowSuccessToast);

  document.body?.addEventListener('htmx:responseError', (evt) => {
    try {
      const xhr = evt?.detail?.xhr;
      toastError(messageFromXhr(xhr));
    } catch {
      toastError(statusMessage(0));
    }
  });

  document.body?.addEventListener('htmx:sendError', () => {
    toastError('Не вдалося відправити запит. Перевірте мережу.');
  });
})();
