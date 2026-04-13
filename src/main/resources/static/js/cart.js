/* ═══════════════════════════════════════════════════════
   SmartDine — Cart & Order JS v5.0 (Multi-Order Flow)
   ═══════════════════════════════════════════════════════ */

let currentCart = null;
let currentActiveOrderId = null;
let activeOrderStatus = null;
let activeOrderPaymentStatus = null;




async function submitActiveOrderReview(orderId) {
    const hint = document.getElementById('activeOrderReviewHint');
    const root = document.getElementById('orderReviewSection');
    try {
        const servicePicker = root ? root.querySelector('.star-picker[data-type="service"]') : null;
        const serviceRating = servicePicker ? parseInt(servicePicker.dataset.value || '0', 10) : 0;
        if (serviceRating < 1 || serviceRating > 5) {
            throw new Error('Оберіть оцінку сервісу (1-5)');
        }

        const dishPickers = root ? Array.from(root.querySelectorAll('.star-picker[data-type="dish"]')) : [];
        const dishRatings = dishPickers.map(p => ({
            dishId: parseInt(p.dataset.dishId, 10),
            rating: parseInt(p.dataset.value || '0', 10)
        })).filter(x => x.dishId && x.rating >= 1 && x.rating <= 5);

        const commentEl = document.getElementById('activeOrderReviewComment');
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
        checkActiveOrder();
    } catch (e) {
        if (hint) hint.textContent = e.message;
        showToast('❌ ' + e.message, 'error');
    }
}

function buildReviewModal(order) {
    const wrap = document.createElement('div');
    wrap.id = 'reviewModalOverlay';
    wrap.className = 'modal-overlay active'; // Use standard class
    wrap.style.display = 'flex';
    wrap.style.alignItems = 'center';
    wrap.style.justifyContent = 'center';
    wrap.style.padding = '1rem';
    wrap.style.zIndex = '9999';

    const modal = document.createElement('div');
    modal.className = 'modal'; // Use standard modal class
    modal.style.width = '100%';
    modal.style.maxWidth = '540px';
    modal.style.background = 'rgba(18, 18, 18, 0.95)'; // Highly opaque but still slightly glass-like
    modal.style.backdropFilter = 'blur(20px)';
    modal.style.webkitBackdropFilter = 'blur(20px)';
    modal.style.padding = '1.5rem';
    modal.style.maxHeight = '90vh';
    modal.style.overflowY = 'auto';
    modal.style.transform = 'translateY(0)'; // Reset transition transform for instant show

    const items = (order.items || []).map(i => `
        <div style="display:flex; justify-content:space-between; gap:1rem; align-items:center; margin-top:0.35rem;">
            <div style="font-size:0.95rem;">${escapeHtml(i.dishName)}</div>
            <div class="star-picker" data-type="dish" data-dish-id="${i.dishId}"></div>
        </div>
    `).join('');

    modal.innerHTML = `
        <div style="display:flex; justify-content:space-between; gap:1rem; align-items:flex-start;">
            <div>
                <div style="font-weight:800; font-size:1.1rem;">Залишити відгук</div>
                <div class="text-muted" style="margin-top:0.15rem; font-size:0.9rem;">Замовлення #${order.id}</div>
            </div>
            <button type="button" class="btn btn-sm btn-secondary" id="reviewModalCloseBtn">✕</button>
        </div>

        <div style="margin-top:0.75rem; display:flex; justify-content:space-between; gap:1rem; align-items:center;">
            <div class="text-muted">Сервіс:</div>
            <div class="star-picker" data-type="service"></div>
        </div>

        <div style="margin-top:0.75rem;">
            <div class="text-muted" style="margin-bottom:0.25rem;">Побажання (опціонально):</div>
            <textarea class="form-input" id="reviewModalComment" rows="2"
                      placeholder="Наприклад: все було супер, дякуємо!"
                      style="width:100%; resize: vertical;"></textarea>
        </div>

        <div style="margin-top:0.75rem;">
            <div class="text-muted" style="margin-bottom:0.25rem;">Страви:</div>
            ${items || '<div class="text-muted">Немає страв для оцінки</div>'}
        </div>

        <button class="btn btn-primary w-full mt-1" id="reviewModalSubmitBtn">
            ✅ Надіслати відгук
        </button>
        <div class="text-muted" id="reviewModalHint" style="margin-top:0.5rem; font-size:0.85rem;"></div>
    `;

    wrap.appendChild(modal);

    // Close handlers
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

    // Initialize stars
    initStarPickersIn(modal);

    // Submit handler
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

            const res = await fetch(`/api/orders/${order.id}/reviews`, {
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
            close();
            checkActiveOrder();
        } catch (e) {
            if (hint) hint.textContent = e.message;
            showToast('❌ ' + e.message, 'error');
        }
    });

    return wrap;
}

