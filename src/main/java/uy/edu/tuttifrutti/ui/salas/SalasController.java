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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

    // Guardamos el listener del lobby para poder restaurarlo
    private Consumer<String> lobbyListener;

    @FXML
    public void initialize() {
        // Registramos un listener específico para esta pantalla
        lobbyListener = msg -> {
            if (msg.startsWith("LOBBY_STATE|")) {
                Platform.runLater(() -> actualizarSalasDesdeLobby(msg));
            } else {
                System.out.println("[CLIENT] [Salas] " + msg);
            }
        };

        SessionContext.getInstance().setServerMessageListener(lobbyListener);

        btnUnirme.setDisable(true);

        // Asegurarnos de tener un MultiplayerClient conectado
        SessionContext ctx = SessionContext.getInstance();
        MultiplayerClient client = ctx.getMultiplayerClient();
        if (client == null) {
            try {
                // Reutilizamos host/port por defecto; podrías parametrizarlo si lo necesitas
                ctx.conectarMultiplayer("localhost", 55555);
                client = ctx.getMultiplayerClient();
            } catch (IOException e) {
                System.out.println("[CLIENT][Salas] No se pudo conectar al servidor: " + e.getMessage());
                salas.clear();
                dibujarSalasGrid();
                return;
            }
        }

        // Pedimos las salas reales al servidor
        if (client != null) {
            client.send("LIST_SALAS");
        }
    }


    // ================== MOCK DE SALAS ==================
    // Eliminado del flujo normal para evitar salas fantasma. Si lo necesitas para pruebas,
    // podés reactivarlo explícitamente desde código de desarrollo.
    // private void crearSalasMock() { ... }

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
            if (parts.length < 8) {
                System.out.println("[CLIENT] Sala mal formada: " + token);
                continue;
            }

            String id = parts[0];
            String nombre = parts[1];
            int maxJugadores = Integer.parseInt(parts[2]);
            int jugadoresActuales = Integer.parseInt(parts[3]);
            int rondas = Integer.parseInt(parts[4]);
            int duracion = Integer.parseInt(parts[5]);
            String temasCsv = parts[6];
            String letrasCsv = parts[7];

            List<String> letras;
            if (letrasCsv == null || letrasCsv.isBlank()) {
                letras = List.of();
            } else {
                letras = List.of(letrasCsv.split(","));
            }

            // Parse temasCsv a categorias
            List<Categoria> categorias = new ArrayList<>();
            if (temasCsv != null && !temasCsv.isBlank()) {
                String[] tks = temasCsv.split(",");
                for (String t : tks) {
                    String s = t.trim();
                    if (!s.isEmpty()) categorias.add(new Categoria(s));
                }
            }
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
            // En lugar de avanzar inmediatamente, pedimos un JOIN al servidor y esperamos SALA_CONFIG
            final String targetSalaId = salaSeleccionada.getId();

            // listener temporal que esperará SALA_CONFIG para esta sala
            final Consumer<String> tempListener = msg -> {
                if (msg.startsWith("SALA_CONFIG|")) {
                    String[] p = msg.split("\\|", -1);
                    if (p.length >= 6) {
                        String id = p[1].trim();
                        if (!id.equals(targetSalaId)) return; // no es la sala que pedimos

                        // formato: SALA_CONFIG|id|duracion|rondas|temasCsv|letrasCsv|maxJug|ptsUnica|ptsDup
                        int dur = Integer.parseInt(p[2].trim());
                        int rnd = Integer.parseInt(p[3].trim());
                        String temasCsv = p.length > 4 ? p[4].trim() : "";
                        String letrasCsv = p.length > 5 ? p[5].trim() : "";
                        String maxJugStr = p.length > 6 ? p[6].trim() : "10";
                        int maxJug = Integer.parseInt(maxJugStr);
                        String ptsUnicaStr = p.length > 7 ? p[7].trim() : "1";
                        String ptsDupStr = p.length > 8 ? p[8].trim() : "1";
                        int ptsUnica = Integer.parseInt(ptsUnicaStr);
                        int ptsDup = Integer.parseInt(ptsDupStr);

                        List<Categoria> categorias = new ArrayList<>();
                        if (!temasCsv.isBlank()) {
                            String[] tks = temasCsv.split(",");
                            for (String t : tks) {
                                String s = t.trim();
                                if (!s.isEmpty()) categorias.add(new Categoria(s));
                            }
                        }

                        GameConfig config = new GameConfig(dur, 0, categorias, ptsUnica, ptsDup);

                        final List<String> letrasFinal;
                        if (!letrasCsv.isBlank()) {
                            letrasFinal = List.of(letrasCsv.split(","));
                        } else {
                            letrasFinal = List.of();
                        }

                        // Crear PartidaContext y navegar a juego (en FX Thread)
                        Platform.runLater(() -> {
                            // Jugador local (después lo podés sacar de SessionContext)
                            Jugador jugadorLocal = new Jugador(SessionContext.getInstance().getNombreJugadorActual() == null ? "JugadorLocal" : SessionContext.getInstance().getNombreJugadorActual());
                            List<Jugador> jugadores = List.of(jugadorLocal);

                            PartidaContext partida = new PartidaContext(
                                    PartidaContext.ModoPartida.MULTIJUGADOR,
                                    config,
                                    jugadores,
                                    rnd,
                                    letrasFinal
                            );

                            SessionContext.getInstance().setPartidaActual(partida);

                            // Restaurar el listener del lobby
                            SessionContext.getInstance().setServerMessageListener(lobbyListener);

                            SceneManager.getInstance().showJuego();
                        });
                    }
                }
            };

            // Seteamos el listener temporal
            SessionContext.getInstance().setServerMessageListener(tempListener);

            // solicitamos unirse
            client.send("JOIN_SALA|" + salaSeleccionada.getId());
        }

        // No creamos todavía el PartidaContext local hasta recibir SALA_CONFIG
    }
}
