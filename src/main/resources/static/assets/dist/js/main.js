import { Modal } from "https://esm.sh/flowbite";  // In order to control the Flobite Modal class programmatically
const apiUrl = "/bankid-registrator/api";

const mainElm = document.querySelector("main");
const headerElm = document.querySelector("header");
const logoutBtnElm = document.getElementById("identity-logout");
const langSwitcherElm = document.getElementById("language-switcher");
const headerActionsDesktopElm = document.getElementById("header-actions-desktop-slot");
const headerActionsMobileElm = document.getElementById("header-actions-mobile-slot");
const goToServiceBtnElms = document.querySelectorAll(".js-gotoservice-btn");
const apiTokenElm = document.getElementById("api-token");
const autoLogoutOnLoadElm = document.getElementById("auto-logout-on-load");

const getApiToken = () => apiTokenElm?.getAttribute("data-value") || null;

/**
 * Progress loader
 * Options:
 * - checkFunctionData: Data to be passed to the check function
 * - loaderElm: The loader element
 * - mainElm: The main content element
 * - progressCircle: The progress circle element
 * - progressNumber: The progress number element
 * - progressTextElm: The progress text element
 * - progressStep: The progress step percentage
 * - checkFunction: The function to check the progress
 * - interval: The interval in milliseconds to call the check function
 * - maxSteps: The maximum number of steps
 * - progressTexts: An array of texts to display at each step
 * - afterCompleteFunction: A function to call after the loader is complete
 * - initContentElm: The initial content element
 * - finalContentElm: The final content element
 */
class ProgressLoader {
    constructor(options) {
        this.checkFunctionData = options.checkFunctionData;
        this.loaderElm = options.loaderElm;
        this.mainElm = options.mainElm;
        this.progressCircle = options.progressCircle;
        this.progressNumber = options.progressNumber;
        this.progressTextElm = options.progressTextElm;
        this.progressStep = options.progressStep;
        this.checkFunction = options.checkFunction;
        this.interval = options.interval;
        this.maxSteps = options.maxSteps;
        this.counter = 0;
        this.intervalId = null;
        this.progressTexts = options.progressTexts || [];
        this.afterCompleteFunction = options.afterCompleteFunction;
        this.initContentElm = options.initContentElm || null;
        this.finalContentElm = options.finalContentElm || null;
    }

    updateProgressCircle(progress) {
        const maxProgress = this.progressStep * this.maxSteps;
      
        if (progress >= maxProgress) {
            progress = maxProgress;
        } else {
            progress = Math.min(progress, maxProgress);
        }

        this.progressCircle.style.setProperty("--progress", `${progress}%`);
        this.progressNumber.textContent = `${Math.round(progress)}%`;
      
        if (this.progressTexts.length > 0) {
            const textIndex = Math.min(
                Math.floor(progress / (100 / this.progressTexts.length)),
                this.progressTexts.length - 1
            );
            this.progressTextElm.innerHTML = this.progressTexts[textIndex < this.progressTexts.length - 1 ? textIndex : textIndex - 1];
        }
    }

    async check() {
        this.counter++;
        this.updateProgressCircle(this.counter * this.progressStep);
        const data = await this.checkFunction(this.checkFunctionData);

        // Complete the loader:
        // - if the result is true
        // - or before the last step if the error is true
        // - or after the last step if it takes too long
        if (data.result === true || (data.error === true && this.counter === this.maxSteps - 1) || this.counter >= this.maxSteps + 3) {
            this.complete();
            if (this.afterCompleteFunction) {
                await this.afterCompleteFunction(this.checkFunctionData);
            }
        }
    }

    complete() {
        clearInterval(this.intervalId);
        this.progressCircle.style.setProperty("--progress", "100%");
        this.progressNumber.textContent = "100%";
        if (this.progressTexts.length > 0) {
          this.progressTextElm.innerHTML = this.progressTexts[this.progressTexts.length - 1];
        }
        setTimeout(() => {
            const isLoaderInsideMain = this.mainElm.contains(this.loaderElm);

            if (isLoaderInsideMain && this.initContentElm && this.finalContentElm) {
                this.initContentElm.classList.add("hidden");
                this.finalContentElm.classList.remove("hidden");
            } else {
                this.loaderElm.classList.add("hidden");
                this.mainElm.classList.remove("hidden");
            }
        }, 2000);
    }

    start() {
        if (this.progressTexts.length > 0) {
          this.progressTextElm.innerHTML = this.progressTexts[0];
        }
        this.intervalId = setInterval(() => this.check(), this.interval);
    }
}

/**
 * Page loader
 */
class PageLoader {
    constructor(options) {
        this.loaderElm = options.loaderElm;
        this.loaderTextElm = options.loaderTextElm;
        this.text = options.text || window.translations["loader.loading"];
    }

    init() {
        this.loaderTextElm.innerHTML = this.text;
    }

    show() {
        this.loaderElm.classList.remove("hidden");
    }

    hide() {
        this.loaderElm.classList.add("hidden");
    }
}

/**
 * Check whether a form submission is already being processed.
 * @param {HTMLFormElement} formElm
 * @returns {boolean}
 */
function isFormSubmitting(formElm) {
    return formElm.dataset.submitting === "true";
}

/**
 * Mark a form as submitted and disable all of its submit buttons.
 * Call this only after all client-side validation has passed.
 * @param {HTMLFormElement} formElm
 */
function lockFormSubmission(formElm) {
    formElm.dataset.submitting = "true";
    formElm.setAttribute("aria-busy", "true");

    formElm.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(submitElm => {
        submitElm.disabled = true;
        submitElm.classList.add("cursor-not-allowed", "opacity-60");
    });
}

