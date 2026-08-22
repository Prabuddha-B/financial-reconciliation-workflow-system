/**
 * FRWSS theme toggle (light/dark), backed by Bootstrap 5.3's
 * data-bs-theme attribute and persisted in localStorage.
 */
(function () {
    var STORAGE_KEY = 'frwss-theme';

    function getPreferredTheme() {
        var stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'light' || stored === 'dark') return stored;
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-bs-theme', theme);
    }

    applyTheme(getPreferredTheme());

    window.toggleFrwssTheme = function () {
        var next = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
        applyTheme(next);
        localStorage.setItem(STORAGE_KEY, next);
    };
})();