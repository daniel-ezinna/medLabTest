package com.medlabapp.login;

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
import javafx.stage.Stage;

import java.io.IOException;

public class ForcePasswordChangeController {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private User currentUser;
    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();

 
    public void initData(User user) {
        this.currentUser = user;
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            errorLabel.setText("Please fill out both fields.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        if (newPass.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters.");
            return;
        }

        String newHash = SecurityUtils.hashPassword(newPass);

 
        if (userDAO.updatePassword(currentUser.getId(), newHash)) {
            auditDAO.logEvent(currentUser.getId(), "PASSWORD_CHANGED", "User completed mandatory password reset.");
            
  
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medlabapp/login/LoginView.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 1024, 768);
                scene.getStylesheets().add(getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            errorLabel.setText("Database error. Could not update password.");
        }
    }
}
