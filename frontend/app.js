
const API_URL = "/api/clients";
const LAWYER_API = "/api/lawyers";
const CASE_API = "/api/cases";
const APPOINTMENT_API = "/api/appointments";

const $ = id => document.getElementById(id);

const clientTable = $("clientTable");
const clientCount = $("clientCount");
const message = $("message");
const refreshButton = $("refreshButton");
const clientForm = $("clientForm");

const lawyerTable = $("lawyerTable");
const lawyerCount = $("lawyerCount");
const caseTable = $("caseTable");
const caseCount = $("caseCount");

const appointmentTable = $("appointmentTable");
const appointmentMessage = $("appointmentMessage");
const appointmentForm = $("appointmentForm");
const refreshAppointmentsButton = $("refreshAppointmentsButton");

let clientsCache = [];
let lawyersCache = [];

function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value == null || value === "" ? "—" : value;
    row.appendChild(cell);
    return cell;
}

async function requestJSON(url, options = {}) {
    const response = await fetch(url, options);
    const text = await response.text();

    let data;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        throw new Error("The server returned an invalid response.");
    }

    if (!response.ok) {
        throw new Error(data?.error || `Request failed (${response.status})`);
    }
    return data;
}

// CLIENT CRUD: preserved
async function loadClients() {
    refreshButton.disabled = true;

    try {
        const clients = await requestJSON(API_URL);
        clientsCache = Array.isArray(clients) ? clients : [];

        clientTable.replaceChildren();
        clientCount.textContent = clientsCache.length;

        if ($("appointmentCount")) {
            // Updated when appointments load.
        }

        if (!clientsCache.length) {
            clientTable.innerHTML =
                '<tr><td colspan="4">No clients found.</td></tr>';
        }

        clientsCache.forEach(client => {
            const row = document.createElement("tr");
            addCell(row, client.clientId);
            addCell(row, client.fullName);
            addCell(row, client.email);

            const actions = addCell(row, "");
            const edit = document.createElement("button");
            edit.textContent = "Edit";
            edit.className = "edit-button";

            edit.addEventListener("click", async () => {
                const fullName = prompt("Enter client name:", client.fullName);
                if (fullName === null) return;

                const email = prompt("Enter client email:", client.email);
                if (email === null) return;

                if (!fullName.trim() || !email.trim()) {
                    message.textContent = "Name and email are required.";
                    return;
                }

                try {
                    await requestJSON(`${API_URL}?id=${client.clientId}`, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            fullName: fullName.trim(),
                            email: email.trim()
                        })
                    });
                    message.textContent = "Client updated successfully.";
                    await loadClients();
                    await loadAppointments();
                } catch (error) {
                    message.textContent = error.message;
                }
            });

            const remove = document.createElement("button");
            remove.textContent = "Delete";
            remove.className = "delete-button";

            remove.addEventListener("click", async () => {
                if (!confirm(`Delete ${client.fullName}?`)) return;

                try {
                    await requestJSON(`${API_URL}?id=${client.clientId}`, {
                        method: "DELETE"
                    });
                    message.textContent = "Client deleted successfully.";
                    await loadClients();
                    await loadAppointments();
                } catch (error) {
                    message.textContent = error.message;
                }
            });

            actions.append(edit, remove);
            clientTable.appendChild(row);
        });

        message.textContent = "Client records loaded successfully.";
        populateAppointmentDropdowns();
    } catch (error) {
        message.textContent = "Unable to load clients: " + error.message;
        console.error("Client loading failed:", error);
    } finally {
        refreshButton.disabled = false;
    }
}

clientForm.addEventListener("submit", async event => {
    event.preventDefault();

    const fullName = $("fullName").value.trim();
    const email = $("email").value.trim();

    try {
        await requestJSON(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fullName, email })
        });

        clientForm.reset();
        message.textContent = "Client added successfully.";
        await loadClients();
    } catch (error) {
        message.textContent = error.message;
    }
});

// LAWYERS

async function loadLawyers() {
    const table = document.getElementById("lawyerTable");
    const count = document.getElementById("lawyerCount");

    if (!table) return;

    table.innerHTML =
        '<tr><td colspan="5">Loading lawyers...</td></tr>';

    try {
        const lawyers = await requestJSON(LAWYER_API);

        lawyersCache = lawyers;
        renderLawyers(lawyers);

    } catch (error) {
        table.innerHTML =
            '<tr><td colspan="5">Unable to load lawyers.</td></tr>';

        if (count) count.textContent = "0 lawyers";
        console.error("Lawyer loading error:", error);
    }
}

