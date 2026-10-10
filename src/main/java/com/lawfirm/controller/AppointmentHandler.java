
package com.lawfirm.controller;

import com.lawfirm.dao.AppointmentDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AppointmentHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                send(exchange, 204, "");
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> appointments =
                        AppointmentDAO.getAllAppointments();

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < appointments.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(toJson(appointments.get(i)));
                }
                json.append("]");

                send(exchange, 200, json.toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> data = parseBody(exchange);

                int clientId = Integer.parseInt(data.get("clientId"));
                int lawyerId = Integer.parseInt(data.get("lawyerId"));
                LocalDateTime dateTime =
                        LocalDateTime.parse(data.get("appointmentDate"));
                String mode = data.get("mode");

                AppointmentDAO.createAppointment(
                        clientId,
                        lawyerId,
                        Timestamp.valueOf(dateTime.toLocalDate().atStartOfDay()),
                        Timestamp.valueOf(dateTime),
                        mode
                );

                send(exchange, 201,
                        "{\"message\":\"Appointment created successfully\"}");
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Map<String, String> data = parseBody(exchange);

                int appointmentId =
                        Integer.parseInt(data.get("appointmentId"));
                String status = data.get("status");

                AppointmentDAO.updateStatus(appointmentId, status);

                send(exchange, 200,
                        "{\"message\":\"Appointment status updated\"}");
                return;
            }

            send(exchange, 405, "{\"error\":\"Method not allowed\"}");

        } catch (IllegalArgumentException e) {
            send(exchange, 400,
                    "{\"error\":\"Invalid appointment input\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Appointment operation failed\"}");
        }
    }

    private Map<String, String> parseBody(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        return new java.util.AbstractMap<String, String>() {
            private final Map<String, String> values = parseJson(body);

            @Override
            public String get(Object key) {
                return values.get(key);
            }

            @Override
            public java.util.Set<Entry<String, String>> entrySet() {
                return values.entrySet();
            }
        };
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> values = new java.util.HashMap<>();

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\s]+))"
                ).matcher(json);

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
            } else {
                String text = value.toString()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                json.append("\"").append(text).append("\"");
            }
        }

        return json.append("}").toString();
    }

    private void send(HttpExchange exchange, int status, String response)
            throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS"
        );
        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type"
        );

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
