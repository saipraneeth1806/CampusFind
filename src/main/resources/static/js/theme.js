(() => {
  if (window.campusFindThemeInitialized) return;
  window.campusFindThemeInitialized = true;
  const savedTheme = localStorage.getItem('campusfind-theme');
  const preferredDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.theme = savedTheme || (preferredDark ? 'dark' : 'light');

  document.addEventListener('DOMContentLoaded', () => {
    const button = document.querySelector('[data-theme-toggle]');
    if (!button) return;
    const updateButton = () => {
      const dark = document.documentElement.dataset.theme === 'dark';
      button.setAttribute('aria-pressed', String(dark));
      button.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
      button.innerHTML = dark ? '<span aria-hidden="true">☀</span><span class="theme-label">Light</span>' : '<span aria-hidden="true">☾</span><span class="theme-label">Dark</span>';
    };
    button.addEventListener('click', () => {
      const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
      document.documentElement.dataset.theme = next;
      localStorage.setItem('campusfind-theme', next);
      updateButton();
    });
    updateButton();
  });
})();
