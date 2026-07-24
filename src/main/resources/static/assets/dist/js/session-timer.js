/**
 * Session Inactivity Timer
 * 
 * George-style timer that:
 * 1. Counts down from the configured inactivity timeout (default 5 min)
 * 2. Resets on user activity (mouse move, click, keydown, scroll, touch)
 * 3. Enforces a hard cap since BankID login (default 45 min)
 * 4. Shows a warning modal before auto-logout
 * 5. Pings keep-alive endpoint to keep server session alive on activity
 * 
 * Configuration is injected via window.sessionTimerConfig from the Thymeleaf template.
 */
(function () {
    "use strict";

    const config = window.sessionTimerConfig;
    if (!config || !config.enabled) return;

    const API_URL = "/bankid-registrator/api";
    const INACTIVITY_TIMEOUT = (config.inactivityTimeoutSeconds || 300) * 1000;
    const HARD_CAP = (config.hardCapSeconds || 2700) * 1000;
    const WARNING_BEFORE = (config.warningBeforeSeconds || 60) * 1000;
    const KEEP_ALIVE_INTERVAL = (config.keepAliveIntervalSeconds || 30) * 1000;
    const LOGIN_TIMESTAMP = config.loginTimestamp || Date.now();
    const LOGOUT_URL = "/bankid-registrator/identity/logout-redirect";
    const WELCOME_URL = "/bankid-registrator/welcome?session=expired";

    const translations = {
        timerLabel: config.translations?.timerLabel || "Session expires in",
        warningTitle: config.translations?.warningTitle || "Session expiring",
        warningMessage: config.translations?.warningMessage || "Your session will expire in less than a minute due to inactivity.",
        warningContinueBtn: config.translations?.warningContinueBtn || "Continue",
        warningLogoutBtn: config.translations?.warningLogoutBtn || "Log out",
        hardCapWarning: config.translations?.hardCapWarning || "Your session will expire soon due to security time limit.",
        hardCapExpired: config.translations?.hardCapExpired || "Your session has expired due to security time limit."
    };

    let inactivityTimer = null;
    let countdownInterval = null;
    let keepAliveTimer = null;
    let lastKeepAlive = 0;
    let warningShown = false;
    let timerBarElm = null;
    let timerTextElm = null;
    let warningModalElm = null;
    let destroyed = false;

    /**
     * Calculate remaining time until hard cap
     */
    function getHardCapRemaining() {
        if (!LOGIN_TIMESTAMP) return Infinity;
        const elapsed = Date.now() - LOGIN_TIMESTAMP;
        return Math.max(0, HARD_CAP - elapsed);
    }

    /**
     * Format milliseconds as M:SS
     */
    function formatTime(ms) {
        const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    /**
     * Get the effective remaining time (minimum of inactivity and hard cap)
     */
    let inactivityDeadline = Date.now() + INACTIVITY_TIMEOUT;

    function getEffectiveRemaining() {
        const inactivityRemaining = Math.max(0, inactivityDeadline - Date.now());
        const hardCapRemaining = getHardCapRemaining();
        return Math.min(inactivityRemaining, hardCapRemaining);
    }

    /**
     * Create the timer bar UI element
     */
    function createTimerBar() {
        timerBarElm = document.createElement("div");
        timerBarElm.id = "session-timer-bar";
        timerBarElm.setAttribute("role", "status");
        timerBarElm.setAttribute("aria-live", "polite");
        timerBarElm.className = "session-timer-bar";
        timerBarElm.innerHTML =
            '<div class="session-timer-bar__inner">' +
                '<i class="fa-regular fa-clock session-timer-bar__icon" aria-hidden="true"></i>' +
                '<span class="session-timer-bar__label">' + translations.timerLabel + '</span>' +
                '<span class="session-timer-bar__time" id="session-timer-time"></span>' +
            '</div>';

        document.body.appendChild(timerBarElm);
        timerTextElm = document.getElementById("session-timer-time");
    }

    /**
     * Create the warning modal
     */
    function createWarningModal() {
        warningModalElm = document.createElement("div");
        warningModalElm.id = "session-timer-warning-modal";
        warningModalElm.className = "session-timer-modal hidden";
        warningModalElm.setAttribute("role", "alertdialog");
        warningModalElm.setAttribute("aria-modal", "true");
        warningModalElm.setAttribute("aria-labelledby", "session-timer-warning-title");

        warningModalElm.innerHTML =
            '<div class="session-timer-modal__backdrop"></div>' +
            '<div class="session-timer-modal__content">' +
                '<div class="session-timer-modal__icon-wrapper">' +
                    '<svg class="session-timer-modal__icon" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">' +
                        '<path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6m0 8h.01M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>' +
                    '</svg>' +
                '</div>' +
                '<h2 id="session-timer-warning-title" class="session-timer-modal__title">' + translations.warningTitle + '</h2>' +
                '<p class="session-timer-modal__message" id="session-timer-warning-message">' + translations.warningMessage + '</p>' +
                '<p class="session-timer-modal__countdown" id="session-timer-warning-countdown"></p>' +
                '<div class="session-timer-modal__actions">' +
                    '<button type="button" id="session-timer-continue-btn" class="session-timer-modal__btn session-timer-modal__btn--primary">' + translations.warningContinueBtn + '</button>' +
                    '<button type="button" id="session-timer-logout-btn" class="session-timer-modal__btn session-timer-modal__btn--secondary">' + translations.warningLogoutBtn + '</button>' +
                '</div>' +
            '</div>';

        document.body.appendChild(warningModalElm);

        document.getElementById("session-timer-continue-btn").addEventListener("click", onContinue);
        document.getElementById("session-timer-logout-btn").addEventListener("click", onLogout);
    }

    /**
     * Update the timer display
     */
    function updateDisplay() {
        if (destroyed) return;

        const remaining = getEffectiveRemaining();
        const hardCapRemaining = getHardCapRemaining();

        if (timerTextElm) {
            timerTextElm.textContent = formatTime(remaining);
        }

        // Color the timer bar based on urgency
        if (timerBarElm) {
            timerBarElm.classList.remove("session-timer-bar--warning", "session-timer-bar--danger");
            if (remaining <= WARNING_BEFORE) {
                timerBarElm.classList.add("session-timer-bar--danger");
            } else if (remaining <= WARNING_BEFORE * 2) {
                timerBarElm.classList.add("session-timer-bar--warning");
            }
        }

        // Show warning modal when crossing the warning threshold
        if (remaining <= WARNING_BEFORE && remaining > 0 && !warningShown) {
            showWarning(hardCapRemaining <= WARNING_BEFORE);
        }

        // Update warning countdown if modal is visible
        const warningCountdownElm = document.getElementById("session-timer-warning-countdown");
        if (warningCountdownElm && warningShown) {
            warningCountdownElm.textContent = formatTime(remaining);
        }

        // Time's up
        if (remaining <= 0) {
            performLogout();
        }
    }

    /**
     * Show the warning modal
     */
    function showWarning(isHardCap) {
        warningShown = true;

        if (!warningModalElm) return;

        const messageElm = document.getElementById("session-timer-warning-message");
        const continueBtn = document.getElementById("session-timer-continue-btn");

        if (isHardCap) {
            messageElm.textContent = translations.hardCapWarning;
            continueBtn.classList.add("hidden");
        } else {
            messageElm.textContent = translations.warningMessage;
            continueBtn.classList.remove("hidden");
        }

        warningModalElm.classList.remove("hidden");
    }

    /**
     * Hide the warning modal
     */
    function hideWarning() {
        warningShown = false;
        if (warningModalElm) {
            warningModalElm.classList.add("hidden");
        }
    }

    /**
     * User clicked "Continue" in warning modal
     */
    function onContinue() {
        hideWarning();
        resetInactivityTimer();
        pingKeepAlive();
    }

    /**
     * User clicked "Log out" in warning modal
     */
    function onLogout() {
        performLogout();
    }

    /**
     * Perform logout - redirect to logout endpoint
     */
    function performLogout() {
        destroyed = true;
        stopTimers();
        window.location.href = LOGOUT_URL + "?redirect=/welcome%3Fsession%3Dexpired";
    }

    /**
     * Reset the inactivity timer (called on user activity)
     */
    function resetInactivityTimer() {
        const hardCapRemaining = getHardCapRemaining();
        // Don't reset beyond the hard cap
        inactivityDeadline = Date.now() + Math.min(INACTIVITY_TIMEOUT, hardCapRemaining);
    }

    /**
     * Ping the keep-alive endpoint (debounced)
     */
    function pingKeepAlive() {
        const now = Date.now();
        if (now - lastKeepAlive < KEEP_ALIVE_INTERVAL) return;
        lastKeepAlive = now;

        fetch(API_URL + "/keep-alive", {
            method: "GET",
            credentials: "same-origin"
        }).then(function (response) {
            if (response.ok) {
                return response.json();
            }
            throw new Error("Keep-alive failed");
        }).then(function (data) {
            if (!data.loggedIn) {
                // Server says we're no longer logged in
                destroyed = true;
                stopTimers();
                window.location.href = WELCOME_URL;
            }
        }).catch(function () {
            // Silently ignore keep-alive errors
        });
    }

    /**
     * Handle user activity events
     */
    function onUserActivity() {
        if (destroyed) return;

        // Don't reset if hard cap has expired
        if (getHardCapRemaining() <= 0) return;

        resetInactivityTimer();

        // If warning is visible and it's not a hard cap warning, hide it
        if (warningShown) {
            const hardCapRemaining = getHardCapRemaining();
            if (hardCapRemaining > WARNING_BEFORE) {
                hideWarning();
            }
        }

        // Ping keep-alive (debounced internally)
        pingKeepAlive();
    }

    /**
     * Debounce user activity events to avoid excessive processing
     */
    let activityRaf = null;
    function onUserActivityDebounced() {
        if (activityRaf) return;
        activityRaf = window.requestAnimationFrame(function () {
            activityRaf = null;
            onUserActivity();
        });
    }

    /**
     * Start listening for user activity events
     */
    function startActivityListeners() {
        var events = ["mousemove", "mousedown", "click", "keydown", "scroll", "touchstart", "touchmove"];
        events.forEach(function (eventName) {
            document.addEventListener(eventName, onUserActivityDebounced, { passive: true });
        });
    }

    /**
     * Stop all timers
     */
    function stopTimers() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
    }

    /**
     * Initialize the session timer
     */
    function init() {
        // Check if payment redirect is in progress (store timer state)
        var paymentRedirectKey = "sessionTimer_paymentRedirect";
        var storedTimestamp = sessionStorage.getItem(paymentRedirectKey);

        if (storedTimestamp) {
            // Returning from payment - check if hard cap has expired while away
            sessionStorage.removeItem(paymentRedirectKey);
            var elapsed = Date.now() - parseInt(storedTimestamp, 10);
            if (elapsed > HARD_CAP) {
                performLogout();
                return;
            }
        }

        // Detect payment redirect pages and store timestamp
        var paymentRedirectElm = document.querySelector(".page-payment-redirect");
        if (paymentRedirectElm) {
            sessionStorage.setItem(paymentRedirectKey, Date.now().toString());
        }

        createTimerBar();
        createWarningModal();
        resetInactivityTimer();
        startActivityListeners();

        // Start the display update interval (every second)
        countdownInterval = setInterval(updateDisplay, 1000);

        // Initial display
        updateDisplay();

        // Initial keep-alive ping (only if not already destroyed by updateDisplay)
        if (!destroyed) {
            pingKeepAlive();
        }
    }

    // Start when DOM is ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
