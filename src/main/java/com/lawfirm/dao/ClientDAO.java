
package com.lawfirm.dao;

import com.lawfirm.model.Client;
import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public static void addClient(String fullName, String email)
            throws SQLException {

        String sql = """
            INSERT INTO CLIENT (CLIENT_ID, FULL_NAME, EMAIL)
            VALUES (NULL, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, email);

            pstmt.executeUpdate();
        }
    }

    public static void deleteClient(int clientId) throws SQLException {
        String sql = "DELETE FROM CLIENT WHERE CLIENT_ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);

            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted == 0) {
                throw new SQLException("Client not found");
            }
        }
    }

    public static void updateClient(int clientId, String fullName, String email)
            throws SQLException {

        String sql = """
        UPDATE CLIENT
        SET FULL_NAME = ?, EMAIL = ?
        WHERE CLIENT_ID = ?
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setInt(3, clientId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException("Client not found");
            }
        }
    }

    public static List<Client> getAllClients() {

        List<Client> clients = new ArrayList<>();

        String sql = "SELECT CLIENT_ID, FULL_NAME, EMAIL FROM CLIENT";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("CLIENT_ID"),
                        rs.getString("FULL_NAME"),
                        rs.getString("EMAIL")
                );

                clients.add(client);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve clients", e);
        }
        return clients;
    }
}
