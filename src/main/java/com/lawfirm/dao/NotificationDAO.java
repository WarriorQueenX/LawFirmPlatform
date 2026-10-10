
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationDAO {

    public static List<Map<String, Object>> getAllNotifications()
            throws SQLException {

        List<Map<String, Object>> notifications = new ArrayList<>();

        String sql = """
            SELECT NOTIFICATION_ID, RECIPIENT_ID, RECIPIENT_TYPE,
                   MESSAGE, SENT_ON, IS_READ
            FROM NOTIFICATION
            ORDER BY NOTIFICATION_ID DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> notification = new LinkedHashMap<>();

                notification.put("notificationId",
                        rs.getInt("NOTIFICATION_ID"));
                notification.put("recipientId",
                        rs.getObject("RECIPIENT_ID"));
                notification.put("recipientType",
                        rs.getString("RECIPIENT_TYPE"));
                notification.put("message",
                        rs.getString("MESSAGE"));
                notification.put("sentOn",
                        rs.getTimestamp("SENT_ON"));
                notification.put("isRead",
                        rs.getObject("IS_READ"));

                notifications.add(notification);
            }
        }

        return notifications;
    }

    public static void addNotification(
            int recipientId,
            String recipientType,
            String message) throws SQLException {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message is required.");
        }

        if (!java.util.Set.of("Client", "Lawyer", "Admin")
                .contains(recipientType)) {
            throw new IllegalArgumentException(
                    "Recipient type must be Client, Lawyer, or Admin.");
        }

        String sql = """
            INSERT INTO NOTIFICATION
                (NOTIFICATION_ID, RECIPIENT_ID, RECIPIENT_TYPE,
                 MESSAGE, SENT_ON, IS_READ)
            VALUES
                ((SELECT NVL(MAX(NOTIFICATION_ID), 0) + 1
                  FROM NOTIFICATION),
                 ?, ?, ?, SYSDATE, 0)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, recipientId);
            stmt.setString(2, recipientType);
            stmt.setString(3, message.trim());

            stmt.executeUpdate();
        }
    }

    public static void markAsRead(int notificationId)
            throws SQLException {

        String sql = """
            UPDATE NOTIFICATION
            SET IS_READ = 1
            WHERE NOTIFICATION_ID = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Notification not found.");
            }
        }
    }
}
