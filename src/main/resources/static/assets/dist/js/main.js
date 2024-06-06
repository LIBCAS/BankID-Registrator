// PAGE: NEW REGISTRATION
if (document.querySelector(".page-new-registration")) {
    const apiUrl = "/bankid-registrator/api";
    const filesElm = document.getElementById("files");
    const filesInputElm = document.querySelector('[name="media"]');
    const filesWrapper = document.getElementById("files-control");
    const alertDuration = 3000;

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

    const checkRfid = async (rfid, bid, patronId) => {
        const response = await fetch(`${apiUrl}/check-rfid`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            },
            body: new URLSearchParams({
                rfid: rfid,
                bid: bid,
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
        const bid = document.querySelector('input[name="bankIdSub"]').value;
        let patronId = document.querySelector('input[name="patronId"]').value;

        if (bid.trim().length === 0) {
            this.value = "";
            showAlert("danger", "An error occurred while checking the RFID.");
            return;
        }

        if (rfid.trim().length === 0) {
            return;
        }

        if (patronId.trim().length === 0) {
            patronId = null;
        }

        checkRfid(rfid, bid, patronId)
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