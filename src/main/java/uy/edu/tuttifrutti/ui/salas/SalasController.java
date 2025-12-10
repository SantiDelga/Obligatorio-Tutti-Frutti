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
import uy.edu.tuttifrutti.infrastructure.net.MultiplayerClient;

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
            } else if (msg.startsWith("CONFIG_SALA|")) {
                Platform.runLater(() -> manejarConfigSala(msg));
            } else if (msg.startsWith("SALA_STATE|")) {
                // si estamos en la vista de salas, también queremos actualizar el detalle
                Platform.runLater(() -> {
                    // actualizar listado mínimo y detalle si corresponde
                    manejarSalaStateEnLobby(msg);
                });
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

            // Split con límite para capturar hasta 8 partes (id,nombre,maxJug,jugAct,rondas,duracion,temasCsv,letrasCsv)
            String[] parts = token.split(",", 8);
            if (parts.length < 6) {
                System.out.println("[CLIENT] Sala mal formada: " + token);
                continue;
            }

            String id = parts[0];
            String nombre = parts[1];
            int maxJugadores = Integer.parseInt(parts[2]);
            int jugadoresActuales = Integer.parseInt(parts[3]);
            int rondas = Integer.parseInt(parts[4]);

            int duracionSegundos = 60;
            String temasCsv = "";
            String letrasCsv = "";

            if (parts.length >= 6 && parts[5] != null && !parts[5].isBlank()) {
                try {
                    duracionSegundos = Integer.parseInt(parts[5]);
                } catch (NumberFormatException e) {
                    // keep default
                }
            }

            if (parts.length >= 7) temasCsv = parts[6] == null ? "" : parts[6];
            if (parts.length >= 8) letrasCsv = parts[7] == null ? "" : parts[7];

            List<Categoria> categorias = new ArrayList<>();
            if (!temasCsv.isBlank()) {
                String[] temas = temasCsv.split(",");
                for (String t : temas) {
                    if (!t.isBlank()) categorias.add(new Categoria(t.trim()));
                }
            }
            if (categorias.isEmpty()) {
                // fallback
                categorias.add(new Categoria("Apellido"));
                categorias.add(new Categoria("Animal"));
            }

            GameConfig config = GameConfig.configDefault(categorias);

            List<String> letras = new ArrayList<>();
            if (!letrasCsv.isBlank()) {
                String[] letrasArr = letrasCsv.split(",");
                for (String l : letrasArr) {
                    if (!l.isBlank()) letras.add(l.trim());
                }
            }
            if (letras.isEmpty()) {
                letras = List.of("A","B","C","D","E","F");
            }

            Sala sala = new Sala(id, nombre, maxJugadores, jugadoresActuales, config, rondas, letras);
            salas.add(sala);
        }

        dibujarSalasGrid();
    }

    private void manejarConfigSala(String msg) {
        // CONFIG_SALA|idSala|maxJugadores|rondas|duracionSegundos|temasCsv|letrasCsv
        String[] parts = msg.split("\\|", -1);
        if (parts.length < 7) return;
        String id = parts[1];
        int maxJug = 10;
        int rondas = 5;
        int dur = 60;
        try { maxJug = Integer.parseInt(parts[2]); } catch (Exception ignored) {}
        try { rondas = Integer.parseInt(parts[3]); } catch (Exception ignored) {}
        try { dur = Integer.parseInt(parts[4]); } catch (Exception ignored) {}
        String temasCsv = parts[5] == null ? "" : parts[5];
        String letrasCsv = parts[6] == null ? "" : parts[6];

        // Buscar sala en la lista y actualizar
        for (int i = 0; i < salas.size(); i++) {
            Sala s = salas.get(i);
            if (s.getId().equals(id)) {
                List<Categoria> cats = new ArrayList<>();
                if (!temasCsv.isBlank()) {
                    for (String t : temasCsv.split(",")) if (!t.isBlank()) cats.add(new Categoria(t.trim()));
                }
                if (cats.isEmpty()) {
                    cats.add(new Categoria("Apellido"));
                    cats.add(new Categoria("Animal"));
                }
                GameConfig cfg = GameConfig.configDefault(cats);
                List<String> letras = new ArrayList<>();
                if (!letrasCsv.isBlank()) for (String l : letrasCsv.split(",")) if (!l.isBlank()) letras.add(l.trim());
                if (letras.isEmpty()) letras = List.of("A","B","C","D","E","F");

                Sala nueva = new Sala(s.getId(), s.getNombre(), maxJug, s.getJugadoresActuales(), cfg, rondas, letras);
                salas.set(i, nueva);
                if (salaSeleccionada != null && salaSeleccionada.getId().equals(id)) {
                    salaSeleccionada = nueva;
                    mostrarDetalleSala(nueva);
                }
                dibujarSalasGrid();
                break;
            }
        }
    }

    private void manejarSalaStateEnLobby(String msg) {
        // SALA_STATE|idSala|jug1;jug2|maxJug|rondas|duracion|temasCsv|letrasCsv
        String[] parts = msg.split("\\|", -1);
        if (parts.length < 3) return;
        String id = parts[1];
        String jugadores = parts[2];
        int maxJug = -1;
        int rondas = -1;
        int dur = -1;
        String temasCsv = "";
        String letrasCsv = "";
        if (parts.length >= 4) {
            try { maxJug = Integer.parseInt(parts[3]); } catch (Exception ignored) {}
        }
        if (parts.length >= 5) {
            try { rondas = Integer.parseInt(parts[4]); } catch (Exception ignored) {}
        }
        if (parts.length >= 6) {
            try { dur = Integer.parseInt(parts[5]); } catch (Exception ignored) {}
        }
        if (parts.length >= 7) temasCsv = parts[6] == null ? "" : parts[6];
        if (parts.length >= 8) letrasCsv = parts[7] == null ? "" : parts[7];

        // Actualizar nombre de jugadores en la sala correspondiente
        for (Sala s : salas) {
            if (s.getId().equals(id)) {
                // actualizar jugadoresActuales
                String[] toks = jugadores.split(";");
                s.setJugadoresActuales(toks.length);
                if (salaSeleccionada != null && salaSeleccionada.getId().equals(id)) {
                    mostrarDetalleSala(s);
                }
                break;
            }
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
        // Solo seteamos modo y navegamos a la configuración.
        // NO enviamos nada al servidor todavía.
        SessionContext.getInstance().setModoConfigActual(PartidaContext.ModoPartida.MULTIJUGADOR);
        SceneManager.getInstance().showConfigSala();
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
