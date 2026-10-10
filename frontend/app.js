
const API_URL = "/api/clients";

const clientTable = document.getElementById("clientTable");
const clientCount = document.getElementById("clientCount");
const message = document.getElementById("message");
const refreshButton = document.getElementById("refreshButton");
const clientForm = document.getElementById("clientForm");

async function loadClients() {
    message.textContent = "Loading clients...";
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

            const idCell = document.createElement("td");
            idCell.textContent = client.clientId;

            const nameCell = document.createElement("td");
            nameCell.textContent = client.fullName ?? "";

            const emailCell = document.createElement("td");
            emailCell.textContent = client.email ?? "";

            const actionCell = document.createElement("td");

            // Edit button
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
                    message.textContent =
                        "Name and email cannot be empty.";
                    return;
                }

                try {
                    const updateResponse = await fetch(
                        `${API_URL}?id=${client.clientId}`,
                        {
                            method: "PUT",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify({
                                fullName: fullName.trim(),
                                email: email.trim()
                            })
                        }
                    );

                    const result = await updateResponse.json();

                    if (!updateResponse.ok) {
                        throw new Error(
                            result.error || "Unable to update client"
                        );
                    }

                    message.textContent =
                        "Client updated successfully.";

                    await loadClients();

                } catch (error) {
                    message.textContent = error.message;
                }
            });

            // Delete button
            const deleteButton = document.createElement("button");
            deleteButton.textContent = "Delete";
            deleteButton.className = "delete-button";

            deleteButton.addEventListener("click", async () => {
                if (!confirm(`Delete ${client.fullName}?`)) return;

                try {
                    const deleteResponse = await fetch(
                        `${API_URL}?id=${client.clientId}`,
                        { method: "DELETE" }
                    );

                    const result = await deleteResponse.json();

                    if (!deleteResponse.ok) {
                        throw new Error(
                            result.error || "Unable to delete client"
                        );
                    }

                    message.textContent =
                        "Client deleted successfully.";

                    await loadClients();

                } catch (error) {
                    message.textContent = error.message;
                }
            });

            actionCell.append(editButton, deleteButton);
            row.append(idCell, nameCell, emailCell, actionCell);
            clientTable.appendChild(row);
        }

        message.textContent = "Client records loaded successfully.";

    } catch (error) {
        message.textContent =
            "Could not load clients. Check the Java server and database.";
        console.error(error);

    } finally {
        refreshButton.disabled = false;
    }
}

// Add client
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
    }
});

// Refresh button
refreshButton.addEventListener("click", loadClients);

// Initial load
loadClients();
