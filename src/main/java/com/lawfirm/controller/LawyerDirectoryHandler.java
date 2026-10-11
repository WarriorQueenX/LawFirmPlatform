
package com.lawfirm.controller;

import com.lawfirm.dao.LawyerDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class LawyerDirectoryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<Map<String, Object>> lawyers =
                    new LawyerDAO().getAllLawyers();

            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < lawyers.size(); i++) {
                if (i > 0) json.append(",");

                Map<String, Object> lawyer = lawyers.get(i);
                json.append("{")
                        .append("\"lawyerId\":")
                        .append(lawyer.get("lawyerId"))
                        .append(",\"fullName\":\"")
                        .append(escape(lawyer.get("fullName")))
                        .append("\"}");
            }

            json.append("]");
            send(exchange, 200, json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Unable to load lawyers\"}");
        } finally {
            exchange.close();
        }
    }

    private String escape(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void send(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