function renderLawyers(lawyers) {
    const table = document.getElementById("lawyerTable");
    const count = document.getElementById("lawyerCount");

    if (!table) return;

    table.innerHTML = "";

    if (count) {
        count.textContent = `${lawyers.length} lawyers`;
    }

    if (lawyers.length === 0) {
        table.innerHTML =
            '<tr><td colspan="5">No matching lawyers found.</td></tr>';
        return;
    }

    lawyers.forEach(lawyer => {
        const row = document.createElement("tr");

        [
            lawyer.lawyerId,
            lawyer.fullName,
            lawyer.specialization,
            lawyer.experienceYears,
            lawyer.consultationFee
        ].forEach(value => addCell(row, value ?? "—"));

        table.appendChild(row);
    });
}

function filterLawyers() {
    const search = (
        document.getElementById("lawyerSearch")?.value || ""
    ).trim().toLowerCase();

    const specialization = (
        document.getElementById("specializationFilter")?.value || ""
    ).trim().toLowerCase();

    const experienceInput =
        document.getElementById("experienceFilter")?.value;

    const feeInput =
        document.getElementById("maxFeeFilter")?.value;

    const minExperience =
        experienceInput === "" || experienceInput == null
            ? null
            : Number(experienceInput);

    const maxFee =
        feeInput === "" || feeInput == null
            ? null
            : Number(feeInput);

    const filtered = lawyersCache.filter(lawyer => {
        const name = String(lawyer.fullName || "").toLowerCase();
        const area = String(lawyer.specialization || "").toLowerCase();
        const years = Number(lawyer.experienceYears);
        const fee = Number(lawyer.consultationFee);

        return (
            name.includes(search) &&
            area.includes(specialization) &&
            (minExperience === null ||
                (!Number.isNaN(years) && years >= minExperience)) &&
            (maxFee === null ||
                (!Number.isNaN(fee) && fee <= maxFee))
        );
    });

    renderLawyers(filtered);
}

document.getElementById("lawyerFilterForm")
    ?.addEventListener("submit", event => {
        event.preventDefault();
        filterLawyers();
    });

document.getElementById("clearLawyerFilters")
    ?.addEventListener("click", () => {
        document.getElementById("lawyerFilterForm").reset();
        renderLawyers(lawyersCache);
    });


// LEGAL CASES
async function loadCases() {
    try {
        const cases = await requestJSON(CASE_API);
        caseTable.replaceChildren();
        caseCount.textContent = `${cases.length} cases`;

        if ($("activeCaseCount")) {
            $("activeCaseCount").textContent = cases.filter(
                item => !["Completed", "Closed"].includes(item.status)
            ).length;
        }

        if (!cases.length) {
            caseTable.innerHTML =
                '<tr><td colspan="5">No legal cases found.</td></tr>';
        }

        cases.forEach(item => {
            const row = document.createElement("tr");
            addCell(row, item.caseId);
            addCell(row, item.clientId);
            addCell(row, item.category);
            addCell(row, item.status);
            addCell(row, item.complexity);
            caseTable.appendChild(row);
        });
    } catch (error) {
        caseCount.textContent = "Unavailable";
        caseTable.innerHTML =
            '<tr><td colspan="5">Unable to load legal cases.</td></tr>';
        console.error("Case loading failed:", error);
    }
}

// APPOINTMENT DROPDOWNS: show names, submit IDs
function populateAppointmentDropdowns() {
    const clientSelect = $("appointmentClientId");
    const lawyerSelect = $("appointmentLawyerId");
    if (!clientSelect || !lawyerSelect) return;

    const selectedClient = clientSelect.value;
    const selectedLawyer = lawyerSelect.value;

    clientSelect.replaceChildren(new Option("Client Name", ""));
    clientsCache.forEach(client => {
        clientSelect.add(new Option(
            `${client.fullName} (ID: ${client.clientId})`,
            client.clientId
        ));
    });

    lawyerSelect.replaceChildren(new Option("Lawyer Name", ""));
    lawyersCache.forEach(lawyer => {
        lawyerSelect.add(new Option(
            `${lawyer.fullName} (ID: ${lawyer.lawyerId})`,
            lawyer.lawyerId
        ));
    });

    if (selectedClient) clientSelect.value = selectedClient;
    if (selectedLawyer) lawyerSelect.value = selectedLawyer;
}

