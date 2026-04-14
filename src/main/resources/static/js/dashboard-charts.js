/**
 * dashboard-charts.js — Admin dashboard Chart.js visualizations & QR generator.
 *
 * Expects the following globals set via Thymeleaf inline script:
 *   window.topDishesData   – array of {name, quantity}
 *   window.hourlyOrdersData – array of {hour, count}
 */

(function () {
    'use strict';

    const topDishesData = window.topDishesData || [];
    const hourlyOrdersData = window.hourlyOrdersData || [];

    const topLabels = topDishesData.map(x => x.name);
    const topValues = topDishesData.map(x => x.quantity);

    const hourLabels = hourlyOrdersData.map(x => String(x.hour).padStart(2, '0') + ':00');
    const hourValues = hourlyOrdersData.map(x => x.count);

    function showChartError(canvasId, message) {
        const canvas = document.getElementById(canvasId);
        const wrap = canvas && canvas.closest('.chart-wrap');
        if (!wrap) return;
        wrap.innerHTML = `<div class="dash-error">${message}</div>`;
    }

    function initCharts() {
        if (typeof Chart === 'undefined') {
            showChartError('topDishesChart', 'Не вдалося завантажити Chart.js. Перевірте блокування CDN або підключення WebJar.');
            showChartError('hourlyOrdersChart', 'Не вдалося завантажити Chart.js. Перевірте блокування CDN або підключення WebJar.');
            return;
        }

        const ctx1 = document.getElementById('topDishesChart');
        new Chart(ctx1, {
            type: 'bar',
            data: {
                labels: topLabels,
                datasets: [{
                    label: 'Продано (шт.)',
                    data: topValues,
                    backgroundColor: 'rgba(201, 169, 110, 0.25)',
                    borderColor: 'rgba(201, 169, 110, 0.9)',
                    borderWidth: 1,
                    borderRadius: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { color: 'rgba(245,240,232,0.75)' }, grid: { color: 'rgba(255,255,255,0.06)' } },
                    y: { beginAtZero: true, ticks: { precision: 0, color: 'rgba(245,240,232,0.75)' }, grid: { color: 'rgba(255,255,255,0.06)' } }
                }
            }
        });

        const ctx2 = document.getElementById('hourlyOrdersChart');
        new Chart(ctx2, {
            type: 'line',
            data: {
                labels: hourLabels,
                datasets: [{
                    label: 'Замовлення',
                    data: hourValues,
                    tension: 0.35,
                    fill: true,
                    pointRadius: 2,
                    borderWidth: 2,
                    backgroundColor: 'rgba(74, 222, 128, 0.12)',
                    borderColor: 'rgba(74, 222, 128, 0.85)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { color: 'rgba(245,240,232,0.75)' }, grid: { color: 'rgba(255,255,255,0.06)' } },
                    y: { beginAtZero: true, ticks: { precision: 0, color: 'rgba(245,240,232,0.75)' }, grid: { color: 'rgba(255,255,255,0.06)' } }
                }
            }
        });
    }

    // Wait for Chart.js (WebJar or CDN fallback) to be ready
    (function pollCharts() {
        const maxWaitMs = 4000;
        const started = Date.now();
        const t = setInterval(() => {
            if (typeof Chart !== 'undefined') {
                clearInterval(t);
                initCharts();
            } else if (Date.now() - started > maxWaitMs) {
                clearInterval(t);
                initCharts();
            }
        }, 50);
    })();

    // ── QR Code Generator ──

    function buildTargetUrl(table) {
        const u = new URL(window.location.origin + "/");
        u.searchParams.set("table", String(table));
        return u.toString();
    }

    window.renderQr = function renderQr() {
        const table = Number(document.getElementById('tableNumber').value);
        const size = Number(document.getElementById('qrSize').value);

        if (!Number.isFinite(table) || table < 1 || table > 500) {
            if (typeof showToast === 'function') showToast('Невірний номер столу (1..500).', 'error');
            else alert('Невірний номер столу (1..500).');
            return;
        }
        if (!Number.isFinite(size) || size < 120 || size > 1024) {
            if (typeof showToast === 'function') showToast('Невірний розмір (120..1024).', 'error');
            else alert('Невірний розмір (120..1024).');
            return;
        }

        const targetUrl = buildTargetUrl(table);
        const qrSrc = `/admin/qr/table?table=${encodeURIComponent(table)}&size=${encodeURIComponent(size)}`;

        const img = document.getElementById('qrImg');
        img.width = size;
        img.height = size;
        img.src = qrSrc;
        if (typeof showToast === 'function') showToast('QR-код згенеровано', 'success');

        const downloadBtn = document.getElementById('downloadBtn');
        downloadBtn.href = qrSrc;
        downloadBtn.download = `table-${table}-qr.png`;

        const targetUrlInput = document.getElementById('targetUrl');
        targetUrlInput.value = targetUrl;

        const openUrlBtn = document.getElementById('openUrlBtn');
        openUrlBtn.href = targetUrl;
    };

    window.copyUrl = async function copyUrl() {
        const val = document.getElementById('targetUrl').value;
        if (!val) return;
        try {
            await navigator.clipboard.writeText(val);
            if (typeof showToast === 'function') showToast('Посилання скопійовано', 'success');
        } catch (e) {
            const input = document.getElementById('targetUrl');
            input.focus();
            input.select();
            document.execCommand('copy');
            if (typeof showToast === 'function') showToast('Посилання скопійовано', 'success');
        }
    };

    document.addEventListener('DOMContentLoaded', window.renderQr);
})();
