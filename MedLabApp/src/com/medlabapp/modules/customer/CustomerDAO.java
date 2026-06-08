package com.medlabapp.modules.customer;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.model.TestType;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for all customer-facing database operations.
 */
public class CustomerDAO {
 
    /**
     * Checks whether an email address already exists in the users table.
     * Used during registration to prevent duplicate accounts.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a new customer account into the users table.
     * is_verified defaults to FALSE — customer must verify email before logging in.
     * Returns the generated user ID so the verification email can reference it.
     */
    public int registerCustomer(String name, String email, String passwordHash) {
        String sql = "INSERT INTO users (name, email, password_hash, role, is_verified, force_password_change) " +
                     "VALUES (?, ?, ?, 'CUSTOMER', FALSE, FALSE) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, passwordHash);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Marks a customer's account as verified.
     * Called after successful email verification.
     */
    public boolean verifyCustomer(int userId) {
        String sql = "UPDATE users SET is_verified = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    
    /**
     * Fetches all available test types from the database.
     * Displayed in the Browse Tests tab.
     */
    public List<TestType> getAllTestTypes() {
        List<TestType> tests = new ArrayList<>();
        String sql = "SELECT * FROM test_types ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tests.add(new TestType(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    rs.getInt("tat_hours"),
                    rs.getString("result_format")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }
 
    /**
     * Inserts a new test request for the customer.
     * The deadline is computed by adding the test's TAT hours to the current time.
     * Also inserts a matching row in the samples table with status COLLECTED.
     */
    public boolean placeTestOrder(int customerId, int testTypeId, int tatHours) {
        String requestSql = "INSERT INTO test_requests (customer_id, test_type_id, payment_status, order_date, deadline_date) " +
                            "VALUES (?, ?, 'UNPAID', NOW(), NOW() + INTERVAL '" + tatHours + " hours') RETURNING id";
        String sampleSql = "INSERT INTO samples (test_request_id, status, is_verified) VALUES (?, 'COLLECTED', FALSE)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // use transaction so both inserts succeed or both fail

            try (PreparedStatement reqStmt = conn.prepareStatement(requestSql)) {
                reqStmt.setInt(1, customerId);
                reqStmt.setInt(2, testTypeId);
                ResultSet rs = reqStmt.executeQuery();

                if (rs.next()) {
                    int requestId = rs.getInt("id");

                    try (PreparedStatement sampleStmt = conn.prepareStatement(sampleSql)) {
                        sampleStmt.setInt(1, requestId);
                        sampleStmt.executeUpdate();
                    }

                    conn.commit();
                    return true;
                }
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    
    /**
     * Fetches all test requests for a specific customer joined with test type
     * and sample data so the dashboard can show status, deadline, and result info.
     * Each row: [requestId, testName, paymentStatus, sampleStatus, deadlineDate,
     *            isVerified, pdfPath, imagePath]
     */
    public List<String[]> getCustomerDashboard(int customerId) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT tr.id, tt.name AS test_name, tr.payment_status, " +
                     "s.status AS sample_status, tr.deadline_date, " +
                     "s.is_verified, s.pdf_report_path, s.image_report_path " +
                     "FROM test_requests tr " +
                     "JOIN test_types tt ON tr.test_type_id = tt.id " +
                     "LEFT JOIN samples s ON s.test_request_id = tr.id " +
                     "WHERE tr.customer_id = ? " +
                     "ORDER BY tr.order_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("test_name"),
                    rs.getString("payment_status"),
                    rs.getString("sample_status") != null ? rs.getString("sample_status") : "PENDING",
                    rs.getTimestamp("deadline_date") != null ? rs.getTimestamp("deadline_date").toString() : "",
                    String.valueOf(rs.getBoolean("is_verified")),
                    rs.getString("pdf_report_path") != null ? rs.getString("pdf_report_path") : "",
                    rs.getString("image_report_path") != null ? rs.getString("image_report_path") : ""
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rows;
    }
}