package com.lawfirm.controller;

import com.lawfirm.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SessionHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, POST, OPTIONS");
                send(exchange, 204, "");
                return;
            }

            String token = getSessionToken(exchange);

            if ("GET".equalsIgnoreCase(method)) {
                SessionManager.Session session =
                        SessionManager.getSession(token);

                if (session == null) {
                    send(exchange, 401,
                            "{\"error\":\"Not authenticated\"}");
                    return;
                }

                String json = "{"
                        + "\"username\":\"" + escape(session.username) + "\","
                        + "\"role\":\"" + escape(session.role) + "\","
                        + "\"clientId\":" + (session.clientId == null
                        ? "null" : session.clientId) + ","
                        + "\"lawyerId\":" + (session.lawyerId == null
                        ? "null" : session.lawyerId)
                        + "}";

                send(exchange, 200, json);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                SessionManager.invalidateSession(token);

                exchange.getResponseHeaders().add(
                        "Set-Cookie",
                        "LEXORA_SESSION=; Path=/; HttpOnly; "
                                + "SameSite=Strict; Max-Age=0");

                send(exchange, 200,
                        "{\"message\":\"Logged out successfully\"}");
                return;
            }

            exchange.getResponseHeaders().set(
                    "Allow", "GET, POST, OPTIONS");

            send(exchange, 405,
                    "{\"error\":\"Method not allowed\"}");

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Session operation failed\"}");
        } finally {
            exchange.close();
        }
    }

    private String getSessionToken(HttpExchange exchange) {
        String cookieHeader =
                exchange.getRequestHeaders().getFirst("Cookie");

        if (cookieHeader == null) {
            return null;
        }

        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);

            if (parts.length == 2
                    && "LEXORA_SESSION".equals(parts[0])) {
                return parts[1];
            }
        }

        return null;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
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
                "Content-Type", "application/json; charset=UTF-8");

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
