
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
import com.lawfirm.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthDAO {

    public static Map<String, Object> authenticate(
            String username, String password) throws SQLException {

        String sql = """
            SELECT USER_ID, USERNAME, PASSWORD_HASH,
                   USER_ROLE, CLIENT_ID, LAWYER_ID
            FROM APP_USER
            WHERE LOWER(USERNAME) = LOWER(?)
              AND IS_ACTIVE = 'Y'
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedHash = rs.getString("PASSWORD_HASH");

                if (!PasswordUtil.verifyPassword(password, storedHash)) {
                    return null;
                }

                Map<String, Object> user = new LinkedHashMap<>();
                user.put("userId", rs.getInt("USER_ID"));
                user.put("username", rs.getString("USERNAME"));
                user.put("role", rs.getString("USER_ROLE"));
                user.put("clientId", rs.getObject("CLIENT_ID"));
                user.put("lawyerId", rs.getObject("LAWYER_ID"));

                return user;
            }
        }
    }
}
