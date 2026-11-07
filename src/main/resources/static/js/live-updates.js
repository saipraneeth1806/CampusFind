/**
 * Real-Time Live Notification Updates for CampusFind
 * Periodically polls /api/notifications/unread-count, updates navbar badges,
 * and alerts the student when a new private message or match alert arrives.
 */
(function() {
  'use strict';

  let lastUnreadMessages = null;
  let lastUnreadAlerts = null;
  let pollInterval = 12000; // 12 seconds

  // Toast Container
  function getToastContainer() {
    let container = document.getElementById('toastContainerCustom');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainerCustom';
      container.className = 'toast-container-custom';
      document.body.appendChild(container);
    }
    return container;
  }

  // Show floating toast
  window.showCampusToast = function(title, message, icon = '✦', linkUrl = null) {
    const container = getToastContainer();
    const toast = document.createElement('div');
    toast.className = 'live-toast';
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');

    toast.innerHTML = `
      <div class="toast-icon">${icon}</div>
      <div class="toast-body">
        <strong>${title}</strong>
        <div>${message}</div>
      </div>
      <button type="button" class="toast-close" aria-label="Close">×</button>
    `;

    if (linkUrl) {
      toast.style.cursor = 'pointer';
      toast.addEventListener('click', (e) => {
        if (!e.target.classList.contains('toast-close')) {
          window.location.href = linkUrl;
        }
      });
    }

    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 350);
    });

    container.appendChild(toast);

    // Trigger enter animation
    requestAnimationFrame(() => {
      toast.classList.add('show');
    });

    // Auto dismiss after 6 seconds
    setTimeout(() => {
      if (toast.parentNode) {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 350);
      }
    }, 6000);
  };

  // Update DOM badges
  function updateBadges(data) {
    if (!data.authenticated) return;

    const inboxBadge = document.getElementById('navInboxBadge');
    const alertsBadge = document.getElementById('navAlertsBadge');

    const unreadMsgs = data.unreadMessages || 0;
    const unreadAlerts = data.unreadAlerts || 0;

    // Check if new messages arrived
    if (lastUnreadMessages !== null && unreadMsgs > lastUnreadMessages) {
      const diff = unreadMsgs - lastUnreadMessages;
      window.showCampusToast(
        'New Message Received',
        `You have ${diff} new private message${diff > 1 ? 's' : ''} in your campus inbox.`,
        '✉',
        '/inbox'
      );
    }

    // Check if new match alerts arrived
    if (lastUnreadAlerts !== null && unreadAlerts > lastUnreadAlerts) {
      window.showCampusToast(
        'Possible Match Alert',
        'A new lost-and-found report might match your item!',
        '✨',
        '/notifications'
      );
    }

    lastUnreadMessages = unreadMsgs;
    lastUnreadAlerts = unreadAlerts;

    // Update Inbox badge in navbar
    if (inboxBadge) {
      if (unreadMsgs > 0) {
        inboxBadge.textContent = unreadMsgs > 99 ? '99+' : unreadMsgs;
        inboxBadge.classList.remove('d-none');
        inboxBadge.classList.add('pulse-badge');
      } else {
        inboxBadge.classList.add('d-none');
        inboxBadge.classList.remove('pulse-badge');
      }
    }

    // Update Match alerts badge in dropdown
    if (alertsBadge) {
      if (unreadAlerts > 0) {
        alertsBadge.textContent = unreadAlerts > 99 ? '99+' : unreadAlerts;
        alertsBadge.classList.remove('d-none');
      } else {
        alertsBadge.classList.add('d-none');
      }
    }
  }

  // Fetch from server
  function checkUnread() {
    fetch('/api/notifications/unread-count', { credentials: 'same-origin' })
      .then(res => {
        if (!res.ok) throw new Error('Unread fetch failed');
        return res.json();
      })
      .then(data => updateBadges(data))
      .catch(() => {});
  }

  // Initial check
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', checkUnread);
  } else {
    checkUnread();
  }

  // Recurring polling
  setInterval(checkUnread, pollInterval);

  // Instant check on tab focus
  window.addEventListener('focus', checkUnread);
})();

