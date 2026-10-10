
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

    public static int addLawyer(
            String fullName,
            String specialization,
            int experienceYears,
            int branchId,
            double consultationFee,
            String languages,
            String roleDesignation
    ) throws SQLException {

        String sql = """
            INSERT INTO LAWYER (
                FULL_NAME,
                SPECIALIZATION,
                EXPERIENCE_YEARS,
                BRANCH_ID,
                CONSULTATION_FEE,
                LANGUAGES,
                ROLE_DESIGNATION
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, specialization);
            stmt.setInt(3, experienceYears);
            stmt.setInt(4, branchId);
            stmt.setDouble(5, consultationFee);
            stmt.setString(6, languages);
            stmt.setString(7, roleDesignation);

            return stmt.executeUpdate();
        }
    }
}
