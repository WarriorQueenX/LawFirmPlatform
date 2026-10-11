
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
                    WHEN c.STATUS IN ('Open', 'In Progress')
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

                int branchId = rs.getInt("BRANCH_ID");
                int maxCapacity = rs.getInt("MAX_CAPACITY");
                int lawyerCount = rs.getInt("LAWYER_COUNT");
                int activeCases = rs.getInt("ACTIVE_CASES");

                double utilization = maxCapacity > 0
                        ? (lawyerCount * 100.0) / maxCapacity
                        : 0.0;

                String workloadStatus;

                if (maxCapacity <= 0) {
                    workloadStatus = "Capacity Not Set";
                } else if (lawyerCount >= maxCapacity) {
                    workloadStatus = "At Capacity";
                } else if (utilization >= 80.0) {
                    workloadStatus = "Near Capacity";
                } else {
                    workloadStatus = "Available";
                }

                branch.put("branchId", branchId);
                branch.put("branchName", rs.getString("BRANCH_NAME"));
                branch.put("location", rs.getString("LOCATION"));
                branch.put("maxCapacity", maxCapacity);
                branch.put("lawyerCount", lawyerCount);
                branch.put("activeCases", activeCases);
                branch.put("availableSlots",
                        Math.max(0, maxCapacity - lawyerCount));
                branch.put("capacityUtilization",
                        Math.round(utilization * 100.0) / 100.0);
                branch.put("workloadStatus", workloadStatus);

                branches.add(branch);
            }
        }

        return branches;
    }
}
