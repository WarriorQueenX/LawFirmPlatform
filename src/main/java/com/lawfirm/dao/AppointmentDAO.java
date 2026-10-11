
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

        String conflictSql = """
        SELECT COUNT(*)
        FROM APPOINTMENT
        WHERE LAWYER_ID = ?
          AND APPOINTMENT_DATE = ?
          AND TIME_SLOT = ?
          AND STATUS = 'Confirmed'
        """;

        String insertSql = """
        INSERT INTO APPOINTMENT
            (APPOINTMENT_ID, CLIENT_ID, LAWYER_ID,
             APPOINTMENT_DATE, TIME_SLOT, APPOINTMENT_MODE, STATUS)
        VALUES
            ((SELECT NVL(MAX(APPOINTMENT_ID), 0) + 1
              FROM APPOINTMENT),
             ?, ?, ?, ?, ?, 'Confirmed')
        """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Check whether this lawyer already has a confirmed
                // appointment for the requested date and time.
                try (PreparedStatement check =
                             conn.prepareStatement(conflictSql)) {

                    check.setInt(1, lawyerId);
                    check.setTimestamp(2, appointmentDate);
                    check.setTimestamp(3, timeSlot);

                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new IllegalArgumentException(
                                    "This lawyer already has a confirmed " +
                                            "appointment at the selected date and time.");
                        }
                    }
                }

                // Insert only when no conflict exists.
                try (PreparedStatement stmt =
                             conn.prepareStatement(insertSql)) {

                    stmt.setInt(1, clientId);
                    stmt.setInt(2, lawyerId);
                    stmt.setTimestamp(3, appointmentDate);
                    stmt.setTimestamp(4, timeSlot);
                    stmt.setString(5, mode);

                    stmt.executeUpdate();
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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

    public static List<Map<String, Object>> getAppointmentsForClient(int clientId)
            throws SQLException {
        return getAppointmentsByOwner("CLIENT_ID", clientId);
    }

    public static List<Map<String, Object>> getAppointmentsForLawyer(int lawyerId)
            throws SQLException {
        return getAppointmentsByOwner("LAWYER_ID", lawyerId);
    }

    private static List<Map<String, Object>> getAppointmentsByOwner(
            String column, int ownerId) throws SQLException {

        String sql = """
        SELECT APPOINTMENT_ID, CLIENT_ID, LAWYER_ID, APPOINTMENT_DATE, TIME_SLOT, APPOINTMENT_MODE, STATUS
        FROM APPOINTMENT
        WHERE %s = ?
        ORDER BY APPOINTMENT_DATE, TIME_SLOT""".formatted(column);
        // The column is selected only by the two fixed methods above.
        if (!"CLIENT_ID".equals(column) && !"LAWYER_ID".equals(column)) {
            throw new IllegalArgumentException("Invalid appointment owner.");
        }

        List<Map<String, Object>> appointments = new ArrayList<>();



        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ownerId);

            try (ResultSet rs = stmt.executeQuery()) {
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
        }

        return appointments;
    }

}