/**
 * Move an element to a target slot if it's not already there
 * @param elm - The element to move
 * @param targetSlot - The target slot element
 * @returns
 */
function moveElmIfNeeded(elm, targetSlot) {
    if (!elm || !targetSlot) return;

    // If the element is already in the target slot, do nothing
    if (elm.parentElement === targetSlot) return;

    targetSlot.appendChild(elm);
}

/**
 * Toggle the visibility of a password input field
 * @param string fieldId - The ID of the password field
 * @param Object toggler - The toggler element
 * @returns void
 */
function togglePasswordVisibility(fieldId, toggler) {
    const passwordField = document.getElementById(fieldId);
    const isPassword = passwordField.type === 'password';
    passwordField.type = isPassword ? 'text' : 'password';
    toggler.innerHTML = isPassword ? '<i class="fa fa-eye-slash" aria-hidden="true"></i>' : '<i class="fa fa-eye" aria-hidden="true"></i>';
}

/**
 * Get the CSRF token and header from the form.
 * @returns {Object} csrfToken and csrfHeader
 */
function getCsrfTokenAndHeader() {
    const csrfInput = document.querySelector('input[name="_csrf"]');
    return {
        csrfToken: csrfInput ? csrfInput.value : null,
        csrfHeader: csrfInput ? csrfInput.name : null
    };
}

/**
 * Trigger the print page
 */
function triggerPrintPage() {
    window.print();
}

/**
 * 
 * Trigger the delete identity
 */
function triggerDeleteIdentity(event) {
    event.preventDefault();

    const endpoint = event.target.href;

    if (confirm(window.translations["indentity.removal.warning"])) {
        window.location.href = endpoint;
    }
}

/**
 * 
 * Trigger the identity restoration
 */
function triggerRestoreIdentity(event) {
    event.preventDefault();

    const endpoint = event.target.href;

    if (confirm(window.translations["indentity.restoration.warning"])) {
        window.location.href = endpoint;
    }
}

/**
 * 
 * Trigger the marking of an identity as deleted in Aleph
 */
function triggerMarkIdentityAsDeletedInAleph(event) {
    event.preventDefault();

    const endpoint = event.target.href;

    if (confirm(window.translations["indentity.markAsDeletedInAleph.warning"])) {
        window.location.href = endpoint;
    }
}

const checkRfid = async (rfid, patronSysId, csrfToken, csrfHeader = null) => {
    const request = {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            rfid: rfid,
            patronSysId: patronSysId
        })
    };

    request.headers[csrfHeader ?? "X-CSRF-TOKEN"] = csrfToken;

    const response = await fetch(`${apiUrl}/check-rfid`, request);

    if (!response.ok) {
        alert(`HTTP-Error: ${response.status} ${response.error}`);
    }

    const data = await response.json();

    return data;
}

const checkEmail = async (email, patronSysId, csrfToken, csrfHeader = null) => {
    const request = {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            email: email,
            patronSysId: patronSysId
        })
    };

    request.headers[csrfHeader ?? "X-CSRF-TOKEN"] = csrfToken;

    const response = await fetch(`${apiUrl}/check-email`, request);

    if (!response.ok) {
        alert(`HTTP-Error: ${response.status} ${response.error}`);
    }

    const data = await response.json();

    return data;
}

const checkLdapAccount = async (params) => {
    const { username, apiToken } = params;

    const response = await fetch(`${apiUrl}/check-ldap-account-login/${username}?token=${apiToken}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    });

    if (!response.ok) {
        return {
            error: true
        };
    }

    const data = await response.json();
    return data;
}

const appLogout = async (params) => {
    const { apiToken, silent = false } = params;

    const response = await fetch(`${apiUrl}/identity/logout?token=${apiToken}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    });

    if (!response.ok) {
        return {
            error: true
        };
    }

    const data = await response.json();

    if (logoutBtnElm) {
        logoutBtnElm.remove();
    }

    const logoutBtn = document.getElementById("identity-logout");
    if (logoutBtn) {
        logoutBtn.remove();
    }

    if (!silent) {
        showAlert(window.translations["alert.identityLoggedOut"], "info", 8000);
    }

    return data;
}

const autoLogoutOnLoad = async () => {
    if (!autoLogoutOnLoadElm) {
        return;
    }

    const apiToken = getApiToken();

    if (!apiToken) {
        return;
    }

    await appLogout({ apiToken });
}

