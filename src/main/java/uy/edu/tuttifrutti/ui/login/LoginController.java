package uy.edu.tuttifrutti.ui.login;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import uy.edu.tuttifrutti.app.SceneManager;
import uy.edu.tuttifrutti.app.SessionContext;

public class LoginController {

    @FXML
    private TextField nombreField;

    @FXML
    private Button ingresarButton;

    @FXML
    private void initialize() {
        ingresarButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> nombreField.getText() == null || nombreField.getText().trim().isEmpty(),
                        nombreField.textProperty()
                )
        );
    }

    @FXML
    private void onIngresar() {
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) {
            return;
        }
        SessionContext.getInstance().setNombreJugadorActual(nombre);
        SceneManager.getInstance().showMenuPrincipal();
    }
}
