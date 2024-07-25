import { Modal } from "https://esm.sh/flowbite";  // In order to control the Flobite Modal class programmatically
const apiUrl = "/bankid-registrator/api";

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

const showAlert = (message, type = "info", duration = 6000, wrapperElmId = "alert-wrapper") => {
    const alertWrapperElm = document.getElementById(wrapperElmId);
    const alertElmId = "alert--" + Math.floor(100000 + Math.random() * 900000);
    let alertColor = "";
    let alertSvgIcon = "";

    switch (type) {
        case "info":
            alertColor = "blue";
            alertSvgIcon = `<svg class="w-6 h-6 text-gray-800 dark:text-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11h2v5m-2 0h4m-2.592-8.5h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "success":
            alertColor = "green";
            alertSvgIcon = `<svg class="w-6 h-6 text-gray-800 dark:text-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 11h2v5m-2 0h4m-2.592-8.5h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "warning":
            alertColor = "yellow";
            alertSvgIcon = `<svg class="w-6 h-6 text-gray-800 dark:text-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 13V8m0 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
        case "danger":
            alertColor = "red";
            alertSvgIcon = `<svg class="w-6 h-6 text-gray-800 dark:text-white" aria-hidden="true" xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" viewBox="0 0 24 24">
                <path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 13V8m0 8h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"/>
            </svg>`;
            break;
    }

    const alertHtml = `<div id="${alertElmId}" class="flex items-center p-4 mb-4 text-${alertColor}-800 border border-${alertColor}-300 rounded-lg bg-${alertColor}-50 dark:bg-gray-800 dark:text-${alertColor}-400 dark:border-${alertElmId}-800" role="alert">
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

    setTimeout(() => {
        document.getElementById(alertElmId).remove();
    }, duration);
};

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


// PAGES: NEW REGISTRATION, MEMBERSHIP RENEWAL
if (document.querySelector(".page-new-registration, .page-membership-renewal")) {
    const filesElm = document.getElementById("files");
    const filesInputElm = document.querySelector('[name="media"]');
    const emailInputElm = document.querySelector('[name="email"]');
    const filesWrapper = document.getElementById("files-control");
    const { csrfToken, csrfHeader } = getCsrfTokenAndHeader();

    function handleCasEmployeeChange(ev) {
        const casEmployeeValue = ev ? ev.target.checked : document.getElementById("isCasEmployee").checked;
    
        if (casEmployeeValue) {
            filesWrapper.style.display = "block";
        } else {
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
        maxFileSize: '20MB'
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
                    showAlert("The RFID already exists.", "danger");
                } else {
                    showAlert("The RFID is available.", "success");
                }
            })
            .catch(error => {
                rfidElm.value = "";
                showAlert("An error occurred while checking the RFID.", "danger");
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
                    emailElm.value = "";
                    showAlert("The email is used by somebody else.", "danger");
                } else {
                    showAlert("The email is available.", "success");
                }
            })
            .catch(error => {
                emailElm.value = "";
                showAlert("An error occurred while checking the email availability.", "danger");
            });
    });

    document.getElementById("isCasEmployee").addEventListener("change", handleCasEmployeeChange);

    document.querySelector("form").addEventListener("submit", (event) => {
        const casEmployeeChecked = document.getElementById("isCasEmployee").checked;
        console.log(pond.getFiles());
        const validFiles = pond.getFiles();

        if (casEmployeeChecked && validFiles.length === 0 && emailInputElm.value.trim().length === 0) {
            event.preventDefault();
            showAlert("CAS employees must fill in a valid CAS e-mail address or upload an identity document (identity card / passport)", "danger");
        }
    });

    const addressAutofillElms = document.querySelectorAll('.js-autocomplete-address');
    for (let i = 0; i < addressAutofillElms.length; i++) {
        (function(inputElem) {
            const autoCompleteJS = new autoComplete({
                selector: () => inputElem,
                placeHolder: '',
                searchEngine: (query, record) => `<mark>${record}</mark>`,
                data: {
                    keys: ["value"],
                    src: async(query) => {
                        try {
                            const fetchData = await fetch(`${apiUrl}/suggest-address/${query}`);
                            const jsonData = await fetchData.json();

                            if (jsonData.items) {
                                return jsonData.items.map(item => ({
                                    value: item.name,
                                    data: item,
                                }));
                            }
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

            inputElem.addEventListener("selection", event => {
                const origData = event.detail.selection.value.data;

                if (!origData.regionalStructure) {
                    return false;
                }

                const regionalStructure = origData.regionalStructure;
                let streetElm, cityElm, zipElm;

                switch (inputElem.id) {
                    case 'address1':
                        streetElm = document.getElementById("address1");
                        cityElm = document.getElementById("address2");
                        zipElm = document.getElementById("zip");
                        break;
                    case 'contactAddress1':
                        streetElm = document.getElementById("contactAddress1");
                        cityElm = document.getElementById("contactAddress2");
                        zipElm = document.getElementById("contactZip");
                        break;
                }

                const addressItem = regionalStructure.filter(item => item.type === 'regional.address');
                const addressName = addressItem.length > 0 ? addressItem[0].name : null;
                const streetItem = regionalStructure.filter(item => item.type === 'regional.street');
                const streetName = streetItem.length > 0 ? streetItem[0].name : null;
                const municipalityItem = regionalStructure.filter(item => item.type === 'regional.municipality');
                const municipalityName = municipalityItem.length > 0 ? municipalityItem[0].name : null;
                const municipalityPartItem = regionalStructure.filter(item => item.type === 'regional.municipality_part');
                const municipalityPartName = municipalityPartItem.length > 0 ? municipalityPartItem[0].name : null;

                if (streetName) {
                    streetElm.value = streetName + (addressName ? (' ' + addressName) : '');
                } else if (municipalityPartName) {
                    streetElm.value = municipalityPartName + (addressName ? (' ' + addressName) : '');
                } else {
                    streetElm.value = '';
                }
                if (municipalityItem.length > 0 && municipalityItem[0].name === 'Praha' && municipalityPartItem.length >= 2) {
                    cityElm.value = municipalityPartItem[1].name + ' - ' + municipalityPartItem[0].name;
                } else {
                    cityElm.value = municipalityName || '';
                }
                zipElm.value = origData.zip || '';
            });
        })(addressAutofillElms[i]);
    }
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

// COMMON
if (document.getElementById("js-printPage")) {
    document.getElementById("js-printPage").addEventListener("click", triggerPrintPage);
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
                    alert(data.message);
                } else {
                    alert("An error occurred while resetting the identities.");
                }
            })
            .catch(error => {
                alert("An error occurred while resetting the identities.");
            });
    });
}