// APPOINTMENTS
async function loadAppointments() {
    if (!appointmentTable) return;

    if (appointmentMessage) {
        appointmentMessage.textContent = "Loading appointments...";
    }

    try {
        const appointments = await requestJSON(APPOINTMENT_API);
        appointmentTable.replaceChildren();

        if ($("appointmentCount")) {
            $("appointmentCount").textContent = appointments.length;
        }

        if (!appointments.length) {
            appointmentTable.innerHTML =
                '<tr><td colspan="8">No appointments found.</td></tr>';
        }

        appointments.forEach(appointment => {
            const row = document.createElement("tr");
            addCell(row, appointment.appointmentId);

            const client = clientsCache.find(
                item => Number(item.clientId) === Number(appointment.clientId)
            );
            const lawyer = lawyersCache.find(
                item => Number(item.lawyerId) === Number(appointment.lawyerId)
            );

            addCell(row, client?.fullName || `Client #${appointment.clientId}`);
            addCell(row, lawyer?.fullName || `Lawyer #${appointment.lawyerId}`);

            const date = appointment.appointmentDate;
            const time = appointment.timeSlot;

            addCell(row, formatDate(date));
            addCell(row, formatTime(time));
            addCell(row, appointment.mode);
            addCell(row, appointment.status);

            const actionCell = addCell(row, "");
            const statusSelect = document.createElement("select");
            statusSelect.className = "appointment-status-select";

            ["Confirmed", "Completed", "Cancelled"].forEach(status => {
                statusSelect.add(new Option(status, status));
            });

            statusSelect.value = appointment.status || "Confirmed";

            const updateButton = document.createElement("button");
            updateButton.type = "button";
            updateButton.textContent = "Update";

            updateButton.addEventListener("click", async () => {
                try {
                    await requestJSON(APPOINTMENT_API, {
                        method: "PUT",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            appointmentId: appointment.appointmentId,
                            status: statusSelect.value
                        })
                    });

                    appointmentMessage.textContent =
                        "Appointment status updated.";
                    await loadAppointments();
                } catch (error) {
                    appointmentMessage.textContent = error.message;
                }
            });

            actionCell.append(statusSelect, updateButton);
            appointmentTable.appendChild(row);
        });

        if (appointmentMessage) {
            appointmentMessage.textContent =
                "Appointment records loaded successfully.";
        }
    } catch (error) {
        appointmentTable.innerHTML =
            '<tr><td colspan="8">Unable to load appointments. Check the server console.</td></tr>';

        if (appointmentMessage) {
            appointmentMessage.textContent =
                "Unable to load appointments: " + error.message;
        }
        console.error("Appointment loading failed:", error);
    }
}

function formatDate(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleDateString();
}

function formatTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit"
        });
}

appointmentForm?.addEventListener("submit", async event => {
    event.preventDefault();

    const clientId = $("appointmentClientId").value;
    const lawyerId = $("appointmentLawyerId").value;
    const appointmentDate = $("appointmentDateTime").value;
    const mode = $("appointmentMode").value;

    if (!clientId || !lawyerId || !appointmentDate || !mode) {
        appointmentMessage.textContent =
            "Please complete every appointment field.";
        return;
    }

    try {
        await requestJSON(APPOINTMENT_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                clientId: Number(clientId),
                lawyerId: Number(lawyerId),
                appointmentDate,
                mode
            })
        });

        appointmentForm.reset();
        appointmentMessage.textContent = "Appointment booked successfully.";
        await loadAppointments();
    } catch (error) {
        appointmentMessage.textContent =
            "Booking failed: " + error.message;
    }
});

refreshButton?.addEventListener("click", async () => {
    await loadClients();
    await loadLawyers();
    await loadCases();
    await loadAppointments();
});

refreshAppointmentsButton?.addEventListener("click", loadAppointments);

async function refreshDashboard() {
    await Promise.all([
        loadClients(),
        loadLawyers(),
        loadCases()
    ]);
    await loadAppointments();
}

refreshDashboard();

const DOCUMENT_API = "/api/documents";

const documentTable = document.getElementById("documentTable");
const documentMessage = document.getElementById("documentMessage");
const documentForm = document.getElementById("documentForm");
const refreshDocumentsButton =
    document.getElementById("refreshDocumentsButton");

