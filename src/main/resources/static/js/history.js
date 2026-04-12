/* ═══════════════════════════════════════════════════════
   SmartDine — Order History Logic
   ═══════════════════════════════════════════════════════ */

let allOrders = [];
let currentTab = 'active';

const ACTIVE_STATUSES = ['NEW', 'PREPARING', 'READY'];
const PAST_STATUSES = ['DELIVERED', 'COMPLETED', 'CANCELLED'];

// ── Initial Load ──
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    if (window.isAuthenticated) {
        loadHistory();
    } else {
        const loading = document.getElementById('historyLoading');
        const empty = document.getElementById('historyEmpty');
        if (loading) loading.style.display = 'none';
        if (empty) {
            empty.innerHTML = `
                <div class="empty-state">
                    <h3>🔐 Авторизація</h3>
                    <p>Для перегляду історії замовлень, будь ласка, увійдіть у свій аккаунт.</p>
                    <a href="/oauth2/authorization/keycloak" class="btn btn-primary mt-1">Увійти</a>
                </div>
            `;
            empty.classList.remove('hidden');
        }
    }
});

function initTabs() {
    const tabs = document.querySelectorAll('.menu-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentTab = tab.getAttribute('data-tab');
            renderOrders();
        });
    });
}

async function loadHistory() {
    const loading = document.getElementById('historyLoading');
    const container = document.getElementById('ordersContainer');
    const empty = document.getElementById('historyEmpty');

    try {
        const res = await fetch('/api/orders/history', { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Помилка завантаження історії');

        allOrders = await res.json();
        loading.style.display = 'none';
        renderOrders();
    } catch (err) {
        loading.style.display = 'none';
        showToast('❌ ' + err.message, 'error');
    }
}

function renderOrders() {
    const container = document.getElementById('ordersContainer');
    const empty = document.getElementById('historyEmpty');

    const filtered = allOrders.filter(order => {
        if (currentTab === 'active') return ACTIVE_STATUSES.includes(order.status);
        return PAST_STATUSES.includes(order.status);
    });

    if (filtered.length === 0) {
        container.innerHTML = '';
        empty.classList.remove('hidden');
        return;
    }

    empty.classList.add('hidden');
    container.innerHTML = filtered.map(order => createOrderCard(order)).join('');
    initStarPickers();
}

function createOrderCard(order) {
    const date = new Date(order.createdAt).toLocaleString('uk-UA', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });

    const total = typeof order.totalPrice === 'number' ? order.totalPrice : (parseFloat(order.totalPrice) || 0);
    const loyaltyDiscount = typeof order.loyaltyDiscount === 'number'
        ? order.loyaltyDiscount
        : (parseFloat(order.loyaltyDiscount) || 0);
    const amountToPay = typeof order.amountToPay === 'number'
        ? order.amountToPay
        : (Number.isFinite(total - loyaltyDiscount) ? (total - loyaltyDiscount) : total);

    const canReview = order.paymentStatus === 'SUCCESS' && (order.status === 'READY' || order.status === 'COMPLETED');
    const hasServiceReview = order.serviceRating != null;

    return `
        <div class="active-order-card history-card fade-in-up">
            <div class="active-order-header">
                <h3>Замовлення #${order.id}</h3>
                <div class="status-badges-group">
                    <span class="order-status-badge status-${order.status.toLowerCase()}">${formatOrderStatus(order.status)}</span>
                    <span class="order-status-badge payment-${order.paymentStatus.toLowerCase()}">${formatPaymentStatus(order.paymentStatus)}</span>
                </div>
            </div>
            
            <div class="history-card-date">${date}</div>

            <div class="active-order-minimal-info mt-1">
                ${order.items.map(item => `
                    <div class="cart-item">
                        <div class="cart-item-info">
                            <div class="cart-item-name">${item.dishName}</div>
                        </div>
                        <div class="cart-item-qty">× ${item.quantity}</div>
                        <div class="cart-item-price">${(item.price * item.quantity).toFixed(2)} ₴</div>
                    </div>
                `).join('')}
            </div>

            <div class="active-order-footer">
                <div class="cart-total flex-column gap-1">
                    <div class="d-flex justify-between gap-2">
                        <span>Загальна сума:</span>
                        <span class="text-gold">${total.toFixed(2)} ₴</span>
                    </div>
                    ${loyaltyDiscount > 0 ? `
                        <div class="d-flex justify-between gap-2">
                            <span class="text-muted">Знижка балами:</span>
                            <span class="text-muted">- ${loyaltyDiscount.toFixed(2)} ₴</span>
                        </div>
                        <div class="d-flex justify-between gap-2">
                            <span>До сплати:</span>
                            <span class="text-gold">${amountToPay.toFixed(2)} ₴</span>
                        </div>
                    ` : ''}
                </div>
                ${order.paymentStatus === 'PENDING' && currentTab === 'active' ? `
                    <button class="btn btn-primary btn-sm w-full mt-1" onclick="window.location.href='/'">
                        💳 Перейти до оплати
                    </button>
                ` : ''}

                ${hasServiceReview ? `
                    <div class="order-review-block mt-1">
                        <div class="order-review-header">
                            <span>🌟 Мій відгук</span>
                            ${renderStars(order.serviceRating)}
                        </div>
                        ${order.serviceComment ? `
                            <div class="order-review-comment text-muted text-sm">
                                "${escapeHtml(order.serviceComment)}"
                            </div>
                        ` : ''}
                    </div>
                    <div class="text-muted mt-1">
                        <div class="mb-1">Оцінки страв:</div>
                        ${order.items.map(item => `
                            <div class="d-flex justify-between align-center gap-2 mt-0">
                                <div class="text-sm">${item.dishName}</div>
                                <div>${item.rating ? renderStars(item.rating) : '<span class="text-muted">—</span>'}</div>
                            </div>
                        `).join('')}
                    </div>
                ` : (canReview ? `
                    <div class="cart-summary mt-1 p-1">
                        <div class="order-review-header mb-1">Залишити відгук</div>

                        <div class="d-flex justify-between align-center gap-2">
                            <div class="text-muted">Сервіс:</div>
                            <div class="star-picker" data-order-id="${order.id}" data-type="service"></div>
                        </div>

                        <div class="mt-1">
                            <div class="text-muted text-xs mb-1">Побажання (опціонально):</div>
                            <textarea class="form-input w-full" id="reviewComment-${order.id}" rows="2"
                                      placeholder="Наприклад: було б добре додати серветки..."></textarea>
                        </div>

                        <div class="mt-1">
                            <div class="text-muted text-xs mb-1">Страви:</div>
                            ${order.items.map(item => `
                                <div class="d-flex justify-between align-center gap-2 mt-0">
                                    <div class="text-sm">${item.dishName}</div>
                                    <div class="star-picker" data-order-id="${order.id}" data-type="dish" data-dish-id="${item.dishId}"></div>
                                </div>
                            `).join('')}
                        </div>

                        <button class="btn btn-primary btn-sm w-full mt-1" onclick="submitOrderReview(${order.id})">
                            ✅ Надіслати відгук
                        </button>
                        <div class="text-muted text-xs mt-1" id="reviewHint-${order.id}"></div>
                    </div>
                ` : '')}
            </div>
        </div>
    `;
}

function renderStars(value) {
    const v = parseInt(value || 0, 10);
    let out = '';
    for (let i = 1; i <= 5; i++) {
        out += i <= v ? '★' : '☆';
    }
    return `<span style="color: var(--accent-gold); letter-spacing: 1px;">${out}</span>`;
}

function initStarPickers() {
    document.querySelectorAll('.star-picker').forEach(el => {
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

async function submitOrderReview(orderId) {
    const hint = document.getElementById(`reviewHint-${orderId}`);
    try {
        const servicePicker = document.querySelector(`.star-picker[data-order-id="${orderId}"][data-type="service"]`);
        const serviceRating = servicePicker ? parseInt(servicePicker.dataset.value || '0', 10) : 0;
        if (serviceRating < 1 || serviceRating > 5) {
            throw new Error('Оберіть оцінку сервісу (1-5)');
        }

        const dishPickers = Array.from(document.querySelectorAll(`.star-picker[data-order-id="${orderId}"][data-type="dish"]`));
        const dishRatings = dishPickers.map(p => ({
            dishId: parseInt(p.dataset.dishId, 10),
            rating: parseInt(p.dataset.value || '0', 10)
        })).filter(x => x.dishId && x.rating >= 1 && x.rating <= 5);

        const commentEl = document.getElementById(`reviewComment-${orderId}`);
        const comment = commentEl ? commentEl.value : null;

        const res = await fetch(`/api/orders/${orderId}/reviews`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ serviceRating, comment, dishRatings })
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка надсилання відгуку');
        }

        showToast('✅ Дякуємо за відгук!', 'success');
        await loadHistory();
    } catch (e) {
        if (hint) hint.textContent = e.message;
        showToast('❌ ' + e.message, 'error');
    }
}

function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
