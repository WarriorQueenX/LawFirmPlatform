package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
import com.lawfirm.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LawyerAccountDAO {

    public static void createLawyerLogin(
            int lawyerId,
            String username,
            String password
    ) throws SQLException {

        if (lawyerId <= 0) {
            throw new IllegalArgumentException("Invalid lawyer ID.");
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

            // Verify that the lawyer exists.
            try (PreparedStatement checkLawyer =
                         connection.prepareStatement(
                                 "SELECT LAWYER_ID FROM LAWYER " +
                                         "WHERE LAWYER_ID = ?")) {

                checkLawyer.setInt(1, lawyerId);

                try (ResultSet rs = checkLawyer.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                                "Lawyer does not exist."
                        );
                    }
                }
            }

            // Prevent duplicate accounts for the same lawyer.
            try (PreparedStatement checkAccount =
                         connection.prepareStatement(
                                 "SELECT USER_ID FROM APP_USER " +
                                         "WHERE LAWYER_ID = ?")) {

                checkAccount.setInt(1, lawyerId);

                try (ResultSet rs = checkAccount.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException(
                                "This lawyer already has a login account."
                        );
                    }
                }
            }

            // Create the lawyer login account.
            String sql =
                    "INSERT INTO APP_USER " +
                            "(USER_ID, USERNAME, PASSWORD_HASH, USER_ROLE, " +
                            "CLIENT_ID, LAWYER_ID, IS_ACTIVE) " +
                            "VALUES (APP_USER_SEQ.NEXTVAL, ?, ?, 'LAWYER', NULL, ?, 'Y')";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, cleanUsername);
                statement.setString(2, passwordHash);
                statement.setInt(3, lawyerId);

                statement.executeUpdate();
            }
        }
    }
}
