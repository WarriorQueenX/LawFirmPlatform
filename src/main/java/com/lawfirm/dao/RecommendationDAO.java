
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecommendationDAO {

    public static List<Map<String, Object>> recommendLawyers(
            String category, Integer caseId) throws SQLException {

        List<Map<String, Object>> recommendations = new ArrayList<>();


        String sql =
                "SELECT l.LAWYER_ID, l.FULL_NAME, l.SPECIALIZATION, " +
                        "l.EXPERIENCE_YEARS, l.CONSULTATION_FEE, " +
                        "l.BRANCH_ID, b.BRANCH_NAME, " +

                        "(SELECT COUNT(DISTINCT c.CASE_ID) " +
                        " FROM LEGAL_CASE c " +
                        " WHERE c.LEAD_PARTNER_ID = l.LAWYER_ID " +
                        " AND c.STATUS IN ('Open', 'In Progress') " +
                        " AND (? IS NULL OR c.CASE_ID <> ?)) " +

                        "+ " +

                        "(SELECT COUNT(DISTINCT ct.CASE_ID) " +
                        " FROM CASE_TEAM ct " +
                        " JOIN LEGAL_CASE c2 ON c2.CASE_ID = ct.CASE_ID " +
                        " WHERE ct.LAWYER_ID = l.LAWYER_ID " +
                        " AND c2.STATUS IN ('Open', 'In Progress') " +
                        " AND (? IS NULL OR c2.CASE_ID <> ?) " +
                        " AND NOT EXISTS (" +
                        "   SELECT 1 FROM LEGAL_CASE c3 " +
                        "   WHERE c3.CASE_ID = ct.CASE_ID " +
                        "   AND c3.LEAD_PARTNER_ID = l.LAWYER_ID" +
                        " )) AS ACTIVE_CASES, " +

                        "(SELECT COUNT(*) FROM LAWYER lb " +
                        " WHERE lb.BRANCH_ID = l.BRANCH_ID) AS BRANCH_LAWYERS, " +

                        "NVL(b.MAX_CAPACITY, 0) AS MAX_CAPACITY " +

                        "FROM LAWYER l " +
                        "LEFT JOIN BRANCH b ON b.BRANCH_ID = l.BRANCH_ID";


        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setObject(1, caseId);
            statement.setObject(2, caseId);
            statement.setObject(3, caseId);
            statement.setObject(4, caseId);


            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> lawyer =
                            new LinkedHashMap<>();

                    int experience = rs.getInt("EXPERIENCE_YEARS");
                    int activeCases = rs.getInt("ACTIVE_CASES");
                    int branchLawyers = rs.getInt("BRANCH_LAWYERS");
                    int maxCapacity = rs.getInt("MAX_CAPACITY");

                    String specialization =
                            rs.getString("SPECIALIZATION");

                    boolean specializationMatch =
                            specialization != null &&
                                    specialization.toLowerCase()
                                            .contains(category.trim().toLowerCase());

                    boolean atCapacity =
                            maxCapacity > 0 &&
                                    branchLawyers >= maxCapacity;

                    int score = 0;

                    if (specializationMatch) {
                        score += 50;
                    }

                    score += Math.min(experience, 20);

                    score += Math.max(0, 30 - activeCases * 5);

                    if (!atCapacity) {
                        score += 10;
                    }

                    lawyer.put("lawyerId", rs.getInt("LAWYER_ID"));
                    lawyer.put("fullName", rs.getString("FULL_NAME"));
                    lawyer.put("specialization", specialization);
                    lawyer.put("experienceYears", experience);
                    lawyer.put("consultationFee",
                            rs.getBigDecimal("CONSULTATION_FEE"));
                    lawyer.put("branchId",
                            rs.getObject("BRANCH_ID"));
                    lawyer.put("branchName", rs.getString("BRANCH_NAME"));
                    lawyer.put("activeCases", activeCases);
                    lawyer.put("branchLawyers", branchLawyers);
                    lawyer.put("maxCapacity", maxCapacity);
                    lawyer.put("specializationMatch",
                            specializationMatch);
                    lawyer.put("branchAtCapacity", atCapacity);
                    lawyer.put("recommendationScore", score);

                    List<String> reasons = new ArrayList<>();

                    if (specializationMatch) {
                        reasons.add("Matches case specialization");
                    }

                    if (experience >= 5) {
                        reasons.add("At least 5 years of experience");
                    }

                    if (activeCases <= 2) {
                        reasons.add("Relatively low active caseload");
                    }

                    if (!atCapacity) {
                        reasons.add("Branch is not at recorded capacity");
                    }

                    if (reasons.isEmpty()) {
                        reasons.add("Consider for partner review");
                    }

                    lawyer.put("reasons", reasons);
                    recommendations.add(lawyer);
                }

                recommendations.sort((a, b) -> {
                    int scoreA = ((Number) a.get("recommendationScore")).intValue();
                    int scoreB = ((Number) b.get("recommendationScore")).intValue();

                    int scoreComparison = Integer.compare(scoreB, scoreA);
                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    int workloadA = ((Number) a.get("activeCases")).intValue();
                    int workloadB = ((Number) b.get("activeCases")).intValue();

                    int workloadComparison = Integer.compare(workloadA, workloadB);
                    if (workloadComparison != 0) {
                        return workloadComparison;
                    }

                    return String.valueOf(a.get("fullName"))
                            .compareToIgnoreCase(String.valueOf(b.get("fullName")));
                });

            }
        }

        // The SQL prioritizes specialization, branch capacity,
        // workload and experience. This score explains the result;
        // it does not automatically assign a lawyer.
        return recommendations;
    }
}
