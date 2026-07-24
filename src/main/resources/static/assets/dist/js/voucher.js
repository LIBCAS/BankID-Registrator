/**
 * Voucher (discount code) validation for registration and renewal forms.
 * 
 * This script handles:
 * - AJAX validation of voucher codes via /api/validate-voucher
 * - Showing discount preview (original price, discount amount, new price)
 * - Storing validated voucher code in a hidden field for form submission
 * 
 * Expected DOM elements (on pages that have a voucher field):
 * - #voucherCode — text input for the voucher code
 * - #btn-validate-voucher — button to trigger validation
 * - #voucher-result — container for validation result message
 * - input[name="voucherCode"] — the same input, its value is sent with the form
 */
(function () {
    'use strict';

    const API_URL = '/bankid-registrator/api/validate-voucher';

    var csrfMeta = document.querySelector('meta[name="_csrf"]');
    var csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');
    var csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : null;
    var csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.getAttribute('content') : null;

    const voucherInput = document.getElementById('voucherCode');
    const validateBtn = document.getElementById('btn-validate-voucher');
    const resultDiv = document.getElementById('voucher-result');

    if (!voucherInput || !validateBtn || !resultDiv) {
        return; // Not on a page with voucher support
    }

    // Force uppercase as user types
    voucherInput.addEventListener('input', function () {
        this.value = this.value.toUpperCase().replace(/\s/g, '');
        clearValidationState();
    });

    validateBtn.addEventListener('click', function () {
        const code = voucherInput.value.trim();
        if (!code) {
            showResult(false, getMsg('voucher.error.empty', 'Please enter a voucher code.'));
            return;
        }

        validateBtn.disabled = true;
        validateBtn.textContent = '...';

        var fetchHeaders = { 'Content-Type': 'application/x-www-form-urlencoded' };
        if (csrfToken && csrfHeaderName) {
            fetchHeaders[csrfHeaderName] = csrfToken;
        }

        fetch(API_URL, {
            method: 'POST',
            headers: fetchHeaders,
            body: 'voucherCode=' + encodeURIComponent(code),
        })
        .then(function (response) {
            if (response.status === 401) {
                showResult(false, getMsg('voucher.error.sessionExpired', 'Session expired. Please reload the page.'));
                return null;
            }
            return response.json();
        })
        .then(function (data) {
            if (!data) return;

            if (data.valid) {
                var discountAmount = parseFloat(data.discountAmount).toFixed(2);
                var amountToPay = parseFloat(data.amountToPay).toFixed(2);
                var feeAmount = parseFloat(data.feeAmount).toFixed(2);

                var message = getMsg('voucher.valid.prefix', 'Valid!') + ' ';

                if (parseFloat(amountToPay) === 0) {
                    message += getMsg('voucher.valid.fullyCovered', 'The fee is fully covered by this voucher. No payment required.');
                } else {
                    message += getMsg('voucher.valid.discount', 'Discount') + ': -' + discountAmount + ' '
                        + getMsg('voucher.currency', 'Kč') + '. '
                        + getMsg('voucher.valid.youPay', 'You pay') + ': ' + amountToPay + ' '
                        + getMsg('voucher.currency', 'Kč')
                        + ' (' + getMsg('voucher.valid.insteadOf', 'instead of') + ' ' + feeAmount + ' '
                        + getMsg('voucher.currency', 'Kč') + ')';
                }

                setValidationState(true, {
                    code: code,
                    discountAmount: discountAmount,
                    amountToPay: amountToPay,
                    feeAmount: feeAmount,
                });
                showResult(true, message);
            } else {
                var errorKey = data.error || 'voucher.error.unknown';
                setValidationState(false);
                showResult(false, getMsg(errorKey, 'Invalid voucher code.'));
            }
        })
        .catch(function () {
            setValidationState(false);
            showResult(false, getMsg('voucher.error.network', 'Network error. Please try again.'));
        })
        .finally(function () {
            validateBtn.disabled = false;
            validateBtn.textContent = getMsg('voucher.validateBtn', 'Validate');
        });
    });

    // Allow pressing Enter in voucher input to trigger validation
    voucherInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            validateBtn.click();
        }
    });

    function showResult(isValid, message) {
        resultDiv.classList.remove('hidden');
        // Reset classes
        resultDiv.className = 'mt-2 p-3 rounded-md text-sm';

        if (isValid) {
            resultDiv.classList.add('bg-green-100', 'text-green-800');
        } else {
            resultDiv.classList.add('bg-red-100', 'text-red-800');
        }

        resultDiv.textContent = message;
    }

    function clearValidationState() {
        resultDiv.classList.add('hidden');
        resultDiv.className = resultDiv.className.replace(/bg-\S+/g, '').replace(/text-\S+/g, '');
        setValidationState(false);
    }

    function setValidationState(isValid, details) {
        var voucherDetails = details || {};
        resultDiv.dataset.valid = isValid ? 'true' : 'false';
        resultDiv.dataset.code = isValid ? (voucherDetails.code || '') : '';
        resultDiv.dataset.discountAmount = isValid ? String(voucherDetails.discountAmount || '0') : '';
        resultDiv.dataset.amountToPay = isValid ? String(voucherDetails.amountToPay || '0') : '';
        resultDiv.dataset.feeAmount = isValid ? String(voucherDetails.feeAmount || '0') : '';

        document.dispatchEvent(new CustomEvent('voucher:validation', {
            detail: {
                valid: isValid,
                code: resultDiv.dataset.code || '',
                discountAmount: parseFloat(resultDiv.dataset.discountAmount || '0'),
                amountToPay: parseFloat(resultDiv.dataset.amountToPay || '0'),
                feeAmount: parseFloat(resultDiv.dataset.feeAmount || '0'),
            },
        }));
    }

    /**
     * Get a localized message from the data attributes on the voucher result div,
     * falling back to the provided default.
     */
    function getMsg(key, defaultMsg) {
        // Messages are injected as data attributes on a meta element
        var meta = document.getElementById('voucher-messages');
        if (meta) {
            var val = meta.getAttribute('data-' + key.replace(/\./g, '-'));
            if (val) return val;
        }
        return defaultMsg;
    }
})();
