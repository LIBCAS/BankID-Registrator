const apiUrl = "/bankid-registrator/api";
const alertDuration = 3000;

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

// PAGES: NEW REGISTRATION, MEMBERSHIP RENEWAL
if (document.querySelector(".page-new-registration, .page-membership-renewal")) {
    const filesElm = document.getElementById("files");
    const filesInputElm = document.querySelector('[name="media"]');
    const filesWrapper = document.getElementById("files-control");
    const { csrfToken, csrfHeader } = getCsrfTokenAndHeader();

    function showAlert(type, message) {
        const mainElm = document.querySelector("form");
        const alert = document.createElement("div");
        const typeCss = {
            success: "green",
            danger: "red",
        };

        alert.className = `relative block w-full p-4 mb-4 text-base leading-5 text-white bg-${typeCss[type]}-500 rounded-lg opacity-100 font-regular`;
        alert.textContent = message;

        mainElm.prepend(alert);

        setTimeout(() => {
            mainElm.removeChild(alert);
        }, alertDuration);
    }

    function handleCasEmployeeChange(ev) {
        const casEmployeeValue = ev ? ev.target.checked : document.getElementById("isCasEmployee").checked;
    
        if (casEmployeeValue) {
            filesInputElm.required = true;
            filesWrapper.style.display = "block";
        } else {
            filesInputElm.required = false;
            filesWrapper.style.display = "none";
        }
    }

    FilePond.registerPlugin(
        FilePondPluginFileValidateSize,
        FilePondPluginImageExifOrientation,
        FilePondPluginImagePreview
    );

    const pond = FilePond.create(filesElm);

    FilePond.setOptions({
        storeAsFile: true,
        acceptedFileTypes: ['image/png', 'image/jpeg', 'application/pdf'],
        maxFileSize: '10MB'
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
                    rfidElm.value = "";
                    showAlert("danger", "The RFID already exists.");
                } else {
                    showAlert("success", "The RFID is available.");
                }
            })
            .catch(error => {
                rfidElm.value = "";
                showAlert("danger", "An error occurred while checking the RFID.");
            });
    });

    document.getElementById("isCasEmployee").addEventListener("change", handleCasEmployeeChange);

    document.querySelector("form").addEventListener("submit", (event) => {
        const casEmployeeChecked = document.getElementById("isCasEmployee").checked;
        console.log(pond.getFiles());
        const validFiles = pond.getFiles();

        if (casEmployeeChecked && validFiles.length === 0) {
            event.preventDefault();
            showAlert("danger", "Please upload at least one valid file.");
        }
    });
}

// PAGE: MEMBERSHIP RENEWAL
if (document.querySelector(".page-membership-renewal")) {
    const jsbtnUseInputValElms = document.querySelectorAll('.jsbtn-useInputVal');

    jsbtnUseInputValElms.forEach(button => {
        button.addEventListener('click', function(event) {
            event.preventDefault();
            const parentElement = this.parentElement;
            const input = parentElement.querySelector('input');
            const value = this.querySelector('i').textContent;
            input.value = value;
        });
    });
}

// FORM: SET/RESET IDENTITY PASSWORD
if (document.getElementById("form-identity-password")) {
    const formElm = document.getElementById("form-identity-password");
    const passwordElm = document.getElementById("newPassword");
    const passwordConfirmElm = document.getElementById("repeatNewPassword");

    formElm.addEventListener("submit", (ev) => {
        const password = passwordElm.value;
        const passwordConfirm = passwordConfirmElm.value;

        if (password !== passwordConfirm) {
            alert("The passwords do not match.");
            ev.preventDefault();
        }
    });
}

// TESTING
const emptyIdentities = async () => {
    const response = await fetch(`${apiUrl}/reset-identities`, {
        method: "GET",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        }
    });

    if (!response.ok) {
        alert(`HTTP-Error: ${response.status} ${response.error}`);
    }

    const data = await response.json();
    return data;
}
const btnEmptyIdentities = document.getElementById("js-resetIdentities");
if (btnEmptyIdentities) {
    btnEmptyIdentities.addEventListener("click", async () => {
        emptyIdentities()
            .then(data => {
                if (data.result === true) {
                    alert("The identities have been reset.");
                } else {
                    alert("An error occurred while resetting the identities.");
                }
            })
            .catch(error => {
                alert("An error occurred while resetting the identities.");
            });
    });
}