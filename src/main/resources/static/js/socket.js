let stompClient = null;

/**
 * Initializes the WebSocket connection for a specific user/guest.
 * Called from layout.html after the userId is successfully fetched.
 */
function initWebSocket(userId) {
    if (!userId) {
        console.warn('Cannot initialize WebSocket: userId is missing');
        return;
    }

    if (stompClient && stompClient.active) {
        return;
    }

    
    stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
        console.log('>>> WEBSOCKET: Connected successfully');
        
        // Subscribe to the topic for this user
        const topic = '/topic/order-updates/' + userId;
        
        stompClient.subscribe(topic, (message) => {
            try {
                const orderUpdate = JSON.parse(message.body);
                
                if (typeof renderActiveOrder === 'function') {
                    renderActiveOrder(orderUpdate);
                } else if (typeof refreshCartUI === 'function') {
                    refreshCartUI();
                } else if (typeof checkActiveOrder === 'function') {
                    checkActiveOrder();
                }
                
                handleOrderNotification(orderUpdate);
            } catch (e) {
                console.error('Error parsing WebSocket message:', e);
            }
        });

        // Global orders topic (Staff/Admin updates)
        stompClient.subscribe('/topic/orders', (message) => {
            if (typeof loadOrders === 'function') {
                loadOrders();
            }
        });
    };

    stompClient.onStompError = (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
        console.error('Details:', frame.body);
    };

    stompClient.onWebSocketError = (event) => {
        console.error('>>> WEBSOCKET: Connection error:', event);
        // Provide more context to the user if possible
        if (window.location.hostname.includes('azure.com') && !stompClient.active) {
            console.warn('>>> DIAGNOSTIC: Detect potential DNS or connectivity issue with Azure domain.');
        }
    };

    console.log('>>> WEBSOCKET: Activating connection...');
    stompClient.activate();
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
