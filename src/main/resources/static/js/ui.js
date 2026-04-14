/**
 * ui.js - Common UI logic and utilities for all sections (Customer, Staff, Admin)
 */

function initSharedUI() {
    if (window.isAuthenticated) {
        loadLoyaltySummary();
    }
    initPushNotifications();
    checkUrlParams();

    // 7. Auto-cleanup for toasts (including those from HTMX OOB)
    const toastContainer = document.getElementById('toastContainer');
    if (toastContainer) {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                mutation.addedNodes.forEach((node) => {
                    if (node.nodeType === 1 && node.classList.contains('toast')) {
                        // Auto-remove after 6 seconds if not already being handled
                        setTimeout(() => {
                            if (node.parentElement) {
                                node.style.opacity = '0';
                                node.style.transform = 'translateY(10px)';
                                setTimeout(() => node.remove(), 400);
                            }
                        }, 6000);
                    }
                });
            });
        });
        observer.observe(toastContainer, { childList: true });
    }
}

// Exposed to central orchestration in layout.html
window.initSharedUI = initSharedUI;
window.renderStars = renderStars;
window.initStarPickersIn = initStarPickersIn;

/**
 * Sends a critical error log to the server for PWA/Lifecycle monitoring.
 */
async function logErrorToServer(message) {
    try {
        await fetch('/api/notifications/log', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain',
                'X-XSRF-TOKEN': getCsrfToken()
            },
            body: String(message)
        });
    } catch (e) {
        // Silent fail to avoid infinite loops if network is down
    }
}

/**
 * Checks URL for specific parameters (like payment status)
 */
function checkUrlParams() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('paymentResult') === 'success') {
        showToast('✅ Оплата успішна! Ваше замовлення готується.', 'success');
        
        // Clean up the URL without refreshing
        const newUrl = window.location.pathname + window.location.hash;
        window.history.replaceState({}, document.title, newUrl);
    }
}

// ── Shared UI Utilities ──

let loyaltyBalanceCache = null;
let loyaltySummaryCache = null;

async function loadLoyaltySummary() {
    if (!window.isAuthenticated) return;
    const balanceEl = document.getElementById('loyaltyBalance');
    const headerBalanceEls = document.querySelectorAll('.js-header-loyalty-balance');
    const headerRateEls = document.querySelectorAll('.js-header-cashback-rate');

    try {
        const res = await fetch('/api/loyalty/summary', { credentials: 'same-origin' });
        if (res.status === 401 || res.status === 403) {
            // Only clear if we don't have a value (prevents flicker)
            if (headerBalanceEls.length > 0 && headerBalanceEls[0].textContent === '—') {
                if (balanceEl) balanceEl.textContent = '—';
                headerBalanceEls.forEach(el => el.textContent = '—');
            }
            return;
        }
        if (!res.ok) {
            if (balanceEl) balanceEl.textContent = '—';
            headerBalanceEls.forEach(el => el.textContent = '—');
            return;
        }
        const data = await res.json();
        loyaltySummaryCache = data;
        loyaltyBalanceCache = typeof data.balance === 'number' ? data.balance : parseFloat(data.balance);
        const rate = typeof data.cashbackRate === 'number' ? data.cashbackRate : parseFloat(data.cashbackRate);

        if (Number.isFinite(loyaltyBalanceCache)) {
            if (balanceEl) balanceEl.textContent = Number(loyaltyBalanceCache).toFixed(2);
            headerBalanceEls.forEach(el => el.textContent = Number(loyaltyBalanceCache).toFixed(2));
        } else {
            if (balanceEl) balanceEl.textContent = '—';
            headerBalanceEls.forEach(el => el.textContent = '—');
        }

        if (headerRateEls.length > 0 && Number.isFinite(rate)) {
            headerRateEls.forEach(el => el.textContent = (Number(rate) * 100).toFixed(0));
        }
    } catch (e) {
        if (balanceEl) balanceEl.textContent = '—';
        headerBalanceEls.forEach(el => el.textContent = '—');
    }
}


/**
 * Standard HTML escaping to prevent XSS
 */
function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

/**
 * Advanced Toast Notification System
 * @param {string} message - Text to display
 * @param {string} type - 'info', 'success', 'warning', 'error'
 * @param {object} action - {text: string, onClick: function}
 */
function showToast(message, type = 'info', action = null) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const textNode = document.createElement('span');
    textNode.textContent = message;
    toast.appendChild(textNode);
    
    if (action) {
        const btn = document.createElement('button');
        btn.className = 'toast-action';
        btn.textContent = action.text;
        btn.onclick = (e) => {
            e.stopPropagation();
            action.onClick();
            toast.remove();
        };
        toast.appendChild(btn);
    }
    
    container.appendChild(toast);

    if (window.twemoji && typeof window.twemoji.parse === 'function') {
        window.twemoji.parse(toast, { folder: 'svg', ext: '.svg', className: 'twemoji' });
    }
    
    const duration = action ? 8000 : 4000;
    setTimeout(() => {
        if (toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            setTimeout(() => toast.remove(), 300);
        }
    }, duration);
}