function maybeAutoOpenReviewModal(order) {
    try {
        if (!order || !order.id) return;
        const canReview = order.paymentStatus === 'SUCCESS' && (order.status === 'READY' || order.status === 'COMPLETED');
        if (!canReview) return;
        if (order.serviceRating != null) return;

        const key = `reviewModalShown:order:${order.id}`;
        if (sessionStorage.getItem(key) === '1') return;
        sessionStorage.setItem(key, '1');

        const overlay = buildReviewModal(order);
        document.body.appendChild(overlay);
    } catch (e) {
        // ignore UI errors
    }
}

// ── Toast Notification with Actions ──


// ── Smart Combo Logic ──
// Flag to prevent chaining combos
window.isTrackingCombos = window.isTrackingCombos ?? true;

async function addToCart(dishId, dishName, isComboAdd = false) {
    // If the addition originated from a Combo, we skip showing ANY future combos in the chain
    if (isComboAdd) {
        window.isTrackingCombos = false;
    }

    let specialReq = '';
    const reqInput = document.getElementById('special-req-' + dishId);
    if (reqInput) {
        specialReq = reqInput.value.trim();
        reqInput.value = ''; // clear out after reading
    }

    // 1. If user HAS an active unpaid order, directly add to IT
    if (currentActiveOrderId && activeOrderPaymentStatus !== 'SUCCESS') {
        try {
            const res = await fetch(`/api/orders/${currentActiveOrderId}/items`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ items: [{ dishId: parseInt(dishId), quantity: 1, specialRequest: specialReq }] })
            });

            if (res.status === 401 || res.status === 403) {
                // Not authenticated for this order
                throw new Error('Немає доступу до замовлення');
            } else if (!res.ok) {
                throw new Error('Помилка ' + res.status);
            }

            showToast(`➕ "${dishName}" додано у замовлення!`, 'success');
            checkActiveOrder(); // Refresh the widget
            window.dispatchEvent(new Event('cartUpdated'));

            if (window.isTrackingCombos) {
                triggerSmartCombo(dishId);
            }
            return;
        } catch (err) {
            console.error('Direct add error:', err);
            showToast('Не вдалось додати страву', 'error');
        }
    }

    // 2. Otherwise (No order OR Order is PAID), use standard cart logic to start a NEW order
    try {
        const res = await fetch('/api/cart/items', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ dishId: parseInt(dishId), quantity: 1, specialRequest: specialReq })
        });

        if (!res.ok) {
            throw new Error('Помилка ' + res.status);
        }

        showToast(`🛒 "${dishName}" додано у кошик`, 'info', {
            text: 'Перейти у кошик',
            onClick: () => window.location.href = '/cart'
        });

        window.dispatchEvent(new Event('cartUpdated'));

        if (window.isTrackingCombos) {
            triggerSmartCombo(dishId);
        }

    } catch (err) {
        showToast('❌ Не вдалось додати: ' + err.message, 'error');
    }
}

async function triggerSmartCombo(dishId) {
    if (window.currentActiveOrderId && window.activeOrderPaymentStatus !== 'SUCCESS') {
        return; // Smart Combo is disabled when an active order exists
    }

    try {
        // Collect existing item IDs from cart to inform backend deduplication
        let existingIds = [];
        if (currentCart && currentCart.items) {
            existingIds.push(...currentCart.items.map(i => i.dishId));
        }
        if (window.activeOrderItemsJson && Array.isArray(window.activeOrderItemsJson)) {
            existingIds.push(...window.activeOrderItemsJson.map(i => i.dishId));
        }

        // Ensure the current dish added is passed to filter out its menu immediately
        existingIds.push(dishId);

        let url = `/api/dishes/${dishId}/smart-combo`;
        if (existingIds.length > 0) {
            url += `?cartDishIds=${existingIds.join(',')}`;
        }

        const comboRes = await fetch(url, { credentials: 'same-origin' });
        if (comboRes.ok && comboRes.status !== 204) {
            const comboDish = await comboRes.json();
            if (comboDish) {
                showSmartComboModal(comboDish);
            }
        }
    } catch (e) {
        console.error('Failed to load smart combo recommendation:', e);
    }
}

