package uy.edu.tuttifrutti.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

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


    // Reemplazar/actualizar método setScene por la versión más robusta:
    private void setScene(String fxmlPath) {
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML no encontrado: " + fxmlPath + " (comprueba src/main/resources)");
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // Cargar CSS si existe (no forzar NPE)
            URL cssUrl = getClass().getResource("/ui/css/tutti-frutti.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo cargar la vista: " + fxmlPath, e);
        }
    }
}