async function loadDocuments() {
    if (!documentTable) return;

    try {
        const response = await fetch(DOCUMENT_API);
        const documents = await response.json();

        if (!response.ok) {
            throw new Error(documents.error || "Unable to load documents");
        }

        documentTable.replaceChildren();

        if (documents.length === 0) {
            documentTable.innerHTML =
                '<tr><td colspan="6">No document records found.</td></tr>';
        }

        documents.forEach(documentRecord => {
            const row = document.createElement("tr");

            addCell(row, documentRecord.documentId);
            addCell(row, documentRecord.caseId);
            addCell(row, documentRecord.uploadedBy);
            addCell(row, documentRecord.category);
            addCell(row, documentRecord.uploadDate);

            const actionCell = addCell(row, "");
            const deleteButton = document.createElement("button");

            deleteButton.type = "button";
            deleteButton.textContent = "Delete";
            deleteButton.className = "delete-button";

            deleteButton.addEventListener("click", async () => {
                if (!confirm(
                    `Delete document record ${documentRecord.documentId}?`
                )) return;

                try {
                    const result = await fetch(
                        `${DOCUMENT_API}?id=${documentRecord.documentId}`,
                        { method: "DELETE" }
                    );

                    const data = await result.json();

                    if (!result.ok) {
                        throw new Error(
                            data.error || "Unable to delete document"
                        );
                    }

                    documentMessage.textContent =
                        "Document record deleted successfully.";

                    await loadDocuments();
                } catch (error) {
                    documentMessage.textContent = error.message;
                }
            });

            actionCell.appendChild(deleteButton);
            documentTable.appendChild(row);
        });

        documentMessage.textContent =
            "Document records loaded successfully.";
    } catch (error) {
        documentTable.innerHTML =
            '<tr><td colspan="6">Unable to load document records.</td></tr>';
        documentMessage.textContent =
            "Document loading failed: " + error.message;
        console.error("Document loading failed:", error);
    }
}

async function populateDocumentCases() {
    const caseSelect = document.getElementById("documentCaseId");
    if (!caseSelect) return;

    try {
        const response = await fetch("/api/cases");
        const cases = await response.json();

        if (!response.ok) {
            throw new Error("Unable to load legal cases");
        }

        caseSelect.replaceChildren(
            new Option("Select legal case", "")
        );

        cases.forEach(legalCase => {
            caseSelect.add(new Option(
                `Case #${legalCase.caseId} — ${legalCase.category || "General"}`,
                legalCase.caseId
            ));
        });
    } catch (error) {
        documentMessage.textContent =
            "Unable to load cases: " + error.message;
    }
}

documentForm?.addEventListener("submit", async event => {
    event.preventDefault();

    const caseId = document.getElementById("documentCaseId").value;
    const uploadedBy =
        document.getElementById("documentUploader").value.trim();
    const category =
        document.getElementById("documentCategory").value.trim();

    try {
        const response = await fetch(DOCUMENT_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                caseId: Number(caseId),
                uploadedBy,
                category
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || "Unable to register document");
        }

        documentForm.reset();
        documentMessage.textContent =
            "Document record registered successfully.";

        await loadDocuments();
    } catch (error) {
        documentMessage.textContent =
            "Registration failed: " + error.message;
    }
});

refreshDocumentsButton?.addEventListener("click", loadDocuments);

populateDocumentCases();
loadDocuments();


const NOTIFICATION_API = "/api/notifications";

const notificationTable = document.getElementById("notificationTable");
const notificationMessage = document.getElementById("notificationMessage");
const notificationForm = document.getElementById("notificationForm");

async function loadNotifications() {
    if (!notificationTable) return;

    try {
        const response = await fetch(NOTIFICATION_API);
        const notifications = await response.json();

        if (!response.ok) {
            throw new Error(
                notifications.error || "Unable to load notifications"
            );
        }

        notificationTable.replaceChildren();

        if (!notifications.length) {
            notificationTable.innerHTML =
                '<tr><td colspan="7">No notifications found.</td></tr>';
        }

        notifications.forEach(notification => {
            const row = document.createElement("tr");

            addCell(row, notification.notificationId);
            addCell(row, notification.recipientId);
            addCell(row, notification.recipientType);
            addCell(row, notification.message);
            addCell(row, notification.sentOn);

            const isRead = Number(notification.isRead) === 1;
            addCell(row, isRead ? "Read" : "Unread");

            const actionCell = addCell(row, "");

            if (!isRead) {
                const button = document.createElement("button");
                button.type = "button";
                button.textContent = "Mark as read";

                button.addEventListener("click", async () => {
                    try {
                        const result = await fetch(NOTIFICATION_API, {
                            method: "PUT",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify({
                                notificationId: notification.notificationId
                            })
                        });

                        const data = await result.json();

                        if (!result.ok) {
                            throw new Error(
                                data.error || "Unable to update notification"
                            );
                        }

                        notificationMessage.textContent =
                            "Notification marked as read.";
                        await loadNotifications();
                    } catch (error) {
                        notificationMessage.textContent = error.message;
                    }
                });

                actionCell.appendChild(button);
            } else {
                actionCell.textContent = "—";
            }

            notificationTable.appendChild(row);
        });

        notificationMessage.textContent =
            "Notification records loaded successfully.";
    } catch (error) {
        notificationTable.innerHTML =
            '<tr><td colspan="7">Unable to load notifications.</td></tr>';
        notificationMessage.textContent =
            "Notification error: " + error.message;
        console.error(error);
    }
}

