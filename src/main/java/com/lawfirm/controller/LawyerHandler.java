
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

            switch (method) {
                case "GET":
                    getLawyers(exchange);
                    break;
                case "POST":
                    addLawyer(exchange);
                    break;
                case "PUT":
                    updateLawyer(exchange);
                    break;
                case "DELETE":
                    deleteLawyer(exchange);
                    break;
                default:
                    send(exchange, 405,
                            "{\"error\":\"Method not allowed\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Database operation failed\"}");
        } catch (IllegalArgumentException e) {
            send(exchange, 400,
                    "{\"error\":\"Invalid request data\"}");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Internal server error\"}");
        } finally {
            exchange.close();
        }
    }

    private void getLawyers(HttpExchange exchange)
            throws SQLException, IOException {

        LawyerDAO lawyerDAO = new LawyerDAO();
        List<Map<String, Object>> lawyers = lawyerDAO.getAllLawyers();
        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < lawyers.size(); i++) {
            if (i > 0) {
                json.append(",");
            }

            Map<String, Object> lawyer = lawyers.get(i);

            json.append("{")
                    .append("\"lawyerId\":")
                    .append(number(lawyer.get("lawyerId"))).append(",")
                    .append("\"fullName\":\"")
                    .append(escape(lawyer.get("fullName"))).append("\",")
                    .append("\"specialization\":\"")
                    .append(escape(lawyer.get("specialization"))).append("\",")
                    .append("\"experienceYears\":")
                    .append(number(lawyer.get("experienceYears"))).append(",")
                    .append("\"consultationFee\":")
                    .append(number(lawyer.get("consultationFee"))).append(",")
                    .append("\"branchId\":")
                    .append(number(lawyer.get("branchId"))).append(",")
                    .append("\"languages\":\"")
                    .append(escape(lawyer.get("languages"))).append("\",")
                    .append("\"roleDesignation\":\"")
                    .append(escape(lawyer.get("roleDesignation"))).append("\"")
                    .append("}");
        }

        json.append("]");
        send(exchange, 200, json.toString());
    }

    private void addLawyer(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);

        String fullName = requiredString(body, "fullName");
        String specialization = requiredString(body, "specialization");
        int experienceYears = requiredInt(body, "experienceYears");
        int branchId = requiredInt(body, "branchId");
        double consultationFee = requiredDouble(body, "consultationFee");
        String languages = requiredString(body, "languages");
        String roleDesignation = requiredString(body, "roleDesignation");

        validateLawyer(experienceYears, consultationFee, roleDesignation);

        LawyerDAO.addLawyer(
                fullName,
                specialization,
                experienceYears,
                branchId,
                consultationFee,
                languages,
                roleDesignation
        );

        send(exchange, 201,
                "{\"message\":\"Lawyer added successfully\"}");
    }

    private void updateLawyer(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);

        int lawyerId = requiredInt(body, "lawyerId");
        String fullName = requiredString(body, "fullName");
        String specialization = requiredString(body, "specialization");
        int experienceYears = requiredInt(body, "experienceYears");
        int branchId = requiredInt(body, "branchId");
        double consultationFee = requiredDouble(body, "consultationFee");
        String languages = requiredString(body, "languages");
        String roleDesignation = requiredString(body, "roleDesignation");

        validateLawyer(experienceYears, consultationFee, roleDesignation);

        LawyerDAO.updateLawyer(
                lawyerId,
                fullName,
                specialization,
                experienceYears,
                branchId,
                consultationFee,
                languages,
                roleDesignation
        );

        send(exchange, 200,
                "{\"message\":\"Lawyer updated successfully\"}");
    }

    private void deleteLawyer(HttpExchange exchange)
            throws IOException, SQLException {

        String body = readBody(exchange);
        int lawyerId = requiredInt(body, "lawyerId");

        LawyerDAO.deleteLawyer(lawyerId);

        send(exchange, 200,
                "{\"message\":\"Lawyer deleted successfully\"}");
    }

    private void validateLawyer(
            int experienceYears,
            double consultationFee,
            String roleDesignation) {

        if (experienceYears < 0 || consultationFee < 0) {
            throw new IllegalArgumentException("Invalid numeric value");
        }

        if (!roleDesignation.equals("Partner")
                && !roleDesignation.equals("Associate")
                && !roleDesignation.equals("Paralegal")
                && !roleDesignation.equals("Intern")) {
            throw new IllegalArgumentException("Invalid role designation");
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

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + key);
        }

        return value.trim();
    }

    private int requiredInt(String body, String key) {
        String value = findNumber(body, key);

        if (value == null) {
            throw new IllegalArgumentException("Missing " + key);
        }

        return Integer.parseInt(value);
    }

    private double requiredDouble(String body, String key) {
        String value = findNumber(body, key);

        if (value == null) {
            throw new IllegalArgumentException("Missing " + key);
        }

        return Double.parseDouble(value);
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
        if (value == null) {
            return "";
        }

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
