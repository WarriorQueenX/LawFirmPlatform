
package com.lawfirm.controller;

import com.lawfirm.dao.DocumentDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> documents =
                        DocumentDAO.getAllDocuments();

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < documents.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(toJson(documents.get(i)));
                }
                json.append("]");

                send(exchange, 200, json.toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> data = readJson(exchange);

                int caseId = Integer.parseInt(data.get("caseId"));
                String uploadedBy = data.get("uploadedBy");
                String category = data.get("category");

                DocumentDAO.addDocument(caseId, uploadedBy, category);

                send(exchange, 201,
                        "{\"message\":\"Document record created successfully\"}");
                return;
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                String query = exchange.getRequestURI().getQuery();
                String id = queryParameter(query, "id");

                if (id == null) {
                    send(exchange, 400,
                            "{\"error\":\"Document ID is required\"}");
                    return;
                }

                DocumentDAO.deleteDocument(Integer.parseInt(id));

                send(exchange, 200,
                        "{\"message\":\"Document record deleted successfully\"}");
                return;
            }

            send(exchange, 405, "{\"error\":\"Method not allowed\"}");

        } catch (IllegalArgumentException e) {
            send(exchange, 400, "{\"error\":\"Invalid document input\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Document operation failed\"}");
        }
    }

    private Map<String, String> readJson(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> values = new java.util.HashMap<>();

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

    private String queryParameter(String query, String name) {
        if (query == null) return null;

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) {
                return pair[1];
            }
        }

        return null;
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
