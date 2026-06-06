package com.project.app;

import com.project.controller.ProjectController;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.datasource.DbInitializer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ProjectClientApplication extends Application {

    private Parent root;
    private FXMLLoader loader;

    public static void main(String[] args) {
        DbInitializer.init(); // 1. Uruchomienie bazy danych
        launch(ProjectClientApplication.class, args); // 2. Odpalenie okienek JavaFX
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        loader = new FXMLLoader();
        // Wskazanie ścieżki do pliku graficznego FXML
        loader.setLocation(getClass().getResource("/fxml/ProjectFrame.fxml"));

        ProjektDAO projektDAO = new ProjektDAOImpl();
        // Przekazanie obiektu bazy danych do kontrolera
        loader.setControllerFactory(controllerClass -> new ProjectController(projektDAO));

        root = loader.load();
        primaryStage.setTitle("Projekty");

        Scene scene = new Scene(root);
        // Podpięcie stylów CSS
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        // Bezpieczne wyłączanie aplikacji po kliknięciu X
        ProjectController controller = loader.getController();
        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            Platform.exit();
        });
    }
}