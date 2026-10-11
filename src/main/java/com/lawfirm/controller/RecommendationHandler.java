
package com.lawfirm.controller;

import com.lawfirm.dao.RecommendationDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RecommendationHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405,
                        "{\"error\":\"Method not allowed\"}");
                return;
            }

            String query = exchange.getRequestURI().getRawQuery();
            String category = getParameter(query, "category");
            String caseIdText = getParameter(query, "caseId");

            if (category == null || category.isBlank()) {
                send(exchange, 400,
                        "{\"error\":\"A case category is required\"}");
                return;
            }

            Integer caseId = null;
            if (caseIdText != null && !caseIdText.isBlank()) {
                try {
                    caseId = Integer.parseInt(caseIdText);
                } catch (NumberFormatException e) {
                    send(exchange, 400,
                            "{\"error\":\"Invalid caseId\"}");
                    return;
                }
            }

            List<Map<String, Object>> recommendations =
                    RecommendationDAO.recommendLawyers(category, caseId);

            send(exchange, 200, toJson(recommendations));

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Unable to generate recommendations\"}");
        } finally {
            exchange.close();
        }
    }

    private static String getParameter(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            String name = URLDecoder.decode(
                    pair[0], StandardCharsets.UTF_8);

            if (key.equals(name)) {
                return pair.length > 1
                        ? URLDecoder.decode(
                        pair[1], StandardCharsets.UTF_8)
                        : "";
            }
        }
        return null;
    }

    private static String toJson(List<Map<String, Object>> rows) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Map<String, Object> row : rows) {
            if (!first) json.append(",");
            first = false;

            json.append("{");
            boolean firstField = true;

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (!firstField) json.append(",");
                firstField = false;

                json.append("\"")
                        .append(escape(entry.getKey()))
                        .append("\":");

                Object value = entry.getValue();

                if (value == null) {
                    json.append("null");
                } else if (value instanceof Number
                        || value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"")
                            .append(escape(value.toString()))
                            .append("\"");
                }
            }

            json.append("}");
        }

        return json.append("]").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void send(
            HttpExchange exchange, int status, String response)
            throws IOException {

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
