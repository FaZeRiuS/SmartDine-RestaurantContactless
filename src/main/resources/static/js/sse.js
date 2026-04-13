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
        try {
            const orderUpdate = JSON.parse(event.data);

            if (typeof renderActiveOrder === 'function') {
                renderActiveOrder(orderUpdate);
            } else if (typeof refreshCartUI === 'function') {
                refreshCartUI();
            } else if (typeof checkActiveOrder === 'function') {
                checkActiveOrder();
            }

            handleOrderNotification(orderUpdate);

            // AUTO-REFRESH: Reload the page if the order reaches a final state
            if (orderUpdate.status === 'COMPLETED' || orderUpdate.status === 'CANCELLED') {
                setTimeout(() => window.location.reload(), 2000);
            }
        } catch {
            // ignore malformed payload
        }
    });

    eventSource.addEventListener('staff-update', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => window.location.reload(), 1500);
        } else if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('staff-notification', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => window.location.reload(), 1500);
        } else if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('order-notification', (event) => {
        const msg = event.data || "";
        if (msg.includes("[RELOAD]")) {
            setTimeout(() => window.location.reload(), 1500);
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
