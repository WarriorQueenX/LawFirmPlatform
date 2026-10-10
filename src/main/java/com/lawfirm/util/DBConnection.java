
package com.lawfirm.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:oracle:thin:@localhost:1521:XE";

    private static final String USERNAME = "SYSTEM";

    private static final String PASSWORD = System.getenv("LAW_FIRM_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                URL, USERNAME, PASSWORD
        );
    }
}
