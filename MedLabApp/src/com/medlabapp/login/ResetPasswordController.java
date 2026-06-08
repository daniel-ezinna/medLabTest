package com.medlabapp.login;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.dao.AuditDAO;
import com.medlabapp.dao.UserDAO;
import com.medlabapp.model.User;
import com.medlabapp.security.SecurityUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
 
    private String email;

    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    /**
     * Called by ForgotPasswordController after loading this screen.
     * Injects the email so we can look up the correct user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #DC3545;");
        messageLabel.setText("");

        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

       
        if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        if (newPassword.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

 
        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            messageLabel.setText("Invalid session. Please start the reset process again.");
            return;
        }

        ValidationResult result = validateCode(user.getId(), code);

        switch (result) {
            case INVALID -> {
                messageLabel.setText("Invalid reset code. Please check your email and try again.");
            }
            case EXPIRED -> {
                messageLabel.setText("This reset code has expired. Please request a new one.");
            }
            case VALID -> {
        
                String hashedPassword = SecurityUtils.hashPassword(newPassword);
                boolean updated = updatePassword(user.getId(), hashedPassword);

                if (updated) {
                    auditDAO.logEvent(user.getId(), "PASSWORD_RESET",
                        "User successfully reset their password via email code.");

                    messageLabel.setStyle("-fx-text-fill: #2E7D32;");
                    messageLabel.setText("Password reset successfully! Redirecting to login...");
 
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            javafx.application.Platform.runLater(() -> navigateToLogin(event));
                        } catch (InterruptedException ignored) {}
                    }).start();
                } else {
                    messageLabel.setText("Failed to update password. Please try again.");
                }
            }
        }
    }

    /**
     * Checks the submitted code against the stored code and expiry in the database.
     */
    private ValidationResult validateCode(int userId, String submittedCode) {
        String sql = "SELECT reset_code, reset_code_expiry FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedCode = rs.getString("reset_code");
                Timestamp expiry = rs.getTimestamp("reset_code_expiry");

                if (storedCode == null || !storedCode.equals(submittedCode)) {
                    return ValidationResult.INVALID;
                }

                if (expiry == null || expiry.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    return ValidationResult.EXPIRED;
                }

                return ValidationResult.VALID;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ValidationResult.INVALID;
    }

    /**
     * Updates the user's password hash and clears the reset code
     * so the same code cannot be reused.
     */
    private boolean updatePassword(int userId, String hashedPassword) {
        String sql = "UPDATE users SET password_hash = ?, reset_code = NULL, reset_code_expiry = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/medlabapp/login/LoginView.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(
                getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        navigateToLogin(event);
    }
 
    private enum ValidationResult {
        VALID, INVALID, EXPIRED
    }
}