package com.medlabapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
     
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medlabapp/login/LoginView.fxml"));
        Parent root = loader.load();
     Scene scene = new Scene(root, 1024, 768);

         scene.getStylesheets().add(getClass().getResource("/com/medlabapp/ui/style.css").toExternalForm());

        primaryStage.setTitle("Sante Diagnostics LIMS");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);  
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}