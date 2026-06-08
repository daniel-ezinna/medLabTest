
package com.medlabapp.modules.admin;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.model.TestType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

 
    public boolean createTestType(String name, double price, int tatHours, String format) {
        String sql = "INSERT INTO test_types (name, price, tat_hours, result_format) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, tatHours);
            pstmt.setString(4, format);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<TestType> getAllTestTypes() {
        List<TestType> tests = new ArrayList<>();
        String sql = "SELECT * FROM test_types ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                tests.add(new TestType(
                    rs.getInt("id"), rs.getString("name"), 
                    rs.getDouble("price"), rs.getInt("tat_hours"), 
                    rs.getString("result_format")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }

  
    // Note: Returning a generic List of Strings/Objects for the TableView for simplicity
    public List<String[]> getPendingRequests() {
        List<String[]> requests = new ArrayList<>();
        // Join with users and test_types to get readable names instead of just IDs
        String sql = "SELECT tr.id, u.name AS patient_name, tt.name AS test_name, tr.payment_status " +
                     "FROM test_requests tr " +
                     "JOIN users u ON tr.customer_id = u.id " +
                     "JOIN test_types tt ON tr.test_type_id = tt.id " +
                     "ORDER BY tr.order_date DESC";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                requests.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("patient_name"),
                    rs.getString("test_name"),
                    rs.getString("payment_status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public boolean markAsPaid(int requestId) {
        String sql = "UPDATE test_requests SET payment_status = 'PAID' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    
    public List<String[]> getAuditLogs() {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT al.timestamp, u.name, al.action_type, al.description " +
                     "FROM audit_log al " +
                     "LEFT JOIN users u ON al.user_id = u.id " +
                     "ORDER BY al.timestamp DESC";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String user = rs.getString("name") != null ? rs.getString("name") : "SYSTEM";
                logs.add(new String[]{
                    rs.getTimestamp("timestamp").toString(),
                    user,
                    rs.getString("action_type"),
                    rs.getString("description")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
 
    public boolean createUser(String name, String email, String passwordHash, String role, boolean forcePasswordChange, boolean isVerified) {
        String sql = "INSERT INTO users (name, email, password_hash, role, force_password_change, is_verified) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);
            pstmt.setString(4, role);
            pstmt.setBoolean(5, forcePasswordChange);
            pstmt.setBoolean(6, isVerified);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}