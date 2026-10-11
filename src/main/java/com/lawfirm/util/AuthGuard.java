package com.lawfirm.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthGuard implements HttpHandler {

    private final HttpHandler next;
    private final Set<String> allowedRoles;

    public AuthGuard(HttpHandler next, String... allowedRoles) {
        this.next = next;
        this.allowedRoles = new HashSet<>(
                Arrays.asList(allowedRoles));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set(
                        "Allow", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String token = getSessionToken(exchange);
            SessionManager.Session session =
                    SessionManager.getSession(token);

            if (session == null) {
                send(exchange, 401,
                        "{\"error\":\"Authentication required\"}");
                return;
            }

            if (!allowedRoles.contains(session.role)) {
                send(exchange, 403,
                        "{\"error\":\"You do not have permission to access this resource\"}");
                return;
            }

            next.handle(exchange);

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

    private void send(
            HttpExchange exchange,
            int status,
            String response
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