const showAlert = (message, type = "info", duration = 6000, wrapperElmId = "alert-wrapper") => {
    const alertWrapperElm = document.getElementById(wrapperElmId);
    const alertElmId = "alert--" + Math.floor(100000 + Math.random() * 900000);
    let alertColor = "";
    let alertSvgIcon = "";
    const alertInlineStyles = "max-width: 900px;";

    switch (type) {
        case "info":
            alertColor = "blue";
            alertSvgIcon = `<svg class="w-6 h-6 text-blue-800 dark:text-blue-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11h2v5m-2 0h4m-2.592-8.5h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "success":
            alertColor = "green";
            alertSvgIcon = `<svg class="w-6 h-6 text-green-800 dark:text-green-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11h2v5m-2 0h4m-2.592-8.5h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "warning":
            alertColor = "yellow";
            alertSvgIcon = `<svg class="w-6 h-6 text-yellow-800 dark:text-yellow-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 13V8m0 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "danger":
            alertColor = "red";
            alertSvgIcon = `<svg class="w-6 h-6 text-red-800 dark:text-red-400" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 13V8m0 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
    }

    const alertHtml = `<div id="${alertElmId}" class="flex items-center p-4 mb-4 text-${alertColor}-800 border border-${alertColor}-300 rounded-lg bg-${alertColor}-50 dark:bg-gray-800 dark:text-${alertColor}-400 dark:border-${alertElmId}-800" style="${alertInlineStyles}" role="alert">
        ${alertSvgIcon}
        <span class="capitalize sr-only">${type}</span>
        <div class="ms-3 text-sm font-medium">
            ${message}
        </div>
        <button type="button" class="ms-auto -mx-1.5 -my-1.5 bg-${alertColor}-50 text-${alertColor}-500 rounded-lg focus:ring-2 focus:ring-${alertColor}-400 p-1.5 hover:bg-${alertColor}-200 inline-flex items-center justify-center h-8 w-8 dark:bg-gray-800 dark:text-${alertColor}-400 dark:hover:bg-gray-700" data-dismiss-target="#${alertElmId}" aria-label="Close">
            <span class="sr-only">Close</span>
            <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6"/>
            </svg>
        </button>
    </div>`;

    alertWrapperElm.innerHTML += alertHtml;

    if (duration > 0) {
        setTimeout(() => {
            document.getElementById(alertElmId).remove();
        }, duration);
    }
};

window.showAlert = showAlert;   // Globally available

const createModal = (options) => {
    const modalWrapperElm = options.custom.modalWrapperElmId ? document.getElementById(options.custom.modalWrapperElmId) : document.getElementById("modal-wrapper");
    const modalElmId = options.custom.modalElmId ? document.getElementById(options.custom.modalElmId) : document.getElementById("modal");
    const modalType = options.custom.type;
    let modalColor = "";
    let modalTitleSvgHtml = "";
    const modalTitle = options.custom.title ?? "";
    const modalCloseBtnText = options.custom.closeBtnText ?? "OK";

    switch (modalType) {
        case "warning":
            modalColor = "yellow";
            modalTitleSvgHtml = `<svg class="mx-auto mb-4 text-${modalColor}-400 w-12 h-12" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6m0 8h.01M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "error":
            modalColor = "red";
            modalTitleSvgHtml = `<svg class="mx-auto mb-4 text-${modalColor}-400 w-12 h-12" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 20 20">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11V6m0 8h.01M19 10a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "success":
            modalColor = "green";
            modalTitleSvgHtml = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="mx-auto mb-4 text-${modalColor}-400 w-12 h-12" aria-hidden="true" fill="none">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10Z"/>
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 16v-4m0-4h.01"/>
            </svg>`;
            break;
        case "info":
            modalColor = "blue";
            modalTitleSvgHtml = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="mx-auto mb-4 text-${modalColor}-400 w-12 h-12" aria-hidden="true" fill="none">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10Z"/>
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 16v-4m0-4h.01"/>
            </svg>`;
            break;
    }

    const modalHtml = `<div id="${modalElmId}" tabindex="-1" class="hidden overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-50 justify-center items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full">
            <div class="relative p-4 w-full max-w-md max-h-full">
                <div class="relative bg-white rounded-lg shadow">
                    <button type="button" class="absolute top-3 end-2.5 bg-transparent hover:bg-gray-200 hover:text-gray-900 rounded-lg text-sm w-8 h-8 ms-auto inline-flex justify-center items-center dark:hover:bg-gray-600 dark:hover:text-white" data-modal-hide="${modalElmId}">
                        <svg class="w-3 h-3" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 14 14">
                            <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6"/>
                        </svg>
                        <span class="sr-only">Exit</span>
                    </button>
                    <div class="p-4 md:p-5 text-center">
                        ${modalTitleSvgHtml}
                        <h2 class="mb-3 text-xl font-bold text-${modalColor}-500" dark:text-${modalColor}-400">${modalTitle}</h2>
                        <h3 class="mb-5 text-lg font-normal text-${modalColor}-500 dark:text-${modalColor}-400">Are you sure you want to delete this product?</h3>
                        <button data-modal-hide="${modalElmId}" type="button" class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 mt-4">${modalCloseBtnText}</button>
                    </div>
                </div>
            </div>
        </aside>`;

        modalWrapperElm.innerHTML = modalHtml;

    return new Modal(document.getElementById(modalElmId), options);
};

/**
 * Responsive header adjustments
 */
function respoHeader() {
    const isMdScreenUp = window.matchMedia("(min-width: 768px)").matches;

    if (headerElm) {
        if (isMdScreenUp) {
            moveElmIfNeeded(langSwitcherElm, headerActionsDesktopElm);
            moveElmIfNeeded(logoutBtnElm, headerActionsDesktopElm);
        } else {
            moveElmIfNeeded(langSwitcherElm, headerActionsMobileElm);
            moveElmIfNeeded(logoutBtnElm, headerActionsMobileElm);
        }
    }
}