/**
 * Modal Management
 */
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

/**
 * Star Rating Visualizer
 */
function renderStars(value) {
    const v = parseInt(value || 0, 10);
    let out = '';
    for (let i = 1; i <= 5; i++) out += i <= v ? '★' : '☆';
    return `<span style="color: var(--accent-gold); letter-spacing: 1px;">${out}</span>`;
}

/**
 * Star Rating Interactive Picker
 */
function initStarPickersIn(container) {
    if (!container) return;
    container.querySelectorAll('.star-picker').forEach(el => {
        if (el.dataset.initialized) return;
        el.dataset.initialized = '1';
        el.dataset.value = el.dataset.value || '0';
        el.style.cursor = 'pointer';
        el.style.userSelect = 'none';
        el.style.color = 'var(--accent-gold)';
        el.style.letterSpacing = '1px';
        el.innerHTML = renderStars(0);

        el.addEventListener('click', (e) => {
            const rect = el.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const percent = x / rect.width;
            const rating = Math.min(5, Math.max(1, Math.ceil(percent * 5)));
            el.dataset.value = String(rating);
            el.innerHTML = renderStars(rating);
        });
    });
}

/**
 * Status Label Formatters
 */
function formatOrderStatus(status) {
    const map = {
        'NEW': 'Нове',
        'PREPARING': 'Готується',
        'READY': 'Готово',
        'DELIVERED': 'Доставлено',
        'COMPLETED': 'Виконано',
        'CANCELLED': 'Скасовано'
    };
    return map[status] || status;
}

function formatPaymentStatus(status) {
    const map = {
        'PENDING': 'Очікує оплати',
        'SUCCESS': 'Оплачено',
        'FAILED': 'Помилка оплати'
    };
    return map[status] || status;
}


// ── Push Notifications ──

async function initPushNotifications() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        return;
    }

    if (Notification.permission === 'denied') {
        return;
    }

    // If permission is default (neither granted nor denied), we might want to ask.
    // However, best practice is to ask after a user action (like placing an order).
    // For now, we'll try to subscribe if already granted, or just check.
    if (Notification.permission === 'granted') {
        subscribeUserToPush();
    } else if (Notification.permission === 'default') {
        // Soft prompt or wait for action. 
        // Showing a toast as a soft prompt:
        setTimeout(() => {
            showToast('Бажаєте отримувати сповіщення про замовлення?', 'info', {
                text: 'Увімкнути',
                onClick: () => {
                    Notification.requestPermission().then(permission => {
                        if (permission === 'granted') {
                            subscribeUserToPush();
                        }
                    });
                }
            });
        }, 3000);
    }
}

async function subscribeUserToPush() {
    try {
        console.time('>>> PWA: Push Check');
        if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
            return;
        }

        // 1. Try to get existing registration first for speed
        let registration = await navigator.serviceWorker.getRegistration();
        // 2. If no registration OR it's not active yet, wait for ready
        if (!registration || !registration.active) {
            registration = await navigator.serviceWorker.ready;
        }

        const subscription = await registration.pushManager.getSubscription();

        if (subscription) {
            sendSubscriptionToServer(subscription);
            return;
        }

        if (!window.vapidPublicKey) {
            logErrorToServer('PWA: VAPID public key not found');
            return;
        }

        const applicationServerKey = urlBase64ToUint8Array(window.vapidPublicKey);
        const newSubscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: applicationServerKey
        });

        sendSubscriptionToServer(newSubscription);
    } catch (err) {
        logErrorToServer('PWA: Failed to subscribe user: ' + err.message);
    }
}

async function sendSubscriptionToServer(subscription) {
    const key = subscription.getKey ? subscription.getKey('p256dh') : '';
    const auth = subscription.getKey ? subscription.getKey('auth') : '';
    
    // Convert array buffers to base64
    const p256dh = btoa(String.fromCharCode.apply(null, new Uint8Array(key)));
    const authSecret = btoa(String.fromCharCode.apply(null, new Uint8Array(auth)));

    const payload = {
        endpoint: subscription.endpoint,
        p256dh: p256dh,
        auth: authSecret
    };

    try {
        await fetch('/api/notifications/subscribe', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': getCsrfToken() // Assuming a helper or available token
            },
            body: JSON.stringify(payload)
        });
    } catch (err) {
        logErrorToServer('PWA: Error syncing subscription with server: ' + err.message);
    }
}

function getCsrfToken() {
    if (window.__csrf && typeof window.__csrf.token === 'function') {
        return window.__csrf.token() || '';
    }
    const name = 'XSRF-TOKEN';
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop()?.split(';').shift() || '';
    return '';
}

function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}
