
package com.lawfirm.controller;

import com.lawfirm.dao.CaseDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            switch (exchange.getRequestMethod()) {
                case "GET" -> getCases(exchange);
                case "POST" -> addCase(exchange);
                case "PUT" -> updateCase(exchange);
                case "DELETE" -> deleteCase(exchange);
                default -> send(exchange, 405,
                        "{\"error\":\"Method not allowed\"}");
            }
        } catch (IllegalArgumentException e) {
            send(exchange, 400,
                    "{\"error\":\"Invalid case data. Check all fields.\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Case database operation failed. Check server logs and database constraints.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Internal server error\"}");
        } finally {
            exchange.close();
        }
    }

    private void getCases(HttpExchange exchange)
            throws SQLException, IOException {

        List<Map<String, Object>> cases = CaseDAO.getAllCases();
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < cases.size(); i++) {
            if (i > 0) json.append(",");

            Map<String, Object> c = cases.get(i);

            json.append("{")
                    .append("\"caseId\":").append(number(c.get("caseId"))).append(",")
                    .append("\"clientId\":").append(number(c.get("clientId"))).append(",")
                    .append("\"leadPartnerId\":").append(number(c.get("leadPartnerId"))).append(",")
                    .append("\"category\":\"").append(escape(c.get("category"))).append("\",")
                    .append("\"budget\":").append(number(c.get("budget"))).append(",")
                    .append("\"status\":\"").append(escape(c.get("status"))).append("\",")
                    .append("\"complexity\":\"").append(escape(c.get("complexity"))).append("\"")
                    .append("}");
        }

        json.append("]");
        send(exchange, 200, json.toString());
    }

    private void addCase(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);

        int clientId = requiredInt(body, "clientId");
        Integer leadPartnerId = optionalInt(body, "leadPartnerId");
        String category = requiredString(body, "category");
        BigDecimal budget = requiredDecimal(body, "budget");
        String status = requiredString(body, "status");
        String complexity = requiredString(body, "complexity");

        validate(clientId, category, budget, status, complexity);

        CaseDAO.addCase(
                clientId, leadPartnerId, category,
                budget, status, complexity
        );

        send(exchange, 201,
                "{\"message\":\"Legal case created successfully\"}");
    }

    private void updateCase(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);

        int caseId = requiredInt(body, "caseId");
        int clientId = requiredInt(body, "clientId");
        Integer leadPartnerId = optionalInt(body, "leadPartnerId");
        String category = requiredString(body, "category");
        BigDecimal budget = requiredDecimal(body, "budget");
        String status = requiredString(body, "status");
        String complexity = requiredString(body, "complexity");

        validate(clientId, category, budget, status, complexity);

        boolean updated = CaseDAO.updateCase(
                caseId, clientId, leadPartnerId, category,
                budget, status, complexity
        );

        if (!updated) {
            send(exchange, 404, "{\"error\":\"Case not found\"}");
            return;
        }

        send(exchange, 200,
                "{\"message\":\"Legal case updated successfully\"}");
    }

    private void deleteCase(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);
        int caseId = requiredInt(body, "caseId");

        boolean deleted = CaseDAO.deleteCase(caseId);

        if (!deleted) {
            send(exchange, 404, "{\"error\":\"Case not found\"}");
            return;
        }

        send(exchange, 200,
                "{\"message\":\"Legal case deleted successfully\"}");
    }

    private void validate(
            int clientId,
            String category,
            BigDecimal budget,
            String status,
            String complexity) {

        if (clientId <= 0 || category.isBlank() || budget.signum() < 0) {
            throw new IllegalArgumentException();
        }

        if (!status.equals("Open")
                && !status.equals("In Progress")
                && !status.equals("Closed")) {
            throw new IllegalArgumentException();
        }

        if (!complexity.equals("Low")
                && !complexity.equals("Medium")
                && !complexity.equals("High")
                && !complexity.equals("Critical")) {
            throw new IllegalArgumentException();
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private String requiredString(String body, String key) {
        String value = findString(body, key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException();
        }

        return value.trim();
    }

    private int requiredInt(String body, String key) {
        String value = findNumber(body, key);

        if (value == null) {
            throw new IllegalArgumentException();
        }

        return Integer.parseInt(value);
    }

    private Integer optionalInt(String body, String key) {
        String pattern = "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(null|-?\\d+)";

        Matcher matcher = Pattern.compile(pattern).matcher(body);

        if (!matcher.find() || matcher.group(1).equals("null")) {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
    }

    private BigDecimal requiredDecimal(String body, String key) {
        String value = findNumber(body, key);

        if (value == null) {
            throw new IllegalArgumentException();
        }

        BigDecimal result = new BigDecimal(value);

        if (result.signum() < 0) {
            throw new IllegalArgumentException();
        }

        return result;
    }

    private String findString(String body, String key) {
        String pattern = "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"";

        Matcher matcher = Pattern.compile(pattern).matcher(body);

        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    private String findNumber(String body, String key) {
        String pattern = "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)";

        Matcher matcher = Pattern.compile(pattern).matcher(body);

        return matcher.find() ? matcher.group(1) : null;
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

    private String number(Object value) {
        return value == null ? "null" : value.toString();
    }

    private void send(
            HttpExchange exchange,
            int status,
            String body) throws IOException {

        byte[] data = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }
}
