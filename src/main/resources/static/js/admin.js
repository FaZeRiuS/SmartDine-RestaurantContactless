/* SmartDine — Admin JS (Menu & Dish CRUD; table refresh via HTMX) */

function refreshMenusTable() {
    const el = document.getElementById('adminMenusRoot');
    if (!el || typeof htmx === 'undefined') return;
    const url = el.getAttribute('hx-get');
    if (!url) return;
    htmx.ajax('GET', url, { target: '#adminMenusRoot', swap: 'innerHTML' });
}

// Ensure tables refresh after successful modal saves (works even if hx-on attribute is mis-typed).
document.body?.addEventListener('htmx:afterRequest', (evt) => {
    try {
        const elt = evt?.detail?.elt;
        const xhr = evt?.detail?.xhr;
        const status = xhr ? xhr.status : 0;
        const successful = status >= 200 && status < 300;
        if (!elt || !successful) return;
        const form = elt.closest ? elt.closest('form') : null;
        const formId = (form && form.id) ? form.id : elt.id;
        if (formId === 'menuSaveForm') {
            if (typeof closeModal === 'function') closeModal('menuModal');
        }
        if (formId === 'dishSaveForm') {
            if (typeof closeModal === 'function') closeModal('dishModal');
        }
    } catch (e) {
        // ignore
    }
});

// HTMX-triggered modal close events (server sends HX-Trigger)
document.body?.addEventListener('admin:closeMenuModal', () => {
    try {
        if (typeof closeModal === 'function') closeModal('menuModal');
    } catch (e) { /* ignore */ }
});

document.body?.addEventListener('admin:closeDishModal', () => {
    try {
        if (typeof closeModal === 'function') closeModal('dishModal');
    } catch (e) { /* ignore */ }
});

function configureMenuSaveForm() {
    // No-op: forms always POST; backend upserts by id.
}

function openMenuModal(data = null) {
    document.getElementById('menuEditId').value = data && data.id ? data.id : '';
    document.getElementById('menuName').value = data && data.name ? data.name : '';
    const start = data && data.start && data.start !== 'null' ? data.start : '';
    const end = data && data.end && data.end !== 'null' ? data.end : '';
    const startEl = document.getElementById('menuStartTime');
    const endEl = document.getElementById('menuEndTime');
    if (startEl) {
        startEl.value = start;
        startEl.dataset.prevValue = start;
    }
    if (endEl) {
        endEl.value = end;
        endEl.dataset.prevValue = end;
    }
    const allDay = document.getElementById('menuAllDay');
    if (allDay) {
        allDay.checked = !(start && end);
    }
    wireMenuTimeInputs();
    applyMenuAllDayState();
    onMenuTimeChanged();
    document.getElementById('menuModalTitle').textContent = data && data.id ? 'Редагувати меню' : 'Нове меню';
    openModal('menuModal');
}

function wireMenuTimeInputs() {
    const form = document.getElementById('menuSaveForm');
    const startEl = document.getElementById('menuStartTime');
    const endEl = document.getElementById('menuEndTime');
    if (startEl && startEl.dataset.wired !== '1') {
        startEl.addEventListener('input', onMenuTimeChanged);
        startEl.addEventListener('change', onMenuTimeChanged);
        startEl.dataset.wired = '1';
    }
    if (endEl && endEl.dataset.wired !== '1') {
        endEl.addEventListener('input', onMenuTimeChanged);
        endEl.addEventListener('change', onMenuTimeChanged);
        endEl.dataset.wired = '1';
    }
    if (form && form.dataset.wired !== '1') {
        form.addEventListener('submit', (e) => {
            const allDay = document.getElementById('menuAllDay');
            const start = document.getElementById('menuStartTime')?.value?.trim() || '';
            const end = document.getElementById('menuEndTime')?.value?.trim() || '';
            const allDayChecked = allDay ? allDay.checked === true : false;
            if (!allDayChecked) {
                const onlyOne = (start && !end) || (!start && end);
                if (onlyOne) {
                    e.preventDefault();
                    if (typeof showToast === 'function') {
                        showToast('Вкажіть і час початку, і час закінчення (або увімкніть «Весь день»).', 'error');
                    }
                }
            }
        });
        form.dataset.wired = '1';
    }
}

