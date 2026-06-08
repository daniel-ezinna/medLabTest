package com.medlabapp.dao;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /**
     * Common method to fetch any User record (including password hash) based on email address.
     * Use this during login for dynamic role-based screen navigation.
     * Returns null if no user is found with that email.
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),  
                        rs.getString("role"), // Will be SUPER_ADMIN, LAB_ATTENDANT, or CUSTOMER
                        rs.getBoolean("is_verified"),
                        rs.getBoolean("force_password_change")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Minimal error handle for high cohesion
            
        }
        return null; // Return null if not found
    }
    
    /**
     * Updates the user's password and removes the force_password_change flag.
     */
    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, force_password_change = false WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newPasswordHash);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
}