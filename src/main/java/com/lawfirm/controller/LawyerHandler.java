
package com.lawfirm.controller;


import com.lawfirm.dao.LawyerDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class LawyerHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<Map<String, Object>> lawyers = LawyerDAO.getAllLawyers();
            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < lawyers.size(); i++) {
                if (i > 0) json.append(",");

                Map<String, Object> lawyer = lawyers.get(i);

                json.append("{")
                        .append("\"lawyerId\":").append(lawyer.get("lawyerId")).append(",")
                        .append("\"fullName\":\"").append(escape(lawyer.get("fullName"))).append("\",")
                        .append("\"specialization\":\"").append(escape(lawyer.get("specialization"))).append("\",")
                        .append("\"experienceYears\":").append(number(lawyer.get("experienceYears"))).append(",")
                        .append("\"consultationFee\":").append(number(lawyer.get("consultationFee")))
                        .append("}");
            }

            json.append("]");
            send(exchange, 200, json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Unable to retrieve lawyers\"}");
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
                .replace("\r", "\\r");
    }

    private String number(Object value) {
        return value == null ? "null" : value.toString();
    }

    private void send(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }
}