// ── Smart Combo Hover/Toast Handlers ──
function showSmartComboModal(dish) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    // Using toast-info creates a nice blue-tinted subtle floating card
    toast.className = `toast toast-info`;
    toast.style.display = 'flex';
    toast.style.flexDirection = 'column';
    toast.style.alignItems = 'flex-start';
    toast.style.gap = '8px';

    // Header
    const title = document.createElement('strong');
    title.innerHTML = '<span class="title-accent">🔥</span> З цим часто беруть:';

    // Content
    const content = document.createElement('div');
    content.innerHTML = `<span style="font-weight:600; font-size: 1.1rem;">${dish.name}</span> <span style="color: #60a5fa; font-weight: 700; margin-left: 4px;">${dish.price.toFixed(2)} ₴</span>`;

    if (dish.description) {
        const desc = document.createElement('div');
        desc.style.fontSize = '0.9rem';
        desc.style.display = '-webkit-box';
        desc.style.webkitLineClamp = '2';
        desc.style.lineClamp = '2';
        desc.style.webkitBoxOrient = 'vertical';
        desc.style.overflow = 'hidden';
        desc.style.color = 'rgba(255, 255, 255, 0.7)'; // Better contrast for blur
        desc.style.marginTop = '2px';
        desc.textContent = dish.description;
        content.appendChild(desc);
    }

    // Action Buttons
    const btnRow = document.createElement('div');
    btnRow.style.width = '100%';
    btnRow.style.display = 'flex';
    btnRow.style.gap = '8px';
    btnRow.style.marginTop = '4px';

    const addBtn = document.createElement('button');
    addBtn.className = 'toast-action';
    addBtn.style.flex = '1';
    addBtn.style.textAlign = 'center';
    addBtn.textContent = 'Додати';
    addBtn.onclick = (e) => {
        e.stopPropagation();
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 300);
        addToCart(dish.id, dish.name, true); // passes isComboAdd = true to prevent chains
    };

    const closeBtn = document.createElement('button');
    closeBtn.className = 'toast-action';
    closeBtn.style.flex = '1';
    closeBtn.style.textAlign = 'center';
    closeBtn.style.background = 'transparent';
    closeBtn.style.color = 'var(--text-primary)';
    closeBtn.style.border = '1px solid var(--border)';
    closeBtn.textContent = 'Ні, дякую';
    closeBtn.onclick = (e) => {
        e.stopPropagation();
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 300);
    };

    btnRow.appendChild(addBtn);
    btnRow.appendChild(closeBtn);

    toast.appendChild(title);
    toast.appendChild(content);
    toast.appendChild(btnRow);

    container.appendChild(toast);

    // Auto-remove after 10s if ignored
    setTimeout(() => {
        if (toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            setTimeout(() => toast.remove(), 300);
        }
    }, 10000);
}

function closeSmartCombo() {
    // Deprecated for modal, toasts manage their own removal
}

// ── Sync Menu Buttons & Mobile Badge ──
async function updateMenuButtons() {
    const buttons = document.querySelectorAll('.add-to-cart-btn');
    const isUnpaidOrder = currentActiveOrderId && activeOrderPaymentStatus !== 'SUCCESS';

    buttons.forEach(btn => {
        btn.innerHTML = isUnpaidOrder ? 'Додати до замовлення' : 'У кошик';
    });

    // Update Mobile Badge
    const mobileBadge = document.getElementById('mobile-cart-count');
    if (mobileBadge) {
        let count = 0;
        if (isUnpaidOrder && window.activeOrderItemsJson) {
            count = window.activeOrderItemsJson.reduce((sum, item) => sum + item.quantity, 0);
        } else if (currentCart && currentCart.items) {
            count = currentCart.items.reduce((sum, item) => sum + item.quantity, 0);
        }
        mobileBadge.textContent = count;
        mobileBadge.style.display = count > 0 ? 'flex' : 'none';
    }
}

// ── Load Cart (for cart page) ──
async function loadCart() {
    const loading = document.getElementById('cartLoading');
    const content = document.getElementById('cartContent');
    const empty = document.getElementById('cartEmpty');

    try {
        const res = await fetch('/api/cart', { credentials: 'same-origin' });

        if (res.status === 401 || res.status === 403) {
            // Guest mode
        } else if (!res.ok) {
            throw new Error('Помилка ' + res.status);
        }

        currentCart = await res.json();

        // Always check active order status in parallel
        await checkActiveOrder();

        if (loading) loading.style.display = 'none';

        // Visibility calculation:
        // 1. If we have an active order + NO cart items -> Show ONLY Order
        // 2. If we have NO order + NO cart items -> Show Empty
        // 3. If we have BOTH -> Show Both (Add More scenario)

        const hasCartItems = currentCart.items && currentCart.items.length > 0;

        if (!hasCartItems && !currentActiveOrderId) {
            if (empty) empty.classList.remove('hidden');
            if (content) content.classList.add('hidden');
        } else {
            if (empty) empty.classList.add('hidden');
            if (hasCartItems) {
                renderCartItems(currentCart.items);
                if (content) content.classList.remove('hidden');
            } else {
                if (content) content.classList.add('hidden');
            }
        }
    } catch (err) {
        if (loading) loading.style.display = 'none';
        showToast('❌ Не вдалось завантажити: ' + err.message, 'error');
    }
}

