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
    // Never surface JSON error bodies for server errors — they may contain internal details.
    if (status >= 500) return statusMessage(status);
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
    try {
      const xhr = evt?.detail?.xhr;
      if (!xhr || xhr._htmxToastSuccess) return;
      const elt = findToastSource(evt?.detail?.elt);
      if (!elt) return;
      const sm = elt.getAttribute?.('data-toast-success');
      if (!sm) return;
      xhr._htmxToastSuccess = sm;
      xhr._htmxToastType = elt.getAttribute?.('data-toast-success-type') || 'success';
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
      const trigger = evt?.detail?.elt;
      const elt = findToastSource(trigger);
      if (!elt || typeof showToast !== 'function') return;

      const successMsg = elt.getAttribute?.('data-toast-success');
      const successType = elt.getAttribute?.('data-toast-success-type') || 'success';
      if (successMsg) {
        elt._htmxSuccessToast = successMsg;
        elt._htmxSuccessToastType = successType;
        const xhr = evt?.detail?.xhr;
        if (xhr && typeof xhr === 'object') {
          xhr._htmxToastSuccess = successMsg;
          xhr._htmxToastType = successType;
        }
      }

      const msg = elt.getAttribute?.('data-toast-pending');
      if (msg) showToast(msg, 'info');
    } catch (err) {
      if (window.__CLIENT_DEBUG && typeof console.warn === 'function') {
        console.warn('HTMX Toast Bridge (Pending) Error:', err);
      }
    }
  }

  function maybeShowSuccessToast(evt) {
    try {
      const xhr = evt?.detail?.xhr;
      if (!xhr || typeof showToast !== 'function') return;
      if (xhr.status < 200 || xhr.status >= 300) return;

      if (xhr._htmxToastSuccess) {
        showToast(xhr._htmxToastSuccess, xhr._htmxToastType || 'success');
        delete xhr._htmxToastSuccess;
        delete xhr._htmxToastType;
        return;
      }

      const trigger = evt?.detail?.elt;
      const elt = findToastSource(trigger);
      if (!elt) return;

      const msg = elt._htmxSuccessToast || elt.getAttribute?.('data-toast-success');
      if (!msg) return;

      const t = elt._htmxSuccessToastType || elt.getAttribute?.('data-toast-success-type') || 'success';
      showToast(msg, t);

      delete elt._htmxSuccessToast;
      delete elt._htmxSuccessToastType;
    } catch (err) {
      if (window.__CLIENT_DEBUG && typeof console.warn === 'function') {
        console.warn('HTMX Toast Bridge (Success) Error:', err);
      }
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
