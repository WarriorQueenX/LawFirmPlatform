
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LawyerHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> lawyers =
                        LawyerDAO.getAllLawyers();

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < lawyers.size(); i++) {
                    if (i > 0) json.append(",");

                    Map<String, Object> lawyer = lawyers.get(i);
                    json.append("{")
                            .append("\"lawyerId\":").append(lawyer.get("lawyerId")).append(",")
                            .append("\"fullName\":").append(quote(lawyer.get("fullName"))).append(",")
                            .append("\"specialization\":").append(quote(lawyer.get("specialization"))).append(",")
                            .append("\"experienceYears\":").append(number(lawyer.get("experienceYears"))).append(",")
                            .append("\"consultationFee\":").append(number(lawyer.get("consultationFee")))
                            .append("}");
                }
                json.append("]");

                send(exchange, 200, json.toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                String body = new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                String fullName = getString(body, "fullName");
                String specialization = getString(body, "specialization");
                String languages = getString(body, "languages");
                String role = getString(body, "roleDesignation");

                int experience = getInt(body, "experienceYears");
                int branchId = getInt(body, "branchId");
                double fee = getDouble(body, "consultationFee");

                if (fullName == null || fullName.isBlank()
                        || specialization == null || specialization.isBlank()
                        || role == null || languages == null
                        || experience < 0 || branchId < 1 || fee < 0
                        || !List.of("Partner", "Associate", "Paralegal", "Intern")
                        .contains(role)) {
                    send(exchange, 400,
                            "{\"error\":\"Invalid or missing lawyer details\"}");
                    return;
                }

                int rows = LawyerDAO.addLawyer(
                        fullName.trim(),
                        specialization.trim(),
                        experience,
                        branchId,
                        fee,
                        languages.trim(),
                        role
                );

                if (rows > 0) {
                    send(exchange, 201,
                            "{\"message\":\"Lawyer added successfully\"}");
                } else {
                    send(exchange, 500,
                            "{\"error\":\"Lawyer was not added\"}");
                }
                return;
            }

            exchange.getResponseHeaders().set("Allow", "GET, POST");
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");

        } catch (IllegalArgumentException e) {
            send(exchange, 400,
                    "{\"error\":\"Invalid request data\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Database error. Check branch ID and database constraints.\"}");
        } finally {
            exchange.close();
        }
    }

    private String getString(String body, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );
        Matcher matcher = pattern.matcher(body);

        if (!matcher.find()) return null;

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    private int getInt(String body, String key) {
        String value = getNumberString(body, key);
        if (value == null) throw new IllegalArgumentException();
        return Integer.parseInt(value);
    }

    private double getDouble(String body, String key) {
        String value = getNumberString(body, key);
        if (value == null) throw new IllegalArgumentException();
        return Double.parseDouble(value);
    }

    private String getNumberString(String body, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)"
        );
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String number(Object value) {
        return value == null ? "null" : value.toString();
    }

    private String quote(Object value) {
        if (value == null) return "null";

        String text = value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        return "\"" + text + "\"";
    }

    private void send(HttpExchange exchange, int status, String response)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