// ── Check for Active Order ──
function resetActiveOrderWidget() {
    window.currentActiveOrderId = null;
    window.activeOrderPaymentStatus = null;
    window.activeOrderItemsJson = [];
    const container = document.getElementById('activeOrderContainer');
    if (container) {
        container.classList.add('hidden');
    }

    // Attempt to show last order widget if no active order
    if (window.isAuthenticated) {
        checkLastOrder();
    }

    setTimeout(() => {
        const lastOrdCont = document.getElementById('lastOrderContainer');
        const heroSection = document.getElementById('heroSection');
        if (heroSection && (!lastOrdCont || lastOrdCont.classList.contains('hidden'))) {
            heroSection.style.display = '';
        }
    }, 100);
}

async function checkActiveOrder() {
    try {
        const res = await fetch('/api/orders/my-active', { credentials: 'same-origin' });
        if (res.status === 204) {
            resetActiveOrderWidget();
            return;
        }
        if (!res.ok) return;

        // Active order exists: hide repeat widget
        const lastOrdCont = document.getElementById('lastOrderContainer');
        if (lastOrdCont) lastOrdCont.classList.add('hidden');

        const heroSection = document.getElementById('heroSection');
        if (heroSection) heroSection.style.display = 'none';

        const order = await res.json();
        currentActiveOrderId = order.id;
        activeOrderStatus = order.status;
        activeOrderPaymentStatus = order.paymentStatus;
        window.activeOrderItemsJson = order.items; // Save items safely for cross-reference

        renderActiveOrder(order);
        updateMenuButtons();
    } catch (err) {
        console.error('Error checking active order:', err);
    }
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

        const history = await res.json();
        // Filter out cancelled orders and empty orders
        const validHistory = (history || []).filter(o =>
            o.status !== 'CANCELLED' &&
            o.items && o.items.length > 0 &&
            o.totalPrice > 0
        );

        if (validHistory && validHistory.length > 0) {
            const lastOrder = validHistory[0];

            const priceEl = document.getElementById('lastOrderTotalPrice');
            if (priceEl) priceEl.textContent = lastOrder.totalPrice.toFixed(2) + ' ₴';

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

// ── Render Active Order (Dual Status) ──
function renderActiveOrder(order) {
    const container = document.getElementById('activeOrderContainer');
    if (!container) return;

    if (order.status === 'COMPLETED' || order.status === 'CANCELLED') {
        resetActiveOrderWidget();
        return;
    }

    const idEl = document.getElementById('activeOrderId');
    const statusEl = document.getElementById('activeOrderStatus');
    const paymentStatusEl = document.getElementById('activePaymentStatus');
    const addMoreBtn = document.getElementById('addMoreBtn');
    const itemsEl = document.getElementById('activeOrderItems');
    const totalEl = document.getElementById('activeOrderTotalPrice');
    const paymentActions = document.getElementById('paymentActions');
    const paidMessage = document.getElementById('paidMessage');
    const loyaltyAccordion = document.getElementById('loyaltyAccordion');
    const loyaltySection = document.getElementById('loyaltySection');
    const loyaltyAppliedEl = document.getElementById('loyaltyApplied');
    const loyaltyInput = document.getElementById('loyaltyAmountInput');
    const loyaltyHint = document.getElementById('loyaltyHint');
    const reviewSection = document.getElementById('orderReviewSection');
    const tipsAccordion = document.getElementById('tipsAccordion');
    const tipsSection = document.getElementById('tipsSection');
    const tipAppliedEl = document.getElementById('tipApplied');
    const tipInput = document.getElementById('tipAmountInput');
    const tipHint = document.getElementById('tipHint');

    if (idEl) idEl.textContent = order.id;

    // Order Status Badge
    if (statusEl) {
        const statusText = formatOrderStatus(order.status);
        statusEl.textContent = statusText;
        statusEl.className = `order-status-badge status-${order.status.toLowerCase()}`;

        const wrapper = document.getElementById('orderStatusWrapper');
        if (wrapper) wrapper.style.display = statusText ? '' : 'none';
    }

    // Payment Status Badge
    if (paymentStatusEl) {
        const paymentText = formatPaymentStatus(order.paymentStatus);
        paymentStatusEl.textContent = paymentText;
        paymentStatusEl.className = `order-status-badge payment-${order.paymentStatus.toLowerCase()}`;

        const wrapper = document.getElementById('paymentStatusWrapper');
        if (wrapper) wrapper.style.display = paymentText ? '' : 'none';
    }

    // Toggle Separator
    const separator = document.querySelector('.status-badge-separator');
    if (separator) {
        const hasOrder = !!statusEl && !!statusEl.textContent;
        const hasPayment = !!paymentStatusEl && !!paymentStatusEl.textContent;
        separator.style.display = (hasOrder && hasPayment) ? 'block' : 'none';
    }

    const discount = typeof order.loyaltyDiscount === 'number' ? order.loyaltyDiscount : 0;
    const amountToPay = typeof order.amountToPay === 'number' ? order.amountToPay : order.totalPrice;
    const tipAmount = typeof order.tipAmount === 'number' ? order.tipAmount : (parseFloat(order.tipAmount) || 0);
    if (totalEl) totalEl.textContent = amountToPay.toFixed(2) + ' ₴';

    if (itemsEl) {
        // Can only remove/decrement if the order is NEW and Unpaid
        const canRemove = order.status === 'NEW' && order.paymentStatus !== 'SUCCESS';

        itemsEl.innerHTML = order.items.map(item => `
            <div class="cart-item">
                <div class="cart-item-info">
                    <div class="cart-item-name">${item.dishName}</div>
                    ${canRemove ?
                `<input type="text" 
                                class="form-input cart-item-special-input" 
                                style="font-size: 0.85rem; padding: 0.2rem; border-radius: 4px; border: 1px solid var(--border); width: 100%; margin-top: 0.25rem;" 
                                placeholder="Особливі побажання..." 
                                value="${item.specialRequest || ''}" 
                                onblur="updateItemSpecialReq(${order.id}, ${item.id}, this.value, true)" />`
                : (item.specialRequest ? `<div class="cart-item-special">💬 ${item.specialRequest}</div>` : '')}
                </div>
                <div class="cart-item-actions-row" style="display: flex; align-items: center; justify-content: space-between; width: 100%; margin-top: 6px; border-top: 1px solid rgba(255,255,255,0.03); padding-top: 6px;">
                    <div class="cart-item-controls" style="display: flex; align-items: center; gap: 1rem;">
                        <div style="display: flex; align-items: center; gap: 0.8rem;">
                            ${canRemove ? `<button class="btn btn-sm btn-secondary" style="padding: 0.25rem 0.6rem; min-width: 32px;" onclick="updateItemQty(${order.id}, ${item.id}, ${item.quantity - 1}, true)">-</button>` : ''}
                            <span class="cart-item-qty" style="font-weight: 600; font-size: 0.95rem; white-space: nowrap;">× ${item.quantity}</span>
                            <button class="btn btn-sm btn-secondary" style="padding: 0.25rem 0.6rem; min-width: 32px;" onclick="updateItemQty(${order.id}, ${item.id}, ${item.quantity + 1}, true)">+</button>
                        </div>
                        ${canRemove ? `<button class="btn btn-sm" style="padding: 0.25rem 0.6rem; color: var(--danger-color); background: transparent; border: 1px solid var(--danger-color);" onclick="removeItem(${order.id}, ${item.id}, true)">🗑️</button>` : ''}
                    </div>
                    <div class="cart-item-price" style="font-weight: 700; color: var(--gold); font-size: 1.1rem; white-space: nowrap;">${(item.price * item.quantity).toFixed(2)} ₴</div>
                </div>
            </div>
        `).join('');
    }

    // Toggle "Add More" and Payment Actions
    if (order.paymentStatus !== 'SUCCESS') {
        if (paymentActions) paymentActions.classList.remove('hidden');
        if (paidMessage) paidMessage.classList.add('hidden');
        if (addMoreBtn) addMoreBtn.classList.remove('hidden');
    } else {
        if (paymentActions) paymentActions.classList.add('hidden');
        if (paidMessage) paidMessage.classList.remove('hidden');
        if (addMoreBtn) addMoreBtn.classList.add('hidden'); // PAID -> final, no more items here

        if (order.status !== 'NEW' && order.status !== 'READY' && order.status !== 'COMPLETED') {
            paidMessage.textContent = "👨‍🍳 Замовлення готується!";
        } else if (order.status === 'READY') {
            paidMessage.textContent = "🍽️ Страви готові! Смачного!";
        } else if (order.status === 'COMPLETED') {
            paidMessage.textContent = "✅ Замовлення завершено. Дякуємо!";
        } else {
            paidMessage.textContent = "✅ Оплачено, скоро почнемо готувати!";
        }
    }

    // Loyalty UI (only meaningful for unpaid active order)
    if (loyaltySection) {
        if (order.paymentStatus !== 'SUCCESS') {
            if (loyaltyAccordion) loyaltyAccordion.style.display = '';
            if (loyaltyAppliedEl) loyaltyAppliedEl.textContent = discount.toFixed(2);
            if (loyaltyInput) loyaltyInput.value = discount > 0 ? discount.toFixed(2) : '';
            if (loyaltyHint) {
                const rate = loyaltySummaryCache && loyaltySummaryCache.cashbackRate != null
                    ? (parseFloat(loyaltySummaryCache.cashbackRate) * 100)
                    : null;
                const rateText = Number.isFinite(rate) ? `Ваш кешбек зараз: ${rate.toFixed(0)}%. ` : '';
                loyaltyHint.textContent = discount > 0
                    ? `${rateText}Знижка балами застосована. До сплати ${amountToPay.toFixed(2)} ₴.`
                    : `${rateText}Вкажіть суму балів, щоб покрити частину замовлення (до 50%).`;
            }
            loadLoyaltySummary();
        } else {
            if (loyaltyAccordion) loyaltyAccordion.style.display = 'none';
        }
    }

    // Tips UI (only meaningful for unpaid active order)
    if (tipsSection) {
        if (order.paymentStatus !== 'SUCCESS') {
            if (tipsAccordion) tipsAccordion.style.display = '';
            if (tipAppliedEl) tipAppliedEl.textContent = tipAmount.toFixed(2);
            if (tipInput) tipInput.value = tipAmount > 0 ? tipAmount.toFixed(2) : '';
            if (tipHint) tipHint.textContent = 'Вкажіть суму чайових або оберіть готовий варіант.';
        } else {
            if (tipsAccordion) tipsAccordion.style.display = 'none';
        }
    }

    // Reviews menu after order is ready/completed (paid)
    if (reviewSection) {
        const canReview = order.paymentStatus === 'SUCCESS' && (order.status === 'READY' || order.status === 'COMPLETED');
        if (!canReview) {
            reviewSection.innerHTML = '';
        } else if (order.serviceRating != null) {
            reviewSection.innerHTML = `
                <div class="cart-summary" style="margin-top:0.75rem; padding:0.75rem;">
                    <div style="font-weight:700; margin-bottom:0.5rem;">Відгук</div>
                    <div class="text-muted">Сервіс: ${renderStars(order.serviceRating)}</div>
                    ${order.serviceComment ? `<div class="text-muted" style="margin-top:0.25rem; font-size:0.9rem;">Побажання: <span style="white-space: pre-wrap;">${escapeHtml(order.serviceComment)}</span></div>` : ''}
                    <div class="text-muted" style="margin-top:0.5rem;">Страви:</div>
                    ${(order.items || []).map(i => `
                        <div style="display:flex; justify-content:space-between; gap:1rem; align-items:center; margin-top:0.15rem;">
                            <div style="font-size:0.95rem;">${i.dishName}</div>
                            <div>${i.rating ? renderStars(i.rating) : '<span class="text-muted">—</span>'}</div>
                        </div>
                    `).join('')}
                </div>
            `;
        } else {
            reviewSection.innerHTML = `
                <div class="cart-summary" style="margin-top:0.75rem; padding:0.75rem;">
                    <div style="font-weight:700; margin-bottom:0.25rem;">Відгук</div>
                    <div class="text-muted" style="font-size:0.9rem;">Ваше замовлення готове — оцініть сервіс і страви.</div>
                    <button class="btn btn-primary btn-sm mt-1" type="button" onclick="(function(){ const o = window.__lastActiveOrderForReview; if(o){ document.body.appendChild(buildReviewModal(o)); } })()">
                        ⭐ Залишити відгук
                    </button>
                </div>
            `;
        }
        window.__lastActiveOrderForReview = order;
        maybeAutoOpenReviewModal(order);
    }

    // Coordinated Accordion (only one open at a time)
    try {
        if (loyaltyAccordion && tipsAccordion) {
            loyaltyAccordion.addEventListener('toggle', () => {
                if (loyaltyAccordion.open) tipsAccordion.open = false;
            });
            tipsAccordion.addEventListener('toggle', () => {
                if (tipsAccordion.open) loyaltyAccordion.open = false;
            });

            // Default: closed on load unless previously opened in this specific session
            const orderKey = order && order.id ? String(order.id) : 'unknown';
            const kl = `accordion:loyalty:${orderKey}`;
            const kt = `accordion:tips:${orderKey}`;

            loyaltyAccordion.open = sessionStorage.getItem(kl) === '1';
            tipsAccordion.open = sessionStorage.getItem(kt) === '1';

            loyaltyAccordion.addEventListener('toggle', () => sessionStorage.setItem(kl, loyaltyAccordion.open ? '1' : '0'));
            tipsAccordion.addEventListener('toggle', () => sessionStorage.setItem(kt, tipsAccordion.open ? '1' : '0'));
        }
    } catch (e) { }

    // Call Waiter Section
    const callWaiterSection = document.getElementById('callWaiterSection');
    if (callWaiterSection) {
        if (order.needsWaiter) {
            callWaiterSection.innerHTML = `
                <button class="btn btn-call-waiter active" disabled>
                    🛎️ Офіціант уже йде до вас...
                </button>
            `;
        } else {
            callWaiterSection.innerHTML = `
                <button class="btn btn-call-waiter" onclick="callWaiter(${order.id})">
                    🙋‍♂️ Викликати офіціанта
                </button>
            `;
        }
    }

    container.classList.remove('hidden');
}

async function callWaiter(orderId) {
    const btn = document.querySelector('.btn-call-waiter');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<div class="loading-spinner"></div> Виклик...';
    }

    try {
        const res = await fetch(`/api/orders/${orderId}/call-waiter`, {
            method: 'POST',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('Помилка виклику');

        showToast('🛎️ Офіціанта викликано. Очікуйте!', 'success');
        checkActiveOrder();
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '🙋‍♂️ Викликати офіціанта';
        }
    }
}

function setTipPreset(amount) {
    const input = document.getElementById('tipAmountInput');
    if (input) input.value = String(amount);
    applyTipToOrder();
}

async function applyTipToOrder() {
    if (!currentActiveOrderId) return;
    const input = document.getElementById('tipAmountInput');
    const hint = document.getElementById('tipHint');
    const amount = input && input.value ? parseFloat(input.value) : 0;

    try {
        const res = await fetch(`/api/orders/${currentActiveOrderId}/tip`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ amount: Number.isFinite(amount) ? amount : 0 })
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка встановлення чайових');
        }

        const updatedOrder = await res.json();
        renderActiveOrder(updatedOrder);
        showToast('✅ Чайові застосовано', 'success');

        // Auto-close accordion after success
        const tipsAccordion = document.getElementById('tipsAccordion');
        if (tipsAccordion) tipsAccordion.open = false;
    } catch (e) {
        if (hint) hint.textContent = e.message;
        showToast('❌ ' + e.message, 'error');
    }
}





