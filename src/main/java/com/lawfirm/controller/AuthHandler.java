package com.lawfirm.controller;

import com.lawfirm.dao.AuthDAO;
import com.lawfirm.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set(
                        "Allow", "POST, OPTIONS");
                send(exchange, 204, "");
                return;
            }

            if (!"POST".equalsIgnoreCase(
                    exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set(
                        "Allow", "POST, OPTIONS");
                send(exchange, 405,
                        "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);

            String username = getJsonString(body, "username");
            String password = getJsonString(body, "password");

            if (username == null || username.isBlank()
                    || password == null || password.isEmpty()) {
                send(exchange, 400,
                        "{\"error\":\"Username and password are required\"}");
                return;
            }

            Map<String, Object> user =
                    AuthDAO.authenticate(username, password);

            if (user == null) {
                send(exchange, 401,
                        "{\"error\":\"Invalid username or password\"}");
                return;
            }

            // Create a server-side session.
            String token = SessionManager.createSession(user);

            // Send the session token as an HttpOnly cookie.
            exchange.getResponseHeaders().add(
                    "Set-Cookie",
                    "LEXORA_SESSION=" + token
                            + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800");

            // Do not expose internal account IDs or session tokens
            // unnecessarily in the login response.
            Map<String, Object> response = new HashMap<>();
            response.put("username", user.get("username"));
            response.put("role", user.get("role"));
            response.put("message", "Login successful");

            send(exchange, 200, toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Authentication failed\"}");
        } finally {
            exchange.close();
        }
    }

    private String getJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                        + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private String toJson(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\":\"")
                    .append(escapeJson(
                            String.valueOf(entry.getValue())))
                    .append("\"");
        }

        return json.append("}").toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void send(
            HttpExchange exchange,
            int status,
            String response
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(
                status, status == 204 ? -1 : bytes.length);

        if (status != 204) {
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        }
    }
}
