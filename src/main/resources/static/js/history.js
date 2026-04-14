/* Customer /orders page: HTMX loads fragments; star pickers + review submit stay in JS */

function renderStars(value) {
    const v = parseInt(value || 0, 10);
    let out = '';
    for (let i = 1; i <= 5; i++) {
        out += i <= v ? '\u2605' : '\u2606';
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

function refreshCustomerOrdersList() {
    if (typeof htmx === 'undefined') return;
    const activeTab = document.querySelector('.customer-order-tab.active');
    const tab = activeTab && activeTab.getAttribute('data-tab') === 'past' ? 'past' : 'active';
    htmx.ajax('GET', `/htmx/customer/orders?tab=${tab}`, { target: '#customerOrdersRoot', swap: 'innerHTML' });
}

window.refreshCustomerOrdersList = refreshCustomerOrdersList;

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
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': typeof getCsrfToken === 'function' ? getCsrfToken() : ''
            },
            credentials: 'same-origin',
            body: JSON.stringify({ serviceRating, comment, dishRatings })
        });

        if (!res.ok) {
            let msg = await res.text();
            try { msg = JSON.parse(msg).message || msg; } catch (e) { /* ignore */ }
            throw new Error(msg || 'Помилка надсилання відгуку');
        }

        showToast('\u2705 \u0414\u044f\u043a\u0443\u0454\u043c\u043e \u0437\u0430 \u0432\u0456\u0434\u0433\u0443\u043a!', 'success');
        refreshCustomerOrdersList();
    } catch (e) {
        if (hint) hint.textContent = e.message;
        showToast('\u274c ' + e.message, 'error');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.customer-order-tab').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.customer-order-tab').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
        });
    });

    document.body.addEventListener('htmx:afterSwap', (evt) => {
        if (evt.detail.target && evt.detail.target.id === 'customerOrdersRoot') {
            initStarPickers();
        }
    });
});
