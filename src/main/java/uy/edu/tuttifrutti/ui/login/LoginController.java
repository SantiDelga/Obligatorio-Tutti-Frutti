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

        // Guardamos el nombre en la sesión
        SessionContext ctx = SessionContext.getInstance();
        ctx.setNombreJugadorActual(nombre);

        // Listener por defecto: solo loguea lo que llega
        ctx.setServerMessageListener(msg -> System.out.println("[CLIENT] Recibido: " + msg));

        // 🔌 Conexión al servidor multiplayer + handshake HELLO
        try {
            ctx.conectarMultiplayer("localhost", 55555);

            // Enviamos HELLO|NombreJugador al servidor
            ctx.getMultiplayerClient().send("HELLO|" + nombre);

        } catch (Exception e) {
            // Si falló la conexión, NO vamos al menú y logueamos el error
            e.printStackTrace();
            // Si querés podés agregar un Label en el FXML para mostrar mensaje al usuario
            return;
        }

        // Si todo salió bien → vamos al menú principal
        SceneManager.getInstance().showMenuPrincipal();
    }

}
