/**
 * Tester's Toolkit — floating side panel for local/testing environments.
 *
 * Allows testers to dynamically change:
 * - The fake BankID identity middle name (used for repeatable registration/renewal testing)
 * - An optional tester email that receives extra copies of all app emails in non-production
 * - Force renewal mode (bypasses membership expiry check)
 *
 * Reads/writes via REST API: GET/PUT /api/test-settings
 */
(function () {
    'use strict';

    var API_BASE = '/bankid-registrator/api/test-settings';

    // CSRF token from meta tags (required by Spring Security)
    var csrfMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : null;
    var csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : null;

    // DOM elements
    var panel = document.getElementById('test-toolkit-panel');
    var toggleBtn = document.getElementById('test-toolkit-toggle');
    var middleNameInput = document.getElementById('test-toolkit-middle-name');
    var testerEmailInput = document.getElementById('test-toolkit-email');
    var generateBtn = document.getElementById('test-toolkit-generate');
    var saveMiddleNameBtn = document.getElementById('test-toolkit-save-mname');
    var forceRenewalCheckbox = document.getElementById('test-toolkit-force-renewal');
    var statusEl = document.getElementById('test-toolkit-status');

    if (!panel || !toggleBtn) return;

    // i18n from data attributes
    var i18n = panel.dataset;

    // Toggle panel open/close
    toggleBtn.addEventListener('click', function () {
        panel.classList.toggle('test-toolkit--open');
        var isOpen = panel.classList.contains('test-toolkit--open');
        toggleBtn.setAttribute('aria-expanded', isOpen);
    });

    // Load current settings on init
    loadSettings();

    // Generate random middle name
    if (generateBtn) {
        generateBtn.addEventListener('click', function () {
            generateBtn.disabled = true;
            fetch(API_BASE + '/generate-middle-name')
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    if (middleNameInput && data.middleName) {
                        middleNameInput.value = data.middleName;
                    }
                })
                .catch(function () {
                    showStatus(i18n.msgError || 'Error', 'error');
                })
                .finally(function () {
                    generateBtn.disabled = false;
                });
        });
    }

    // Save middle name
    if (saveMiddleNameBtn) {
        saveMiddleNameBtn.addEventListener('click', function () {
            var value = middleNameInput ? middleNameInput.value.trim() : '';
            var testerEmail = testerEmailInput ? testerEmailInput.value.trim() : '';
            if (!value) return;
            if (testerEmail && !isValidEmail(testerEmail)) {
                showStatus(i18n.msgInvalidEmail || i18n.msgError || 'Error', 'error');
                return;
            }
            saveSettings({ middleName: value, testerEmail: testerEmail });
        });
    }

    // Toggle force renewal
    if (forceRenewalCheckbox) {
        forceRenewalCheckbox.addEventListener('change', function () {
            saveSettings({ forceRenewal: forceRenewalCheckbox.checked });
        });
    }

    function loadSettings() {
        fetch(API_BASE)
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (middleNameInput && data.middleName != null) {
                    middleNameInput.value = data.middleName;
                }
                if (testerEmailInput) {
                    testerEmailInput.value = data.testerEmail || '';
                }
                if (forceRenewalCheckbox && data.forceRenewal != null) {
                    forceRenewalCheckbox.checked = data.forceRenewal;
                }
            })
            .catch(function () {
                // Silently fail — toolkit is non-critical
            });
    }

    function saveSettings(params) {
        var queryParts = [];
        if (params.middleName != null) {
            queryParts.push('middleName=' + encodeURIComponent(params.middleName));
        }
        if (params.testerEmail != null) {
            queryParts.push('testerEmail=' + encodeURIComponent(params.testerEmail));
        }
        if (params.forceRenewal != null) {
            queryParts.push('forceRenewal=' + encodeURIComponent(params.forceRenewal));
        }

        var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfHeaderName && csrfToken) {
            headers[csrfHeaderName] = csrfToken;
        }

        fetch(API_BASE + '?' + queryParts.join('&'), {
            method: 'PUT',
            headers: headers
        })
            .then(function (res) {
                if (!res.ok) throw new Error('Save failed');
                return res.json();
            })
            .then(function (data) {
                // Sync UI with response
                if (middleNameInput && data.middleName != null) {
                    middleNameInput.value = data.middleName;
                }
                if (testerEmailInput) {
                    testerEmailInput.value = data.testerEmail || '';
                }
                if (forceRenewalCheckbox && data.forceRenewal != null) {
                    forceRenewalCheckbox.checked = data.forceRenewal;
                }
                showStatus(i18n.msgSaved || 'Saved', 'success');
            })
            .catch(function () {
                showStatus(i18n.msgError || 'Error', 'error');
            });
    }

    function showStatus(text, type) {
        if (!statusEl) return;
        statusEl.textContent = text;
        statusEl.className = 'test-toolkit__status test-toolkit__status--' + type;
        clearTimeout(statusEl._timer);
        statusEl._timer = setTimeout(function () {
            statusEl.textContent = '';
            statusEl.className = 'test-toolkit__status';
        }, 2500);
    }

    function isValidEmail(value) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    }
})();
