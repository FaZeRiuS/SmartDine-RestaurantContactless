/* ═══════════════════════════════════════════════════════
   SmartDine — Cart & Order JS v5.0 (Multi-Order Flow)
   ═══════════════════════════════════════════════════════ */

async function injectReviewModalFromServer(orderId) {
    const res = await fetch(`/htmx/orders/${orderId}/review-modal`, { credentials: 'same-origin' });
    if (!res.ok) return null;
    const html = await res.text();
    const t = document.createElement('template');
    t.innerHTML = html.trim();
    const wrap = t.content.firstElementChild;
    if (!wrap) return null;

    const modal = wrap.querySelector('.modal');
    if (!modal) return null;

    const close = () => {
        wrap.remove();
        document.removeEventListener('keydown', onKeydown);
    };
    const onKeydown = (e) => {
        if (e.key === 'Escape') close();
    };
    document.addEventListener('keydown', onKeydown);
    wrap.addEventListener('click', (e) => {
        if (e.target === wrap) close();
    });
    modal.querySelector('#reviewModalCloseBtn')?.addEventListener('click', close);

    initStarPickersIn(modal);

    modal.querySelector('#reviewModalSubmitBtn')?.addEventListener('click', async () => {
        const hint = modal.querySelector('#reviewModalHint');
        try {
            showToast('Надсилаємо відгук...', 'info');
            const servicePicker = modal.querySelector('.star-picker[data-type="service"]');
            const serviceRating = servicePicker ? parseInt(servicePicker.dataset.value || '0', 10) : 0;
            if (serviceRating < 1 || serviceRating > 5) {
                throw new Error('Оберіть оцінку сервісу (1-5)');
            }

            const dishPickers = Array.from(modal.querySelectorAll('.star-picker[data-type="dish"]'));
            const dishRatings = dishPickers.map(p => ({
                dishId: parseInt(p.dataset.dishId, 10),
                rating: parseInt(p.dataset.value || '0', 10)
            })).filter(x => x.dishId && x.rating >= 1 && x.rating <= 5);

            const comment = modal.querySelector('#reviewModalComment')?.value ?? null;

            const postRes = await fetch(`/api/orders/${orderId}/reviews`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ serviceRating, comment, dishRatings })
            });

            if (!postRes.ok) {
                let msg = await postRes.text();
                try { msg = JSON.parse(msg).message || msg; } catch (e) { /* ignore */ }
                throw new Error(msg || 'Помилка надсилання відгуку');
            }

            showToast('\u2705 \u0414\u044f\u043a\u0443\u0454\u043c\u043e \u0437\u0430 \u0432\u0456\u0434\u0433\u0443\u043a!', 'success');
            close();
            checkActiveOrder();
        } catch (e) {
            const fb = 'Не вдалося надіслати відгук. Спробуйте ще раз.';
            const display =
                typeof userFacingErrorMessage === 'function'
                    ? userFacingErrorMessage(e, fb, (m) => m.includes('Оберіть оцінку'))
                    : fb;
            if (hint) hint.textContent = display;
            showToast('\u274c ' + display, 'error');
        }
    });

    return wrap;
}

function maybeAutoOpenReviewModal(order) {
    try {
        if (window.isCustomer === false) return;
        if (!order || !order.id) return;
        const canReview = order.paymentStatus === 'SUCCESS' && (order.status === 'READY' || order.status === 'COMPLETED');
        if (!canReview) return;
        if (order.serviceRating != null) return;

        const key = `reviewModalShown:order:${order.id}`;
        if (sessionStorage.getItem(key) === '1') return;
        sessionStorage.setItem(key, '1');

        injectReviewModalFromServer(order.id).then((overlay) => {
            if (overlay) document.body.appendChild(overlay);
        }).catch(() => {});
    } catch (e) {
        // ignore UI errors
    }
}

// ── Active order panel (HTMX + Thymeleaf fragment) ──
function refreshActiveOrderPanel() {
    const el = document.getElementById('activeOrderContainer');
    if (!el || !window.htmx) return;
    const url = el.getAttribute('hx-get') || '/htmx/orders/active-panel';
    window.htmx.ajax('GET', url, { target: '#activeOrderContainer', swap: 'innerHTML' });
}

