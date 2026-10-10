
package com.lawfirm.controller;

import com.lawfirm.dao.NotificationDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> notifications =
                        NotificationDAO.getAllNotifications();

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < notifications.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(toJson(notifications.get(i)));
                }
                json.append("]");

                send(exchange, 200, json.toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> data = readJson(exchange);

                int recipientId =
                        Integer.parseInt(data.get("recipientId"));
                String recipientType = data.get("recipientType");
                String message = data.get("message");

                NotificationDAO.addNotification(
                        recipientId, recipientType, message
                );

                send(exchange, 201,
                        "{\"message\":\"Notification created successfully\"}");
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Map<String, String> data = readJson(exchange);

                int notificationId =
                        Integer.parseInt(data.get("notificationId"));

                NotificationDAO.markAsRead(notificationId);

                send(exchange, 200,
                        "{\"message\":\"Notification marked as read\"}");
                return;
            }

            send(exchange, 405, "{\"error\":\"Method not allowed\"}");

        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"Invalid notification input\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Notification operation failed\"}");
        }
    }

    private Map<String, String> readJson(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> values = new HashMap<>();

        Pattern pattern = Pattern.compile(
                "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\s]+))"
        );
        Matcher matcher = pattern.matcher(body);

        while (matcher.find()) {
            String value = matcher.group(2) != null
                    ? matcher.group(2)
                    : matcher.group(3);

            values.put(matcher.group(1), value);
        }

        return values;
    }

    private String toJson(Map<String, Object> data) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (count++ > 0) json.append(",");

            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();

            if (value == null) {
                json.append("null");
            } else if (value instanceof Number
                    || value instanceof Boolean) {
                json.append(value);
            } else {
                String text = value.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");

                json.append("\"").append(text).append("\"");
            }
        }

        return json.append("}").toString();
    }

    private void send(
            HttpExchange exchange, int status, String response
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
