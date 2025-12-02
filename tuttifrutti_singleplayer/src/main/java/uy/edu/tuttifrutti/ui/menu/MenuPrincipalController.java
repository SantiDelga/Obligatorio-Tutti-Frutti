package uy.edu.tuttifrutti.ui.menu;

import javafx.fxml.FXML;
import uy.edu.tuttifrutti.app.SceneManager;

public class MenuPrincipalController {

    @FXML
    private void onJugar() {
        SceneManager.getInstance().showJuego();
    }

    @FXML
    private void onConfigurar() {
        SceneManager.getInstance().showConfigSala();
    }
}
