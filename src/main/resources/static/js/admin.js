/* ═══════════════════════════════════════════════════════
   SmartDine — Admin JS (Menu & Dish CRUD operations)
   ═══════════════════════════════════════════════════════ */



// ── Modal helpers ──


// ═══════════════════════════════════
// ── MENU CRUD ──
// ═══════════════════════════════════

function openMenuModal(data = null) {
    document.getElementById('menuEditId').value = data ? data.id : '';
    document.getElementById('menuName').value = data ? data.name : '';
    document.getElementById('menuStartTime').value = data && data.start !== 'null' ? data.start : '';
    document.getElementById('menuEndTime').value = data && data.end !== 'null' ? data.end : '';
    document.getElementById('menuModalTitle').textContent = data ? 'Редагувати меню' : 'Нове меню';
    openModal('menuModal');
}

async function saveMenu() {
    const id = document.getElementById('menuEditId').value;
    const name = document.getElementById('menuName').value.trim();
    const startTime = document.getElementById('menuStartTime').value || null;
    const endTime = document.getElementById('menuEndTime').value || null;

    if (!name) {
        showToast('Введіть назву меню', 'error');
        return;
    }

    const body = { name, startTime, endTime };
    const url = id ? `/api/menus/${id}` : '/api/menus';
    const method = id ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(body)
        });

        if (!res.ok) throw new Error('Помилка ' + res.status);

        showToast(`✅ Меню "${name}" ${id ? 'оновлено' : 'створено'}`, 'success');
        closeModal('menuModal');
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

async function deleteMenu(id) {
    if (!confirm('Видалити це меню?')) return;

    try {
        const res = await fetch(`/api/menus/${id}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('Помилка ' + res.status);

        showToast('✅ Меню видалено', 'success');
        setTimeout(() => location.reload(), 800);
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

// ═══════════════════════════════════
// ── DISH CRUD ──
// ═══════════════════════════════════

async function loadDishes() {
    const loading = document.getElementById('dishesLoading');
    const table = document.getElementById('dishesTable');

    try {
        const res = await fetch('/api/dishes/all', { credentials: 'same-origin' });
        if (!res.ok) throw new Error('Помилка ' + res.status);

        const dishes = await res.json();
        loading.style.display = 'none';
        table.style.display = '';

        const tbody = document.getElementById('dishesTableBody');
        tbody.innerHTML = dishes.map(dish => `
            <tr>
                <td style="font-weight:700;color:var(--text-primary)">${dish.id}</td>
                <td style="font-weight:600;color:var(--text-primary)">${dish.name}</td>
                <td style="color:var(--gold);font-weight:600;white-space:nowrap">${dish.price.toFixed(2)} ₴</td>
                <td>${dish.isAvailable ? '<span style="color:var(--accent-green)">✅</span>' : '<span style="color:var(--accent-red)">❌</span>'}</td>
                <td>
                    <div class="dish-tags" style="margin:0">
                        ${(dish.tags || []).map(t => `<span class="dish-tag">${t}</span>`).join('')}
                    </div>
                </td>
                <td>
                    <div class="d-flex gap-1">
                        <button class="btn btn-secondary btn-sm"
                                onclick='openDishModalWithData(${JSON.stringify(dish)})'>✏️</button>
                        <button class="btn btn-danger btn-sm"
                                onclick="deleteDish(${dish.id})">🗑️</button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        loading.style.display = 'none';
        showToast('❌ Не вдалось завантажити страви: ' + err.message, 'error');
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
    openModal('dishModal');
}

async function saveDish() {
    const id = document.getElementById('dishEditId').value;
    const name = document.getElementById('dishName').value.trim();
    const description = document.getElementById('dishDescription').value.trim();
    const price = parseFloat(document.getElementById('dishPrice').value);
    const menuIdsStr = document.getElementById('dishMenuIds').value;
    const isAvailable = document.getElementById('dishAvailable').checked;

    const tags = document.getElementById('dishTags').value
        ? document.getElementById('dishTags').value.split(',').map(s => s.trim()).filter(s => s !== '')
        : [];

    if (!name || isNaN(price)) {
        showToast('Заповніть назву та ціну', 'error');
        return;
    }

    const menuIds = menuIdsStr
        ? menuIdsStr.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
        : [];

    const imageUrl = document.getElementById('dishImageUrl').value;
    const body = { name, description, price, isAvailable, menuIds, tags, imageUrl };
    const url = id ? `/api/dishes/${id}` : '/api/dishes';
    const method = id ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(body)
        });

        if (!res.ok) throw new Error('Помилка ' + res.status);

        showToast(`✅ Страву "${name}" ${id ? 'оновлено' : 'створено'}`, 'success');
        closeModal('dishModal');
        loadDishes();
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

async function deleteDish(id) {
    if (!confirm('Видалити цю страву?')) return;

    try {
        const res = await fetch(`/api/dishes/${id}`, {
            method: 'DELETE',
            credentials: 'same-origin'
        });

        if (!res.ok) throw new Error('Помилка ' + res.status);

        showToast('✅ Страву видалено', 'success');
        loadDishes();
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
    }
}

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
            // NOTE: Fetch with FormData automatically sets multipart/form-data and boundary
        });

        if (!res.ok) throw new Error('Помилка завантаження ' + res.status);

        const data = await res.json();
        document.getElementById('dishImageUrl').value = data.imageUrl;
        
        const preview = document.getElementById('dishImagePreview');
        preview.querySelector('img').src = data.imageUrl;
        preview.style.display = 'block';

        showToast('✅ Фото завантажено', 'success');
    } catch (err) {
        showToast('❌ ' + err.message, 'error');
        fileInput.value = '';
    }
}

function removeDishImage() {
    document.getElementById('dishImageUrl').value = '';
    document.getElementById('dishImageFile').value = '';
    document.getElementById('dishImagePreview').style.display = 'none';
}

// ── Init ──
document.addEventListener('DOMContentLoaded', loadDishes);
