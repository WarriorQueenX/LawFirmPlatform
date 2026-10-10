
package com.lawfirm.controller;

import com.lawfirm.dao.ClientDAO;
import com.lawfirm.model.Client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );

        String method = exchange.getRequestMethod();

        if (method.equals("GET")) {
            getClients(exchange);
        } else if (method.equals("POST")) {
            addClient(exchange);
        } else {
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void getClients(HttpExchange exchange) throws IOException {
        try {
            List<Client> clients = ClientDAO.getAllClients();
            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < clients.size(); i++) {
                Client client = clients.get(i);
                if (i > 0) json.append(",");

                json.append("{")
                        .append("\"clientId\":").append(client.getClientId()).append(",")
                        .append("\"fullName\":\"").append(escapeJson(client.getFullName())).append("\",")
                        .append("\"email\":\"").append(escapeJson(client.getEmail())).append("\"")
                        .append("}");
            }

            json.append("]");
            send(exchange, 200, json.toString());

        } catch (RuntimeException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Unable to retrieve clients\"}");
        }
    }

    private void addClient(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream input = exchange.getRequestBody()) {
            body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        String name = extractJsonString(body, "fullName");
        String email = extractJsonString(body, "email");

        if (name == null || name.isBlank() ||
                email == null || email.isBlank()) {
            send(exchange, 400,
                    "{\"error\":\"Full name and email are required\"}");
            return;
        }

        try {
            ClientDAO.addClient(name.trim(), email.trim());
            send(exchange, 201, "{\"message\":\"Client created successfully\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Unable to create client\"}");
        }
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private void send(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
