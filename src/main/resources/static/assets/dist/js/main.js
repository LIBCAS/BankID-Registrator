const apiUrl = "/bankid-registrator/api";
const alertDuration = 3000;

function getCsrfTokenAndHeader() {
    const csrfInput = document.querySelector('input[name="_csrf"]');
    return {
        csrfToken: csrfInput ? csrfInput.value : null,
        csrfHeader: csrfInput ? csrfInput.name : null
    };
}

// PAGE: NEW REGISTRATION
if (document.querySelector(".page-new-registration")) {
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

    const checkRfid = async (rfid, patronId) => {
        const response = await fetch(`${apiUrl}/check-rfid`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                [csrfHeader]: csrfToken
            },
            body: new URLSearchParams({
                rfid: rfid,
                patronId: patronId
            })
        });

        if (!response.ok) {
            alert(`HTTP-Error: ${response.status} ${response.error}`);
        }

        const data = await response.json();
        return data;
    }

    FilePond.registerPlugin(
        FilePondPluginFileValidateSize,
        FilePondPluginImageExifOrientation,
        FilePondPluginImagePreview
    );

    FilePond.create(filesElm);

    FilePond.setOptions({
        storeAsFile: true
    });

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

        checkRfid(rfid, patronId)
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

    document.getElementById("isCasEmployee").addEventListener("change", (ev) => {
        const casEmployee = ev.target;
        const casEmployeeValue = casEmployee.checked;

        if (casEmployeeValue) {
            filesInputElm.required = true;
            filesWrapper.style.display = "block";
        } else {
            filesInputElm.required = false;
            filesWrapper.style.display = "none";
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