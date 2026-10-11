
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class CaseDAO {

    public static List<Map<String, Object>> getAllCases()
            throws SQLException {

        List<Map<String, Object>> cases = new ArrayList<>();

        String sql = """
            SELECT CASE_ID, CLIENT_ID, LEAD_PARTNER_ID,
                   CATEGORY, BUDGET, STATUS, COMPLEXITY
            FROM LEGAL_CASE
            ORDER BY CASE_ID
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> legalCase = new LinkedHashMap<>();

                legalCase.put("caseId", rs.getInt("CASE_ID"));
                legalCase.put("clientId", rs.getInt("CLIENT_ID"));
                legalCase.put("leadPartnerId",
                        rs.getObject("LEAD_PARTNER_ID"));
                legalCase.put("category", rs.getString("CATEGORY"));
                legalCase.put("budget", rs.getBigDecimal("BUDGET"));
                legalCase.put("status", rs.getString("STATUS"));
                legalCase.put("complexity",
                        rs.getString("COMPLEXITY"));

                cases.add(legalCase);
            }
        }

        return cases;
    }

    public static void addCase(
            int clientId,
            Integer leadPartnerId,
            String category,
            BigDecimal budget,
            String status,
            String complexity) throws SQLException {

        String sql = """
            INSERT INTO LEGAL_CASE
                (CASE_ID, CLIENT_ID, LEAD_PARTNER_ID,
                 CATEGORY, BUDGET, STATUS, COMPLEXITY)
            VALUES
                ((SELECT NVL(MAX(CASE_ID), 0) + 1 FROM LEGAL_CASE),
                 ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);

            if (leadPartnerId == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, leadPartnerId);
            }

            stmt.setString(3, category);
            stmt.setBigDecimal(4, budget);
            stmt.setString(5, status);
            stmt.setString(6, complexity);

            stmt.executeUpdate();
        }
    }

    public static boolean updateCase(
            int caseId,
            int clientId,
            Integer leadPartnerId,
            String category,
            BigDecimal budget,
            String status,
            String complexity) throws SQLException {

        String sql = """
            UPDATE LEGAL_CASE
            SET CLIENT_ID = ?,
                LEAD_PARTNER_ID = ?,
                CATEGORY = ?,
                BUDGET = ?,
                STATUS = ?,
                COMPLEXITY = ?
            WHERE CASE_ID = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);

            if (leadPartnerId == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, leadPartnerId);
            }

            stmt.setString(3, category);
            stmt.setBigDecimal(4, budget);
            stmt.setString(5, status);
            stmt.setString(6, complexity);
            stmt.setInt(7, caseId);

            return stmt.executeUpdate() > 0;
        }
    }

    public static boolean deleteCase(int caseId)
            throws SQLException {

        String sql = "DELETE FROM LEGAL_CASE WHERE CASE_ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, caseId);
            return stmt.executeUpdate() > 0;
        }
    }
}