// PAGE: NEW REGISTRATION FOR EMPLOYEES - PASSWORD SETTING SUCCESSFUL
if (document.querySelector(".page-identity-set-password-success")) {
    const mainElm = document.getElementById("main");

    if (document.getElementById("progress-loader")) {
        const loader = new ProgressLoader({
            checkFunctionData: {
                username: document.getElementById("patron-username").getAttribute("data-value"),
                apiToken: getApiToken(),
            },
            loaderElm: document.getElementById("progress-loader"),
            mainElm: mainElm,
            progressTextElm: document.querySelector("#progress-loader .progress-loader__text"),
            progressCircle: document.querySelector("#progress-loader .progress-loader__circle"),
            progressNumber: document.querySelector("#progress-loader .progress-loader__circle-number"),
            progressStep: 12,
            checkFunction: checkLdapAccount,
            interval: 5000, // !IMPORTANT: Keep this 1000 ms only for testing, in production it should 5000 ms
            maxSteps: 8,
            progressTexts: [
                window.translations["loader.newRegistration.validatingData"],
                window.translations["loader.newRegistration.creatingIdentity"],
                window.translations["loader.newRegistration.savingData"],
                window.translations["loader.newRegistration.done"],
            ],
            afterCompleteFunction: appLogout,
            initContentElm: document.getElementById("employee-initial-content"),
            finalContentElm: document.getElementById("employee-final-content"),
        });

        loader.start();
    }
}

// PAGE: NEW REGISTRATION FOR NORMIES - FEE PAYMENT
if (document.querySelector(".page-fee-payment")) {
    const mainElm = document.getElementById("main");

    if (document.getElementById("progress-loader")) {
        const loader = new ProgressLoader({
            checkFunctionData: {
                username: document.getElementById("patron-username").getAttribute("data-value"),
                apiToken: getApiToken(),
            },
            loaderElm: document.getElementById("progress-loader"),
            mainElm: mainElm,
            progressTextElm: document.querySelector("#progress-loader .progress-loader__text"),
            progressCircle: document.querySelector("#progress-loader .progress-loader__circle"),
            progressNumber: document.querySelector("#progress-loader .progress-loader__circle-number"),
            progressStep: 12,
            checkFunction: checkLdapAccount,
            interval: 5000, // !IMPORTANT: Keep this 1000 ms only for testing, in production it should 5000 ms
            maxSteps: 8,
            progressTexts: [
                window.translations["loader.newRegistration.validatingData"],
                window.translations["loader.newRegistration.creatingIdentity"],
                window.translations["loader.newRegistration.savingData"],
                window.translations["loader.newRegistration.done"],
            ],
            afterCompleteFunction: appLogout,
            initContentElm: document.getElementById("payment-initial-content"),
            finalContentElm: document.getElementById("payment-final-content"),
        });

        loader.start();
    }
}

// PAGES: NEW REGISTRATION, MEMBERSHIP RENEWAL
function initClearableInputs() {
    const clearBtnElms = Array.from(document.querySelectorAll(".js-clear-input"));
    if (clearBtnElms.length === 0) {
        return;
    }

    const clearablePairs = clearBtnElms
        .map((btnElm) => {
            const targetSelector = btnElm.getAttribute("data-target");
            const inputElm = targetSelector ? document.querySelector(targetSelector) : null;
            return inputElm ? { btnElm, inputElm } : null;
        })
        .filter(Boolean);

    const updateClearButtonVisibility = () => {
        clearablePairs.forEach(({ btnElm, inputElm }) => {
            if ((inputElm.value || "").length > 0) {
                btnElm.classList.remove("hidden");
            } else {
                btnElm.classList.add("hidden");
            }
        });
    };

    clearablePairs.forEach(({ btnElm, inputElm }) => {
        if (btnElm.dataset.clearInputBound !== "true") {
            btnElm.dataset.clearInputBound = "true";
            btnElm.addEventListener("click", () => {
                inputElm.value = "";
                inputElm.dispatchEvent(new Event("input", { bubbles: true }));
                inputElm.dispatchEvent(new Event("change", { bubbles: true }));
                updateClearButtonVisibility();
                inputElm.focus();
            });
        }

        if (inputElm.dataset.clearInputObserved !== "true") {
            inputElm.dataset.clearInputObserved = "true";
            inputElm.addEventListener("input", updateClearButtonVisibility);
            inputElm.addEventListener("change", updateClearButtonVisibility);
        }
    });

    updateClearButtonVisibility();
}

initClearableInputs();

