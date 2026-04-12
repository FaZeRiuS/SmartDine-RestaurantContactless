/**
 * ui.js - Common UI logic and utilities for all sections (Customer, Staff, Admin)
 */

document.addEventListener('DOMContentLoaded', () => {
    initMobileMenu();
    if (window.isAuthenticated) {
        loadLoyaltySummary();
    }
    initPushNotifications();
});

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
            if (balanceEl) balanceEl.textContent = loyaltyBalanceCache.toFixed(2);
            headerBalanceEls.forEach(el => el.textContent = loyaltyBalanceCache.toFixed(2));
        } else {
            if (balanceEl) balanceEl.textContent = '—';
            headerBalanceEls.forEach(el => el.textContent = '—');
        }

        if (headerRateEls.length > 0 && Number.isFinite(rate)) {
            headerRateEls.forEach(el => el.textContent = (rate * 100).toFixed(0));
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

// ── Responsive Burger Menu Handler (Sitewide) ──
function initMobileMenu() {
    const toggle = document.getElementById('menuToggle');
    const nav = document.getElementById('burgerNav');
    
    if (toggle && nav) {
        // Toggle on click
        toggle.addEventListener('click', (e) => {
            e.stopPropagation();
            nav.classList.toggle('open');
            toggle.innerHTML = nav.classList.contains('open') ? '✕' : '☰';
        });

        // Close on outside click
        document.addEventListener('click', (e) => {
            if (nav.classList.contains('open') && !nav.contains(e.target) && !toggle.contains(e.target)) {
                nav.classList.remove('open');
                toggle.innerHTML = '☰';
            }
        });

        // Close on link click
        nav.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                nav.classList.remove('open');
                toggle.innerHTML = '☰';
            });
        });
    }
}
// ── Push Notifications ──

async function initPushNotifications() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        console.log('>>> PWA: Push messaging is not supported');
        return;
    }

    if (Notification.permission === 'denied') {
        console.log('>>> PWA: Push notification permission denied');
        return;
    }

    // If permission is default (neither granted nor denied), we might want to ask.
    // However, best practice is to ask after a user action (like placing an order).
    // For now, we'll try to subscribe if already granted, or just check.
    if (Notification.permission === 'granted') {
        subscribeUserToPush();
    } else {
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
        const registration = await navigator.serviceWorker.ready;
        const subscription = await registration.pushManager.getSubscription();

        if (subscription) {
            // Already subscribed, but check if we need to update the server (e.g. after login)
            sendSubscriptionToServer(subscription);
            return;
        }

        if (!window.vapidPublicKey) {
            console.error('>>> PWA: VAPID public key not found');
            return;
        }

        const applicationServerKey = urlBase64ToUint8Array(window.vapidPublicKey);
        const newSubscription = await registration.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: applicationServerKey
        });

        console.log('>>> PWA: User subscribed:', newSubscription);
        sendSubscriptionToServer(newSubscription);
    } catch (err) {
        console.error('>>> PWA: Failed to subscribe user:', err);
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
        console.log('>>> PWA: Subscription synced with server');
    } catch (err) {
        console.error('>>> PWA: Error syncing subscription with server:', err);
    }
}

function getCsrfToken() {
    const name = 'XSRF-TOKEN';
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
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
