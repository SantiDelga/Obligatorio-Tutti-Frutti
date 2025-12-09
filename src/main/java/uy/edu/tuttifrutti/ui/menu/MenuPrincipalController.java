package uy.edu.tuttifrutti.ui.menu;

import javafx.fxml.FXML;
import uy.edu.tuttifrutti.app.SceneManager;

public class MenuPrincipalController {

    @FXML
    private void onJugarEnSolitario() {
        SceneManager.getInstance().showConfigSala();
    }

    @FXML
    private void onJugarMultijugador() {
        SceneManager.getInstance().showSalasMultijugador();
    }

}
