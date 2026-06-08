package com.medlabapp.login;

import com.medlabapp.config.DatabaseConnection;
import com.medlabapp.dao.UserDAO;
import com.medlabapp.model.User;
import com.medlabapp.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private Button sendCodeBtn;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleSendCode(ActionEvent event) {
        messageLabel.setStyle("-fx-text-fill: #DC3545;");
        messageLabel.setText("");

        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            messageLabel.setText("Please enter your email address.");
            return;
        }

        // Check if the email exists in the database
        User user = userDAO.getUserByEmail(email);
        if (user == null) {
             
            messageLabel.setStyle("-fx-text-fill: #2E7D32;");
            messageLabel.setText("If that email is registered, a reset code has been sent.");
            sendCodeBtn.setDisable(true);
            return;
        }

        // Generate a random 6-digit code
        String code = String.format("%06d", new Random().nextInt(999999));

         
        boolean saved = saveResetCode(user.getId(), code);
        if (!saved) {
            messageLabel.setText("Something went wrong. Please try again.");
            return;
        }

    
        EmailService.sendPasswordResetEmail(email, user.getName(), code);

        messageLabel.setStyle("-fx-text-fill: #2E7D32;");
        messageLabel.setText("If that email is registered, a reset code has been sent.");
        sendCodeBtn.setDisable(true);

        
        navigateToResetScreen(event, email);
    }

    /**
     * Saves the reset code and its expiry (15 minutes from now) to the users table.
     */
    private boolean saveResetCode(int userId, String code) {
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

    private void navigateToResetScreen(ActionEvent event, String email) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/medlabapp/login/ResetPasswordView.fxml")
            );
            Parent root = loader.load();
 
            ResetPasswordController controller = loader.getController();
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
            messageLabel.setStyle("-fx-text-fill: #DC3545;");
            messageLabel.setText("Internal Error: Could not load reset screen.");
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