function syncHomeLayoutAfterActiveOrderSwap() {
    const container = document.getElementById('activeOrderContainer');
    if (!container) return;

    const hasActive = container.querySelector('.active-order-card') != null;

    if (hasActive) {
        const lastOrdCont = document.getElementById('lastOrderContainer');
        if (lastOrdCont) lastOrdCont.classList.add('hidden');
        const heroSection = document.getElementById('heroSection');
        if (heroSection) heroSection.style.display = 'none';
    } else {
        if (window.isAuthenticated) {
            checkLastOrder().finally(() => {
                syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: false });
            });
        } else {
            const lastOrdCont = document.getElementById('lastOrderContainer');
            const heroSection = document.getElementById('heroSection');
            if (heroSection && (!lastOrdCont || lastOrdCont.classList.contains('hidden'))) {
                heroSection.style.display = '';
            }
            syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: false });
        }
    }
}

document.body?.addEventListener('htmx:afterSwap', (evt) => {
    const t = evt.detail?.target;
    if (!t || t.id !== 'activeOrderContainer') return;
    if (t.querySelector('.active-order-card')) {
        t.classList.remove('hidden');
    } else {
        t.classList.add('hidden');
    }
    // Unlock / relabel first; then load last-order widget (async). A second sync runs in checkLastOrder().finally.
    syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: true });
    syncHomeLayoutAfterActiveOrderSwap();

    // Cart page UX: if there is an active (unpaid) order, hide cart content block to avoid
    // showing an empty cart alongside the active order card (items are added to order directly).
    const cartContent = document.getElementById('cartContent');
    const suppressedHint = document.getElementById('cartSuppressedHint');
    if (cartContent) {
        const hasActive = t.querySelector('.active-order-card') != null;
        if (hasActive) {
            cartContent.classList.add('hidden');
            if (suppressedHint) suppressedHint.classList.remove('hidden');
        } else {
            cartContent.classList.remove('hidden');
            if (suppressedHint) suppressedHint.classList.add('hidden');
        }
    }
});

document.body?.addEventListener('htmx:afterSwap', (evt) => {
    const t = evt.detail?.target;
    if (!t || t.id !== 'menuCategoriesRoot') return;
    updateAddToCartButtonLabels();
});

// Forms use hx-swap="none" + toast OOB; refresh labels and active-order panel so UX matches API state.
document.body?.addEventListener('htmx:afterRequest', (evt) => {
    if (!evt.detail?.successful) return;
    const elt = evt.detail?.elt;
    if (!elt || !elt.closest?.('form[hx-post="/htmx/cart/items"]')) return;
    syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: true });
    refreshActiveOrderPanel();
});

// Home / cart: after active-order fragment loads (incl. SSE refresh), resync buttons even if afterSwap order differs.
document.body?.addEventListener('htmx:afterRequest', (evt) => {
    if (!evt.detail?.successful) return;
    const xhr = evt.detail?.xhr;
    if (!xhr || typeof xhr.responseURL !== 'string' || !xhr.responseURL.includes('/htmx/orders/active-panel')) {
        return;
    }
    syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: false });
});

function checkActiveOrder() {
    refreshActiveOrderPanel();
}

const ADD_TO_CART_LABEL = '\u{1F6D2} \u0423 \u043a\u043e\u0448\u0438\u043a';
const ADD_TO_ORDER_LABEL = '\u{1F6CE}\uFE0F \u0414\u043e\u0434\u0430\u0442\u0438 \u043d\u0430 \u0437\u0430\u043c\u043e\u0432\u043b\u0435\u043d\u043d\u044f';

/** Avoid stale "active order" after status changes (browser cache of GET /api/orders/my-active). */
const MY_ACTIVE_FETCH = {
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' }
};

function dishNameForAddButton(btn) {
    const card = btn.closest('.dish-card');
    const n = card?.querySelector('.dish-name')?.textContent?.trim();
    return n || '';
}

const ACTIVE_ORDER_STATUSES = ['NEW', 'PREPARING', 'READY'];

/**
 * Paid + still in kitchen flow → cannot add dishes. COMPLETED/CANCELLED with SUCCESS must NOT lock.
 * @param {object | null} order
 * @param {{ bypassLock?: boolean }} [extra]
 */
