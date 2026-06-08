package com.medlabapp.modules.admin;

import com.medlabapp.model.TestType;
import com.medlabapp.security.SecurityUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SuperAdminController {

    // --- FXML UI Injections (Must match fx:id in SuperAdminView.fxml) ---
    
    // Tab 1: Test Builder
    @FXML private TextField testNameField;
    @FXML private TextField priceField;
    @FXML private TextField tatField;
    @FXML private ComboBox<String> formatComboBox;
    @FXML private Label testMessageLabel;
    @FXML private TableView<TestType> testsTable;
    

    // Tab 2: Requests Queue
    @FXML private TableView<String[]> requestsTable;

    // Tab 3: Audit Trail
    @FXML private TableView<String[]> auditTable;
    
    // Tab 4: User Management
    @FXML private TextField userNameField;
    @FXML private TextField userEmailField;
    @FXML private ComboBox<String> userRoleComboBox;
    @FXML private Label userMessageLabel;

    // Data Access Object
    private final AdminDAO adminDAO = new AdminDAO();

    /**
     * Called automatically by JavaFX after the FXML is loaded.
     * Use this to set up table columns and default data.
     */
    @FXML
    public void initialize() {
        // 1. Setup ComboBox options
        formatComboBox.setItems(FXCollections.observableArrayList("NUMERIC", "TEXT", "PDF", "IMAGE"));

        // 2. Setup Table Columns
        setupTestsTable();
        setupRequestsTable();
        setupAuditTable();
        userRoleComboBox.setItems(FXCollections.observableArrayList("LAB_ATTENDANT", "CUSTOMER", "SUPER_ADMIN"));

        // 3. Load Initial Data from Database
        refreshAllData();
    }

    // --- ACTION HANDLERS ---

    @FXML
    private void handleCreateTest(ActionEvent event) {
        testMessageLabel.setText(""); // Reset message

        try {
            String name = testNameField.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());
            int tat = Integer.parseInt(tatField.getText().trim());
            String format = formatComboBox.getValue();

            if (name.isEmpty() || format == null) {
                testMessageLabel.setText("Please fill all fields.");
                return;
            }

            boolean success = adminDAO.createTestType(name, price, tat, format);
            if (success) {
                testMessageLabel.setStyle("-fx-text-fill: #2E7D32;"); // Green success
                testMessageLabel.setText("Test created successfully!");
                clearTestForm();
                refreshAllData(); // Update the table instantly
            } else {
                testMessageLabel.setStyle("-fx-text-fill: #DC3545;"); // Red error
                testMessageLabel.setText("Failed to create test. Name might exist.");
            }
        } catch (NumberFormatException e) {
            testMessageLabel.setStyle("-fx-text-fill: #DC3545;");
            testMessageLabel.setText("Price and TAT must be valid numbers.");
        }
    }
    
    @FXML
    private void handleCreateUser(ActionEvent event) {
        userMessageLabel.setText(""); // Reset error message

        String name = userNameField.getText().trim();
        String email = userEmailField.getText().trim();
        String role = userRoleComboBox.getValue();

        if (name.isEmpty() || email.isEmpty() || role == null) {
            userMessageLabel.setStyle("-fx-text-fill: #DC3545;"); // Red error
            userMessageLabel.setText("Please fill all fields.");
            return;
        }

       
        String defaultPassword = "Welcome123!";
        String hashedPassword = SecurityUtils.hashPassword(defaultPassword);

        // 2. Determine security flags based on role
        boolean forceChange = true;
        boolean isVerified = true; // If Super Admin creates them, they are trusted/verified instantly

        // 3. Save to database
        boolean success = adminDAO.createUser(name, email, hashedPassword, role, forceChange, isVerified);

        if (success) {
            userMessageLabel.setStyle("-fx-text-fill: #2E7D32;"); // Green success
            userMessageLabel.setText(role + " account created successfully!");
            userNameField.clear();
            userEmailField.clear();
            userRoleComboBox.getSelectionModel().clearSelection();
        } else {
            userMessageLabel.setStyle("-fx-text-fill: #DC3545;"); // Red error
            userMessageLabel.setText("Failed to create user. Email might already exist.");
        }
    }

    @FXML
    private void handleMarkAsPaid(ActionEvent event) {
        // Get the selected row from the table
        String[] selected = requestsTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a request to mark as paid.");
            return;
        }

        int requestId = Integer.parseInt(selected[0]); // ID is at index 0
        
        if (adminDAO.markAsPaid(requestId)) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Request #" + requestId + " marked as PAID.");
            refreshAllData(); // Refresh to show updated status
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update payment status.");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Route back to the Login screen
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
    }

    // --- HELPER METHODS ---

    private void refreshAllData() {
        // Fetch fresh lists from the DB and push them to the JavaFX tables
        testsTable.setItems(FXCollections.observableArrayList(adminDAO.getAllTestTypes()));
        requestsTable.setItems(FXCollections.observableArrayList(adminDAO.getPendingRequests()));
        auditTable.setItems(FXCollections.observableArrayList(adminDAO.getAuditLogs()));
    }

    private void clearTestForm() {
        testNameField.clear();
        priceField.clear();
        tatField.clear();
        formatComboBox.getSelectionModel().clearSelection();
    }

    private void setupTestsTable() {
        TableColumn<TestType, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<TestType, String> nameCol = new TableColumn<>("Test Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<TestType, Double> priceCol = new TableColumn<>("Price (₦)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<TestType, Integer> tatCol = new TableColumn<>("TAT (Hrs)");
        tatCol.setCellValueFactory(new PropertyValueFactory<>("tatHours"));

        TableColumn<TestType, String> formatCol = new TableColumn<>("Format");
        formatCol.setCellValueFactory(new PropertyValueFactory<>("resultFormat"));

        testsTable.getColumns().addAll(idCol, nameCol, priceCol, tatCol, formatCol);
        testsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupRequestsTable() {
        TableColumn<String[], String> idCol = new TableColumn<>("Req ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> patientCol = new TableColumn<>("Patient Name");
        patientCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> testCol = new TableColumn<>("Test Type");
        testCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> statusCol = new TableColumn<>("Payment Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));

        requestsTable.getColumns().addAll(idCol, patientCol, testCol, statusCol);
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupAuditTable() {
        TableColumn<String[], String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[0]));

        TableColumn<String[], String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));

        TableColumn<String[], String> actionCol = new TableColumn<>("Action Type");
        actionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));

        TableColumn<String[], String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));

        auditTable.getColumns().addAll(timeCol, userCol, actionCol, descCol);
        auditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}