function applyMenuAllDayState() {
    const allDay = document.getElementById('menuAllDay');
    const startEl = document.getElementById('menuStartTime');
    const endEl = document.getElementById('menuEndTime');
    if (!allDay || !startEl || !endEl) return;
    const disabled = allDay.checked === true;
    if (disabled) {
        // Save current values so we can restore them if user unchecks "All day".
        startEl.dataset.prevValue = startEl.value || '';
        endEl.dataset.prevValue = endEl.value || '';
        startEl.value = '';
        endEl.value = '';
        startEl.disabled = true;
        endEl.disabled = true;
    } else {
        startEl.disabled = false;
        endEl.disabled = false;
        // Restore previous values (if any) after leaving "All day".
        if (!startEl.value && startEl.dataset.prevValue) startEl.value = startEl.dataset.prevValue;
        if (!endEl.value && endEl.dataset.prevValue) endEl.value = endEl.dataset.prevValue;
    }
}

function toggleMenuAllDay() {
    applyMenuAllDayState();
}

function onMenuTimeChanged() {
    const allDay = document.getElementById('menuAllDay');
    const startEl = document.getElementById('menuStartTime');
    const endEl = document.getElementById('menuEndTime');
    if (!allDay || !startEl || !endEl) return;
    const hasAny = (startEl.value && startEl.value.trim() !== '') || (endEl.value && endEl.value.trim() !== '');
    if (hasAny) {
        allDay.checked = false;
        startEl.disabled = false;
        endEl.disabled = false;
    } else {
        allDay.checked = true;
        applyMenuAllDayState();
    }
}

function clearMenuTime(which) {
    const el = which === 'end' ? document.getElementById('menuEndTime') : document.getElementById('menuStartTime');
    if (!el) return;
    el.value = '';
    onMenuTimeChanged();
}

function refreshDishesTable() {
    const el = document.getElementById('adminDishesRoot');
    if (!el || typeof htmx === 'undefined') return;
    const url = el.getAttribute('hx-get');
    if (!url) return;
    htmx.ajax('GET', url, { target: '#adminDishesRoot', swap: 'innerHTML' });
}

function configureDishSaveForm() {
    // No-op: forms always POST; backend upserts by id.
}

