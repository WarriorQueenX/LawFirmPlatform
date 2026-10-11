package com.lawfirm;

import com.lawfirm.util.DBConnection;
import com.lawfirm.util.PasswordUtil;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

public class CreateAdmin {

    public static void main(String[] args) throws Exception {
        Console console = System.console();

        if (console == null) {
            System.out.println(
                    "Run this class in a terminal so the password can be hidden."
            );
            return;
        }

        String username = console.readLine("Admin username: ").trim();

        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }

        char[] password = console.readPassword("Admin password: ");
        char[] confirmation =
                console.readPassword("Confirm password: ");

        try {
            if (password.length < 12) {
                System.out.println(
                        "Use a password with at least 12 characters."
                );
                return;
            }

            if (!Arrays.equals(password, confirmation)) {
                System.out.println("Passwords do not match.");
                return;
            }

            String passwordHash =
                    PasswordUtil.hashPassword(new String(password));

            try (Connection connection = DBConnection.getConnection()) {
                connection.setAutoCommit(false);

                try {
                    try (PreparedStatement check = connection.prepareStatement(
                            "SELECT COUNT(*) FROM APP_USER "
                                    + "WHERE LOWER(USERNAME) = LOWER(?)")) {

                        check.setString(1, username);

                        try (ResultSet rs = check.executeQuery()) {
                            rs.next();

                            if (rs.getInt(1) > 0) {
                                throw new IllegalArgumentException(
                                        "That username already exists."
                                );
                            }
                        }
                    }

                    String sql =
                            "INSERT INTO APP_USER "
                                    + "(USER_ID, USERNAME, PASSWORD_HASH, "
                                    + "USER_ROLE, IS_ACTIVE) "
                                    + "VALUES (APP_USER_SEQ.NEXTVAL, ?, ?, 'ADMIN', 'Y')";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(sql)) {

                        statement.setString(1, username);
                        statement.setString(2, passwordHash);
                        statement.executeUpdate();
                    }

                    connection.commit();
                    System.out.println(
                            "Admin account created successfully."
                    );

                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }

        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
