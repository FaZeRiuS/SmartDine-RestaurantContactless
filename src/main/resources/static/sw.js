// Bump this to force clients to pick up fresh CSS/JS after UI changes.
const CACHE_NAME = 'smartdine-v14';

function swDebugLogsEnabled() {
  try {
    return new URL(self.location.href).searchParams.get('debug') === '1';
  } catch (e) {
    return false;
  }
}

function swLog() {
  if (!swDebugLogsEnabled() || typeof console === 'undefined' || !console.log) return;
  console.log.apply(console, arguments);
}
const ASSETS_TO_CACHE = [
  '/css/base.css',
  '/css/layout.css',
  '/css/components.css',
  '/css/pages.css',
  '/css/responsive.css',
  '/css/admin.css',
  '/css/dashboard.css',
  '/js/ui.js',
  '/js/cart.js',
  '/js/csrf.js',
  '/js/sse.js',
  '/js/htmx-bridge.js',
  '/js/history.js',
  '/js/admin.js',
  '/js/dashboard-charts.js',
  '/js/staff.js',
  '/js/orders-history-admin.js',

  '/icons/android-chrome-192x192.png',
  '/icons/android-chrome-512x512.png',
  '/icons/apple-touch-icon.png',
  '/favicon.ico',
  '/manifest.json'
];

// Install Event
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      swLog('[Service Worker] Caching app shell');
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
  self.skipWaiting();
});

// Activate Event
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
      );
    }).then(() => self.clients.claim())
  );
});

// Fetch Event
self.addEventListener('fetch', event => {
  // Only handle GET requests
  if (event.request.method !== 'GET') return;

  const url = new URL(event.request.url);

  // 1. Do not intercept API or streaming responses (SSE lives under /api/sse/...)
  if (url.pathname.startsWith('/api/') ||
      url.pathname.includes('/sse/') ||
      event.request.headers.get('Accept') === 'text/event-stream') {
    return;
  }

  // Strategy for Navigation (HTML pages)
  // Logic: Network First. Try to get latest from server to ensure correct auth state.
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          // Update cache for offline use (avoid caching admin pages or URLs with query params)
          if (!url.pathname.startsWith('/admin') && !url.search) {
            const copy = response.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
          }
          return response;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // Strategy for Static Assets
  // Logic: Cache-First with Network Update (Stale-while-revalidate)
  // For CSS/JS: Network-first to avoid stale UI (admin toasts, styles, etc.)
  if (url.origin === self.location.origin && (url.pathname.startsWith('/css/') || url.pathname.startsWith('/js/'))) {
    event.respondWith(
      fetch(event.request, { cache: 'no-store' })
        .then(networkResponse => {
          if (networkResponse && networkResponse.ok) {
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, networkResponse.clone()));
          }
          return networkResponse;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  if (ASSETS_TO_CACHE.includes(url.pathname) || url.origin !== self.location.origin) {
    event.respondWith(
      caches.match(event.request).then(cachedResponse => {
        const fetchPromise = fetch(event.request).then(networkResponse => {
          if (networkResponse.ok) {
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, networkResponse.clone()));
          }
          return networkResponse;
        }).catch(() => null); // If fetch fails, we already have cachedResponse if it existed
        
        return cachedResponse || fetchPromise || Response.error();
      })
    );
  } else {
    // Default: Network-only (let browser handle it naturally if no interception logic matches)
    return;
  }
});

// Push Event
self.addEventListener('push', event => {
  let data = {};
  if (event.data) {
    try {
      data = event.data.json();
    } catch (e) {
      data = { title: 'SmartDine', body: event.data.text() };
    }
  }

  const title = data.title || 'SmartDine';
  const origin = self.location.origin;
  const dataUrl = (data && typeof data.url === 'string') ? data.url : '/';
  const alwaysShow = data && data.always === true;
  const options = {
    body: data.body || 'Оновлення у замовленні',
    icon: origin + '/icons/android-chrome-192x192.png',
    badge: origin + '/favicon.ico',
    data: {
      url: dataUrl || '/'
    }
  };

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
      if (alwaysShow) {
        swLog('[Service Worker] Force: always-show push notification');
        return self.registration.showNotification(title, options);
      }
      // Requirement: show push only when user is NOT on the site.
      // Staff exception: suppress staff notifications only when a visible tab is exactly /staff/orders.
      const hasVisibleSiteTab = windowClients.some(client => {
        try {
          if (typeof client.visibilityState === 'string' && client.visibilityState !== 'visible') {
            return false;
          }
          const u = new URL(client.url);
          return u.origin === origin;
        } catch (e) {
          return false;
        }
      });

      const isStaffOrdersPush = typeof dataUrl === 'string' && dataUrl.startsWith('/staff/orders');
      if (isStaffOrdersPush) {
        const hasVisibleStaffOrdersTab = windowClients.some(client => {
          try {
            if (typeof client.visibilityState === 'string' && client.visibilityState !== 'visible') {
              return false;
            }
            const u = new URL(client.url);
            return u.origin === origin && u.pathname === '/staff/orders';
          } catch (e) {
            return false;
          }
        });
        if (hasVisibleStaffOrdersTab) {
          swLog('[Service Worker] Suppression: Visible /staff/orders tab detected, skipping staff notification');
          return;
        }
      } else {
        if (hasVisibleSiteTab) {
          swLog('[Service Worker] Suppression: Visible site tab detected, skipping user notification');
          return;
        }
      }

      return self.registration.showNotification(title, options);
    })
  );
});

// Notification Click Event
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const urlToOpen = new URL(event.notification.data.url, self.location.origin).href;

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
      for (let i = 0; i < windowClients.length; i++) {
        const client = windowClients[i];
        if (client.url === urlToOpen && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }
    })
  );
});
