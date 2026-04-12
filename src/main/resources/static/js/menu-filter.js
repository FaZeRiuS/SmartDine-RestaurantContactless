/**
 * menu-filter.js — Shared menu category filter logic.
 *
 * Used on index.html and menu.html.
 * The selected menu ID is read from a data attribute on the script's
 * surrounding container: <div data-selected-menu-id="...">
 */

function filterMenu(menuId, btn) {
    document.querySelectorAll('.menu-tab').forEach(t => t.classList.remove('active'));
    if (btn) btn.classList.add('active');

    document.querySelectorAll('.menu-category').forEach(cat => {
        if (menuId === 'all') {
            cat.style.display = '';
        } else {
            cat.style.display = cat.dataset.menuId == menuId ? '' : 'none';
        }
    });
}

// Auto-select menu category on page load
document.addEventListener('DOMContentLoaded', () => {
    const holder = document.querySelector('[data-selected-menu-id]');
    const selectedId = holder ? holder.getAttribute('data-selected-menu-id') : null;
    if (selectedId) {
        filterMenu(Number(selectedId),
            document.querySelector('.menu-tab[data-menu-id="' + selectedId + '"]'));
    }
});
