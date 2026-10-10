
package com.lawfirm.controller;

import com.lawfirm.dao.BranchDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BranchHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            List<Map<String, Object>> branches =
                    BranchDAO.getBranchWorkload();

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < branches.size(); i++) {
                Map<String, Object> branch = branches.get(i);

                if (i > 0) {
                    json.append(",");
                }

                json.append("{")
                        .append("\"branchId\":").append(branch.get("branchId")).append(",")
                        .append("\"branchName\":").append(quote(branch.get("branchName"))).append(",")
                        .append("\"location\":").append(quote(branch.get("location"))).append(",")
                        .append("\"maxCapacity\":").append(branch.get("maxCapacity")).append(",")
                        .append("\"lawyerCount\":").append(branch.get("lawyerCount")).append(",")
                        .append("\"activeCases\":").append(branch.get("activeCases"))
                        .append("}");
            }
            json.append("]");

            send(exchange, 200, json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500, "{\"error\":\"Database error while loading branch workload\"}");
        } finally {
            exchange.close();
        }
    }

    private String quote(Object value) {
        if (value == null) {
            return "null";
        }

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