function applyCartButtonStateFromOrder(order, extra) {
    const bypassLock = extra && extra.bypassLock === true;
    const isActivePhase = !!(order && order.status && ACTIVE_ORDER_STATUSES.includes(order.status));
    const unpaid = order && order.paymentStatus !== 'SUCCESS';
    let shouldLock = !!(isActivePhase && order && order.paymentStatus === 'SUCCESS');
    if (bypassLock) {
        shouldLock = false;
    }

    document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
        const name = dishNameForAddButton(btn);
        if (order && isActivePhase && unpaid) {
            btn.textContent = ADD_TO_ORDER_LABEL;
            btn.setAttribute('aria-label', name ? `Додати ${name} на замовлення` : 'Додати на замовлення');
        } else {
            btn.textContent = ADD_TO_CART_LABEL;
            btn.setAttribute('aria-label', name ? `Додати ${name} у кошик` : 'Додати у кошик');
        }
    });

    // Select by class and by form/type to be safe (robustness)
    const activeBtnSelector = '.add-to-cart-btn, form[hx-post="/htmx/cart/items"] button[type="submit"]';
    document.querySelectorAll(activeBtnSelector).forEach(btn => {
        try {
            if (shouldLock) {
                btn.disabled = true;
                btn.setAttribute('disabled', 'disabled');
                btn.setAttribute('aria-disabled', 'true');
            } else {
                btn.disabled = false;
                btn.removeAttribute('disabled');
                btn.removeAttribute('aria-disabled');
            }
        } catch { /* ignore */ }
    });

    const repeatBtn = document.getElementById('repeatOrderBtn');
    if (repeatBtn) {
        if (shouldLock) {
            repeatBtn.disabled = true;
            repeatBtn.setAttribute('aria-disabled', 'true');
        } else {
            repeatBtn.disabled = false;
            repeatBtn.removeAttribute('aria-disabled');
        }
    }

    return { shouldLock };
}

/**
 * Instant UI from SSE payload (no HTTP). Call before HTMX refresh so stale /api/orders/my-active cannot flash-lock.
 * @param {object} order OrderResponseDto JSON
 */
function applyAddToCartButtonsFromOrderSnapshot(order) {
    if (!order || typeof order !== 'object') return;
    applyCartButtonStateFromOrder(order);
    const active = ACTIVE_ORDER_STATUSES.includes(order.status);
    const paid = order.paymentStatus === 'SUCCESS';
    if (!(active && paid)) {
        window.__cartButtonsUnlockPriorityUntil = Date.now() + 10000;
    }
}

window.applyAddToCartButtonsFromOrderSnapshot = applyAddToCartButtonsFromOrderSnapshot;

/** Once per order per tab session — avoid spamming after every HTMX/SSE refresh when order is paid. */
const PAID_LOCK_TOAST_STORAGE_KEY = (orderId) => `paidLockToastShown:order:${orderId}`;
const PENDING_PAID_LOCK_TOASTS = new Set();

function paidLockToastAlreadyShownForOrder(orderId) {
    if (orderId == null) return true;
    if (PENDING_PAID_LOCK_TOASTS.has(String(orderId))) return true;
    try {
        return sessionStorage.getItem(PAID_LOCK_TOAST_STORAGE_KEY(orderId)) === '1';
    } catch {
        return true;
    }
}

function markPaidLockToastShownForOrder(orderId) {
    if (orderId == null) return;
    PENDING_PAID_LOCK_TOASTS.add(String(orderId));
    try {
        sessionStorage.setItem(PAID_LOCK_TOAST_STORAGE_KEY(orderId), '1');
    } catch { /* ignore */ }
}

/**
 * Single source of truth: labels + paid-lock for add-to-cart / repeat-last-order.
 * @param {{ showPaidLockToast?: boolean }} opts
 */
