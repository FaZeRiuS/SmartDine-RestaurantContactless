/* 
 * Customer /orders page: HTMX loads fragments; 
 * Star pickers + review submit now use unified globals from ui.js
 */

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
            const body = await res.text();
            if (window.__CLIENT_DEBUG) {
                try {
                    console.warn('[review] request failed', res.status, body && body.slice(0, 500));
                } catch (ignore) { /* ignore */ }
            }
            // Generic code only — user-facing text comes from userFacingErrorMessage fallback (no API/stack leakage).
            throw new Error('REVIEW_SUBMIT_FAILED');
        }

        showToast('✅ Дякуємо за відгук!', 'success');
        refreshCustomerOrdersList();
    } catch (e) {
        const fb = 'Не вдалося надіслати відгук. Спробуйте ще раз.';
        const display =
            typeof userFacingErrorMessage === 'function'
                ? userFacingErrorMessage(e, fb, (m) => m.includes('Оберіть оцінку'))
                : fb;
        if (hint) hint.textContent = display;
        showToast('❌ ' + display, 'error');
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
            if (typeof window.initStarPickersIn === 'function') {
                window.initStarPickersIn(evt.detail.target);
            }
        }
    });
});
