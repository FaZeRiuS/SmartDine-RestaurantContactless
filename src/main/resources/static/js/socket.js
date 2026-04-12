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
        console.log('WebSocket already active');
        return;
    }

    console.log('Initializing Modern WebSocket for:', userId);
    
    // Determine the WebSocket URL: ws:// or wss:// based on the current protocol
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    // When using Spring's .withSockJS(), the native websocket endpoint is at /ws/websocket
    const brokerURL = `${protocol}${window.location.host}/ws/websocket`;

    stompClient = new StompJs.Client({
        brokerURL: brokerURL,
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        
        // Optional: debug: (str) => console.log('>>> STOMP DEBUG:', str),
    });

    stompClient.onConnect = (frame) => {
        console.log('STOMP Connected to', brokerURL);
        
        // Subscribe to the topic for this user
        const topic = '/topic/order-updates/' + userId;
        console.log('>>> SOCKET: Subscribing to topic:', topic);
        
        stompClient.subscribe(topic, (message) => {
            try {
                const orderUpdate = JSON.parse(message.body);
                console.log('Real-time Order Update:', orderUpdate);
                
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
            console.log('>>> SOCKET: Global Orders Update:', message.body);
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
        console.error('WebSocket Error:', event);
    };

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
