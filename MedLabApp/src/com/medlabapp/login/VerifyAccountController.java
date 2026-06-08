package com.medlabapp.login;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.dao.AuditDAO;
import com.medlabapp.dao.UserDAO;
import com.medlabapp.model.User;
import com.medlabapp.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

public class VerifyAccountController {

    @FXML private TextField codeField;
    @FXML private Label messageLabel;

 
    private String email;

    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    /**
     * Called by RegisterController after loading this screen.
     * Injects the email so we can look up the correct user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #DC3545;");
        messageLabel.setText("");

        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            messageLabel.setText("Please enter the verification code.");
            return;
        }

        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            messageLabel.setText("Invalid session. Please register again.");
            return;
        }

        
        ValidationResult result = validateCode(user.getId(), code);

        switch (result) {
            case INVALID -> {
                messageLabel.setText("Invalid code. Please check your email and try again.");
            }
            case EXPIRED -> {
                messageLabel.setText("This code has expired. Please click Resend Code to get a new one.");
            }
            case VALID -> {
                
                boolean verified = markAsVerified(user.getId());

                if (verified) {
                    auditDAO.logEvent(user.getId(), "ACCOUNT_VERIFIED",
                        "Customer verified their email address: " + email);

                    messageLabel.setStyle("-fx-text-fill: #2E7D32;");
                    messageLabel.setText("Account verified successfully! Redirecting to login...");

                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            javafx.application.Platform.runLater(() -> navigateToLogin(event));
                        } catch (InterruptedException ignored) {}
                    }).start();

                } else {
                    messageLabel.setText("Something went wrong. Please try again.");
                }
            }
        }
    }

    @FXML
    private void handleResendCode(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #DC3545;");
        messageLabel.setText("");

        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            messageLabel.setText("Invalid session. Please register again.");
            return;
        }

       
        String newCode = String.format("%06d", new Random().nextInt(999999));
        boolean saved = saveNewCode(user.getId(), newCode);

        if (saved) {
            EmailService.sendVerificationEmail(email, user.getName(), newCode);
            messageLabel.setStyle("-fx-text-fill: #2E7D32;");
            messageLabel.setText("A new code has been sent to your email.");
        } else {
            messageLabel.setText("Could not resend the code. Please try again.");
        }
    }

    /**
     * Validates the submitted code against what is stored in the database.
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
     * Marks the account as verified and clears the code columns
     * so the same code cannot be reused.
     */
    private boolean markAsVerified(int userId) {
        String sql = "UPDATE users SET is_verified = TRUE, reset_code = NULL, reset_code_expiry = NULL WHERE id = ?";
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
     * Saves a fresh verification code and resets the 15-minute expiry.
     * Used when the customer clicks Resend Code.
     */
    private boolean saveNewCode(int userId, String code) {
        String sql = "UPDATE users SET reset_code = ?, reset_code_expiry = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));
            pstmt.setInt(3, userId);
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