if (document.querySelector(".page-new-registration, .page-membership-renewal")) {
    const canContinue = document.querySelector(".page-membership-renewal-impossible") ? false : true;

    if (canContinue) {
        const formState = {
            rfid: {
                elm: document.getElementById("rfid"),
                error: false,
            },
            email: {
                elm: document.getElementById("email"),
                error: false,
            },
        };
        const filesElm = document.getElementById("files");
        const filesInputElm = document.querySelector('[name="media"]');
        const emailInputElm = document.querySelector('[name="email"]');
        const filesWrapper = document.getElementById("files-control");
        const declaration4Wrapper = document.getElementById("declaration4-control");
        const { csrfToken, csrfHeader } = getCsrfTokenAndHeader();
        const loaderAfterSubmit = new PageLoader({
            loaderElm: document.getElementById("page-loader"),
            loaderTextElm: document.querySelector(".page-loader__text"),
            text: window.translations["loader.submittingData"],
        });
        const isRenewalPage = document.body.classList.contains("page-membership-renewal");
        const renewalActionInputElm = document.getElementById("renewalAction");
        const renewalVoucherSection = document.getElementById("renewal-voucher-section");
        const renewalPaymentRecapElm = document.getElementById("renewal-payment-recap");
        const renewalSubmitButtonElm = document.getElementById("renewal-submit-button");
        const renewalSubmitAndPayButtonElm = document.getElementById("renewal-submit-and-pay-button");
        const renewalOptionalNoteElm = document.getElementById("renewal-payment-optional-note");
        const renewalFeeAmountElm = document.getElementById("renewal-fee-amount");
        const renewalEmployeeWaiverRowElm = document.getElementById("renewal-employee-waiver-row");
        const renewalEmployeeWaiverAmountElm = document.getElementById("renewal-employee-waiver-amount");
        const renewalVoucherDiscountRowElm = document.getElementById("renewal-voucher-discount-row");
        const renewalVoucherDiscountAmountElm = document.getElementById("renewal-voucher-discount-amount");
        const renewalOutstandingFinesRowElm = document.getElementById("renewal-outstanding-fines-row");
        const renewalOutstandingFinesAmountElm = document.getElementById("renewal-outstanding-fines-amount");
        const renewalTotalAmountElm = document.getElementById("renewal-total-amount");
        const renewalVoucherInputElm = document.getElementById("voucherCode");
        const renewalVoucherResultElm = document.getElementById("voucher-result");
        const renewalUiState = {
            voucherValidated: false,
            discountAmount: 0,
            amountToPayAfterVoucher: null,
        };

        loaderAfterSubmit.init();

        function parseAmount(value) {
            const parsed = parseFloat(value || "0");
            return Number.isFinite(parsed) ? parsed : 0;
        }

        function formatAmount(value) {
            const currencyLabel = renewalPaymentRecapElm?.dataset.currencyLabel || "Kč";
            const documentLanguage = document.documentElement.lang || "cs";
            const localizedAmount = new Intl.NumberFormat(documentLanguage, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
            }).format(value);
            return `${localizedAmount} ${currencyLabel}`;
        }

        function setElementDisplay(element, shouldShow, displayValue = "flex") {
            if (!element) {
                return;
            }

            element.style.display = shouldShow ? displayValue : "none";
        }

        function resetRenewalVoucherState() {
            renewalUiState.voucherValidated = false;
            renewalUiState.discountAmount = 0;
            renewalUiState.amountToPayAfterVoucher = null;

            if (renewalVoucherInputElm) {
                renewalVoucherInputElm.value = "";
            }

            if (renewalVoucherResultElm) {
                renewalVoucherResultElm.classList.add("hidden");
                renewalVoucherResultElm.className = renewalVoucherResultElm.className.replace(/bg-\S+/g, "").replace(/text-\S+/g, "");
                renewalVoucherResultElm.dataset.valid = "false";
                renewalVoucherResultElm.dataset.code = "";
                renewalVoucherResultElm.dataset.discountAmount = "0";
                renewalVoucherResultElm.dataset.amountToPay = "0";
                renewalVoucherResultElm.dataset.feeAmount = "0";
            }
        }

        function updateRenewalPaymentUI() {
            if (!isRenewalPage || !renewalPaymentRecapElm || !renewalActionInputElm) {
                return;
            }

            const casEmployeeValue = document.getElementById("isCasEmployee").checked;
            const declaration4InputElm = document.getElementById("declaration4");
            const standardRenewalFeeAmount = parseAmount(renewalPaymentRecapElm.dataset.standardRenewalFee);
            const outstandingFinesAmount = parseAmount(renewalPaymentRecapElm.dataset.outstandingFines);
            const voucherDiscountAmount = !casEmployeeValue && renewalUiState.voucherValidated
                ? renewalUiState.discountAmount
                : 0;
            const employeeWaiverAmount = casEmployeeValue ? standardRenewalFeeAmount : 0;
            const renewalFeeAmountToPay = Math.max(
                standardRenewalFeeAmount - employeeWaiverAmount - voucherDiscountAmount,
                0
            );
            const totalAmountToPay = renewalFeeAmountToPay + outstandingFinesAmount;
            const feeFullyCovered = !casEmployeeValue
                && renewalUiState.voucherValidated
                && renewalUiState.amountToPayAfterVoucher === 0;
            const requiresPaymentToCompleteRenewal = !casEmployeeValue && !feeFullyCovered;
            const showSubmitOnlyButton = casEmployeeValue || feeFullyCovered;
            const showSubmitAndPayButton = totalAmountToPay > 0;

            renewalFeeAmountElm.textContent = formatAmount(standardRenewalFeeAmount);
            renewalTotalAmountElm.textContent = formatAmount(totalAmountToPay);

            setElementDisplay(renewalEmployeeWaiverRowElm, employeeWaiverAmount > 0);
            if (renewalEmployeeWaiverAmountElm) {
                renewalEmployeeWaiverAmountElm.textContent = `-${formatAmount(employeeWaiverAmount)}`;
            }

            setElementDisplay(renewalVoucherDiscountRowElm, voucherDiscountAmount > 0);
            if (renewalVoucherDiscountAmountElm) {
                renewalVoucherDiscountAmountElm.textContent = `-${formatAmount(voucherDiscountAmount)}`;
            }

            setElementDisplay(renewalOutstandingFinesRowElm, outstandingFinesAmount > 0);
            if (renewalOutstandingFinesAmountElm) {
                renewalOutstandingFinesAmountElm.textContent = formatAmount(outstandingFinesAmount);
            }

            if (renewalVoucherSection) {
                renewalVoucherSection.style.display = casEmployeeValue ? "none" : "block";
            }

            if (declaration4Wrapper && declaration4InputElm) {
                declaration4Wrapper.style.display = requiresPaymentToCompleteRenewal ? "block" : "none";
                declaration4InputElm.required = requiresPaymentToCompleteRenewal;
            }

            setElementDisplay(renewalSubmitButtonElm, showSubmitOnlyButton, "inline-flex");
            setElementDisplay(renewalSubmitAndPayButtonElm, showSubmitAndPayButton, "inline-flex");
            setElementDisplay(renewalOptionalNoteElm, showSubmitOnlyButton && showSubmitAndPayButton, "block");

            renewalActionInputElm.value = showSubmitAndPayButton && !showSubmitOnlyButton
                ? "submitAndPay"
                : "submit";
        }

        function handleCasEmployeeChange(ev) {
            const casEmployeeValue = ev ? ev.target.checked : document.getElementById("isCasEmployee").checked;
            const declaration4InputElm = document.getElementById("declaration4");

            if (casEmployeeValue) {
                resetRenewalVoucherState();
                filesWrapper.style.display = "block";
                declaration4Wrapper.style.display = "none";
                declaration4InputElm.required = false;
            } else {
                filesWrapper.style.display = "none";
                declaration4Wrapper.style.display = "block";
                declaration4InputElm.required = true;
            }

            updateRenewalPaymentUI();
        }

        FilePond.registerPlugin(
            FilePondPluginFileValidateSize,
            FilePondPluginImageExifOrientation,
            FilePondPluginFileValidateType,
            FilePondPluginImagePreview
        );

        const pond = FilePond.create(filesElm);

        FilePond.setOptions({
            storeAsFile: true,
            allowFileTypeValidation: true,
            allowFileSizeValidation: true,
            allowImagePreview: true,
            acceptedFileTypes: ["image/png", "image/jpeg", "application/pdf"],
            maxFileSize: "20MB",
            labelIdle: window.translations["filepond.labelIdle"],
            labelMaxFileSizeExceeded: window.translations["filepond.maxFileSizeExceeded"],
            labelMaxFileSize: window.translations["filepond.maxFileSize"],
            labelFileTypeNotAllowed: window.translations["filepond.invalidFileType"],
            fileValidateTypeLabelExpectedTypes: window.translations["filepond.allowedFileTypes"],
        });

        handleCasEmployeeChange();

        document.getElementById("rfid").addEventListener("change", (ev) => {
            const rfidElm = ev.target;
            const rfid = rfidElm.value;
            let patronId = document.querySelector('input[name="patronId"]').value;

            if (rfid.trim().length === 0) {
                return;
            }

            if (patronId.trim().length === 0) {
                patronId = null;
            }

            checkRfid(rfid, patronId, csrfToken)
                .then(data => {
                    if (data.result === true) {
                        const errorMsg = window.translations["alert.rfidAlreadyInUse"];
                        formState.rfid.error = errorMsg;
                        showAlert(errorMsg, "danger");
                    } else {
                        formState.rfid.error = false;
                        showAlert(window.translations["alert.rfidIsAvailable"], "success");
                    }
                })
                .catch(error => {
                    const errorMsg = window.translations["alert.rfidCheckFailed"];
                    formState.rfid.error = errorMsg;
                    showAlert(window.translations["alert.rfidCheckFailed"], "danger");
                });
        });

        document.getElementById("email").addEventListener("change", (ev) => {
            const emailElm = ev.target;
            const email = emailElm.value;
            let patronId = document.querySelector('input[name="patronId"]').value;

            if (email.trim().length === 0) {
                return;
            }

            if (patronId.trim().length === 0) {
                patronId = null;
            }

            checkEmail(email, patronId, csrfToken)
                .then(data => {
                    if (data.result === true) {
                        const errorMsg = window.translations["alert.emailAlreadyInUse"];
                        formState.email.error = errorMsg;
                        showAlert(window.translations["alert.emailAlreadyInUse"], "danger");
                    } else {
                        formState.email.error = false;
                        showAlert(window.translations["alert.emailIsAvailable"], "success");
                    }
                })
                .catch(error => {
                    const errorMsg = window.translations["alert.emailCheckFailed"];
                    formState.email.error = errorMsg;
                    showAlert(window.translations["alert.emailCheckFailed"], "danger");
                });
        });

        document.getElementById("isCasEmployee").addEventListener("change", handleCasEmployeeChange);

        document.getElementById("useContactAddress").addEventListener("change", (ev) => {
            const contactAddressFields = document.getElementById("contactAddressFields");
            if (ev.target.checked) {
                contactAddressFields.classList.remove("hidden");
            } else {
                contactAddressFields.classList.add("hidden");
            }
        });

        document.querySelector("form").addEventListener("submit", (event) => {
            const formElm = event.currentTarget;
            if (isFormSubmitting(formElm)) {
                event.preventDefault();
                return;
            }

            const casEmployeeChecked = document.getElementById("isCasEmployee").checked;
            const validFiles = pond.getFiles().filter(file => file.status === FilePond.FileStatus.IDLE);
            const nowLoadingFiles = pond.getFiles().filter(file => file.status === FilePond.FileStatus.INIT || file.status === FilePond.FileStatus.LOADING);
            const invalidFiles = pond.getFiles().filter(file => file.status !== FilePond.FileStatus.IDLE && file.status !== FilePond.FileStatus.INIT && file.status !== FilePond.FileStatus.LOADING);

            if (renewalActionInputElm && event.submitter && event.submitter.dataset.renewalAction) {
                renewalActionInputElm.value = event.submitter.dataset.renewalAction;
            }

            for (let formField in formState) {
                if (formState[formField].error) {
                    event.preventDefault();
                    alert(formState[formField].error);
                    formState[formField].elm.scrollIntoView({ behavior: "smooth" });
                    return;
                }
            }

            if (nowLoadingFiles.length > 0) {
                event.preventDefault();
                showAlert(window.translations["alert.attachedFilesStillLoading"], "danger");
                return;
            }

            if (invalidFiles.length > 0) {
                event.preventDefault();
                showAlert(window.translations["alert.attachedFilesInvalid"], "danger");
                return;
            }

            if (casEmployeeChecked && validFiles.length === 0 && emailInputElm.value.trim().length === 0) {
                event.preventDefault();
                showAlert(window.translations["alert.casEmployeesFormRequirements"], "danger");
            } else {
                const totalSize = validFiles.reduce((sum, file) => sum + file.fileSize, 0);
                const maxTotalSize = 60 * 1024 * 1024; // 60 MB in bytes

                if (totalSize > maxTotalSize) {
                    event.preventDefault();
                    showAlert(window.translations["alert.totalFileSizeExceeded"], "danger");
                } else {
                    lockFormSubmission(formElm);
                    loaderAfterSubmit.show();
                }
            }
        });

        updateRenewalPaymentUI();

        if (isRenewalPage) {
            document.addEventListener("voucher:validation", (event) => {
                const detail = event.detail || {};
                renewalUiState.voucherValidated = detail.valid === true;
                renewalUiState.discountAmount = detail.valid === true ? parseAmount(detail.discountAmount) : 0;
                renewalUiState.amountToPayAfterVoucher = detail.valid === true
                    ? parseAmount(detail.amountToPay)
                    : null;
                updateRenewalPaymentUI();
            });
        }

        const addressAutofillElms = document.querySelectorAll(".js-autocomplete-address");
        for (let i = 0; i < addressAutofillElms.length; i++) {
            (function(inputElm) {
                const autoCompleteJS = new autoComplete({
                    selector: () => inputElm,
                    placeHolder: "",
                    searchEngine: (query, record) => `<mark>${record}</mark>`,
                    data: {
                        keys: ["value"],
                        src: async(query) => {
                            try {
                                const params = new URLSearchParams({ query });
                                const fetchData = await fetch(`${apiUrl}/suggest-address?${params.toString()}`);

                                if (!fetchData.ok) {
                                    return [];
                                }

                                const jsonData = await fetchData.json();

                                if (jsonData.items) {
                                    return jsonData.items.map(item => ({
                                        value: item.name,
                                        data: item,
                                    }));
                                }

                                return [];
                            } catch (exc) {
                                console.log(exc);

                                return [];
                            }
                        },
                        cache: false,
                    },
                    resultItem: {
                        element: (item, data) => {
                            const itemData = data.value.data;
                            let desc = document.createElement("div");

                            desc.style = "overflow: hidden; white-space: nowrap; text-overflow: ellipsis;";
                            desc.innerHTML = `${itemData.label}, ${itemData.location}`;
                            item.append(
                                desc,
                            );
                        },
                        highlight: true
                    },
                    resultsList: {
                        element: (list, data) => {
                            list.style.maxHeight = "max-content";
                            list.style.overflow = "hidden";

                            if (!data.results.length) {
                                let message = document.createElement("div");

                                message.setAttribute("class", "no_result");
                                message.style = "padding: 5px";
                                message.innerHTML = `<span>Žádné výsledky pro "${data.query}"</span>`;
                                list.prepend(message);
                            } else {
                                let logoHolder = document.createElement("div");
                                let text = document.createElement("span");
                                const img = new Image();

                                logoHolder.style = "padding: 5px; display: flex; align-items: center; justify-content: end; gap: 5px; font-size: 12px;";
                                text.textContent = "Powered by";
                                img.src = "https://api.mapy.cz/img/api/logo-small.svg";
                                img.style = "width: 60px";
                                logoHolder.append(text, img);
                                list.append(logoHolder);
                            }
                        },
                        noResults: true,
                    },
                });

                inputElm.addEventListener("selection", event => {
                    const origData = event.detail.selection.value.data;

                    if (!origData.regionalStructure) {
                        return false;
                    }

                    const regionalStructure = origData.regionalStructure;
                    let streetElm, cityElm, zipElm;

                    switch (inputElm.id) {
                        case "address1":
                            streetElm = document.getElementById("address1");
                            cityElm = document.getElementById("address2");
                            zipElm = document.getElementById("zip");
                            break;
                        case "contactAddress1":
                            streetElm = document.getElementById("contactAddress1");
                            cityElm = document.getElementById("contactAddress2");
                            zipElm = document.getElementById("contactZip");
                            break;
                    }

                    const addressItem = regionalStructure.filter(item => item.type === "regional.address");
                    const addressName = addressItem.length > 0 ? addressItem[0].name : null;
                    const streetItem = regionalStructure.filter(item => item.type === "regional.street");
                    const streetName = streetItem.length > 0 ? streetItem[0].name : null;
                    const municipalityItem = regionalStructure.filter(item => item.type === "regional.municipality");
                    const municipalityName = municipalityItem.length > 0 ? municipalityItem[0].name : null;
                    const municipalityPartItem = regionalStructure.filter(item => item.type === "regional.municipality_part");
                    const municipalityPartName = municipalityPartItem.length > 0 ? municipalityPartItem[0].name : null;

                    if (streetName) {
                        streetElm.value = streetName + (addressName ? (" " + addressName) : "");
                    } else if (municipalityPartName) {
                        streetElm.value = municipalityPartName + (addressName ? (" " + addressName) : "");
                    } else {
                        streetElm.value = "";
                    }
                    if (municipalityItem.length > 0 && municipalityItem[0].name === "Praha" && municipalityPartItem.length >= 2) {
                        cityElm.value = municipalityPartItem[1].name + " - " + municipalityPartItem[0].name;
                    } else {
                        cityElm.value = municipalityName || "";
                    }
                    zipElm.value = origData.zip || "";
                });
            })(addressAutofillElms[i]);
        }
    }
}

