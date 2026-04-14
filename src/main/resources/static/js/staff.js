/*
 * SmartDine — Staff orders board (HTMX + fragments/staff-orders)
 */

let loadOrdersDebounceTimer = null;

/**
 * Refreshes the staff board using the same URL as the root element’s hx-get
 * (preserves current filter). Used by sse.js on staff-update.
 * Debounced: notifyStatusChange also emits staff-update SSE while the PUT response
 * is swapping #staffBoardRoot — concurrent swaps leave HTMX with a null target.
 */
function loadOrders() {
    const el = document.getElementById('staffBoardRoot');
    if (!el || !window.htmx) return;
    const url = el.getAttribute('hx-get');
    if (!url) return;
    if (loadOrdersDebounceTimer) {
        clearTimeout(loadOrdersDebounceTimer);
    }
    loadOrdersDebounceTimer = setTimeout(() => {
        loadOrdersDebounceTimer = null;
        const t = document.getElementById('staffBoardRoot');
        if (!t || !window.htmx) return;
        window.htmx.ajax('GET', url, { target: '#staffBoardRoot', swap: 'outerHTML' });
    }, 200);
}
