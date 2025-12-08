package uy.edu.tuttifrutti.ui.menu;

import javafx.fxml.FXML;
import uy.edu.tuttifrutti.app.SceneManager;

public class MenuPrincipalController {

    @FXML
    private void onJugarEnSolitario() {
        // Por ahora vamos directo al juego single player con config default
        SceneManager.getInstance().showConfigSala();
    }

    @FXML
    private void onJugarMultijugador() {
        // En esta versión solo jugador único, se podría luego mostrar lobby
        SceneManager.getInstance().showJuego();
    }
}