// PAGE: MEMBERSHIP RENEWAL
if (document.querySelector(".page-membership-renewal")) {
    const jsbtnUseInputValElms = document.querySelectorAll(".jsbtn-useInputVal");
    const renewalPrefillAlephElms = document.querySelectorAll(".js-renewal-prefill-aleph");

    jsbtnUseInputValElms.forEach(button => {
        button.addEventListener("click", function(event) {
            event.preventDefault();
            const targetSelector = this.getAttribute("data-input-target");
            const parentElm = this.parentElement;
            const input = targetSelector ? document.querySelector(targetSelector) : parentElm.querySelector("input");
            const value = this.querySelector("i").textContent;
            if (input) {
                input.value = value;
            }
        });
    });

    let prefilledCount = 0;
    renewalPrefillAlephElms.forEach(button => {
        const targetSelector = button.getAttribute("data-input-target");
        const input = targetSelector ? document.querySelector(targetSelector) : null;
        const valueElm = button.querySelector("i");
        const value = valueElm ? valueElm.textContent.trim() : "";

        if (!input || !value || input.value.trim().length > 0) {
            return;
        }

        input.value = value;
        prefilledCount += 1;
    });

    if (prefilledCount > 0) {
        showAlert(window.translations["alert.renewalAlephPrefillApplied"], "info", 10000);
    }
}

