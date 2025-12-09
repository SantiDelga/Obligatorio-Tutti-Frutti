package uy.edu.tuttifrutti.ui.salas;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import uy.edu.tuttifrutti.app.PartidaContext;
import uy.edu.tuttifrutti.app.SceneManager;
import uy.edu.tuttifrutti.app.SessionContext;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.multiplayer.Sala;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

public class SalasController {

    @FXML
    private GridPane salasGrid;

    @FXML
    private Label lblRondas;

    @FXML
    private Label lblTiempo;

    @FXML
    private FlowPane temasFlow;

    @FXML
    private FlowPane letrasFlow;

    @FXML
    private Button btnUnirme;

    private final List<Sala> salas = new ArrayList<>();
    private Sala salaSeleccionada;

    @FXML
    public void initialize() {
        // Registramos un listener específico para esta pantalla
        SessionContext.getInstance().setServerMessageListener(msg -> {
            if (msg.startsWith("LOBBY_STATE|")) {
                Platform.runLater(() -> actualizarSalasDesdeLobby(msg));
            } else {
                System.out.println("[CLIENT] [Salas] " + msg);
            }
        });

        btnUnirme.setDisable(true);

        // Pedimos las salas reales al servidor
        var client = SessionContext.getInstance().getMultiplayerClient();
        if (client != null) {
            client.send("LIST_SALAS");
        } else {
            // Fallback: si no hay server, usamos mock local
            crearSalasMock();
            dibujarSalasGrid();
        }
    }


    // ================== MOCK DE SALAS ==================
    private void crearSalasMock() {

        // categorías simples para todas las salas
        List<Categoria> categorias = List.of(
                new Categoria("Apellido"),
                new Categoria("Animal")
        );

        GameConfig baseConfig = GameConfig.configDefault(categorias);

        // letras base para todas las salas
        List<String> letrasBase = List.of("a", "b", "c", "d", "e", "f");

        salas.add(new Sala("1", "Sala 001", 10, 3, baseConfig, 3, letrasBase));
        salas.add(new Sala("2", "Sala 002", 10, 1, baseConfig, 5, letrasBase));
        salas.add(new Sala("3", "Sala 003", 10, 5, baseConfig, 7, letrasBase));
        // podés agregar más si querés llenar el grid
    }

    private void dibujarSalasGrid() {
        salasGrid.getChildren().clear();

        int col = 0;
        int row = 0;
        for (Sala sala : salas) {
            VBox card = crearCardSala(sala);
            salasGrid.add(card, col, row);

            col++;
            if (col >= 4) { // 4 columnas
                col = 0;
                row++;
            }
        }
    }

    private VBox crearCardSala(Sala sala) {
        Label nombre = new Label(sala.getNombre());
        nombre.getStyleClass().add("panel-title");

        Label jugadores = new Label(
                sala.getJugadoresActuales() + "/" + sala.getMaxJugadores() + " jugadores");
        jugadores.getStyleClass().add("hint-label");

        VBox box = new VBox(5, nombre, jugadores);
        box.getStyleClass().add("sala-card");
        box.setOnMouseClicked(e -> seleccionarSala(sala, box));

        return box;
    }

    private void seleccionarSala(Sala sala, VBox card) {
        this.salaSeleccionada = sala;
        btnUnirme.setDisable(false);

        // Quitar selección previa
        salasGrid.getChildren().forEach(node ->
                node.getStyleClass().remove("sala-card-selected")
        );
        card.getStyleClass().add("sala-card-selected");

        mostrarDetalleSala(sala);
    }

    private void mostrarDetalleSala(Sala sala) {
        GameConfig config = sala.getConfig();

        lblRondas.setText(String.valueOf(sala.getRondas()));
        lblTiempo.setText(config.getDuracionSegundos() + " seg");

        temasFlow.getChildren().clear();
        for (Categoria cat : config.getCategoriasActivas()) {
            Label chip = new Label(cat.getNombre());
            chip.getStyleClass().add("categoria-chip");
            temasFlow.getChildren().add(chip);
        }

        letrasFlow.getChildren().clear();
        for (String letra : sala.getLetras()) {
            Label chip = new Label(letra.toUpperCase());
            chip.getStyleClass().add("letra-chip");
            letrasFlow.getChildren().add(chip);
        }
    }

    private void actualizarSalasDesdeLobby(String msg) {
        // msg = "LOBBY_STATE|...."
        String data = msg.substring("LOBBY_STATE|".length()).trim();
        salas.clear();

        if (data.isEmpty()) {
            dibujarSalasGrid();
            return;
        }

        // Cada sala separada por ';'
        String[] salaTokens = data.split(";");
        for (String token : salaTokens) {
            if (token.isBlank()) continue;

            String[] parts = token.split(",");
            if (parts.length < 6) {
                System.out.println("[CLIENT] Sala mal formada: " + token);
                continue;
            }

            String id = parts[0];
            String nombre = parts[1];
            int maxJugadores = Integer.parseInt(parts[2]);
            int jugadoresActuales = Integer.parseInt(parts[3]);
            int rondas = Integer.parseInt(parts[4]);
            String letrasCsv = parts[5];
            List<String> letras = List.of(letrasCsv.split("-"));

            // Por ahora usamos una misma config simple para todas
            List<Categoria> categorias = List.of(
                    new Categoria("Apellido"),
                    new Categoria("Animal")
            );
            GameConfig config = GameConfig.configDefault(categorias);

            Sala sala = new Sala(id, nombre, maxJugadores, jugadoresActuales, config, rondas, letras);
            salas.add(sala);
        }

        dibujarSalasGrid();
    }


    // ================== BOTONES ==================

    @FXML
    private void onSalir() {
        SceneManager.getInstance().showMenuPrincipal();
    }

    @FXML
    private void onCrearSala() {
        var ctx = SessionContext.getInstance();
        var client = ctx.getMultiplayerClient();
        if (client == null) {
            System.out.println("[CLIENT] No hay conexión al servidor (onCrearSala)");
            return;
        }

        String nombreJugador = ctx.getNombreJugadorActual();
        String nombreSala = "Sala de " + nombreJugador;

        client.send("CREATE_SALA|" + nombreSala);
        // El servidor hará broadcast del nuevo LOBBY_STATE,
        // y tu listener de LOBBY_STATE actualizará la grilla.
    }

    @FXML
    private void onUnirme() {
        if (salaSeleccionada == null) return;

        var ctx = SessionContext.getInstance();
        ctx.setSalaActualId(salaSeleccionada.getId());
        var client = ctx.getMultiplayerClient();
        if (client != null) {
            client.send("JOIN_SALA|" + salaSeleccionada.getId());
        }

        GameConfig config = salaSeleccionada.getConfig();

        // Jugador local (después lo podés sacar de SessionContext)
        Jugador jugadorLocal = new Jugador("JugadorLocal");
        List<Jugador> jugadores = List.of(jugadorLocal);

        // Crear PartidaContext en modo MULTIJUGADOR
        PartidaContext partida = new PartidaContext(
                PartidaContext.ModoPartida.MULTIJUGADOR,
                config,
                jugadores,
                salaSeleccionada.getRondas(),
                salaSeleccionada.getLetras()
        );

        SessionContext.getInstance().setPartidaActual(partida);
        SceneManager.getInstance().showJuego();
    }
}
