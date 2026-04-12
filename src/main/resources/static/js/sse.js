window.eventSource = null;
window.sseConnected = false;

/**
 * Opens the SSE (Server-Sent Events) stream for order and staff notifications.
 * Called from the layout script after `GET /api/user/me` (user id = Keycloak subject or `GUEST_<session>`).
 */
function initSse(userId) {
    if (!userId) {
        console.warn('>>> NOTIFICATIONS: Cannot initialize: userId is missing');
        return;
    }

    startSseConnection(userId);
}

function startSseConnection(userId) {
    // 1. Cleanup existing connection if any
    if (eventSource && eventSource.readyState !== 2) { // 2 = CLOSED
        console.log('>>> NOTIFICATIONS: Closing existing SSE connection before re-init');
        eventSource.close();
        window.sseConnected = false;
    }

    console.log('>>> NOTIFICATIONS: Connecting to SSE stream...');
    eventSource = new EventSource('/api/sse/subscribe/' + userId);

    eventSource.onopen = () => {
        console.log('>>> NOTIFICATIONS: Connected successfully');
        // We don't set sseConnected=true here because we wait for the 'connected' event from the server
    };

    // Initial connection event
    eventSource.addEventListener('connected', (event) => {
        console.log('>>> NOTIFICATIONS:', event.data);
        window.sseConnected = true;
    });

    // Listen for order status updates
    eventSource.addEventListener('order-update', (event) => {
        try {
            const orderUpdate = JSON.parse(event.data);
            console.log('>>> NOTIFICATIONS: Received order update:', orderUpdate);
            
            if (typeof renderActiveOrder === 'function') {
                renderActiveOrder(orderUpdate);
            } else if (typeof refreshCartUI === 'function') {
                refreshCartUI();
            } else if (typeof checkActiveOrder === 'function') {
                checkActiveOrder();
            }
            
            handleOrderNotification(orderUpdate);
        } catch (e) {
            console.error('>>> NOTIFICATIONS: Error parsing order update:', e);
        }
    });

    // Listen for staff notifications (new orders, waiter calls, etc.)
    eventSource.addEventListener('staff-update', (event) => {
        console.log('>>> NOTIFICATIONS: Staff update received:', event.data);
        if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.addEventListener('staff-notification', (event) => {
        console.log('>>> NOTIFICATIONS: Staff message:', event.data);
        if (typeof loadOrders === 'function') {
            loadOrders();
        }
    });

    eventSource.onerror = () => {
        // CONNECTING = browser is retrying; do not clear sseConnected or we flap forever in the UI/logs.
        if (eventSource.readyState === EventSource.CONNECTING) {
            console.log('>>> NOTIFICATIONS: Connection lost, attempting to reconnect...');
            return;
        }
        window.sseConnected = false;
        if (eventSource.readyState === EventSource.CLOSED) {
            console.warn('>>> NOTIFICATIONS: Connection closed. Browser will attempt to reconnect...');
        } else {
            console.error('>>> NOTIFICATIONS: EventSource experienced an error');
        }
    };
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
