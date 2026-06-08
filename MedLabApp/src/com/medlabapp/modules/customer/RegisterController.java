package com.medlabapp.modules.customer;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.dao.AuditDAO;
import com.medlabapp.login.VerifyAccountController;
import com.medlabapp.security.SecurityUtils;
import com.medlabapp.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerBtn;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @FXML
    private void handleRegister(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #DC3545;");
        messageLabel.setText("");

        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

         if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

          if (customerDAO.emailExists(email)) {
            messageLabel.setText("An account with this email already exists.");
            return;
        }

         String hashedPassword = SecurityUtils.hashPassword(password);
        int newUserId = customerDAO.registerCustomer(name, email, hashedPassword);

        if (newUserId <= 0) {
            messageLabel.setText("Registration failed. Please try again.");
            return;
        }

          String verifyCode = String.format("%06d", new Random().nextInt(999999));
        boolean codeSaved = saveVerificationCode(newUserId, verifyCode);

        if (!codeSaved) {
            messageLabel.setText("Account created but could not send verification email. Please contact support.");
            return;
        }

          EmailService.sendVerificationEmail(email, name, verifyCode);

        auditDAO.logEvent(newUserId, "CUSTOMER_REGISTERED",
            "New customer self-registered with email: " + email);

         registerBtn.setDisable(true);

   try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/medlabapp/login/VerifyAccountView.fxml")
            );
            Parent root = loader.load();

            VerifyAccountController controller = loader.getController();
            controller.setEmail(email);

            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(
                getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm()
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: #2E7D32;");
            messageLabel.setText("Account created! Check your email for a verification code, then log in.");
        }
    }

    /**
     * Saves the verification code and a 15-minute expiry into the users table.
     * Reuses the reset_code and reset_code_expiry columns since they serve the same purpose.
     */
    private boolean saveVerificationCode(int userId, String code) {
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

    @FXML
    private void handleBackToLogin(ActionEvent event) {
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
}