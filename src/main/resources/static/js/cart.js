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

            const headers = { 'Content-Type': 'application/json' };
            if (typeof getCsrfToken === 'function') {
                headers['X-XSRF-TOKEN'] = getCsrfToken();
            }

            const postRes = await fetch(`/api/orders/${orderId}/reviews`, {
                method: 'POST',
                headers,
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
            if (hint) hint.textContent = e.message;
            showToast('\u274c ' + e.message, 'error');
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
            checkLastOrder();
        } else {
            const lastOrdCont = document.getElementById('lastOrderContainer');
            const heroSection = document.getElementById('heroSection');
            if (heroSection && (!lastOrdCont || lastOrdCont.classList.contains('hidden'))) {
                heroSection.style.display = '';
            }
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
    syncHomeLayoutAfterActiveOrderSwap();
});

function checkActiveOrder() {
    refreshActiveOrderPanel();
}

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
        console.error('Error fetching last order:', e);
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
        showToast('❌ Помилка: ' + err.message, 'error');
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
        showToast('❌ Помилка оплати: ' + err.message, 'error');
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
    // Initial load happens in layout.html after identity is fetched.
    loadLoyaltySummary();
});