async function syncAddToCartButtonsWithActiveOrder(opts) {
    const showPaidLockToast = opts && opts.showPaidLockToast === true;
    try {
        const res = await fetch('/api/orders/my-active?_=' + Date.now(), MY_ACTIVE_FETCH);
        if (!res.ok) return;

        let order = null;
        if (res.status === 200) {
            order = await res.json();
        } else if (res.status === 204) {
            // No active order found (order === null, which is correct for unlock)
        } else {
            // Handle other status codes if necessary, but for now just assume no order
        }

        const priorityUnlock = window.__cartButtonsUnlockPriorityUntil
            && Date.now() < window.__cartButtonsUnlockPriorityUntil;
        const rawLock = !!(order && ACTIVE_ORDER_STATUSES.includes(order.status) && order.paymentStatus === 'SUCCESS');
        const bypassLock = priorityUnlock && rawLock;

        const { shouldLock } = applyCartButtonStateFromOrder(order, { bypassLock });

        if (shouldLock && showPaidLockToast) {
            const oid = order && order.id != null ? order.id : null;
            if (oid != null && !paidLockToastAlreadyShownForOrder(oid)) {
                // Mark immediately to avoid concurrent async calls showing it too.
                markPaidLockToastShownForOrder(oid);
                const msg = '⚠️ Активне замовлення вже оплачене — додавання страв недоступне';
                if (typeof showToast === 'function') showToast(msg, 'info');
            }
        }
    } catch (e) {
        /* ignore */
    }
}

/**
 * When customer has an active unpaid order (NEW/PREPARING/READY), show "Додати на замовлення" on menu cards.
 */
async function updateAddToCartButtonLabels() {
    await syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: false });
}

window.updateAddToCartButtonLabels = updateAddToCartButtonLabels;

async function enforcePaidActiveOrderLock() {
    await syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: true });
}

window.enforcePaidActiveOrderLock = enforcePaidActiveOrderLock;
window.syncAddToCartButtonsWithActiveOrder = syncAddToCartButtonsWithActiveOrder;

function openActiveOrderReviewModal() {
    if (window.isCustomer === false) return;
    const o = window.__lastActiveOrderForReview;
    if (!o || !o.id) return;
    injectReviewModalFromServer(o.id).then((overlay) => {
        if (overlay) document.body.appendChild(overlay);
    }).catch(() => {});
}

// ── Repeat Last Order Logic ──
async function checkLastOrder() {
    const container = document.getElementById('lastOrderContainer');
    if (!container) return; // Only exists on specific pages (like Index)

    try {
        const res = await fetch('/api/orders/history', { credentials: 'same-origin' });
        if (!res.ok) {
            container.classList.add('hidden');
            return;
        }

        const data = await res.json();
        const history = Array.isArray(data) ? data : (data && data.content) ? data.content : [];
        // Filter out cancelled orders and empty orders
        const validHistory = (history || []).filter(o =>
            o.status !== 'CANCELLED' &&
            o.items && o.items.length > 0 &&
            o.totalPrice > 0
        );

        if (validHistory && validHistory.length > 0) {
            const lastOrder = validHistory[0];

            const priceEl = document.getElementById('lastOrderTotalPrice');
            if (priceEl) priceEl.textContent = Number(lastOrder.totalPrice).toFixed(2) + ' ₴';

            const itemsEl = document.getElementById('lastOrderItems');
            if (itemsEl) {
                itemsEl.innerHTML = lastOrder.items.map(item => `
                    <div class="cart-item" style="padding: 0.5rem 0;">
                        <div class="cart-item-info">
                            <div class="cart-item-name">${item.dishName}</div>
                            ${item.specialRequest ? `<div class="cart-item-special">💬 ${item.specialRequest}</div>` : ''}
                        </div>
                        <div class="cart-item-qty">× ${item.quantity}</div>
                    </div>
                `).join('');
            }

            window.lastOrderToRepeat = lastOrder;

            // Allow review modal to open even after order becomes COMPLETED (not "active" anymore)
            window.__lastActiveOrderForReview = lastOrder;
            maybeAutoOpenReviewModal(lastOrder);

            // Show the last order widget
            const container = document.getElementById('lastOrderContainer');
            if (container) {
                container.classList.remove('hidden');
            }

            const heroSection = document.getElementById('heroSection');
            if (heroSection) heroSection.style.display = 'none';
        } else {
            // No last order found or UI is not set up
            const container = document.getElementById('lastOrderContainer');
            if (container) container.classList.add('hidden');

            // Only show hero if neither active nor last order are visible
            const heroSection = document.getElementById('heroSection');
            const activeCont = document.getElementById('activeOrderContainer');
            if (heroSection && (!activeCont || activeCont.classList.contains('hidden'))) {
                heroSection.style.display = '';
            }
        }
    } catch (e) {
        if (window.__CLIENT_DEBUG && typeof console.error === 'function') {
            console.error('Error fetching last order:', e);
        }
    }
}