// FORM: SET/RESET IDENTITY PASSWORD
if (document.getElementById("form-identity-password")) {

    // Password regex validation
    const isValidPassword = (password) => {
        const regex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d\W]{6,13}$/;
        return regex.test(password);
    };

    // Password validation
    const pswFormElm = document.getElementById('form-identity-password');

    pswFormElm.addEventListener('submit', function(event) {
        if (isFormSubmitting(pswFormElm)) {
            event.preventDefault();
            return;
        }

        const password = document.getElementById('newPassword').value;
        const repeatPassword = document.getElementById('repeatNewPassword').value;
        
        if (!isValidPassword(password)) {
            event.preventDefault();
            alert(window.translations["form.patronPassword.error.invalid"]);
            return;
        }
        
        if (password !== repeatPassword) {
            event.preventDefault();
            alert(window.translations["form.patronPassword.error.notMatching"]);
            return;
        }

        // Prevent repeated submissions while the server updates Aleph and prepares payment.
        lockFormSubmission(pswFormElm);
    });

    // Add event listeners to password toggler buttons
    document.querySelectorAll('.password-toggler').forEach(button => {
        button.addEventListener('click', function() {
            const fieldId = this.getAttribute('data-field-id');
            togglePasswordVisibility(fieldId, this);
        });
    });

}

