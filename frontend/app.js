
const API_URL = "http://localhost:8082/api/clients";

const clientTable = document.getElementById("clientTable");
const clientCount = document.getElementById("clientCount");
const message = document.getElementById("message");
const refreshButton = document.getElementById("refreshButton");

async function loadClients() {
    message.textContent = "Loading clients from Oracle…";
    refreshButton.disabled = true;

    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error(`Server returned HTTP ${response.status}`);
        }

        const clients = await response.json();

        clientTable.replaceChildren();
        clientCount.textContent = clients.length;

        if (clients.length === 0) {
            clientTable.innerHTML =
                '<tr><td colspan="4">No clients found.</td></tr>';
        } else {
            for (const client of clients) {
                const row = document.createElement("tr");

                const idCell = document.createElement("td");
                idCell.textContent = client.clientId;

                const nameCell = document.createElement("td");
                nameCell.textContent = client.fullName ?? "";

                const emailCell = document.createElement("td");
                emailCell.textContent = client.email ?? "";

                const statusCell = document.createElement("td");
                const status = document.createElement("span");
                status.className = "status";
                status.textContent = "Registered";
                statusCell.appendChild(status);

                row.append(idCell, nameCell, emailCell, statusCell);
                clientTable.appendChild(row);
            }
        }

        message.textContent = "Client records loaded successfully.";
    } catch (error) {
        message.textContent =
            "Could not load clients. Ensure the Java server is running on port 8082.";
        clientTable.innerHTML =
            '<tr><td colspan="4">Unable to load client records.</td></tr>';

        console.error(error);
    } finally {
        refreshButton.disabled = false;
    }
}

refreshButton.addEventListener("click", loadClients);

loadClients();

const clientForm = document.getElementById("clientForm");

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

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.error || "Unable to add client");
        }

        clientForm.reset();
        message.textContent = "Client added successfully.";
        await loadClients();

    } catch (error) {
        message.textContent = error.message;
        console.error(error);
    }
});