async function applyLoyaltyToOrder() {
    if (!currentActiveOrderId) return;
    const input = document.getElementById('loyaltyAmountInput');
    const hint = document.getElementById('loyaltyHint');
    const amount = input && input.value ? parseFloat(input.value) : 0;

    try {
        const res = await fetch(`/api/orders/${currentActiveOrderId}/loyalty/apply`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({ amount: Number.isFinite(amount) ? amount : 0 })
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка застосування балів');
        }

        const updatedOrder = await res.json();
        renderActiveOrder(updatedOrder);
        showToast('✅ Бали застосовано', 'success');

        // Auto-close accordion after success
        const loyaltyAccordion = document.getElementById('loyaltyAccordion');
        if (loyaltyAccordion) loyaltyAccordion.open = false;
    } catch (e) {
        if (hint) hint.textContent = e.message;
        showToast('❌ ' + e.message, 'error');
    }
}

async function clearLoyaltyOnOrder() {
    const input = document.getElementById('loyaltyAmountInput');
    if (input) input.value = '0';
    return applyLoyaltyToOrder();
}

// ── Render Cart Items ──
function renderCartItems(items) {
    const container = document.getElementById('cartItems');
    const summary = document.getElementById('cartSummary');
    if (!container) return;

    let total = 0;
    container.innerHTML = items.map(item => {
        const subtotal = (item.price || 0) * (item.quantity || 1);
        total += subtotal;
        return `
            <div class="cart-item fade-in-up">
                <div class="cart-item-info">
                    <div class="cart-item-name">${item.dishName}</div>
                    <input type="text" 
                            class="form-input cart-item-special-input" 
                            style="font-size: 0.85rem; padding: 0.2rem; border-radius: 4px; border: 1px solid var(--border-color); width: 100%; margin-top: 0.2rem;" 
                            placeholder="Особливі побажання..." 
                            value="${item.specialRequest || ''}" 
                            onblur="updateItemSpecialReq(null, ${item.id}, this.value, false)" />
                </div>
                <div class="cart-item-controls" style="display: flex; align-items: center; gap: 0.5rem;">
                    <button class="btn btn-sm btn-secondary" style="padding: 0.2rem 0.5rem;" onclick="updateItemQty(null, ${item.id}, ${item.quantity - 1}, false)">-</button>
                    <span class="cart-item-qty">× ${item.quantity}</span>
                    <button class="btn btn-sm btn-secondary" style="padding: 0.2rem 0.5rem;" onclick="updateItemQty(null, ${item.id}, ${item.quantity + 1}, false)">+</button>
                    <button class="btn btn-sm" style="padding: 0.2rem 0.5rem; color: var(--danger-color); background: transparent; border: 1px solid var(--danger-color);" onclick="removeItem(null, ${item.id}, false)">🗑️</button>
                </div>
                <div class="cart-item-price">${subtotal.toFixed(2)} ₴</div>
            </div>
        `;
    }).join('');

    const priceEl = document.getElementById('cartTotalPrice');
    if (priceEl) priceEl.textContent = total.toFixed(2) + ' ₴';

    // Change Cart page button text
    const btn = document.getElementById('confirmOrderBtn');
    const isUnpaidOrder = currentActiveOrderId && activeOrderPaymentStatus !== 'SUCCESS';
    if (btn && isUnpaidOrder) {
        btn.innerHTML = '➕ Додати до чинного замовлення';
        btn.classList.add('btn-secondary');
    } else if (btn) {
        btn.innerHTML = '✅ Оформити замовлення';
        btn.classList.remove('btn-secondary');
    }

    if (summary) summary.style.display = '';
}

