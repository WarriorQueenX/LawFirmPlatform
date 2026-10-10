
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;
import java.sql.*;

public class ClientDAO {

    public static void getAllClients() {

        String sql = "SELECT * FROM CLIENT";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                System.out.println(
                        rs.getInt("CLIENT_ID") + " | " +
                                rs.getString("FULL_NAME") + " | " +
                                rs.getString("EMAIL")
                );
            }

        } catch (SQLException e) {
            System.out.println("Database operation failed!");
            e.printStackTrace();
        }
    }
}
