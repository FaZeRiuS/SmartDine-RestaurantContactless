/* SmartDine — Admin JS (Menu & Dish CRUD; table refresh via HTMX) */

function refreshMenusTable() {
    const el = document.getElementById('adminMenusRoot');
    if (!el || typeof htmx === 'undefined') return;
    const url = el.getAttribute('hx-get');
    if (!url) return;
    htmx.ajax('GET', url, { target: '#adminMenusRoot', swap: 'innerHTML' });
}

function configureMenuSaveForm() {
    const form = document.getElementById('menuSaveForm');
    if (!form) return;
    form.removeAttribute('hx-post');
    form.removeAttribute('hx-put');
    const id = document.getElementById('menuEditId').value;
    if (id) {
        form.setAttribute('hx-put', '/htmx/admin/menus/' + id);
    } else {
        form.setAttribute('hx-post', '/htmx/admin/menus');
    }
}

function openMenuModal(data = null) {
    document.getElementById('menuEditId').value = data && data.id ? data.id : '';
    document.getElementById('menuName').value = data && data.name ? data.name : '';
    document.getElementById('menuStartTime').value =
        data && data.start && data.start !== 'null' ? data.start : '';
    document.getElementById('menuEndTime').value =
        data && data.end && data.end !== 'null' ? data.end : '';
    document.getElementById('menuModalTitle').textContent = data && data.id ? 'Редагувати меню' : 'Нове меню';
    configureMenuSaveForm();
    openModal('menuModal');
}

function refreshDishesTable() {
    const el = document.getElementById('adminDishesRoot');
    if (!el || typeof htmx === 'undefined') return;
    const url = el.getAttribute('hx-get');
    if (!url) return;
    htmx.ajax('GET', url, { target: '#adminDishesRoot', swap: 'innerHTML' });
}

function configureDishSaveForm() {
    const form = document.getElementById('dishSaveForm');
    if (!form) return;
    form.removeAttribute('hx-post');
    form.removeAttribute('hx-put');
    const id = document.getElementById('dishEditId').value;
    if (id) {
        form.setAttribute('hx-put', '/htmx/admin/dishes/' + id);
    } else {
        form.setAttribute('hx-post', '/htmx/admin/dishes');
    }
}

async function openDishModalById(id) {
    try {
        const res = await fetch('/api/dishes/' + id, { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Помилка ' + res.status);
        const dish = await res.json();
        openDishModalWithData(dish);
    } catch (err) {
        showToast('Не вдалося завантажити страву: ' + err.message, 'error');
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
    configureDishSaveForm();
    openModal('dishModal');
}

function openDishModalWithData(dish) {
    document.getElementById('dishEditId').value = dish.id;
    document.getElementById('dishName').value = dish.name || '';
    document.getElementById('dishDescription').value = dish.description || '';
    document.getElementById('dishPrice').value = dish.price || '';
    document.getElementById('dishTags').value = (dish.tags || []).join(', ');
    document.getElementById('dishMenuIds').value = (dish.menuIds || []).join(', ');
    document.getElementById('dishAvailable').checked = dish.isAvailable !== false;

    const imageUrl = dish.imageUrl || '';
    document.getElementById('dishImageUrl').value = imageUrl;
    document.getElementById('dishImageFile').value = '';
    const preview = document.getElementById('dishImagePreview');
    if (imageUrl) {
        preview.querySelector('img').src = imageUrl;
        preview.style.display = 'block';
    } else {
        preview.style.display = 'none';
    }

    document.getElementById('dishModalTitle').textContent = 'Редагувати страву';
    configureDishSaveForm();
    openModal('dishModal');
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

        if (!res.ok) throw new Error('Помилка завантаження ' + res.status);

        const data = await res.json();
        document.getElementById('dishImageUrl').value = data.imageUrl;

        const preview = document.getElementById('dishImagePreview');
        preview.querySelector('img').src = data.imageUrl;
        preview.style.display = 'block';

        showToast('\u2705 \u0424\u043e\u0442\u043e \u0437\u0430\u0432\u0430\u043d\u0442\u0430\u0436\u0435\u043d\u043e', 'success');
    } catch (err) {
        showToast('\u274c ' + err.message, 'error');
        fileInput.value = '';
    }
}

function removeDishImage() {
    document.getElementById('dishImageUrl').value = '';
    document.getElementById('dishImageFile').value = '';
    document.getElementById('dishImagePreview').style.display = 'none';
}
