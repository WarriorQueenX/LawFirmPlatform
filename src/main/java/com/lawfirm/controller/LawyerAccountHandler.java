package com.lawfirm.controller;

import com.lawfirm.dao.LawyerAccountDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LawyerAccountHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "POST");
            send(exchange, 405,
                    "{\"error\":\"Method not allowed\"}");
            return;
        }

        try (InputStream input = exchange.getRequestBody()) {
            String body = new String(
                    input.readAllBytes(), StandardCharsets.UTF_8
            );

            Integer lawyerId = extractJsonInteger(body, "lawyerId");
            String username = extractJsonString(body, "username");
            String password = extractJsonString(body, "password");

            if (lawyerId == null || username == null || password == null) {
                send(exchange, 400,
                        "{\"error\":\"Lawyer ID, username and password are required\"}");
                return;
            }

            LawyerAccountDAO.createLawyerLogin(
                    lawyerId, username, password
            );

            send(exchange, 201,
                    "{\"message\":\"Lawyer login created successfully\"}");

        } catch (IllegalArgumentException e) {
            send(exchange, 400,
                    "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Unable to create lawyer login. Check server logs.\"}");
        }
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) +
                        "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private Integer extractJsonInteger(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)"
        ).matcher(json);

        if (!matcher.find()) return null;

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void send(
            HttpExchange exchange, int status, String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
