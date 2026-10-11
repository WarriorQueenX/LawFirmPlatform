
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
import com.lawfirm.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientAccountDAO {

    public static void createClientLogin(
            int clientId,
            String username,
            String password
    ) throws SQLException {

        if (clientId <= 0) {
            throw new IllegalArgumentException("Invalid client ID.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters."
            );
        }

        String cleanUsername = username.trim();

        if (cleanUsername.length() > 100) {
            throw new IllegalArgumentException(
                    "Username must not exceed 100 characters."
            );
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        try (Connection connection = DBConnection.getConnection()) {

            // Verify that the client exists.
            try (PreparedStatement checkClient = connection.prepareStatement(
                    "SELECT CLIENT_ID FROM CLIENT WHERE CLIENT_ID = ?")) {

                checkClient.setInt(1, clientId);

                try (ResultSet rs = checkClient.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Client does not exist."
                        );
                    }
                }
            }

            // Prevent a client from receiving duplicate login accounts.
            try (PreparedStatement checkAccount = connection.prepareStatement(
                    "SELECT USER_ID FROM APP_USER WHERE CLIENT_ID = ?")) {

                checkAccount.setInt(1, clientId);

                try (ResultSet rs = checkAccount.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException(
                                "This client already has a login account."
                        );
                    }
                }
            }

            // Create the client login account.
            String sql =
                    "INSERT INTO APP_USER " +
                            "(USER_ID, USERNAME, PASSWORD_HASH, USER_ROLE, " +
                            "CLIENT_ID, LAWYER_ID, IS_ACTIVE) " +
                            "VALUES (APP_USER_SEQ.NEXTVAL, ?, ?, 'CLIENT', ?, NULL, 'Y')";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, cleanUsername);
                statement.setString(2, passwordHash);
                statement.setInt(3, clientId);

                statement.executeUpdate();
            }
        }
    }
}
