
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
import java.sql.*;
import java.util.*;

public class LawyerDAO {

    public static List<Map<String, Object>> getAllLawyers()
            throws SQLException {

        List<Map<String, Object>> lawyers = new ArrayList<>();

        String sql = """
            SELECT LAWYER_ID, FULL_NAME, SPECIALIZATION,
                   EXPERIENCE_YEARS, CONSULTATION_FEE
            FROM LAWYER
            ORDER BY LAWYER_ID
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> lawyer = new LinkedHashMap<>();

                lawyer.put("lawyerId", rs.getInt("LAWYER_ID"));
                lawyer.put("fullName", rs.getString("FULL_NAME"));
                lawyer.put("specialization",
                        rs.getString("SPECIALIZATION"));
                lawyer.put("experienceYears",
                        rs.getObject("EXPERIENCE_YEARS"));
                lawyer.put("consultationFee",
                        rs.getBigDecimal("CONSULTATION_FEE"));

                lawyers.add(lawyer);
            }
        }

        return lawyers;
    }
}
