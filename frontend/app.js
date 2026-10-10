
const API_URL = "/api/clients";
const LAWYER_API = "/api/lawyers";
const CASE_API = "/api/cases";

const clientTable = document.getElementById("clientTable");
const clientCount = document.getElementById("clientCount");
const message = document.getElementById("message");
const refreshButton = document.getElementById("refreshButton");
const clientForm = document.getElementById("clientForm");

const lawyerTable = document.getElementById("lawyerTable");
const lawyerCount = document.getElementById("lawyerCount");
const caseTable = document.getElementById("caseTable");
const caseCount = document.getElementById("caseCount");

// Safely create a table cell
function addCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "—";
    row.appendChild(cell);
}

// Load clients
async function loadClients() {
    refreshButton.disabled = true;

    try {
        const response = await fetch(API_URL);
        if (!response.ok) throw new Error("Unable to load clients");

        const clients = await response.json();
        clientTable.replaceChildren();
        clientCount.textContent = clients.length;

        if (clients.length === 0) {
            clientTable.innerHTML =
                '<tr><td colspan="4">No clients found.</td></tr>';
        }

        for (const client of clients) {
            const row = document.createElement("tr");

            addCell(row, client.clientId);
            addCell(row, client.fullName);
            addCell(row, client.email);

            const actions = document.createElement("td");

            const editButton = document.createElement("button");
            editButton.textContent = "Edit";
            editButton.className = "edit-button";

            editButton.addEventListener("click", async () => {
                const fullName = prompt(
                    "Enter client name:",
                    client.fullName
                );
                if (fullName === null) return;

                const email = prompt(
                    "Enter client email:",
                    client.email
                );
                if (email === null) return;

                if (!fullName.trim() || !email.trim()) {
                    message.textContent = "Name and email are required.";
                    return;
                }

                try {
                    const result = await fetch(
                        `${API_URL}?id=${client.clientId}`,
                        {
                            method: "PUT",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({
                                fullName: fullName.trim(),
                                email: email.trim()
                            })
                        }
                    );

                    const data = await result.json();
                    if (!result.ok) {
                        throw new Error(data.error || "Update failed");
                    }

                    message.textContent = "Client updated successfully.";
                    await loadClients();
                } catch (error) {
                    message.textContent = error.message;
                }
            });

            const deleteButton = document.createElement("button");
            deleteButton.textContent = "Delete";
            deleteButton.className = "delete-button";

            deleteButton.addEventListener("click", async () => {
                if (!confirm(`Delete ${client.fullName}?`)) return;

                try {
                    const result = await fetch(
                        `${API_URL}?id=${client.clientId}`,
                        { method: "DELETE" }
                    );

                    const data = await result.json();
                    if (!result.ok) {
                        throw new Error(data.error || "Delete failed");
                    }

                    message.textContent = "Client deleted successfully.";
                    await loadClients();
                } catch (error) {
                    message.textContent = error.message;
                }
            });

            actions.append(editButton, deleteButton);
            row.appendChild(actions);
            clientTable.appendChild(row);
        }

        message.textContent = "Client records loaded successfully.";
    } catch (error) {
        message.textContent = "Unable to load clients: " + error.message;
        console.error(error);
    } finally {
        refreshButton.disabled = false;
    }
}

// Add a client
clientForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const fullName = document.getElementById("fullName").value.trim();
    const email = document.getElementById("email").value.trim();

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fullName, email })
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || "Unable to add client");
        }

        clientForm.reset();
        message.textContent = "Client added successfully.";
        await loadClients();
    } catch (error) {
        message.textContent = error.message;
    }
});

// Load lawyers
async function loadLawyers() {
    try {
        const response = await fetch(LAWYER_API);
        if (!response.ok) throw new Error("Unable to load lawyers");

        const lawyers = await response.json();
        lawyerTable.replaceChildren();
        lawyerCount.textContent = `${lawyers.length} lawyers`;

        if (lawyers.length === 0) {
            lawyerTable.innerHTML =
                '<tr><td colspan="5">No lawyers found.</td></tr>';
            return;
        }

        for (const lawyer of lawyers) {
            const row = document.createElement("tr");
            addCell(row, lawyer.lawyerId);
            addCell(row, lawyer.fullName);
            addCell(row, lawyer.specialization);

            addCell(
                row,
                lawyer.experienceYears == null
                    ? "—"
                    : `${lawyer.experienceYears} years`
            );

            addCell(
                row,
                lawyer.consultationFee == null
                    ? "—"
                    : `₹${lawyer.consultationFee}`
            );

            lawyerTable.appendChild(row);
        }
    } catch (error) {
        lawyerCount.textContent = "Unavailable";
        lawyerTable.innerHTML =
            '<tr><td colspan="5">Unable to load lawyers.</td></tr>';
        console.error(error);
    }
}

// Load legal cases
async function loadCases() {
    try {
        const response = await fetch(CASE_API);
        if (!response.ok) throw new Error("Unable to load cases");

        const cases = await response.json();
        caseTable.replaceChildren();
        caseCount.textContent = `${cases.length} cases`;

        if (cases.length === 0) {
            caseTable.innerHTML =
                '<tr><td colspan="5">No legal cases found.</td></tr>';
            return;
        }

        for (const legalCase of cases) {
            const row = document.createElement("tr");
            addCell(row, legalCase.caseId);
            addCell(row, legalCase.clientId);
            addCell(row, legalCase.category);
            addCell(row, legalCase.status);
            addCell(row, legalCase.complexity);
            caseTable.appendChild(row);
        }
    } catch (error) {
        caseCount.textContent = "Unavailable";
        caseTable.innerHTML =
            '<tr><td colspan="5">Unable to load legal cases.</td></tr>';
        console.error(error);
    }
}

// Refresh all dashboard data
async function refreshDashboard() {
    await Promise.all([
        loadClients(),
        loadLawyers(),
        loadCases()
    ]);
}

refreshButton.addEventListener("click", refreshDashboard);

// Initial dashboard load
refreshDashboard();
