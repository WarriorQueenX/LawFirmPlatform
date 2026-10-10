
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.*;

public class AppointmentDAO {

    public static List<Map<String, Object>> getAllAppointments()
            throws SQLException {

        List<Map<String, Object>> appointments = new ArrayList<>();

        String sql = """
            SELECT APPOINTMENT_ID, CLIENT_ID, LAWYER_ID,
                   APPOINTMENT_DATE, TIME_SLOT,
                   APPOINTMENT_MODE, STATUS
            FROM APPOINTMENT
            ORDER BY APPOINTMENT_DATE, TIME_SLOT
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("appointmentId", rs.getInt("APPOINTMENT_ID"));
                a.put("clientId", rs.getObject("CLIENT_ID"));
                a.put("lawyerId", rs.getObject("LAWYER_ID"));
                a.put("appointmentDate", rs.getDate("APPOINTMENT_DATE"));
                a.put("timeSlot", rs.getTimestamp("TIME_SLOT"));
                a.put("mode", rs.getString("APPOINTMENT_MODE"));
                a.put("status", rs.getString("STATUS"));
                appointments.add(a);
            }
        }

        return appointments;
    }

    public static void createAppointment(
            int clientId,
            int lawyerId,
            Timestamp appointmentDate,
            Timestamp timeSlot,
            String mode) throws SQLException {

        if (!"Physical".equals(mode) && !"Online".equals(mode)) {
            throw new IllegalArgumentException(
                    "Mode must be Physical or Online.");
        }

        String sql = """
            INSERT INTO APPOINTMENT
                (APPOINTMENT_ID, CLIENT_ID, LAWYER_ID,
                 APPOINTMENT_DATE, TIME_SLOT, APPOINTMENT_MODE, STATUS)
            VALUES
                ((SELECT NVL(MAX(APPOINTMENT_ID), 0) + 1
                  FROM APPOINTMENT),
                 ?, ?, ?, ?, ?, 'Confirmed')
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clientId);
            stmt.setInt(2, lawyerId);
            stmt.setTimestamp(3, appointmentDate);
            stmt.setTimestamp(4, timeSlot);
            stmt.setString(5, mode);

            stmt.executeUpdate();
        }
    }

    public static void updateStatus(int appointmentId, String status)
            throws SQLException {

        if (!Set.of("Confirmed", "Completed", "Cancelled").contains(status)) {
            throw new IllegalArgumentException("Invalid appointment status.");
        }

        String sql = """
            UPDATE APPOINTMENT
            SET STATUS = ?
            WHERE APPOINTMENT_ID = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, appointmentId);

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Appointment not found.");
            }
        }
    }
}
