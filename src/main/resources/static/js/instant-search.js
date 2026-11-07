/**
 * Instant Real-Time Search, Category, Type, and Status Filters for CampusFind
 */
(function() {
  'use strict';

  document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('q');
    const categorySelect = document.getElementById('category');
    const typeSelect = document.getElementById('type');
    const statusSelect = document.getElementById('status');
    const cards = Array.from(document.querySelectorAll('.report-card'));
    const resultsCountEl = document.querySelector('.results-count');
    const emptyStateEl = document.getElementById('instantEmptyState');

    if (!cards.length && !emptyStateEl) return;

    // Debounce utility
    function debounce(fn, delay) {
      let timer;
      return function(...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
      };
    }

    // Filter cards
    function applyFilter() {
      const query = (searchInput ? searchInput.value : '').toLowerCase().trim();
      const type = (typeSelect ? typeSelect.value : '').toUpperCase();
      const cat = (categorySelect ? categorySelect.value : '').toUpperCase();
      const status = (statusSelect ? statusSelect.value : '').toUpperCase();

      let visibleCount = 0;

      cards.forEach(card => {
        const text = card.textContent.toLowerCase();
        const cardType = card.dataset.type || '';
        const cardCat = card.dataset.category || '';
        const cardStatus = card.dataset.status || '';

        const matchesQuery = !query || text.includes(query);
        const matchesType = !type || cardType === type;
        const matchesCat = !cat || cardCat === cat;
        const matchesStatus = !status || status === 'ALL' || cardStatus === status;

        if (matchesQuery && matchesType && matchesCat && matchesStatus) {
          card.closest('.report-col').style.display = '';
          visibleCount++;
        } else {
          card.closest('.report-col').style.display = 'none';
        }
      });

      // Update counter
      if (resultsCountEl) {
        resultsCountEl.innerHTML = `<strong>${visibleCount}</strong> report${visibleCount === 1 ? '' : 's'}`;
      }

      // Show/hide zero-state
      if (emptyStateEl) {
        if (visibleCount === 0) {
          emptyStateEl.classList.remove('d-none');
        } else {
          emptyStateEl.classList.add('d-none');
        }
      }

      // Sync active state on Category pills
      document.querySelectorAll('.cat-pill').forEach(pill => {
        const pillCat = pill.dataset.category || '';
        if (pillCat === cat) {
          pill.classList.add('active');
        } else {
          pill.classList.remove('active');
        }
      });

      // Sync active state on Type pills
      document.querySelectorAll('.type-pill-btn').forEach(pill => {
        const pillType = pill.dataset.type || '';
        if (pillType === type) {
          pill.classList.add('active');
        } else {
          pill.classList.remove('active');
        }
      });

      // Sync active state on Status pills
      document.querySelectorAll('.status-pill-btn').forEach(pill => {
        const pillStatus = pill.dataset.status || '';
        if (pillStatus === status) {
          pill.classList.add('active');
        } else {
          pill.classList.remove('active');
        }
      });

      // Sync active state on Hotspot chips
      document.querySelectorAll('.hotspot-chip').forEach(chip => {
        const chipLoc = (chip.dataset.location || '').toLowerCase();
        if (chipLoc && query.includes(chipLoc)) {
          chip.classList.add('active');
        } else {
          chip.classList.remove('active');
        }
      });
    }

    // Event listeners
    if (searchInput) {
      searchInput.addEventListener('input', debounce(applyFilter, 120));
      // Keyboard shortcut: Press '/' anywhere to focus search
      window.addEventListener('keydown', function(e) {
        if (e.key === '/' && document.activeElement !== searchInput && !['input', 'textarea'].includes(document.activeElement.tagName.toLowerCase())) {
          e.preventDefault();
          searchInput.focus();
        }
      });
    }
    if (categorySelect) categorySelect.addEventListener('change', applyFilter);
    if (typeSelect) typeSelect.addEventListener('change', applyFilter);
    if (statusSelect) statusSelect.addEventListener('change', applyFilter);

    // Category pills interaction
    document.querySelectorAll('.cat-pill').forEach(pill => {
      pill.addEventListener('click', function(e) {
        e.preventDefault();
        if (categorySelect) categorySelect.value = this.dataset.category || '';
        applyFilter();
      });
    });

    // Type pills interaction
    document.querySelectorAll('.type-pill-btn').forEach(pill => {
      pill.addEventListener('click', function(e) {
        e.preventDefault();
        if (typeSelect) typeSelect.value = this.dataset.type || '';
        applyFilter();
      });
    });

    // Status pills interaction
    document.querySelectorAll('.status-pill-btn').forEach(pill => {
      pill.addEventListener('click', function(e) {
        e.preventDefault();
        if (statusSelect) statusSelect.value = this.dataset.status || '';
        applyFilter();
      });
    });

    // Hotspots building chips interaction
    function updateHotspotCounts() {
      document.querySelectorAll('.hotspot-chip').forEach(chip => {
        const loc = (chip.dataset.location || '').toLowerCase();
        if (!loc) return;
        const count = cards.filter(c => {
          const txt = c.textContent.toLowerCase();
          const cardStatus = (c.dataset.status || '').toUpperCase();
          return txt.includes(loc) && (cardStatus === 'OPEN' || !cardStatus);
        }).length;

        let badge = chip.querySelector('.hotspot-count');
        if (!badge) {
          badge = document.createElement('span');
          badge.className = 'hotspot-count';
          chip.appendChild(badge);
        }
        badge.textContent = count;
      });
    }

    updateHotspotCounts();

    document.querySelectorAll('.location-pill').forEach(pill => {
      pill.addEventListener('click', function(e) {
        e.preventDefault();
        const loc = this.dataset.location || '';
        if (searchInput) {
          searchInput.value = loc;
          searchInput.focus();
        }
        document.querySelectorAll('.hotspot-chip').forEach(p => {
          p.classList.toggle('active', loc !== '' && p.dataset.location === loc);
        });
        applyFilter();
      });
    });

    // Reset button in zero-state
    const resetBtn = document.getElementById('resetInstantFilter');
    if (resetBtn) {
      resetBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (searchInput) searchInput.value = '';
        if (categorySelect) categorySelect.value = '';
        if (typeSelect) typeSelect.value = '';
        if (statusSelect) statusSelect.value = 'OPEN';
        applyFilter();
      });
    }
  });
})();
