// Voucher generation: preserve the entered date while toggling unlimited validity.
document.querySelectorAll('[data-expiry-input]').forEach(function (checkbox) {
    var input = document.getElementById(checkbox.dataset.expiryInput);
    if (!input) return;

    function updateExpiryInput() {
        input.disabled = checkbox.checked;
    }

    checkbox.addEventListener('change', updateExpiryInput);
    updateExpiryInput();
});
