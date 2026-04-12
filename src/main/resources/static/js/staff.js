/* ═══════════════════════════════════════════════════════
   SmartDine — Staff Orders JS (Active orders management)
   ═══════════════════════════════════════════════════════ */



// ── Status labels ──
const STATUS_LABELS = {
    NEW: 'Нове',
    PREPARING: 'Готується',
    READY: 'Готове',
    COMPLETED: 'Завершене',
    CANCELLED: 'Скасоване'
};

const STATUS_FLOW = {
    NEW: 'PREPARING',
    PREPARING: 'READY',
    READY: 'COMPLETED'
};

const STATUS_FLOW_LABELS = {
    NEW: '🔥 Готувати',
    PREPARING: '✅ Готове',
    READY: '🏁 Завершити'
};

// ── State ──
let activeOrders = [];
let currentFilter = 'all';

// ── Load active orders ──
async function loadOrders() {
    try {
        const res = await fetch('/api/orders/active', { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Помилка ' + res.status);
        activeOrders = await res.json();
        renderOrders();
        document.getElementById('lastUpdate').textContent =
            'Оновлено: ' + new Date().toLocaleTimeString('uk-UA');
    } catch (err) {
        showToast('❌ Не вдалось завантажити: ' + err.message, 'error');
    }
}

// ── Filter orders ──
function filterOrders(status, btn) {
    currentFilter = status;
    document.querySelectorAll('.menu-tab').forEach(t => t.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderOrders();
}

// ── Render orders board ──
function renderOrders() {
    const activeBoard = document.getElementById('activeOrdersBoard');
    const readyBoard = document.getElementById('readyOrdersBoard');
    const activeSection = document.getElementById('activeOrdersSection');
    const readySection = document.getElementById('readyOrdersSection');
    const loading = document.getElementById('ordersLoading');
    const empty = document.getElementById('ordersEmpty');

    loading.style.display = 'none';

    let filtered = activeOrders;
    if (currentFilter !== 'all') {
        filtered = filtered.filter(o => o.status === currentFilter);
    }

    if (filtered.length === 0) {
        activeBoard.innerHTML = '';
        readyBoard.innerHTML = '';
        activeSection.style.display = 'none';
        readySection.style.display = 'none';
        empty.classList.remove('hidden');
        return;
    }

    empty.classList.add('hidden');

    // Split orders into active and ready
    const activeItems = filtered.filter(o => o.status === 'NEW' || o.status === 'PREPARING');
    const readyItems = filtered.filter(o => o.status === 'READY');

    // Render active orders
    if (activeItems.length > 0) {
        activeSection.style.display = 'block';
        activeBoard.innerHTML = activeItems.map(createOrderCardHtml).join('');
    } else {
        activeSection.style.display = 'none';
        activeBoard.innerHTML = '';
    }

    // Render ready orders
    if (readyItems.length > 0) {
        readySection.style.display = 'block';
        readyBoard.innerHTML = readyItems.map(createOrderCardHtml).join('');
    } else {
        readySection.style.display = 'none';
        readyBoard.innerHTML = '';
    }
}

function createOrderCardHtml(order) {
    const statusLabel = STATUS_LABELS[order.status] || order.status;
    const nextStatus = STATUS_FLOW[order.status];
    const nextLabel = STATUS_FLOW_LABELS[order.status];
    const date = order.createdAt ? new Date(order.createdAt).toLocaleString('uk-UA') : '—';
    const tableLabel = (order.tableNumber !== null && order.tableNumber !== undefined) ? `• Стіл №${order.tableNumber}` : '';

    const itemsHtml = (order.items || []).map(item => `
        <div class="order-item-row">
            <span>${item.dishName} × ${item.quantity}</span>
            <span class="text-muted">${item.specialRequest || ''}</span>
        </div>
    `).join('');

    const actionBtn = nextStatus ? `
        <button class="btn ${order.status === 'NEW' ? 'btn-warning' : order.status === 'PREPARING' ? 'btn-success' : 'btn-secondary'} btn-sm"
                onclick="updateStatus(${order.id}, '${nextStatus}')">
            ${nextLabel}
        </button>
    ` : '';

    const cancelBtn = (order.status === 'NEW' || order.status === 'PREPARING') ? `
        <button class="btn btn-danger btn-sm" onclick="updateStatus(${order.id}, 'CANCELLED')">
            ❌ Скасувати
        </button>
    ` : '';

    const waiterBtn = order.needsWaiter ? `
        <button class="btn btn-warning btn-sm" onclick="dismissWaiterCall(${order.id})">
            🙋‍♂️ Підійти до столу
        </button>
    ` : '';

    return `
        <div class="order-card fade-in-up ${order.needsWaiter ? 'waiter-alert' : ''}">
            <div class="order-card-header">
                <div class="d-flex align-center gap-1 flex-wrap">
                    <span class="order-id">#${order.id} <span class="text-muted order-price-cell">${tableLabel}</span></span>
                    ${order.needsWaiter ? '<span class="waiter-called-badge">Потрібен офіціант!</span>' : ''}
                </div>
                <span class="order-status-badge status-${order.status.toLowerCase()}">${statusLabel}</span>
            </div>
            <div class="order-card-body">
                ${itemsHtml}
            </div>
            <div class="order-card-footer">
                <div class="order-info-block">
                    <span class="order-total">${order.totalPrice.toFixed(2)} ₴</span>
                    <div class="order-time">${date}</div>
                </div>
                <div class="order-actions-container">
                    ${waiterBtn}
                    ${actionBtn}
                    ${cancelBtn}
                </div>
            </div>
        </div>
    `;
}

// ── Update order status ──
async function updateStatus(orderId, newStatus) {
    try {
        const res = await fetch(`/api/orders/${orderId}/status?newStatus=${newStatus}`, {
            method: 'PUT',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('Помилка ' + res.status);

        const statusLabel = STATUS_LABELS[newStatus] || newStatus;
        showToast(`✅ Замовлення #${orderId} → ${statusLabel}`, 'success');
        loadOrders();
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

async function dismissWaiterCall(orderId) {
    try {
        const res = await fetch(`/api/orders/${orderId}/call-waiter`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('Помилка');

        showToast(`✅ Виклик до столу #${orderId} скасовано`, 'success');
        loadOrders();
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

/* // ── Auto-refresh removed in favor of SSE (sse.js) ──
let refreshInterval = setInterval(loadOrders, 10000); */

// ── Init ──
document.addEventListener('DOMContentLoaded', loadOrders);
