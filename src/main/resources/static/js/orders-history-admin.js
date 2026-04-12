/**
 * orders-history-admin.js — Admin order history page logic.
 *
 * Depends on ui.js for: showToast(), closeModal(), renderStars(), escapeHtml()
 */

(function () {
    'use strict';

    // ── State ──
    let allOrders = [];

    // ── Status helpers ──
    function getStatusLabel(status) {
        const labels = { NEW: 'Нове', PREPARING: 'Готується', READY: 'Готове', COMPLETED: 'Завершене', CANCELLED: 'Скасоване' };
        return labels[status] || status;
    }

    function getPaymentLabel(status) {
        const labels = { PENDING: 'Очікує', SUCCESS: 'Оплачено', FAILED: 'Помилка' };
        return labels[status] || status || 'Очікує';
    }

    // ── Load all orders ──
    async function loadAllOrders() {
        document.getElementById('historyLoading').style.display = '';
        document.getElementById('historyPanel').style.display = 'none';
        document.getElementById('historyEmpty').classList.add('hidden');

        try {
            const res = await fetch('/api/orders', { credentials: 'same-origin' });
            if (!res.ok) throw new Error('Помилка ' + res.status);
            allOrders = await res.json();
            applyFilters();
        } catch (err) {
            showToast('Не вдалось завантажити замовлення: ' + err.message, 'error');
            document.getElementById('historyLoading').style.display = 'none';
        }
    }

    // ── Apply filters ──
    function applyFilters() {
        const statusFilter = document.getElementById('filterStatus').value;
        const paymentFilter = document.getElementById('filterPayment').value;

        let filtered = allOrders;
        if (statusFilter) filtered = filtered.filter(o => o.status === statusFilter);
        if (paymentFilter) filtered = filtered.filter(o => o.paymentStatus === paymentFilter);

        renderOrders(filtered);
    }

    // ── Render orders table ──
    function renderOrders(orders) {
        document.getElementById('historyLoading').style.display = 'none';

        if (orders.length === 0) {
            document.getElementById('historyPanel').style.display = 'none';
            document.getElementById('historyEmpty').classList.remove('hidden');
            return;
        }

        document.getElementById('historyEmpty').classList.add('hidden');
        document.getElementById('historyPanel').style.display = '';

        const tbody = document.getElementById('historyTableBody');
        tbody.innerHTML = orders.map(order => {
            const statusLabel = getStatusLabel(order.status);
            const paymentLabel = getPaymentLabel(order.paymentStatus);
            const date = order.createdAt ? new Date(order.createdAt).toLocaleString('uk-UA') : '—';
            const table = (order.tableNumber !== null && order.tableNumber !== undefined) ? `№${order.tableNumber}` : '—';

            return `<tr>
                <td class="order-id-cell">#${order.id}</td>
                <td>${table}</td>
                <td>${order.userId ? order.userId.substring(0, 8) + '…' : '—'}</td>
                <td><span class="status-badge status-${order.status}">${statusLabel}</span></td>
                <td><span class="status-badge payment-${order.paymentStatus || 'PENDING'}">${paymentLabel}</span></td>
                <td class="order-price-cell">${order.totalPrice.toFixed(2)} ₴</td>
                <td>${date}</td>
                <td><button class="btn btn-secondary btn-sm" onclick='showOrderDetail(${JSON.stringify(order)})' aria-label="Переглянути деталі замовлення #${order.id}">👁 Деталі</button></td>
            </tr>`;
        }).join('');
    }

    // ── Order detail modal ──
    function showOrderDetail(order) {
        document.getElementById('orderDetailTitle').textContent = `Замовлення #${order.id}`;
        const body = document.getElementById('orderDetailBody');
        const tableLine = (order.tableNumber !== null && order.tableNumber !== undefined)
            ? `<div class="mb-1"><strong>Стіл:</strong> №${order.tableNumber}</div>`
            : '';

        const itemsHtml = (order.items || []).map(item => `
            <div class="order-detail-item">
                <div>
                    <span>${item.dishName} × ${item.quantity}</span>
                    <div class="text-muted text-xs">${item.specialRequest || ''}</div>
                </div>
                ${item.rating ? `<div>${renderStars(item.rating)}</div>` : ''}
            </div>
        `).join('');

        const reviewHtml = (order.serviceRating || order.serviceComment) ? `
            <div class="order-review-block">
                <div class="order-review-header">
                    <span>💬 Відгук клієнта</span>
                    ${order.serviceRating ? renderStars(order.serviceRating) : ''}
                </div>
                ${order.serviceComment ? `
                    <div class="text-muted text-sm order-review-comment">
                        "${escapeHtml(order.serviceComment)}"
                    </div>
                ` : ''}
            </div>
        ` : '';

        body.innerHTML = `
            ${tableLine}
            <div class="mb-2">
                <strong>Статус:</strong> <span class="status-badge status-${order.status}">${getStatusLabel(order.status)}</span>
                <strong class="ml-auto">Оплата:</strong> <span class="status-badge payment-${order.paymentStatus || 'PENDING'}">${getPaymentLabel(order.paymentStatus)}</span>
            </div>
            <div class="mb-2">
                <strong>Сума:</strong> <span class="order-price-cell">${order.totalPrice.toFixed(2)} ₴</span>
            </div>
            <div class="mb-1"><strong>Страви:</strong></div>
            <div class="order-detail-items-list">
                ${itemsHtml}
            </div>
            ${reviewHtml}
        `;

        document.getElementById('orderDetailModal').classList.add('active');
    }

    // ── Expose to HTML onclick handlers ──
    window.loadAllOrders = loadAllOrders;
    window.applyFilters = applyFilters;
    window.showOrderDetail = showOrderDetail;

    // ── Init ──
    document.addEventListener('DOMContentLoaded', loadAllOrders);
})();
