/**
 * orders-history-admin.js — Admin order history (HTMX + fragments/admin-orders-history).
 * Opens detail modal after hx swaps #orderDetailBody.
 */
(function () {
    'use strict';

    document.body.addEventListener('htmx:afterSwap', function (evt) {
        if (evt.detail.target && evt.detail.target.id === 'orderDetailBody') {
            if (typeof openModal === 'function') {
                openModal('orderDetailModal');
            }
        }
    });
})();