async function repeatLastOrder() {
    if (!window.lastOrderToRepeat || !window.lastOrderToRepeat.items) return;

    const btn = document.getElementById('repeatOrderBtn');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<div class="loading-spinner"></div> Додавання...';
    }

    try {
        // Sequentially post all items to cart
        for (const item of window.lastOrderToRepeat.items) {
            const res = await fetch('/api/cart/items', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({
                    dishId: item.dishId,
                    quantity: item.quantity,
                    specialRequest: item.specialRequest || ''
                })
            });

            if (!res.ok) throw new Error('Помилка при додаванні: ' + item.dishName);
        }

        showToast('✅ Страви додані у кошик. Перенаправлення...', 'success');

        // Allow user to see the success message before redirecting
        setTimeout(() => window.location.href = '/cart', 1000);
    } catch (err) {
        const fb = 'Не вдалося повторити замовлення. Спробуйте ще раз.';
        const display =
            typeof userFacingErrorMessage === 'function'
                ? userFacingErrorMessage(err, fb, (m) => m.startsWith('Помилка при додаванні'))
                : fb;
        showToast('❌ ' + display, 'error');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '🛒 Додати в кошик';
        }
    }
}

// ── Pay Order ──
async function payOrder() {
    const btn = document.getElementById('payBtn');
    const container = document.getElementById('activeOrderContainer');
    let orderIdRaw = container?.dataset?.orderId;
    if (orderIdRaw == null || String(orderIdRaw).trim() === '') {
        // Fallback: active order fragment renders the id inside #activeOrderId
        const idEl = container ? container.querySelector('#activeOrderId') : document.getElementById('activeOrderId');
        orderIdRaw = idEl ? idEl.textContent : null;
    }
    const orderId = orderIdRaw && String(orderIdRaw).trim() !== '' ? String(orderIdRaw).trim() : null;

    if (!orderId || !btn) {
        showToast('❌ Не знайдено ID замовлення для оплати. Оновіть сторінку.', 'error');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<div class="loading-spinner"></div> Обробка платежу...';
    showToast('Обробляємо оплату. Зараз відкриється сторінка LiqPay...', 'info');

    try {
        // LiqPay Checkout flow must be initiated by a browser navigation (not fetch),
        // because backend returns an HTML view with an auto-submit form.
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/api/payment/init';

        const orderIdInput = document.createElement('input');
        orderIdInput.type = 'hidden';
        orderIdInput.name = 'orderId';
        orderIdInput.value = orderId;
        form.appendChild(orderIdInput);

        if (window.__csrf && typeof window.__csrf.addHiddenInput === 'function') {
            await window.__csrf.addHiddenInput(form);
        }

        document.body.appendChild(form);
        form.submit();
    } catch (err) {
        const fb = 'Не вдалося почати оплату. Спробуйте ще раз.';
        const display =
            typeof userFacingErrorMessage === 'function' ? userFacingErrorMessage(err, fb) : fb;
        showToast('❌ ' + display, 'error');
        btn.disabled = false;
        btn.innerHTML = '💳 Оплатити замовлення (LiqPay)';
    }
}

// ── Global refresh hook (also used when SSE triggers cart UI updates) ──
window.refreshCartUI = function () {
    if (!window.htmx) return;
    try {
        window.htmx.ajax('GET', '/htmx/cart/widget', { swap: 'none' });
    } catch (e) { /* ignore */ }
    if (document.getElementById('cartContent')) {
        try {
            window.htmx.ajax('GET', '/htmx/cart/content', { target: '#cartContent', swap: 'outerHTML' });
        } catch (e) { /* ignore */ }
    }
};

// ── Auto-load ──
document.addEventListener('DOMContentLoaded', () => {
    syncAddToCartButtonsWithActiveOrder({ showPaidLockToast: true });
});