notificationForm?.addEventListener("submit", async event => {
    event.preventDefault();

    const recipientId =
        document.getElementById("notificationRecipientId").value;
    const recipientType =
        document.getElementById("notificationRecipientType").value;
    const message =
        document.getElementById("notificationText").value.trim();

    try {
        const response = await fetch(NOTIFICATION_API, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                recipientId: Number(recipientId),
                recipientType,
                message
            })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || "Unable to create notification");
        }

        notificationForm.reset();
        notificationMessage.textContent =
            "Notification created successfully.";

        await loadNotifications();
    } catch (error) {
        notificationMessage.textContent =
            "Creation failed: " + error.message;
    }
});

document.getElementById("refreshNotificationsButton")
    ?.addEventListener("click", loadNotifications);

loadNotifications();

const BRANCH_API = "/api/branches";

async function loadBranches() {
    const table = document.getElementById("branchTable");
    const message = document.getElementById("branchMessage");

    if (!table) return;

    table.innerHTML =
        '<tr><td colspan="6">Loading branch workload...</td></tr>';

    try {
        const branches = await requestJSON(BRANCH_API);

        table.innerHTML = "";

        if (!branches.length) {
            table.innerHTML =
                '<tr><td colspan="6">No branch records found.</td></tr>';
            if (message) message.textContent = "";
            return;
        }

        branches.forEach(branch => {
            const row = document.createElement("tr");

            [
                branch.branchId,
                branch.branchName,
                branch.location,
                branch.maxCapacity,
                branch.lawyerCount,
                branch.activeCases
            ].forEach(value => addCell(row, value ?? "—"));

            table.appendChild(row);
        });

        if (message) {
            message.textContent =
                `Loaded workload for ${branches.length} branches.`;
        }
    } catch (error) {
        table.innerHTML =
            '<tr><td colspan="6">Unable to load branch workload.</td></tr>';

        if (message) {
            message.textContent = error.message;
        }

        console.error("Branch workload error:", error);
    }
}

document
    .getElementById("refreshBranchesButton")
    ?.addEventListener("click", loadBranches);

loadBranches();

const addLawyerForm = document.getElementById("addLawyerForm");

addLawyerForm?.addEventListener("submit", async function (event) {
    event.preventDefault();

    const message = document.getElementById("addLawyerMessage");
    const submitButton = addLawyerForm.querySelector(
        'button[type="submit"]'
    );

    const lawyer = {
        fullName: document.getElementById("newLawyerName").value.trim(),
        specialization: document.getElementById(
            "newLawyerSpecialization"
        ).value.trim(),
        experienceYears: Number(
            document.getElementById("newLawyerExperience").value
        ),
        consultationFee: Number(
            document.getElementById("newLawyerFee").value
        ),
        branchId: Number(
            document.getElementById("newLawyerBranch").value
        ),
        roleDesignation: document.getElementById("newLawyerRole").value,
        languages: document.getElementById("newLawyerLanguages").value.trim()
    };

    if (
        !lawyer.fullName ||
        !lawyer.specialization ||
        !lawyer.roleDesignation ||
        !Number.isInteger(lawyer.experienceYears) ||
        lawyer.experienceYears < 0 ||
        !Number.isInteger(lawyer.branchId) ||
        lawyer.branchId < 1 ||
        !Number.isFinite(lawyer.consultationFee) ||
        lawyer.consultationFee < 0
    ) {
        message.textContent = "Please enter valid lawyer details.";
        return;
    }

    submitButton.disabled = true;
    message.textContent = "Adding lawyer...";

    try {
        const response = await fetch(LAWYER_API, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(lawyer)
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(
                result.error || "Unable to add lawyer."
            );
        }

        message.textContent = "Lawyer added successfully.";
        addLawyerForm.reset();

        await loadLawyers();

        // Refresh appointment dropdowns if the existing helper is available.
        if (typeof populateAppointmentDropdowns === "function") {
            populateAppointmentDropdowns();
        }

    } catch (error) {
        message.textContent = error.message;
        console.error("Add lawyer error:", error);
    } finally {
        submitButton.disabled = false;
    }
});

