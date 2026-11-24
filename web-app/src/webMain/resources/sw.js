const CACHE_NAME = 'logic-labs-pwa-v1.3.1-web1';

const CORE_ASSETS = [
  './',
  './index.html',
  './manifest.webmanifest',
  './favicon.png',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './font/inter_semibold.ttf',
  './font/jetbrains_mono_medium.ttf'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(CORE_ASSETS))
      .then(() => self.skipWaiting())
      .catch(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('message', (event) => {
  if (!event.data || event.data.type !== 'CACHE_ASSETS') return;
  const urls = Array.isArray(event.data.urls) ? event.data.urls : [];
  const replyPort = event.ports && event.ports[0];
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      for (const rawUrl of urls) {
        try {
          const parsed = new URL(rawUrl, self.location.href);
          if (parsed.origin !== self.location.origin) continue;
          const existing = await cache.match(parsed.href, { ignoreVary: true });
          if (!existing) {
            const resp = await fetch(parsed.href);
            if (resp && resp.ok) {
              await cache.put(parsed.href, resp.clone());
            }
          }
        } catch (_) {}
      }
      if (replyPort) replyPort.postMessage({ ok: true, count: urls.length });
    }).catch(() => {
      if (replyPort) replyPort.postMessage({ ok: false });
    })
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // Navigation requests: network-first with instant offline fallback to cached index.html
  if (req.mode === 'navigate') {
    const indexUrl = new URL('./index.html', self.location.href).href;
    event.respondWith(
      fetch(req)
        .then((resp) => {
          if (resp && resp.ok) {
            const copy = resp.clone();
            caches.open(CACHE_NAME).then((c) => c.put(indexUrl, copy)).catch(() => {});
          }
          return resp;
        })
        .catch(() => caches.match(indexUrl, { ignoreSearch: true, ignoreVary: true }))
    );
    return;
  }

  // Static assets (.wasm, .js, .ttf, .png): cache-first, dynamically caching the active engine bundle
  event.respondWith(
    caches.match(req, { ignoreVary: true }).then((cached) => {
      if (cached) return cached;
      return fetch(req).then((resp) => {
        if (resp && resp.ok && (resp.type === 'basic' || resp.type === 'cors')) {
          const copy = resp.clone();
          caches.open(CACHE_NAME).then((c) => c.put(req, copy)).catch(() => {});
        }
        return resp;
      });
    })
  );
});
