package uy.edu.tuttifrutti.ui.menu;

import javafx.fxml.FXML;
import uy.edu.tuttifrutti.app.SceneManager;
import uy.edu.tuttifrutti.app.SessionContext;
import uy.edu.tuttifrutti.app.PartidaContext;

public class MenuPrincipalController {

    @FXML
    private void onJugarEnSolitario() {
        SessionContext.getInstance().setModoConfigActual(PartidaContext.ModoPartida.SINGLEPLAYER);
        SceneManager.getInstance().showConfigSala();
    }

    @FXML
    private void onJugarMultijugador() {
        SceneManager.getInstance().showSalasMultijugador();
    }
}
