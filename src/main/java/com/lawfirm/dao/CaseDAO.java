
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
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
}
