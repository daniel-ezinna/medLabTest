package com.medlabapp.login;

import com.medlabapp.dao.AuditDAO;
import com.medlabapp.dao.UserDAO;
import com.medlabapp.model.User;
import com.medlabapp.modules.customer.CustomerController;
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
import javafx.scene.control.ToggleButton;


import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
@FXML private TextField passwordTextField;  
@FXML private ToggleButton togglePasswordBtn;  

    private UserDAO userDAO = new UserDAO();
    private AuditDAO auditDAO = new AuditDAO();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        errorLabel.setText("");

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password cannot be empty.");
            return;
        }

        User user = userDAO.getUserByEmail(email);

        if (user == null) {
            errorLabel.setText("Invalid email address.");
            return;
        }

        if (!SecurityUtils.checkPassword(password, user.getPasswordHash())) {
            errorLabel.setText("Incorrect password.");
            return;
        }

        auditDAO.logEvent(user.getId(), "LOGIN_SUCCESSFUL",
            "User logged in successfully via email: " + email);

        if (user.isForcePasswordChange()) {
            System.out.println("Audited event: ROUTING_FORCE_PASSWORD_CHANGE (Placeholder needed)");
            return;
        }

        
         
        if ("CUSTOMER".equals(user.getRole()) && !user.isVerified()) {
            // Log the blocked attempt
            auditDAO.logEvent(user.getId(), "LOGIN_BLOCKED_UNVERIFIED", 
                "Unverified customer attempted login: " + email);
            
            
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/medlabapp/login/VerifyAccountView.fxml")
                );
                Parent root = loader.load();

              
                VerifyAccountController controller = loader.getController();
                controller.setEmail(user.getEmail());

                Scene scene = new Scene(root, 1024, 768);
                scene.getStylesheets().add(
                    getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm()
                );
                
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.show();
                
            } catch (IOException e) {
                e.printStackTrace();
                errorLabel.setText("Internal Error: Could not load verification screen.");
            }
            return;  
        }

         
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        String fxmlPath = "";
        if ("SUPER_ADMIN".equals(user.getRole())) {
            fxmlPath = "/com/medlabapp/modules/admin/SuperAdminView.fxml";
        } else if ("LAB_ATTENDANT".equals(user.getRole())) {
            fxmlPath = "/com/medlabapp/modules/attendant/LabAttendantView.fxml";
        } else if ("CUSTOMER".equals(user.getRole())) {
            fxmlPath = "/com/medlabapp/modules/customer/CustomerView.fxml";
        }

        if (fxmlPath.isEmpty()) {
            errorLabel.setText("Internal Error: Dynamic role screen not found.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

        
            if ("CUSTOMER".equals(user.getRole())) {
                CustomerController controller = loader.getController();
                controller.setCurrentUser(user);
            }

            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Internal Error: Could not load dynamic dashboard.");
        }
    }
 
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/medlabapp/login/ForgotPasswordView.fxml")
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
            errorLabel.setText("Internal Error: Could not load password reset screen.");
        }
    }
 
    @FXML
    private void handleSelfRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/medlabapp/modules/customer/RegisterView.fxml")
            );
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Internal Error: Could not load registration screen.");
        }
    }
    
    @FXML
public void initialize() {
   
    passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
}


@FXML
private void togglePasswordVisibility(ActionEvent event) {
    if (togglePasswordBtn.isSelected()) {
        
        passwordTextField.setVisible(true);
        passwordField.setVisible(false);
        togglePasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E7D32; -fx-cursor: hand;"); // Turn green when visible
    } else {
     
        passwordTextField.setVisible(false);
        passwordField.setVisible(true);
        togglePasswordBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6C757D; -fx-cursor: hand;"); // Gray when masked
    }
}
}