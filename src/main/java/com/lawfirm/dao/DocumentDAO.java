
package com.lawfirm.dao;

import com.lawfirm.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocumentDAO {

    public static List<Map<String, Object>> getAllDocuments()
            throws SQLException {

        List<Map<String, Object>> documents = new ArrayList<>();

        String sql = """
            SELECT DOCUMENT_ID, CASE_ID, UPLOADED_BY,
                   DOC_CATEGORY, UPLOAD_DATE
            FROM DOCUMENT
            ORDER BY DOCUMENT_ID
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> document = new LinkedHashMap<>();

                document.put("documentId", rs.getInt("DOCUMENT_ID"));
                document.put("caseId", rs.getObject("CASE_ID"));
                document.put("uploadedBy", rs.getString("UPLOADED_BY"));
                document.put("category", rs.getString("DOC_CATEGORY"));
                document.put("uploadDate", rs.getDate("UPLOAD_DATE"));

                documents.add(document);
            }
        }

        return documents;
    }

    public static void addDocument(
            int caseId,
            String uploadedBy,
            String category) throws SQLException {

        if (uploadedBy == null || uploadedBy.isBlank()
                || category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Uploader and document category are required.");
        }

        String sql = """
            INSERT INTO DOCUMENT
                (DOCUMENT_ID, CASE_ID, UPLOADED_BY,
                 DOC_CATEGORY, UPLOAD_DATE)
            VALUES
                ((SELECT NVL(MAX(DOCUMENT_ID), 0) + 1 FROM DOCUMENT),
                 ?, ?, ?, SYSDATE)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, caseId);
            stmt.setString(2, uploadedBy.trim());
            stmt.setString(3, category.trim());

            stmt.executeUpdate();
        }
    }

    public static void deleteDocument(int documentId)
            throws SQLException {

        String sql =
                "DELETE FROM DOCUMENT WHERE DOCUMENT_ID = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, documentId);

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Document not found.");
            }
        }
    }
}
