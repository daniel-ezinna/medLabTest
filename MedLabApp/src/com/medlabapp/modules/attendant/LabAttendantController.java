package com.medlabapp.modules.attendant;

import com.medlabapp.dao.AuditDAO;
import com.medlabapp.security.SecurityUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class LabAttendantController {
 
    @FXML
    private TableView<String[]> requestsTable;
    @FXML
    private Label queueMessageLabel;

 
    @FXML
    private TextField newCustomerName;
    @FXML
    private TextField newCustomerEmail;
    @FXML
    private PasswordField newCustomerPassword;
    @FXML
    private Label createCustomerMessage;

    private final AttendantDAO attendantDAO = new AttendantDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    
    @FXML
    public void initialize() {
        setupRequestsTable();
        refreshRequests();
    }
    
    private void setupRequestsTable() {
        TableColumn<String[], String> idCol = new TableColumn<>("Req ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        idCol.setPrefWidth(65);

        TableColumn<String[], String> patientCol = new TableColumn<>("Patient");
        patientCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> testCol = new TableColumn<>("Test");
        testCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        payCol.setPrefWidth(90);

        TableColumn<String[], String> sampleCol = new TableColumn<>("Sample Status");
        sampleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));

        requestsTable.getColumns().addAll(idCol, patientCol, testCol, payCol, sampleCol);
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        requestsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if ("UNPAID".equals(item[3])) {
                    setStyle("-fx-background-color: #FFF3CD;");
                } else {
                    setStyle("-fx-background-color: #F1F8F1;");
                }
            }
        });
    }
    
    private void refreshRequests() {
        List<String[]> rows = attendantDAO.getAllRequests();
        requestsTable.setItems(FXCollections.observableArrayList(rows));
    }
    
    @FXML
    private void handleMarkAsPaid(ActionEvent event) {
        String[] selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showQueueMessage("Select a request first.", true);
            return;
        }

        int reqId = Integer.parseInt(selected[0]);

        if ("PAID".equals(selected[3])) {
            showQueueMessage("Request #" + reqId + " is already PAID.", false);
            return;
        }

        if (attendantDAO.markAsPaid(reqId)) {
            auditDAO.logEvent(0, "PAYMENT_CONFIRMED",
                    "Lab Attendant marked request #" + reqId + " as PAID.");
            showQueueMessage("Request #" + reqId + " marked as PAID.", false);
            refreshRequests();
        } else {
            showQueueMessage("Failed to update payment status.", true);
        }
    }
    
    @FXML
    private void handleInitiateSample(ActionEvent event) {
        String[] selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showQueueMessage("Select a request first.", true);
            return;
        }

        int reqId = Integer.parseInt(selected[0]);
        int sampleId = attendantDAO.createSample(reqId);

        if (sampleId < 0) {
            showQueueMessage("Failed to create sample.", true);
            return;
        }

        auditDAO.logEvent(0, "SAMPLE_CREATED",
                "Sample created for request #" + reqId + " (sample #" + sampleId + ")");

        openSampleDetail(sampleId, event);
        refreshRequests();
    }
    
    @FXML
    private void handleOpenSampleDetail(ActionEvent event) {
        String[] selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showQueueMessage("Select a row first.", true);
            return;
        }
        if ("—".equals(selected[5])) {
            showQueueMessage("No sample yet. Click 'Initiate Sample' first.", true);
            return;
        }
        openSampleDetail(Integer.parseInt(selected[5]), event);
        refreshRequests();
    }
    
    private void openSampleDetail(int sampleId, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/medlabapp/modules/attendant/SampleDetailView.fxml"));
            Parent root = loader.load();

            SampleDetailController ctrl = loader.getController();
            ctrl.initData(sampleId);

            Stage detailStage = new Stage();
            detailStage.setTitle("Sample Detail – #" + sampleId);
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(((Node) event.getSource()).getScene().getWindow());

            Scene scene = new Scene(root, 620, 560);
            scene.getStylesheets().add(
                    getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());

            detailStage.setScene(scene);
            detailStage.setResizable(false);
            detailStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showQueueMessage("Could not open sample detail window.", true);
        }
    }
    
    @FXML
    private void handleCreateCustomer(ActionEvent event) {
        createCustomerMessage.setText("");
        String name = newCustomerName.getText().trim();
        String email = newCustomerEmail.getText().trim();
        String password = newCustomerPassword.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showCreateMessage("All fields are required.", true);
            return;
        }
        if (!email.contains("@")) {
            showCreateMessage("Please enter a valid email address.", true);
            return;
        }
        if (password.length() < 6) {
            showCreateMessage("Password must be at least 6 characters.", true);
            return;
        }

        String bcryptHash = SecurityUtils.hashPassword(password);

        if (attendantDAO.createCustomerAccount(name, email, bcryptHash)) {
            auditDAO.logEvent(0, "CUSTOMER_CREATED",
                    "Lab Attendant created customer account for: " + email);

            com.medlabapp.util.EmailService.sendAccountCreatedEmail(email, name, password);

            showCreateMessage("Account created for " + name
                    + ". Login credentials sent to " + email + ".", false);
            clearCustomerForm();
        }else {
            showCreateMessage("Failed. Email may already be registered.", true);
        }
    }
    
    
    
    
    

    private void clearCustomerForm() {
        newCustomerName.clear();
        newCustomerEmail.clear();
        newCustomerPassword.clear();
    }
    
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/medlabapp/login/LoginView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1024, 768);
            scene.getStylesheets().add(
                    getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showQueueMessage(String text, boolean isError) {
        queueMessageLabel.setText(text);
        queueMessageLabel.setStyle(isError
                ? "-fx-text-fill: #DC3545; -fx-font-weight: bold;"
                : "-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
    }

    private void showCreateMessage(String text, boolean isError) {
        createCustomerMessage.setText(text);
        createCustomerMessage.setStyle(isError
                ? "-fx-text-fill: #DC3545; -fx-font-weight: bold;"
                : "-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
    }
}
    
    
