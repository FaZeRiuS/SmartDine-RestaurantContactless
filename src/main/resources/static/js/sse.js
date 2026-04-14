window.eventSource = null;
window.sseConnected = false;

/**
 * Opens the SSE (Server-Sent Events) stream for order and staff notifications.
 * Called from the layout script after `GET /api/user/me` (user id = Keycloak subject or `GUEST_<session>`).
 */
function initSse(userId) {
    if (!userId) {
        return;
    }

    startSseConnection(userId);
}

function startSseConnection(userId) {
    if (eventSource && eventSource.readyState !== 2) { // 2 = CLOSED
        eventSource.close();
        window.sseConnected = false;
    }

    eventSource = new EventSource('/api/sse/subscribe/' + encodeURIComponent(userId));

    eventSource.onopen = () => {
        // sseConnected set on server's `connected` event
    };

    eventSource.addEventListener('connected', () => {
        window.sseConnected = true;
    });

    eventSource.addEventListener('order-update', (event) => {
        // Refresh UI even if JSON.parse fails (payload is only needed for toast).
        refreshOrderDrivenUiFromSse();
        try {
            handleOrderNotification(JSON.parse(event.data));
        } catch {
            // ignore malformed payload (toast only)
        }
    });

    eventSource.addEventListener('staff-update', (event) => {
        if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('staff-notification', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => refreshUiAfterReloadNotification(), 1500);
        } else if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('order-notification', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => refreshUiAfterReloadNotification(), 1500);
        }
    });

    eventSource.onerror = () => {
        if (eventSource.readyState === EventSource.CONNECTING) {
            return;
        }
        window.sseConnected = false;
    };
}

window.startSseConnection = startSseConnection;

/** Cart badge, cart fragment, active-order panel — shared by order-update SSE and [RELOAD] refresh. */
function refreshOrderDrivenUiFromSse() {
    if (!window.htmx) {
        return;
    }
    if (document.getElementById('mobile-cart-count')) {
        try {
            // Fragment is OOB-only (hx-swap-oob); outerHTML on target breaks HTMX (no main swap root).
            window.htmx.ajax('GET', '/htmx/cart/widget', { swap: 'none' });
        } catch {
            // ignore
        }
    }
    if (document.getElementById('cartContent')) {
        try {
            window.htmx.ajax('GET', '/htmx/cart/content', { target: '#cartContent', swap: 'outerHTML' });
        } catch {
            // ignore
        }
    }
    const activeOrderEl = document.getElementById('activeOrderContainer');
    const activeUrl = activeOrderEl && activeOrderEl.getAttribute('hx-get');
    if (activeUrl) {
        try {
            window.htmx.ajax('GET', activeUrl, { target: '#activeOrderContainer', swap: 'innerHTML' });
        } catch {
            // ignore
        }
    }
}

/**
 * Replaces full page reload for SSE messages tagged with [RELOAD]: refresh HTMX regions
 * that already exist on the page (cart, active order, customer orders, staff board).
 */
function refreshUiAfterReloadNotification() {
    refreshOrderDrivenUiFromSse();
    if (window.htmx) {
        if (typeof window.refreshCustomerOrdersList === 'function') {
            try {
                window.refreshCustomerOrdersList();
            } catch {
                // ignore
            }
        }
    }
    if (typeof loadOrders === 'function') {
        loadOrders();
    }
}

window.refreshUiAfterReloadNotification = refreshUiAfterReloadNotification;

if (typeof navigator !== 'undefined' && 'serviceWorker' in navigator) {
    let sseAfterSwTimer = null;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
        const uid = window.currentUserId;
        if (!uid) return;
        clearTimeout(sseAfterSwTimer);
        sseAfterSwTimer = setTimeout(() => {
            try {
                if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
                    startSseConnection(uid);
                }
            } catch {
                // ignore
            }
        }, 400);
    });
}

function handleOrderNotification(order) {
    const statusMap = {
        'PREPARING': '👨‍🍳 Ваше замовлення почали готувати!',
        'READY': '✅ Ваше замовлення готове! Смачного!',
        'COMPLETED': '🥡 Замовлення завершено. Чекаємо на вас знову!',
        'CANCELLED': '❌ Замовлення було скасовано.'
    };

    const message = statusMap[order.status];
    if (message && typeof showToast === 'function') {
        showToast(message, order.status === 'READY' ? 'success' : 'info');
    }
}
