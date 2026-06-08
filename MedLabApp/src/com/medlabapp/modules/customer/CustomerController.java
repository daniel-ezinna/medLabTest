package com.medlabapp.modules.customer;

import com.medlabapp.dao.AuditDAO;
import com.medlabapp.model.TestType;
import com.medlabapp.model.User;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CustomerController {
 
    @FXML private Label welcomeLabel;

    @FXML private TableView<String[]> dashboardTable;
    @FXML private TableColumn<String[], String> dashReqIdCol;
    @FXML private TableColumn<String[], String> dashTestNameCol;
    @FXML private TableColumn<String[], String> dashPaymentCol;
    @FXML private TableColumn<String[], String> dashStatusCol;
    @FXML private TableColumn<String[], String> dashDeadlineCol;
    @FXML private TableColumn<String[], String> dashCountdownCol;
    @FXML private TableColumn<String[], String> dashResultCol;

   @FXML private TableView<TestType> testsTable;
    @FXML private TableColumn<TestType, String>  testNameCol;
    @FXML private TableColumn<TestType, Double>  testPriceCol;
    @FXML private TableColumn<TestType, Integer> testTatCol;
    @FXML private TableColumn<TestType, String>  testFormatCol;
    @FXML private Label selectedTestLabel;
    @FXML private Label orderMessageLabel;
    @FXML private javafx.scene.layout.VBox bankDetailsBox;
    @FXML private Label bankAmountLabel;

     @FXML private TableView<String[]> resultsTable;
    @FXML private TableColumn<String[], String> resReqIdCol;
    @FXML private TableColumn<String[], String> resTestCol;
    @FXML private TableColumn<String[], String> resFormatCol;
    @FXML private TableColumn<String[], String> resDateCol;

    private User currentUser;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final AuditDAO auditDAO = new AuditDAO();

     private Timeline countdownTimer;
 
    @FXML
    public void initialize() {
        setupDashboardTable();
        setupTestsTable();
        setupResultsTable();

        testsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedTestLabel.setText(newVal.getName() + " — ₦" + String.format("%,.2f", newVal.getPrice()));
                   bankDetailsBox.setVisible(false);
                bankDetailsBox.setManaged(false);
            }
        });
    }

    /**
     * Called by LoginController after loading this view.
     * Injects the authenticated user and loads all data.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getName());
        loadAllData();
        startCountdownTimer();
    }

    // ── Data loading ──────────────────────────────────────────────────

    private void loadAllData() {
        loadDashboard();
        loadTestCatalog();
        loadResults();
    }

    private void loadDashboard() {
        if (currentUser == null) return;
        List<String[]> rows = customerDAO.getCustomerDashboard(currentUser.getId());
        dashboardTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void loadTestCatalog() {
        List<TestType> tests = customerDAO.getAllTestTypes();
        testsTable.setItems(FXCollections.observableArrayList(tests));
    }

    private void loadResults() {
        if (currentUser == null) return;
         List<String[]> all = customerDAO.getCustomerDashboard(currentUser.getId());
        List<String[]> validated = all.stream()
            .filter(row -> "true".equalsIgnoreCase(row[5])) // index 5 = is_verified
            .toList();
        resultsTable.setItems(FXCollections.observableArrayList(validated));
    }
   private void setupDashboardTable() {
        dashReqIdCol.setCellValueFactory(data    -> new SimpleStringProperty(data.getValue()[0]));
        dashTestNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));
        dashPaymentCol.setCellValueFactory(data  -> new SimpleStringProperty(data.getValue()[2]));
        dashStatusCol.setCellValueFactory(data   -> new SimpleStringProperty(data.getValue()[3]));
        dashDeadlineCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue()[4].isEmpty() ? "N/A" : data.getValue()[4].substring(0, 16)
        ));
          dashCountdownCol.setCellValueFactory(data -> new SimpleStringProperty(
            computeCountdown(data.getValue()[4])
        ));
        dashResultCol.setCellValueFactory(data -> new SimpleStringProperty(
            "true".equalsIgnoreCase(data.getValue()[5]) ? "✔ Ready" : "Pending"
        ));
        dashboardTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupTestsTable() {
        testNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        testPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        testTatCol.setCellValueFactory(new PropertyValueFactory<>("tatHours"));
        testFormatCol.setCellValueFactory(new PropertyValueFactory<>("resultFormat"));
        testsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupResultsTable() {
        resReqIdCol.setCellValueFactory(data  -> new SimpleStringProperty(data.getValue()[0]));
        resTestCol.setCellValueFactory(data   -> new SimpleStringProperty(data.getValue()[1]));
        // Format is stored in the test type — use sample status as a proxy here
        resFormatCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));
        resDateCol.setCellValueFactory(data   -> new SimpleStringProperty(
            data.getValue()[4].isEmpty() ? "N/A" : data.getValue()[4].substring(0, 16)
        ));
        resultsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

     
    /**
     * Starts a JavaFX Timeline that fires every second.
     * It refreshes the countdown column on the dashboard table
     * without doing a full database reload.
     */
    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
             Platform.runLater(() -> dashboardTable.refresh());
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
    }

    /**
     * Computes a human-readable countdown string from a deadline timestamp string.
     * Returns "Overdue" if the deadline has passed, or "N/A" if no deadline exists.
     */
    private String computeCountdown(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) return "N/A";
        try {
             String clean = deadlineStr.substring(0, 19); // trim millis
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime deadline = LocalDateTime.parse(clean, fmt);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(deadline)) return "Overdue";

            long hours = ChronoUnit.HOURS.between(now, deadline);
            long minutes = ChronoUnit.MINUTES.between(now, deadline) % 60;
            long seconds = ChronoUnit.SECONDS.between(now, deadline) % 60;

            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } catch (Exception e) {
            return "N/A";
        }
    }

      @FXML
    private void handlePlaceOrder(ActionEvent event) {
        orderMessageLabel.setStyle("-fx-text-fill: #DC3545;");
        orderMessageLabel.setText("");
        bankDetailsBox.setVisible(false);
        bankDetailsBox.setManaged(false);

        TestType selected = testsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            orderMessageLabel.setText("Please select a test from the list first.");
            return;
        }

        boolean success = customerDAO.placeTestOrder(
            currentUser.getId(), selected.getId(), selected.getTatHours()
        );

        if (success) {
              auditDAO.logEvent(currentUser.getId(), "TEST_ORDER_PLACED",
                "Customer placed order for test: " + selected.getName());

              bankAmountLabel.setText("Amount: ₦" + String.format("%,.2f", selected.getPrice()));
            bankDetailsBox.setVisible(true);
            bankDetailsBox.setManaged(true);

            orderMessageLabel.setStyle("-fx-text-fill: #2E7D32;");
            orderMessageLabel.setText("Order placed! Please complete your bank transfer using the details below.");

              loadDashboard();

        } else {
            orderMessageLabel.setText("Failed to place order. Please try again.");
        }
    }

    @FXML
    private void handleRefreshDashboard(ActionEvent event) {
        loadDashboard();
        loadResults();
    }
    @FXML
    private void handleDownloadPdf(ActionEvent event) {
        String[] selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a result row first.");
            return;
        }

        String pdfPath = selected[6]; // index 6 = pdf_report_path
        if (pdfPath == null || pdfPath.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No PDF Available",
                "No PDF report has been uploaded for this result yet.");
            return;
        }

         openFile(pdfPath);

        auditDAO.logEvent(currentUser.getId(), "RESULT_DOWNLOADED",
            "Customer downloaded PDF report for request #" + selected[0]);
    }

    @FXML
    private void handleViewImage(ActionEvent event) {
        String[] selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a result row first.");
            return;
        }

        String imagePath = selected[7]; // index 7 = image_report_path
        if (imagePath == null || imagePath.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Image Available",
                "No medical image has been uploaded for this result yet.");
            return;
        }

        openFile(imagePath);

        auditDAO.logEvent(currentUser.getId(), "RESULT_VIEWED",
            "Customer viewed medical image for request #" + selected[0]);
    }

    /**
     * Opens a file at the given path using the operating system's default application.
     */
    private void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                showAlert(Alert.AlertType.ERROR, "File Not Found",
                    "The file could not be found at: " + filePath);
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert(Alert.AlertType.ERROR, "Not Supported",
                    "File opening is not supported on this system.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not open the file.");
        }
    }

      @FXML
    private void handleLogout(ActionEvent event) {
        // Stop the countdown timer to prevent resource leaks
        if (countdownTimer != null) countdownTimer.stop();

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
    }

   
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}