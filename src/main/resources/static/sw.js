/**
 * CampusFind Service Worker — Lightweight PWA Offline Cache
 */
const CACHE_NAME = 'campusfind-v1';
const PRECACHE_ASSETS = [
  '/',
  '/css/style.css',
  '/js/theme.js',
  '/js/instant-search.js',
  '/js/live-updates.js',
  '/images/campusfind-mark.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_ASSETS).catch(() => {}))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  const url = event.request.url;
  if (!url.startsWith('http')) return;

  // For static assets: cache first, fall back to network
  if (url.includes('/css/') || url.includes('/js/') || url.includes('/images/')) {
    event.respondWith(
      caches.match(event.request).then((cached) => {
        return cached || fetch(event.request).then((response) => {
          if (response.status === 200) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, clone));
          }
          return response;
        });
      })
    );
    return;
  }

  // For HTML pages / API: network first, fall back to cache
  event.respondWith(
    fetch(event.request)
      .then((response) => response)
      .catch(() => caches.match(event.request))
  );
});

