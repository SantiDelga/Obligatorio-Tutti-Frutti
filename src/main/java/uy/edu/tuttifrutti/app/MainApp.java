package uy.edu.tuttifrutti.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.getInstance().init(primaryStage);
        SceneManager.getInstance().showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