// FORM: INITIATE PAYMENT
const paymentInitiateButtonElm = document.getElementById("payment-initiate-button");
if (paymentInitiateButtonElm) {
    const paymentInitiateFormElm = paymentInitiateButtonElm.closest("form");

    paymentInitiateFormElm.addEventListener("submit", function(event) {
        if (isFormSubmitting(paymentInitiateFormElm)) {
            event.preventDefault();
            return;
        }

        lockFormSubmission(paymentInitiateFormElm);
    });
}

// COMMON
document.querySelectorAll("form.js-prevent-repeat-submit").forEach(formElm => {
    formElm.addEventListener("submit", function(event) {
        if (isFormSubmitting(formElm)) {
            event.preventDefault();
            return;
        }

        lockFormSubmission(formElm);
    });
});

if (document.getElementById("js-printPage")) {
    document.getElementById("js-printPage").addEventListener("click", triggerPrintPage);
}
if (document.getElementById("js-deleteIdentity")) {
    document.getElementById("js-deleteIdentity").addEventListener("click", triggerDeleteIdentity);
}
if (document.getElementById("js-restoreIdentity")) {
    document.getElementById("js-restoreIdentity").addEventListener("click", triggerRestoreIdentity);
}
if (document.getElementById("js-identityAlephDeleted")) {
    document.getElementById("js-identityAlephDeleted").addEventListener("click", triggerMarkIdentityAsDeletedInAleph);
}
if (goToServiceBtnElms.length > 0) {
    goToServiceBtnElms.forEach(link => {
        link.addEventListener('click', () => {
            const textToCopy = link.getAttribute('data-copy');
            navigator.clipboard.writeText(textToCopy);
            showAlert(window.translations["alert.copiedToClipboard"], "success");
        });
    });
}

void autoLogoutOnLoad();

// Header respo
respoHeader();
let resizeRaf = null;   // Throttle resize events
window.addEventListener("resize", () => {
    if (resizeRaf) return;
    resizeRaf = window.requestAnimationFrame(() => {
        resizeRaf = null;
        respoHeader();
    });
});

// // TESTING
// const emptyIdentities = async () => {
//     const response = await fetch(`${apiUrl}/reset-identities`, {
//         method: "GET",
//         headers: {
//             "Content-Type": "application/x-www-form-urlencoded",
//         }
//     });

//     if (!response.ok) {
//         alert(`HTTP-Error: ${response.status} ${response.error}`);
//     }

//     const data = await response.json();
//     return data;
// }
// const btnEmptyIdentities = document.getElementById("js-resetIdentities");
// if (btnEmptyIdentities) {
//     btnEmptyIdentities.addEventListener("click", async () => {
//         emptyIdentities()
//             .then(data => {
//                 if (data.result === true) {
//                     alert(data.message);
//                 } else {
//                     alert("An error occurred while resetting the identities.");
//                 }
//             })
//             .catch(error => {
//                 alert("An error occurred while resetting the identities.");
//             });
//     });
// }
