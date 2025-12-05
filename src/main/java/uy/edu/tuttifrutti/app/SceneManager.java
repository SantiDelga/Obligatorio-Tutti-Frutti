package uy.edu.tuttifrutti.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class SceneManager {

    private static final SceneManager INSTANCE = new SceneManager();
    private Stage primaryStage;

    private SceneManager() {
    }

    public static SceneManager getInstance() {
        return INSTANCE;
    }

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Tutti Frutti");
        this.primaryStage.setResizable(false);
    }

    public void showLogin() {
        setScene("/ui/fxml/login-view.fxml");
    }

    public void showMenuPrincipal() {
        setScene("/ui/fxml/menu-principal.fxml");
    }

    public void showJuego() {
        setScene("/ui/fxml/juego.fxml");
    }

    public void showConfigSala() {
        setScene("/ui/fxml/config-sala.fxml");
    }


    private void setScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // 👉 AQUÍ forzamos a cargar el CSS siempre
            scene.getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource("/ui/css/tutti-frutti.css")
                    ).toExternalForm()
            );

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo cargar la vista: " + fxmlPath, e);
        }
    }
}
