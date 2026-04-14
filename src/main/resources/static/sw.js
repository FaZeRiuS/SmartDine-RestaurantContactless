// Bump this to force clients to pick up fresh CSS/JS after UI changes.
const CACHE_NAME = 'smartdine-v15';
const ASSETS_TO_CACHE = [
  '/css/public-bundle.css',
  '/css/admin.css',
  '/css/dashboard.css',
  '/js/ui.js',
  '/js/cart.js',
  '/js/csrf.js',
  '/js/sse.js',
  '/js/htmx-bridge.js',
  '/js/vendor/alpine-3.14.9.min.js',
  '/js/vendor/htmx-1.9.11.min.js',
  '/js/vendor/htmx-sse-1.9.11.js',
  '/js/vendor/twemoji-14.0.2.min.js',
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
      console.log('[Service Worker] Caching app shell');
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
    })
  );
  self.clients.claim();
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
  const options = {
    body: data.body || 'Оновлення у замовленні',
    icon: '/icons/android-chrome-192x192.png',
    badge: '/favicon.ico',
    data: {
      url: data.url || '/'
    }
  };

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
      // 1. Check if any window of our app is currently focused
      const isUserActiveOnSite = windowClients.some(client => {
        if (!client.focused) return false;
        
        const url = new URL(client.url);
        // suppression paths: staff pages, orders, cart, or main landing (where active order widget lives)
        return url.pathname.startsWith('/staff/') || 
               url.pathname === '/orders' || 
               url.pathname === '/cart' ||
               url.pathname === '/';
      });

      // 2. If user is active and looking at a relevant page, skip native push
      if (isUserActiveOnSite) {
        console.log('[Service Worker] Suppression: User is focused on relevant page, skipping native notification');
        return;
      }

      // 3. Otherwise, show the native notification
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
