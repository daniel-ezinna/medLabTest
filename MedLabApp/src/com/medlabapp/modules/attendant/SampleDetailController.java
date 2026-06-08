package com.medlabapp.modules.attendant;

import com.medlabapp.dao.AuditDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class SampleDetailController {

    @FXML
    private Label sampleIdLabel;
    @FXML
    private Label currentStatusLabel;
    @FXML
    private Label verifiedLabel;
    @FXML
    private Label pdfPathLabel;
    @FXML
    private Label imagePathLabel;
    @FXML
    private Label messageLabel;

    @FXML
    private Button btnCollected;
    @FXML
    private Button btnProcessing;
    @FXML
    private Button btnValidated;

    private int sampleId;
    private String pendingPdfPath = null;
    private String pendingImagePath = null;

    private final AttendantDAO attendantDAO = new AttendantDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    
    public void initData(int sampleId) {
        this.sampleId = sampleId;
        refreshView();
    }
    
    private void refreshView() {
        String[] detail = attendantDAO.getSampleDetail(sampleId);
        if (detail == null) {
            messageLabel.setText("Error: could not load sample #" + sampleId);
            return;
        }

        // detail: [0]=id, [1]=status, [2]=pdfPath, [3]=imagePath, [4]=isVerified
        sampleIdLabel.setText("Sample #" + detail[0]);
        currentStatusLabel.setText(detail[1]);
        pdfPathLabel.setText(detail[2].isEmpty() ? "(none)" : detail[2]);
        imagePathLabel.setText(detail[3].isEmpty() ? "(none)" : detail[3]);
        verifiedLabel.setText("true".equals(detail[4])
                ? "Verified — visible to patient"
                : "Not yet verified");

        styleLifecycleButton(btnCollected, "COLLECTED", detail[1]);
        styleLifecycleButton(btnProcessing, "PROCESSING", detail[1]);
        styleLifecycleButton(btnValidated, "VALIDATED", detail[1]);
    }
    
    private void styleLifecycleButton(Button btn, String btnStatus, String currentStatus) {
        btn.getStyleClass().removeAll("button-primary", "button-secondary");
        if (btnStatus.equals(currentStatus)) {
            btn.getStyleClass().add("button-primary");
        } else {
            btn.getStyleClass().add("button-secondary");
        }
    }
    
    @FXML
    private void handleSetCollected(ActionEvent e) {
        advanceStatus("COLLECTED");
    }

    @FXML
    private void handleSetProcessing(ActionEvent e) {
        advanceStatus("PROCESSING");
    }

    @FXML
    private void handleSetValidated(ActionEvent e) {
        advanceStatus("VALIDATED");
    }

    private void advanceStatus(String newStatus) {
        if (attendantDAO.updateSampleStatus(sampleId, newStatus)) {
            auditDAO.logEvent(0, "SAMPLE_STATUS_UPDATED",
                    "Sample #" + sampleId + " status set to " + newStatus);
            showMessage("Status updated to " + newStatus + ".", false);
            refreshView();
        } else {
            showMessage("Failed to update status.", true);
        }
    }
    
    @FXML
    private void handleBrowsePdf(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF Report");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showOpenDialog(
                ((Node) event.getSource()).getScene().getWindow());
        if (file != null) {
            pendingPdfPath = file.getAbsolutePath();
            pdfPathLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleBrowseImage(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Medical Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.tiff", "*.bmp"));
        File file = chooser.showOpenDialog(
                ((Node) event.getSource()).getScene().getWindow());
        if (file != null) {
            pendingImagePath = file.getAbsolutePath();
            imagePathLabel.setText(file.getName());
        }
    }
    
    @FXML
    private void handleSaveFiles(ActionEvent event) {
        if (pendingPdfPath == null && pendingImagePath == null) {
            showMessage("No files selected. Use Browse to select files first.", true);
            return;
        }

        if (attendantDAO.attachFiles(sampleId, pendingPdfPath, pendingImagePath)) {
            auditDAO.logEvent(0, "RESULT_UPLOADED",
                    "Files attached to sample #" + sampleId);
            pendingPdfPath = null;
            pendingImagePath = null;
            showMessage("Files saved successfully.", false);
            refreshView();
        } else {
            showMessage("Failed to save files.", true);
        }
    }
    
    @FXML
    private void handleVerifyResult(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Verification");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Mark this result as VERIFIED? It will be visible to the patient immediately.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        if (attendantDAO.setVerified(sampleId, true)) {
            auditDAO.logEvent(0, "RESULT_VERIFIED",
                    "Sample #" + sampleId + " verified and released to patient.");

            
            String[] patient = attendantDAO.getPatientDetailsForSample(sampleId);
            if (patient != null) {
                com.medlabapp.util.EmailService.sendResultReadyEmail(
                        patient[1], // email
                        patient[0], // name
                        patient[2] // test name
                );
            }

            showMessage("Result verified. Patient notified by email.", false);
            refreshView();
        } else {
            showMessage("Failed to verify result.", true);
        }
    }
    
    @FXML
    private void handleClose(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.setStyle(isError
                ? "-fx-text-fill: #DC3545; -fx-font-weight: bold;"
                : "-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
    }
    
    
    
    
    
    
}
