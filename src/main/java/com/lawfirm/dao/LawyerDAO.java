
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.*;

public class LawyerDAO {



    public List<Map<String, Object>> getAllLawyers() throws SQLException {
        List<Map<String, Object>> lawyers = new ArrayList<>();

        String sql = "SELECT LAWYER_ID, FULL_NAME, SPECIALIZATION, " +
                "EXPERIENCE_YEARS, CONSULTATION_FEE, BRANCH_ID, " +
                "LANGUAGES, ROLE_DESIGNATION FROM LAWYER " +
                "ORDER BY LAWYER_ID";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> lawyer = new LinkedHashMap<>();

                lawyer.put("lawyerId", rs.getInt("LAWYER_ID"));
                lawyer.put("fullName", rs.getString("FULL_NAME"));
                lawyer.put("specialization", rs.getString("SPECIALIZATION"));
                lawyer.put("experienceYears", rs.getInt("EXPERIENCE_YEARS"));
                lawyer.put("consultationFee", rs.getBigDecimal("CONSULTATION_FEE"));
                lawyer.put("branchId", rs.getInt("BRANCH_ID"));
                lawyer.put("languages", rs.getString("LANGUAGES"));
                lawyer.put("roleDesignation", rs.getString("ROLE_DESIGNATION"));

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
    public static int updateLawyer(
            int lawyerId,
            String fullName,
            String specialization,
            int experienceYears,
            int branchId,
            double consultationFee,
            String languages,
            String roleDesignation
    ) throws SQLException {

        String sql = """
        UPDATE LAWYER
        SET FULL_NAME = ?,
            SPECIALIZATION = ?,
            EXPERIENCE_YEARS = ?,
            BRANCH_ID = ?,
            CONSULTATION_FEE = ?,
            LANGUAGES = ?,
            ROLE_DESIGNATION = ?
        WHERE LAWYER_ID = ?
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
            stmt.setInt(8, lawyerId);

            return stmt.executeUpdate();
        }
    }
    public static int deleteLawyer(int lawyerId) throws SQLException {
        String sql = "DELETE FROM LAWYER WHERE LAWYER_ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, lawyerId);
            return stmt.executeUpdate();
        }
    }

}
