window.eventSource = null;
window.sseConnected = false;
window.currentSseUserId = null;

/**
 * Opens the SSE (Server-Sent Events) stream for order and staff notifications.
 * Called from the layout script after `GET /api/user/me` (user id = Keycloak subject or `GUEST_<session>`).
 */
function initSse(userId) {
    if (!userId) {
        return;
    }

    window.currentSseUserId = userId;
    startSseConnection(userId);
}

function startSseConnection(userId) {
    window.currentSseUserId = userId;

    // Exponential backoff reconnect state.
    if (!window.__sseReconnect) {
        window.__sseReconnect = { attempt: 0, timer: null };
    }
    const st = window.__sseReconnect;

    const clearReconnectTimer = () => {
        if (st.timer) {
            clearTimeout(st.timer);
            st.timer = null;
        }
    };

    const scheduleReconnect = () => {
        if (!window.currentSseUserId) return;
        if (st.timer) return;
        const attempt = Math.min(10, st.attempt || 0);
        const base = Math.min(30000, 800 * Math.pow(2, attempt)); // 0.8s, 1.6s, 3.2s ... up to 30s
        const jitter = Math.floor(Math.random() * 400);
        st.timer = setTimeout(() => {
            st.timer = null;
            st.attempt = Math.min(20, (st.attempt || 0) + 1);
            try {
                startSseConnection(window.currentSseUserId);
            } catch {
                // ignore
            }
        }, base + jitter);
    };

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
        st.attempt = 0;
        clearReconnectTimer();
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
        try {
            const lower = String(msg).toLowerCase();
            // Show localized toast only for "waiter called" (dismiss message is intentionally hidden).
            if (lower.includes("waiter called") && typeof showToast === 'function') {
                const m = String(msg).match(/Table\s*#\s*(\d+)/i);
                const tableNo = m && m[1] ? m[1] : null;
                const localized = tableNo
                    ? ("Офіціанта викликано до столу №" + tableNo)
                    : "Офіціанта викликано";
                showToast(localized, "info");
            }
        } catch {
            // ignore (toast is optional)
        }
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => refreshUiAfterReloadNotification(), 200);
        } else if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('order-notification', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => refreshUiAfterReloadNotification(), 200);
        }
    });

    // Heartbeat from server (flush through proxies). No-op, but keeps the stream active.
    eventSource.addEventListener('ping', () => {
        // intentionally empty
    });

    eventSource.onerror = () => {
        if (eventSource.readyState === EventSource.CONNECTING) {
            return;
        }
        window.sseConnected = false;
        scheduleReconnect();
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
    if (typeof window.syncAddToCartButtonsWithActiveOrder === 'function') {
        try {
            window.syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: false });
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

// Safari/Background-tab mitigation: when tab becomes visible again, force a resync for staff board.
// EventSource callbacks/timers can be throttled/suspended in background.
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible') return;

    // Only do this on pages that actually have staff board UI.
    if (!document.getElementById('staffBoardRoot')) return;

    try {
        if (typeof loadOrders === 'function') loadOrders();
    } catch {
        // ignore
    }

    const uid = window.currentSseUserId || window.currentUserId;
    if (!uid) return;

    try {
        if (window.eventSource) {
            try { window.eventSource.close(); } catch { /* ignore */ }
            window.sseConnected = false;
        }
        startSseConnection(uid);
    } catch {
        // ignore
    }
});

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