// ── Item Manipulation ──
async function updateItemQty(orderId, itemId, newQty, isOrder = false) {
    try {
        let url = isOrder ? `/api/orders/${orderId}/items/${itemId}/quantity?quantity=${newQty}` : `/api/cart/items/${itemId}/quantity?quantity=${newQty}`;
        const res = await fetch(url, {
            method: 'PUT',
            credentials: 'same-origin'
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка додавання');
        }

        if (isOrder) {
            checkActiveOrder();
        } else {
            loadCart();
        }
        window.dispatchEvent(new Event('cartUpdated'));
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

async function removeItem(orderId, itemId, isOrder = false) {
    if (!confirm('Видалити цю страву?')) return;
    try {
        let url = isOrder ? `/api/orders/${orderId}/items/${itemId}` : `/api/cart/items/${itemId}`;
        const res = await fetch(url, {
            method: 'DELETE',
            credentials: 'same-origin'
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка видалення');
        }

        if (isOrder) {
            checkActiveOrder();
        } else {
            loadCart();
        }
        window.dispatchEvent(new Event('cartUpdated'));
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

async function updateItemSpecialReq(orderId, itemId, text, isOrder = false) {
    try {
        let url = isOrder ? `/api/orders/${orderId}/items/${itemId}/special-request` : `/api/cart/items/${itemId}/special-request`;
        const res = await fetch(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'text/plain' },
            credentials: 'same-origin',
            body: text
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { }
            throw new Error(msg || 'Помилка збереження побажання');
        }

        // No need to full-reload the UI since we just updated text inside an input field.
        // It's silently saved.
    } catch (err) {
        showToast('❌ Оновлення не вдалось: ' + err.message, 'error');
    }
}

// ── Confirm Order / Add Items ──
async function confirmOrder() {
    const btn = document.getElementById('confirmOrderBtn');
    if (!btn) return;
    btn.disabled = true;
    const isUpdate = currentActiveOrderId && activeOrderPaymentStatus !== 'SUCCESS';
    btn.innerHTML = `<div class="loading-spinner"></div> ${isUpdate ? 'Додавання...' : 'Оформлення...'}`;

    try {
        let url = isUpdate ? `/api/orders/${currentActiveOrderId}/items` : '/api/orders/confirm';
        let body = JSON.stringify({
            items: currentCart.items.map(i => ({
                dishId: i.dishId,
                quantity: i.quantity,
                specialRequest: i.specialRequest || ''
            }))
        });

        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: body
        });

        if (!res.ok) throw new Error(await res.text() || 'Помилка ' + res.status);

        const order = await res.json();
        showToast(isUpdate ? `✅ Страви додано до замовлення #${order.id}` : `✅ Замовлення #${order.id} створено!`, 'success');

        // IMMEDIATELY hide the cart section to prevent "Double Block" syndrome
        const content = document.getElementById('cartContent');
        if (content) {
            content.classList.add('hidden');
            // Reset local cart to prevent flickering
            currentCart = { items: [] };
        }

        setTimeout(() => location.reload(), 1500);
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
        btn.disabled = false;
        btn.innerHTML = isUpdate ? '➕ Додати до чинного замовлення' : '✅ Оформити замовлення';
    }
}

// ── Pay Order ──
async function payOrder() {
    const btn = document.getElementById('payBtn');
    if (!currentActiveOrderId || !btn) return;

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
        orderIdInput.value = currentActiveOrderId;
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
    if (document.getElementById('cartLoading')) {
        loadCart();
    } else {
        checkActiveOrder();
    }
};

// ── Auto-load ──
document.addEventListener('DOMContentLoaded', () => {
    // Initial load happens in layout.html after identity is fetched.
    loadLoyaltySummary();
});

