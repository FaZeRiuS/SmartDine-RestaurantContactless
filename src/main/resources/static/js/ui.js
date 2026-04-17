/**
 * ui.js - Common UI logic and utilities for all sections (Customer, Staff, Admin)
 */

function initSharedUI() {
    if (window.isAuthenticated) {
        loadLoyaltySummary();
    }
    initPushNotifications();
    checkUrlParams();
    initAccessibleTabsIn(document);
    initKeyboardNavigationIn(document);
    initMobileSiteTabSwipe();
    initSequentialKeyboardTraversal();

    // Re-init after HTMX swaps (tabs/content can be injected)
    if (!document.documentElement.dataset.a11yTabsHtmxBound) {
        document.documentElement.dataset.a11yTabsHtmxBound = '1';
        document.body?.addEventListener('htmx:afterSwap', function (evt) {
            const t = evt && evt.detail ? evt.detail.target : null;
            initAccessibleTabsIn(t || document);
            initKeyboardNavigationIn(t || document);
        });
        document.body?.addEventListener('htmx:oobAfterSwap', function (evt) {
            const t = evt && evt.detail ? evt.detail.target : null;
            initAccessibleTabsIn(t || document);
            initKeyboardNavigationIn(t || document);
            // Ensure HTMX behaviors are attached for OOB-inserted nodes (e.g., toast forms).
            try {
                if (window.htmx && typeof window.htmx.process === 'function' && t) {
                    window.htmx.process(t);
                }
            } catch (e) { /* ignore */ }
        });
    }

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
window.initAccessibleTabsIn = initAccessibleTabsIn;
window.initKeyboardNavigationIn = initKeyboardNavigationIn;

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
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    
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

// ── Keyboard + Swipe Accessible Tabs (menu-tabs) ──────────────────────────────

function initAccessibleTabsIn(root) {
    const scope = root && root.querySelectorAll ? root : document;
    scope.querySelectorAll('.menu-tabs').forEach(tablist => {
        enhanceTablist(tablist);
    });
}

function enhanceTablist(tablist) {
    if (!tablist || tablist.dataset.a11yTabsInit === '1') return;
    tablist.dataset.a11yTabsInit = '1';

    const tabs = Array.from(tablist.querySelectorAll('.menu-tab'));
    if (tabs.length === 0) return;

    tablist.setAttribute('role', tablist.getAttribute('role') || 'tablist');

    tabs.forEach((tab, i) => {
        if (!tab.id) tab.id = `tab-${Math.random().toString(36).slice(2)}-${i}`;
        tab.setAttribute('role', 'tab');
        tab.setAttribute('aria-selected', tab.classList.contains('active') ? 'true' : 'false');
        // Keep all tabs reachable via Tab (sequential top-down navigation requirement).
        // We still keep aria-selected in sync for screen readers.
        if (!tab.hasAttribute('tabindex')) tab.setAttribute('tabindex', '0');

        tab.addEventListener('click', () => syncTabStateFromDom(tablist));
    });

    tablist.addEventListener('keydown', (e) => {
        const key = e.key;
        const current = document.activeElement && document.activeElement.classList.contains('menu-tab')
            ? document.activeElement
            : null;
        if (!current || current.parentElement !== tablist) return;

        if (key === 'ArrowRight' || key === 'ArrowLeft' || key === 'Home' || key === 'End') {
            // Horizontal arrow handling is overridden by global sequential traversal (see initSequentialKeyboardTraversal).
            // Keep only Home/End as a convenience within the tablist.
            const enabledTabs = Array.from(tablist.querySelectorAll('.menu-tab'))
                .filter(t => !t.disabled && t.getAttribute('aria-disabled') !== 'true');
            if (enabledTabs.length === 0) return;
            e.preventDefault();
            const t = key === 'Home' ? enabledTabs[0] : enabledTabs[enabledTabs.length - 1];
            t?.focus({ preventScroll: true });
            scrollTabIntoView(tablist, t);
            return;
        }

        if (key === 'Enter' || key === ' ') {
            e.preventDefault();
            current.click();
        }
    });

    // Swipe support (mobile): add `data-swipe-target="#someId"` on tablist.
    const targetSelector = tablist.getAttribute('data-swipe-target');
    if (targetSelector) {
        const swipeTarget = document.querySelector(targetSelector);
        if (swipeTarget) bindSwipeToSwitchTabs(swipeTarget, tablist);
    }

    syncTabStateFromDom(tablist);
}

function syncTabStateFromDom(tablist) {
    const tabs = Array.from(tablist.querySelectorAll('.menu-tab'));
    if (tabs.length === 0) return;

    const active = tabs.find(t => t.classList.contains('active')) || tabs[0];
    tabs.forEach(t => {
        t.setAttribute('aria-selected', t === active ? 'true' : 'false');
        // Do not rove tabindex; allow Tab to reach each tab button.
        if (!t.hasAttribute('tabindex')) t.setAttribute('tabindex', '0');
    });
}

function scrollTabIntoView(tablist, tab) {
    try {
        const left = tab.offsetLeft;
        const right = left + tab.offsetWidth;
        const viewLeft = tablist.scrollLeft;
        const viewRight = viewLeft + tablist.clientWidth;
        if (left < viewLeft) tablist.scrollTo({ left: Math.max(0, left - 16), behavior: 'smooth' });
        else if (right > viewRight) tablist.scrollTo({ left: right - tablist.clientWidth + 16, behavior: 'smooth' });
    } catch (e) {
        // ignore
    }
}

function bindSwipeToSwitchTabs(swipeArea, tablist) {
    if (!swipeArea || !tablist || swipeArea.dataset.a11ySwipeInit === '1') return;
    swipeArea.dataset.a11ySwipeInit = '1';

    const isTouchLike = () => window.matchMedia && window.matchMedia('(pointer: coarse)').matches;
    const getTabs = () => Array.from(tablist.querySelectorAll('.menu-tab'))
        .filter(t => !t.disabled && t.getAttribute('aria-disabled') !== 'true');

    let startX = 0;
    let startY = 0;
    let tracking = false;
    let startTime = 0;

    const onStart = (x, y) => {
        if (!isTouchLike()) return;
        startX = x;
        startY = y;
        startTime = Date.now();
        tracking = true;
    };

    const onEnd = (x, y) => {
        if (!tracking) return;
        tracking = false;

        const dx = x - startX;
        const dy = y - startY;
        const dt = Date.now() - startTime;

        // Ignore mostly-vertical gestures and very short swipes
        if (Math.abs(dx) < 40) return;
        if (Math.abs(dy) > Math.abs(dx) * 0.75) return;
        if (dt > 800) return;

        const tabs = getTabs();
        if (tabs.length < 2) return;
        const active = tabs.find(t => t.classList.contains('active')) || tabs[0];
        const idx = tabs.indexOf(active);
        if (idx < 0) return;

        // Swipe left => next tab, swipe right => prev tab
        const nextIdx = dx < 0 ? idx + 1 : idx - 1;
        const next = tabs[(nextIdx + tabs.length) % tabs.length];
        if (!next) return;
        next.click();
        scrollTabIntoView(tablist, next);
    };

    swipeArea.addEventListener('touchstart', (e) => {
        const t = e.touches && e.touches[0];
        if (!t) return;
        onStart(t.clientX, t.clientY);
    }, { passive: true });

    swipeArea.addEventListener('touchend', (e) => {
        const t = e.changedTouches && e.changedTouches[0];
        if (!t) return;
        onEnd(t.clientX, t.clientY);
    }, { passive: true });
}

// ── General keyboard navigation (header nav, dish grids) ─────────────────────

function initKeyboardNavigationIn(root) {
    const scope = root && root.querySelectorAll ? root : document;
    enhanceHeaderNavKeyboard(scope);
    enhanceDishGridKeyboard(scope);
}

function enhanceHeaderNavKeyboard(scope) {
    const mainNav = (scope.getElementById && scope.getElementById('mainNav')) || document.getElementById('mainNav');
    if (!mainNav || mainNav.dataset.a11yNavInit === '1') return;
    mainNav.dataset.a11yNavInit = '1';

    mainNav.addEventListener('keydown', (e) => {
        const key = e.key;
        if (key !== 'ArrowLeft' && key !== 'ArrowRight' && key !== 'Home' && key !== 'End') return;

        const activeEl = document.activeElement;
        if (!activeEl || activeEl.tagName !== 'A' || !mainNav.contains(activeEl)) return;

        const links = Array.from(mainNav.querySelectorAll('a')).filter(a => !a.hasAttribute('disabled') && a.getAttribute('aria-disabled') !== 'true');
        if (links.length === 0) return;

        const idx = links.indexOf(activeEl);
        if (idx < 0) return;

        e.preventDefault();
        let nextIdx = idx;
        if (key === 'Home') nextIdx = 0;
        else if (key === 'End') nextIdx = links.length - 1;
        else if (key === 'ArrowRight') nextIdx = idx + 1;
        else nextIdx = idx - 1;

        const next = links[(nextIdx + links.length) % links.length];
        next?.focus({ preventScroll: true });
    });
}

function enhanceDishGridKeyboard(scope) {
    scope.querySelectorAll('.dishes-grid').forEach(grid => {
        if (grid.dataset.a11yGridInit === '1') return;
        grid.dataset.a11yGridInit = '1';

        // Make cards keyboard focusable so Tab can walk cards sequentially
        const cards = Array.from(grid.querySelectorAll('.dish-card'));
        cards.forEach(card => {
            if (!card.hasAttribute('tabindex')) card.setAttribute('tabindex', '0');
        });

        // If user tabs into any element inside a card, keep a marker of "current" card
        grid.addEventListener('focusin', (e) => {
            const card = e.target && e.target.closest ? e.target.closest('.dish-card') : null;
            if (!card || !grid.contains(card)) return;
            cards.forEach(c => c.dataset.kbCurrent = c === card ? '1' : '');
        });

        grid.addEventListener('keydown', (e) => {
            const key = e.key;
            if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End', 'Enter'].includes(key)) return;

            const activeEl = document.activeElement;
            const currentCard = activeEl && activeEl.closest ? activeEl.closest('.dish-card') : null;
            if (!currentCard || !grid.contains(currentCard)) return;

            const currentIdx = cards.indexOf(currentCard);
            if (currentIdx < 0) return;

            const moveTo = (idx) => {
                const card = cards[Math.min(cards.length - 1, Math.max(0, idx))];
                if (!card) return;
                cards.forEach(c => c.dataset.kbCurrent = c === card ? '1' : '');
                card.focus({ preventScroll: true });
                try { card.scrollIntoView({ block: 'nearest', inline: 'nearest' }); } catch (err) { /* ignore */ }
            };

            // Calculate columns from first row geometry (best-effort, responsive-safe)
            const getColumns = () => {
                const tops = cards.map(c => Math.round(c.getBoundingClientRect().top));
                const firstTop = tops[0];
                if (!Number.isFinite(firstTop)) return 1;
                let cols = 0;
                for (let i = 0; i < tops.length; i++) {
                    if (tops[i] !== firstTop) break;
                    cols++;
                }
                return Math.max(1, cols || 1);
            };

            if (key === 'Enter') {
                // Prefer focusing the primary action in the card (add-to-cart)
                const btn = currentCard.querySelector('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
                if (btn && btn !== currentCard) {
                    e.preventDefault();
                    btn.focus({ preventScroll: true });
                }
                return;
            }

            e.preventDefault();
            const cols = getColumns();
            if (key === 'Home') moveTo(0);
            else if (key === 'End') moveTo(cards.length - 1);
            else if (key === 'ArrowRight') moveTo(currentIdx + 1);
            else if (key === 'ArrowLeft') moveTo(currentIdx - 1);
            else if (key === 'ArrowDown') moveTo(currentIdx + cols);
            else if (key === 'ArrowUp') moveTo(currentIdx - cols);
        });

        // Grid itself does not need to be a tab stop
    });
}

// ── Mobile swipe between site tabs (home/menu/cart) ─────────────────────────

function initMobileSiteTabSwipe() {
    if (document.documentElement.dataset.mobileSiteSwipeInit === '1') return;
    document.documentElement.dataset.mobileSiteSwipeInit = '1';

    const isTouchLike = () => window.matchMedia && window.matchMedia('(pointer: coarse)').matches;
    const isMobileWidth = () => window.matchMedia && window.matchMedia('(max-width: 768px)').matches;

    let startX = 0;
    let startY = 0;
    let tracking = false;
    let startTime = 0;
    let activePointerId = null;

    const normalizePath = (p) => {
        const s = String(p || '/');
        if (s === '/') return '/';
        return s.endsWith('/') ? s.slice(0, -1) : s;
    };

    const hasHorizontalScroll = (el) => {
        try {
            if (!el || el === document.body || el === document.documentElement) return false;
            const style = window.getComputedStyle(el);
            const ox = style.overflowX;
            if (ox !== 'auto' && ox !== 'scroll') return false;
            return el.scrollWidth > el.clientWidth + 4;
        } catch (e) {
            return false;
        }
    };

    const shouldIgnoreStart = (target) => {
        if (!target) return false;
        // Don't hijack swipes meant for tablists / horizontal scrollers / interactive controls
        if (target.closest && target.closest('.menu-tabs')) return true;
        if (target.closest && target.closest('a, button, input, textarea, select, label')) return true;
        // If any ancestor is a horizontal scroller, ignore
        let el = target;
        for (let i = 0; i < 6 && el; i++) {
            if (hasHorizontalScroll(el)) return true;
            el = el.parentElement;
        }
        return false;
    };

    const onStart = (x, y, target, pointerId = null) => {
        if (!isTouchLike() || !isMobileWidth()) return;
        if (shouldIgnoreStart(target)) return;
        startX = x;
        startY = y;
        startTime = Date.now();
        tracking = true;
        activePointerId = pointerId;
    };

    const onEnd = (x, y) => {
        if (!tracking) return;
        tracking = false;
        activePointerId = null;

        const dx = x - startX;
        const dy = y - startY;
        const dt = Date.now() - startTime;

        if (Math.abs(dx) < 60) return;
        if (Math.abs(dy) > Math.abs(dx) * 0.75) return;
        if (dt > 900) return;

        const nav = document.querySelector('.mobile-nav');
        if (!nav) return;
        const links = Array.from(nav.querySelectorAll('a.mobile-nav-item'))
            .filter(a => a.href && a.offsetParent !== null);
        if (links.length < 2) return;

        const currentPath = normalizePath(window.location.pathname || '/');
        const idx = links.findIndex(a => {
            try {
                const linkPath = normalizePath(new URL(a.href).pathname);
                if (linkPath === currentPath) return true;
                // Treat subpaths as belonging to the parent tab (e.g. /menu/123)
                return linkPath !== '/' && currentPath.startsWith(linkPath + '/');
            } catch (e) {
                return false;
            }
        });
        if (idx < 0) return;

        const nextIdx = dx < 0 ? idx + 1 : idx - 1;
        const next = links[(nextIdx + links.length) % links.length];
        if (!next) return;
        window.location.href = next.href;
    };

    // Prefer Pointer Events for modern Chromium (GrapheneOS/Android), fallback to Touch Events.
    document.addEventListener('pointerdown', (e) => {
        if (!e || e.pointerType !== 'touch') return;
        onStart(e.clientX, e.clientY, e.target, e.pointerId);
    }, { passive: true, capture: true });

    document.addEventListener('pointerup', (e) => {
        if (!e || e.pointerType !== 'touch') return;
        if (activePointerId != null && e.pointerId !== activePointerId) return;
        onEnd(e.clientX, e.clientY);
    }, { passive: true, capture: true });

    document.addEventListener('pointercancel', (e) => {
        if (!e || e.pointerType !== 'touch') return;
        tracking = false;
        activePointerId = null;
    }, { passive: true, capture: true });

    document.addEventListener('touchstart', (e) => {
        const t = e.touches && e.touches[0];
        if (!t) return;
        onStart(t.clientX, t.clientY, e.target, null);
    }, { passive: true, capture: true });

    document.addEventListener('touchend', (e) => {
        const t = e.changedTouches && e.changedTouches[0];
        if (!t) return;
        onEnd(t.clientX, t.clientY);
    }, { passive: true, capture: true });
}

// ── Sequential traversal (top-down) with arrows ─────────────────────────────

function initSequentialKeyboardTraversal() {
    if (document.documentElement.dataset.seqKbInit === '1') return;
    document.documentElement.dataset.seqKbInit = '1';

    const isEditable = (el) => {
        if (!el) return false;
        const tag = el.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true;
        return !!el.isContentEditable;
    };

    const isVisible = (el) => {
        try {
            if (!el) return false;
            if (el.disabled) return false;
            if (el.getAttribute && el.getAttribute('aria-disabled') === 'true') return false;
            // offsetParent is null for display:none (but also for position:fixed); check rect too
            const rect = el.getBoundingClientRect();
            if (!rect || rect.width <= 0 || rect.height <= 0) return false;
            const style = window.getComputedStyle(el);
            if (style.visibility === 'hidden' || style.display === 'none') return false;
            return true;
        } catch (e) {
            return false;
        }
    };

    const focusablesInOrder = () => {
        // We include the key UI regions requested by the user.
        const selector = [
            '#mainNav a',
            '.menu-tabs .menu-tab',
            '.dishes-grid .dish-card',
            '.dishes-grid button, .dishes-grid a, .dishes-grid input, .dishes-grid select, .dishes-grid textarea',
            '.mobile-nav a.mobile-nav-item'
        ].join(',');

        return Array.from(document.querySelectorAll(selector))
            .filter(el => el && typeof el.focus === 'function' && isVisible(el));
    };

    const inManagedArea = (el) => {
        if (!el || !el.closest) return false;
        return !!el.closest('#mainNav, .menu-tabs, .dishes-grid, .mobile-nav');
    };

    const moveFocus = (dir) => {
        const list = focusablesInOrder();
        if (list.length === 0) return;
        const active = document.activeElement;
        let idx = list.indexOf(active);
        if (idx < 0) {
            // If focus is inside a dish-card (e.g. on button), start from that card
            const card = active && active.closest ? active.closest('.dish-card') : null;
            if (card) idx = list.indexOf(card);
        }
        const next = list[(idx + dir + list.length) % list.length] || list[0];
        next.focus({ preventScroll: true });
        try { next.scrollIntoView({ block: 'nearest', inline: 'nearest' }); } catch (e) { /* ignore */ }
    };

    document.addEventListener('keydown', (e) => {
        if (!e) return;
        if (e.altKey || e.ctrlKey || e.metaKey) return;

        const key = e.key;
        if (!['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(key)) return;

        const active = document.activeElement;
        if (isEditable(active)) return;
        if (!inManagedArea(active)) return;
        // Let specialized handlers (e.g. dish grid navigation) own arrow keys.
        // This global sequential traversal runs in capture phase and can otherwise
        // prevent component-level keydown handlers from ever firing.
        if (active && active.closest && active.closest('.dishes-grid')) return;

        e.preventDefault();
        // Top-down sequential behavior: Up/Left => previous, Down/Right => next
        if (key === 'ArrowUp' || key === 'ArrowLeft') moveFocus(-1);
        else moveFocus(1);
    }, { capture: true });
}
