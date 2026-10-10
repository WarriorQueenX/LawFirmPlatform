
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BranchDAO {

    public static List<Map<String, Object>> getBranchWorkload()
            throws SQLException {

        List<Map<String, Object>> branches = new ArrayList<>();

        String sql = """
            SELECT
                b.BRANCH_ID,
                b.BRANCH_NAME,
                b.LOCATION,
                b.MAX_CAPACITY,
                COUNT(DISTINCT l.LAWYER_ID) AS LAWYER_COUNT,
                COUNT(DISTINCT CASE
                    WHEN c.STATUS IN ('Open', 'In Progress', 'Pending')
                    THEN c.CASE_ID
                END) AS ACTIVE_CASES
            FROM BRANCH b
            LEFT JOIN LAWYER l
                ON b.BRANCH_ID = l.BRANCH_ID
            LEFT JOIN LEGAL_CASE c
                ON c.LEAD_PARTNER_ID = l.LAWYER_ID
            GROUP BY
                b.BRANCH_ID,
                b.BRANCH_NAME,
                b.LOCATION,
                b.MAX_CAPACITY
            ORDER BY b.BRANCH_ID
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> branch = new LinkedHashMap<>();

                branch.put("branchId", rs.getInt("BRANCH_ID"));
                branch.put("branchName", rs.getString("BRANCH_NAME"));
                branch.put("location", rs.getString("LOCATION"));
                branch.put("maxCapacity", rs.getInt("MAX_CAPACITY"));
                branch.put("lawyerCount", rs.getInt("LAWYER_COUNT"));
                branch.put("activeCases", rs.getInt("ACTIVE_CASES"));

                branches.add(branch);
            }
        }

        return branches;
    }
}
