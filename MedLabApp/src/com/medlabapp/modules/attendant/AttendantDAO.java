package com.medlabapp.modules.attendant;

import com.medlabapp.config.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendantDAO {
    
    public List<String[]> getAllRequests() {
    List<String[]> rows = new ArrayList<>();

    String sql = "SELECT tr.id        AS req_id, "
               + "       u.name       AS patient_name, "
               + "       tt.name      AS test_name, "
               + "       tr.payment_status, "
               + "       s.status     AS sample_status, "
               + "       s.id         AS sample_id "
               + "FROM   test_requests tr "
               + "JOIN   users         u  ON tr.customer_id  = u.id "
               + "JOIN   test_types    tt ON tr.test_type_id = tt.id "
               + "LEFT JOIN samples    s  ON s.test_request_id = tr.id "
               + "ORDER BY tr.order_date DESC";

    try (Connection conn = DatabaseConnection.getConnection();
         Statement stmt  = conn.createStatement();
         ResultSet rs    = stmt.executeQuery(sql)) {

        while (rs.next()) {
            rows.add(new String[]{
                String.valueOf(rs.getInt("req_id")),
                rs.getString("patient_name"),
                rs.getString("test_name"),
                rs.getString("payment_status"),
                rs.getString("sample_status") != null ? rs.getString("sample_status") : "—",
                rs.getString("sample_id")     != null ? String.valueOf(rs.getInt("sample_id")) : "—"
            });
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return rows;
    }
    
    public boolean markAsPaid(int requestId) {
        String sql = "UPDATE test_requests SET payment_status = 'PAID' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public int createSample(int requestId) {
        String check = "SELECT id FROM samples WHERE test_request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        String sql = "INSERT INTO samples (test_request_id, status) VALUES (?, 'COLLECTED') RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public boolean updateSampleStatus(int sampleId, String newStatus) {
        String sql = "UPDATE samples SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, sampleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean attachFiles(int sampleId, String pdfPath, String imagePath) {
        StringBuilder sql = new StringBuilder("UPDATE samples SET updated_at = CURRENT_TIMESTAMP");
        if (pdfPath != null) {
            sql.append(", pdf_report_path = ?");
        }
        if (imagePath != null) {
            sql.append(", image_report_path = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (pdfPath != null) {
                pstmt.setString(idx++, pdfPath);
            }
            if (imagePath != null) {
                pstmt.setString(idx++, imagePath);
            }
            pstmt.setInt(idx, sampleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean setVerified(int sampleId, boolean verified) {
        String sql = "UPDATE samples SET is_verified = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, verified);
            pstmt.setInt(2, sampleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String[] getSampleDetail(int sampleId) {
        String sql = "SELECT id, status, pdf_report_path, image_report_path, is_verified "
                + "FROM samples WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sampleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("status"),
                        rs.getString("pdf_report_path") != null ? rs.getString("pdf_report_path") : "",
                        rs.getString("image_report_path") != null ? rs.getString("image_report_path") : "",
                        String.valueOf(rs.getBoolean("is_verified"))
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createCustomerAccount(String name, String email, String bcryptHash) {
        String sql = "INSERT INTO users (name, email, password_hash, role, is_verified, force_password_change) "
                + "VALUES (?, ?, ?, 'CUSTOMER', TRUE, TRUE)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, bcryptHash);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("createCustomerAccount error: " + e.getMessage());
            return false;
        }
    }    
    public String[] getPatientDetailsForSample(int sampleId) {
        String sql = "SELECT u.name, u.email, tt.name AS test_name "
                + "FROM samples s "
                + "JOIN test_requests tr ON s.test_request_id = tr.id "
                + "JOIN users u ON tr.customer_id = u.id "
                + "JOIN test_types tt ON tr.test_type_id = tt.id "
                + "WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sampleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("test_name")
                    };
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