async function openDishModalById(id) {
    try {
        const res = await fetch('/api/dishes/' + id, { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Помилка ' + res.status);
        const ct = (res.headers.get('content-type') || '').toLowerCase();
        if (!ct.includes('application/json')) {
            const body = await res.text().catch(() => '');
            const hint = body && body.trim().length > 0
                ? ' (сервер повернув не-JSON; можливо редірект/помилка авторизації)'
                : '';
            throw new Error('Некоректна відповідь сервера' + hint);
        }
        const dish = await res.json();
        openDishModalWithData(dish);
    } catch (err) {
        const fb = 'Не вдалося завантажити страву. Спробуйте ще раз.';
        const display =
            typeof userFacingErrorMessage === 'function' ? userFacingErrorMessage(err, fb) : fb;
        showToast(display, 'error');
    }
}

function openDishModal() {
    document.getElementById('dishEditId').value = '';
    document.getElementById('dishName').value = '';
    document.getElementById('dishDescription').value = '';
    document.getElementById('dishPrice').value = '';
    document.getElementById('dishTags').value = '';
    document.getElementById('dishMenuIds').value = '';
    document.getElementById('dishAvailable').checked = true;
    document.getElementById('dishImageUrl').value = '';
    document.getElementById('dishImageFile').value = '';
    document.getElementById('dishImagePreview').style.display = 'none';
    document.getElementById('dishModalTitle').textContent = 'Нова страва';
    openModal('dishModal');
}

function openDishModalWithData(dish) {
    try {
        const toStringList = (value) => {
            if (value == null) return [];
            if (Array.isArray(value)) return value.map(v => String(v).trim()).filter(Boolean);
            if (typeof value === 'string') {
                return value.split(',').map(s => s.trim()).filter(Boolean);
            }
            return [String(value).trim()].filter(Boolean);
        };

        const toIntListString = (value) => {
            const parts = toStringList(value);
            const nums = parts
                .map(p => parseInt(p, 10))
                .filter(n => Number.isFinite(n) && !Number.isNaN(n));
            return nums.join(', ');
        };

        const id = dish && dish.id != null ? dish.id : '';
        document.getElementById('dishEditId').value = id;
        document.getElementById('dishName').value = (dish && dish.name) ? dish.name : '';
        document.getElementById('dishDescription').value = (dish && dish.description) ? dish.description : '';

        const priceVal = dish && dish.price != null ? dish.price : '';
        document.getElementById('dishPrice').value = priceVal === 0 ? '0' : (priceVal || '');

        const tagsVal = dish ? (dish.tags ?? dish.tagList ?? dish.tagNames) : null;
        document.getElementById('dishTags').value = toStringList(tagsVal).join(', ');

        const menuIdsVal = dish ? (dish.menuIds ?? dish.menuIdList ?? dish.menus) : null;
        // Support menuIds=[1,2] or menus=[{id:1},{id:2}]
        let menuIdsNormalized = menuIdsVal;
        if (Array.isArray(menuIdsVal) && menuIdsVal.length > 0 && typeof menuIdsVal[0] === 'object') {
            menuIdsNormalized = menuIdsVal.map(m => m && (m.id ?? m.menuId)).filter(v => v != null);
        }
        document.getElementById('dishMenuIds').value = toIntListString(menuIdsNormalized);

        const available = dish ? (dish.isAvailable ?? dish.available) : null;
        document.getElementById('dishAvailable').checked = available !== false;

        const imageUrl = dish && dish.imageUrl ? dish.imageUrl : '';
        document.getElementById('dishImageUrl').value = imageUrl;
        document.getElementById('dishImageFile').value = '';
        const preview = document.getElementById('dishImagePreview');
        const img = preview ? preview.querySelector('img') : null;
        if (preview && img && imageUrl) {
            img.src = imageUrl;
            preview.style.display = 'block';
        } else if (preview) {
            preview.style.display = 'none';
        }

        document.getElementById('dishModalTitle').textContent = id ? 'Редагувати страву' : 'Нова страва';
        openModal('dishModal');
    } catch (e) {
        const fb = 'Не вдалося відкрити форму редагування. Спробуйте ще раз.';
        const display =
            typeof userFacingErrorMessage === 'function' ? userFacingErrorMessage(e, fb) : fb;
        showToast(display, 'error');
    }
}

/**
 * Image upload: stays on fetch (multipart); not migrated to HTMX.
 */
async function uploadDishImage() {
    const fileInput = document.getElementById('dishImageFile');
    if (!fileInput.files || fileInput.files.length === 0) return;

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await fetch('/api/admin/dishes/upload-image', {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
        });

        if (!res.ok) {
            let msg = 'Не вдалося завантажити фото.';
            try {
                const ct = (res.headers.get('content-type') || '').toLowerCase();
                if (ct.includes('application/json')) {
                    const j = await res.json();
                    if (j && j.error) msg = String(j.error);
                }
            } catch (_) { /* keep default */ }
            throw new Error(msg);
        }

        const data = await res.json();
        document.getElementById('dishImageUrl').value = data.imageUrl;

        const preview = document.getElementById('dishImagePreview');
        preview.querySelector('img').src = data.imageUrl;
        preview.style.display = 'block';

        showToast('\u2705 \u0424\u043e\u0442\u043e \u0437\u0430\u0432\u0430\u043d\u0442\u0430\u0436\u0435\u043d\u043e', 'success');
    } catch (err) {
        const fb = 'Не вдалося завантажити фото. Спробуйте інше зображення.';
        const display =
            typeof userFacingErrorMessage === 'function'
                ? userFacingErrorMessage(err, fb, (m) => {
                      if (!m || m.length > 500) return false;
                      if (/failed to fetch/i.test(m) || /networkerror/i.test(m)) return false;
                      return true;
                  })
                : fb;
        showToast('\u274c ' + display, 'error');
        fileInput.value = '';
    }
}

function removeDishImage() {
    document.getElementById('dishImageUrl').value = '';
    document.getElementById('dishImageFile').value = '';
    document.getElementById('dishImagePreview').style.display = 'none';
}
