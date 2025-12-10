package uy.edu.tuttifrutti.ui.juego;

import javafx.application.Platform;
import uy.edu.tuttifrutti.infrastructure.net.MultiplayerClient;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import uy.edu.tuttifrutti.app.PartidaContext;
import uy.edu.tuttifrutti.app.SessionContext;
import uy.edu.tuttifrutti.application.singleplayer.SinglePlayerGameService;
import uy.edu.tuttifrutti.application.singleplayer.SinglePlayerRoundResult;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juez.JudgeResult;
import java.util.ArrayList;
import java.util.List;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JuegoController {

    private static final Logger LOGGER = Logger.getLogger(JuegoController.class.getName());

    @FXML
    private Label letraLabel;

    @FXML
    private Label rondaLabel;

    @FXML
    private ProgressBar tiempoBar;

    @FXML
    private Button tuttiFruttiButton;

    @FXML
    private Button rendirseButton;

    @FXML
    private Button reintentarButton;

    @FXML
    private GridPane categoriasGrid;

    @FXML
    private TextArea resultadoArea;

    @FXML private TextArea jugadoresArea;

    private final List<TextField> camposCategorias = new ArrayList<>();

    private Timeline timer;
    private int tiempoRestante;

    // Modelo de la partida
    private PartidaContext partida;
    private SinglePlayerGameService gameService;

    // Guardamos la binding para poder unbind/rebind cuando sea necesario
    private BooleanBinding todosValidosBinding;

    private boolean esMultijugador;
    private MultiplayerClient multiplayerClient;

    private boolean rondaEnviada = false;



    // -----------------------------------------------------------------
    //                      INIT DEL CONTROLADOR
    // -----------------------------------------------------------------
    @FXML
    public void initialize() {

        // 1) Traemos la partida desde SessionContext
        partida = SessionContext.getInstance().getPartidaActual();
        if (partida == null) {
            throw new IllegalStateException("Error: PartidaContext es null. No abriste la partida desde la Config.");
        }

        esMultijugador = (partida.getModo() == PartidaContext.ModoPartida.MULTIJUGADOR);

        GameConfig config = partida.getGameConfig();

        // 2) Creamos un SinglePlayerGameService con el 1er jugador
        Jugador jugador = partida.getJugadores().get(0);
        gameService = new SinglePlayerGameService(jugador, config);

        // 3) Construimos dinámicamente las categorías
        construirCamposCategorias(config.getCategoriasActivas());

        // 4) Binding para habilitar / deshabilitar el botón TUTTI FRUTTI
        configurarHabilitadoTuttiFrutti();

        // 5) Arrancamos la primera ronda
        if (esMultijugador) {
            configurarMultijugador();
        } else {
            iniciarNuevaRonda();
        }
    }

    // -----------------------------------------------------------------
    //             UI - GENERACIÓN DINÁMICA + VALIDACIÓN CAMPOS
    // -----------------------------------------------------------------
    private void construirCamposCategorias(List<Categoria> categorias) {
        categoriasGrid.getChildren().clear();
        camposCategorias.clear();

        for (int i = 0; i < categorias.size(); i++) {
            Categoria cat = categorias.get(i);

            Label lbl = new Label(cat.getNombre());
            lbl.getStyleClass().add("pill-label");

            TextField tf = new TextField();
            tf.setPromptText("Escribe aquí...");
            tf.getStyleClass().add("tutti-textfield");

            categoriasGrid.add(lbl, 0, i);
            categoriasGrid.add(tf, 1, i);

            camposCategorias.add(tf);
        }
    }

    private void configurarHabilitadoTuttiFrutti() {
        // guardamos la binding para poder re-utilizarla (unbind/rebind) cuando necesitemos setear manualmente
        todosValidosBinding = Bindings.createBooleanBinding(
                this::camposValidos,
                camposCategorias.stream()
                        .map(TextField::textProperty)
                        .toArray(javafx.beans.Observable[]::new)
        );

        // Aplicamos la binding al botón
        bindTuttiFrutti();
    }

    private boolean camposValidos() {
        String letraTxt = letraLabel.getText();
        if (letraTxt == null || letraTxt.isBlank()) return false;

        char inicial = Character.toUpperCase(letraTxt.charAt(0));

        for (TextField tf : camposCategorias) {
            String t = tf.getText() == null ? "" : tf.getText().trim();
            if (t.isEmpty()) return false;
            if (Character.toUpperCase(t.charAt(0)) != inicial) return false;
        }
        return true;
    }

    // Helpers para manejar con seguridad la propiedad disable del botón (que suele estar ligada)
    private void bindTuttiFrutti() {
        if (todosValidosBinding != null) {
            // asegurarnos de que no esté ligada a otra cosa
            if (tuttiFruttiButton.disableProperty().isBound()) {
                tuttiFruttiButton.disableProperty().unbind();
            }
            tuttiFruttiButton.disableProperty().bind(todosValidosBinding.not());
        }
    }

    private void unbindTuttiFrutti() {
        if (tuttiFruttiButton.disableProperty().isBound()) {
            tuttiFruttiButton.disableProperty().unbind();
        }
    }

    // -----------------------------------------------------------------
    //                      LÓGICA DE UNA RONDA
    // -----------------------------------------------------------------
    private void iniciarNuevaRonda() {
        // Sortea letra localmente
        String letra = partida.sortearLetra();
        if (letra == null || letra.isBlank()) {
            letra = "A";
        }
        prepararRondaConLetra(letra);
    }

    private void prepararRondaConLetra(String letra) {

        // Ronda actual
        rondaLabel.setText("Ronda: " + partida.getRondaActual() + "/" + partida.getRondasTotales());

        // Letra sorteada desde PartidaContext
        if (letra == null || letra.isBlank()) {
            letra = "A"; // fallback defensivo
        }
        partida.setLetraActual(letra);
        letraLabel.setText(letra.toUpperCase());

        // Limpiar UI
        resultadoArea.clear();
        // Asegurarnos que use el estilo por defecto (texto blanco sobre fondo oscuro)
        resultadoArea.getStyleClass().remove("result-area-final");
        if (!resultadoArea.getStyleClass().contains("result-area")) {
            resultadoArea.getStyleClass().add("result-area");
        }

        // 👉 Nueva ronda: habilitamos campos y limpiamos texto
        camposCategorias.forEach(tf -> {
            tf.setDisable(false);
            tf.setText("");
        });

        // 👉 Reset de botones/estado de ronda
        rondaEnviada = false;
        rendirseButton.setDisable(false);
        reintentarButton.setVisible(false);
        reintentarButton.setManaged(false);

        // Timer
        tiempoRestante = partida.getGameConfig().getDuracionSegundos();
        tiempoBar.setProgress(1.0);
        iniciarTimer();

        // Restauramos la binding del botón (si fue deshecha antes)
        bindTuttiFrutti();
    }

    private void iniciarTimer() {
        if (timer != null) timer.stop();

        int duracion = partida.getGameConfig().getDuracionSegundos();

        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    tiempoRestante--;
                    double progress = (double) tiempoRestante / duracion;
                    tiempoBar.setProgress(progress);
                    if (tiempoRestante <= 0) {
                        timer.stop();
                        finalizarRonda(false);
                    }
                })
        );
        timer.setCycleCount(duracion);
        timer.playFromStart();
    }

    private void detenerTimer() {
        try {
            if (timer != null) {
                timer.stop();
            }
        } catch (Exception ignored) {
        }
    }


    // -----------------------------------------------------------------
    //                      BOTONES JUEGO
    // -----------------------------------------------------------------
    @FXML
    private void onTuttiFrutti() {
        if (timer != null) timer.stop();
        finalizarRonda(true);
    }

    @FXML
    private void onRendirse() {
        if (timer != null) timer.stop();
        finalizarRonda(false);
    }

    @FXML
    private void onReintentar() {
        // Podrías hacer que reinicie la partida completa
        partida.reiniciar();
        iniciarNuevaRonda();
        // reestablecemos comportamiento reactivo del boton
        bindTuttiFrutti();
        rendirseButton.setDisable(false);
    }

    @FXML
    private void onSalir() {
        // Al presionar SALIR desde la cabecera: detenemos timers y volvemos a la pantalla de configuración
        if (timer != null) timer.stop();
        // Optionally clear the current partida in session (keeps user flow consistent)
        try {
            SessionContext.getInstance().setPartidaActual(null);
        } catch (Exception ignored) {
        }
        uy.edu.tuttifrutti.app.SceneManager.getInstance().showConfigSala();
    }

    // -----------------------------------------------------------------
    //                      LÓGICA FINALIZAR RONDA
    // -----------------------------------------------------------------
    private void finalizarRonda(boolean fueTuttiFrutti) {

        if (esMultijugador && multiplayerClient != null) {
            enviarRespuestasAlServidor(fueTuttiFrutti);
        }

        // Log para usar el parámetro y mejorar trazabilidad
        LOGGER.fine("finalizarRonda called, fueTuttiFrutti=" + fueTuttiFrutti);

        Map<Categoria, String> respuestas = new HashMap<>();
        List<Categoria> cats = partida.getGameConfig().getCategoriasActivas();

        for (int i = 0; i < cats.size(); i++) {
            respuestas.put(cats.get(i), camposCategorias.get(i).getText());
        }

        int numeroRondaActual = partida.getRondaActual();

        // Declaramos aquí para usar después sin volver a avanzar la ronda
        boolean hayMasRondas = false;

        try {
            SinglePlayerRoundResult result =
                    gameService.evaluarRonda(letraLabel.getText().charAt(0), respuestas);

            // 1) Sumamos puntaje de esta ronda al acumulado de la partida
            if (!esMultijugador) {
                partida.sumarPuntajeRonda(result.getPuntajeTotal());
            }

            // 2) Avanzamos UNA sola vez y guardamos si hay más rondas
            hayMasRondas = partida.avanzarRonda();

            // 3) Mostrar resultado (ronda + acumulado + si es final o no)
            mostrarResultadoRonda(result, numeroRondaActual, hayMasRondas);

            // 4) Si es la última ronda: deshabilitamos botones de juego
            if (!hayMasRondas) {
                unbindTuttiFrutti();
                tuttiFruttiButton.setDisable(true);
                rendirseButton.setDisable(true);
                // Aplicar estilo de resultado final (fondo claro + texto negro)
                resultadoArea.getStyleClass().remove("result-area");
                if (!resultadoArea.getStyleClass().contains("result-area-final")) {
                    resultadoArea.getStyleClass().add("result-area-final");
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al llamar al juez IA", e);
            resultadoArea.setText(
                    "Ocurrió un error al llamar al juez IA.\n" +
                            "Revisa la consola y la configuración de la API.\n\n" +
                            "Detalle técnico: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            );
        }

        // ⛔ Deshabilito el botón después de usarlo (para evitar doble click)
        // Si la propiedad estaba ligada, la desenganchamos antes de setear
        unbindTuttiFrutti();
        tuttiFruttiButton.setDisable(true);

        // Si hay más rondas → esperamos un momento y pasamos a la siguiente
        if (hayMasRondas) {
            if (esMultijugador && multiplayerClient != null) {
                // 👉 MODO MULTIJUGADOR:
                // Pedimos al servidor que inicie la próxima ronda para la sala actual.
                String salaId = SessionContext.getInstance().getSalaActualId();
                if (salaId == null || salaId.isBlank()) {
                    salaId = "default";
                }
                multiplayerClient.send("START_ROUND|" + salaId);
                // NO llamamos a iniciarNuevaRonda(): cuando llegue ROUND_START,
                // se ejecuta manejarRoundStart(...) y ahí se llama a prepararRondaConLetra(...)
            } else {
                // 👉 SINGLEPLAYER: comportamiento anterior
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            iniciarNuevaRonda();
                            bindTuttiFrutti();
                        });
                    }
                }, 800);
            }
        }

    }

    // -----------------------------------------------------------------
    //                      MOSTRAR RESULTADO
    // -----------------------------------------------------------------
    private void mostrarResultadoRonda(SinglePlayerRoundResult result,
                                       int numeroRonda,
                                       boolean hayMasRondas) {

        // Si NO hay más rondas -> queremos mostrar un resumen de TODAS las rondas
        if (!hayMasRondas) {
            StringBuilder sb = new StringBuilder();
            List<SinglePlayerRoundResult> history = gameService.getRoundHistory();

            for (int i = 0; i < history.size(); i++) {
                SinglePlayerRoundResult r = history.get(i);
                sb.append("Ronda ").append(i + 1)
                        .append(" - Letra: ")
                        .append(r.getLetra())
                        .append("\n\n");

                // Estados por categoría para el jugador
                Map<Categoria, JudgeResult.EstadoRespuesta> estados = r.getJudgeResult()
                        .getEstados()
                        .get(gameService.getJugador());

                Map<Categoria, Integer> puntosPorCategoria = r.getPuntosPorCategoria();

                // Mostrar cada categoría y su estado + puntos
                if (estados != null) {
                    Map<Categoria, String> respuestasRonda = r.getRespuestas();
                    for (Categoria cat : r.getConfig().getCategoriasActivas()) {
                        JudgeResult.EstadoRespuesta estado = estados.get(cat);
                        int puntos = puntosPorCategoria.getOrDefault(cat, 0);
                        String respuestaJugador = respuestasRonda == null ? "" : respuestasRonda.getOrDefault(cat, "");
                        String estadoTexto;
                        if (estado == null) {
                            estadoTexto = "Vacío";
                        } else switch (estado) {
                            case VALIDA_UNICA:
                            case VALIDA_DUPLICADA:
                                estadoTexto = "Válido";
                                break;
                            case INVALIDA:
                                estadoTexto = "No válido";
                                break;
                            case VACIA:
                                estadoTexto = "Vacío";
                                break;
                            default:
                                estadoTexto = estado.name();
                        }

                        sb.append(cat.getNombre())
                                .append(": ")
                                .append(respuestaJugador == null ? "" : respuestaJugador)
                                .append(" - ")
                                .append(estadoTexto)
                                .append(" (")
                                .append(puntos)
                                .append(")\n");
                    }
                }

                sb.append("\nPuntaje de esta ronda: ")
                        .append(r.getPuntajeTotal())
                        .append("\n\n");
            }

            sb.append("PUNTAJE FINAL: ")
                    .append(partida.getPuntajeAcumulado())
                    .append("\n");

            resultadoArea.setText(sb.toString());
            return;
        }

        // Comportamiento anterior (mientras queden rondas)
        StringBuilder sb = new StringBuilder();
        sb.append("Resultados ronda ")
                .append(numeroRonda)
                .append(" - Letra: ")
                .append(result.getLetra())
                .append("\n\n");

        Map<Categoria, JudgeResult.EstadoRespuesta> estadosActual = result.getJudgeResult().getEstados().get(gameService.getJugador());
        Map<Categoria, String> respuestasActual = result.getRespuestas();
        if (estadosActual != null) {
            for (Categoria categoria : result.getConfig().getCategoriasActivas()) {
                JudgeResult.EstadoRespuesta estado = estadosActual.get(categoria);
                String respuestaJugador = respuestasActual == null ? "" : respuestasActual.getOrDefault(categoria, "");
                String estadoTexto = estado == null ? "Vacío" :
                        (estado == JudgeResult.EstadoRespuesta.VALIDA_UNICA || estado == JudgeResult.EstadoRespuesta.VALIDA_DUPLICADA) ? "Válido"
                                : (estado == JudgeResult.EstadoRespuesta.INVALIDA ? "No válido" : "Vacío");

                sb.append(categoria.getNombre())
                        .append(": ")
                        .append(respuestaJugador == null ? "" : respuestaJugador)
                        .append(" - ")
                        .append(estadoTexto)
                        .append("\n");
            }
        }

        sb.append("\nPuntaje de esta ronda: ")
                .append(result.getPuntajeTotal())
                .append("\n");

        sb.append("Puntaje acumulado: ")
                .append(partida.getPuntajeAcumulado())
                .append("\n");

        if (hayMasRondas) {
            sb.append("\n➡ Pasando a la siguiente ronda...\n");
        } else {
            sb.append("\n✅ PARTIDA FINALIZADA\n");
            sb.append("PUNTAJE FINAL: ").append(partida.getPuntajeAcumulado()).append("\n");
        }

        resultadoArea.setText(sb.toString());
    }

    private void configurarMultijugador() {
        multiplayerClient = SessionContext.getInstance().getMultiplayerClient();
        if (multiplayerClient == null) {
            LOGGER.warning("MultiplayerClient es null; se comportará como singleplayer");
            esMultijugador = false;
            iniciarNuevaRonda();
            return;
        }

        // Listener de mensajes mientras estamos en la pantalla de juego
        SessionContext.getInstance().setServerMessageListener(msg -> {
            if (msg.startsWith("ROUND_START|")) {
                Platform.runLater(() -> manejarRoundStart(msg));
            } else if (msg.startsWith("ROUND_RESULT|")) {
                Platform.runLater(() -> manejarRoundResult(msg));
            } else if (msg.startsWith("SCOREBOARD|")) {
                Platform.runLater(() -> manejarScoreboard(msg));
            } else if (msg.startsWith("ROUND_FORCE_END|")) {
                Platform.runLater(() -> manejarRoundForceEnd(msg));
            } else if (msg.startsWith("SALA_STATE|")) {
                Platform.runLater(() -> manejarSalaState(msg));
            } else {
                LOGGER.info("[Juego] Mensaje no manejado: " + msg);
            }
        });


        // Pedimos al servidor que arranque una ronda para la sala actual
        String salaId = SessionContext.getInstance().getSalaActualId();
        if (salaId == null || salaId.isBlank()) {
            salaId = "default";
        }
        multiplayerClient.send("START_ROUND|" + salaId);
    }

    private void manejarRoundStart(String msg) {
        // ROUND_START|X
        String[] parts = msg.split("\\|", -1);
        if (parts.length < 2) return;
        String letraStr = parts[1].trim();
        if (letraStr.isEmpty()) return;
        String letra = letraStr.substring(0, 1).toUpperCase();

        prepararRondaConLetra(letra);
    }

    private void manejarRoundResult(String msg) {
        // ROUND_RESULT|textoLibre
        String texto = msg.substring("ROUND_RESULT|".length());
        if (texto == null) texto = "";
        resultadoArea.appendText("\n\n[Servidor] " + texto + "\n");
    }

    private void enviarRespuestasAlServidor(boolean fueTuttiFrutti) {
        String salaId = SessionContext.getInstance().getSalaActualId();
        if (salaId == null || salaId.isBlank()) {
            salaId = "default";
        }
        String nombre = SessionContext.getInstance().getNombreJugadorActual();
        if (nombre == null || nombre.isBlank()) {
            nombre = "Anonimo";
        }

        GameConfig config = partida.getGameConfig();
        StringBuilder sb = new StringBuilder();

        // payload = letra|cat1=resp1;cat2=resp2;...
        String letra = partida.getLetraActual();
        if (letra == null || letra.isBlank()) {
            letra = letraLabel.getText();
        }
        if (letra == null) letra = "A";

        List<Categoria> categorias = config.getCategoriasActivas();
        for (int i = 0; i < categorias.size(); i++) {
            if (i > 0) sb.append(";");
            Categoria cat = categorias.get(i);
            String resp = camposCategorias.get(i).getText();
            if (resp == null) resp = "";
            // evitamos meter | o ; en la respuesta
            resp = resp.replace("|", " ").replace(";", " ");
            sb.append(cat.getNombre()).append("=").append(resp);
        }

        String payload = sb.toString();
        String flag = fueTuttiFrutti ? "TUTTI" : "RENDIRSE";

        // NUEVO FORMATO:
        // SUBMIT_RONDA|idSala|nombre|letra|FLAG|Categoria=resp;Categoria=resp;...
        multiplayerClient.send(
                "SUBMIT_RONDA|" + salaId + "|" + nombre + "|" + letra + "|" + flag + "|" + payload
        );

        rondaEnviada = true;
    }

    private void manejarScoreboard(String msg) {
        // Formato: SCOREBOARD|Nombre1=10;Nombre2=7;...
        String data = msg.substring("SCOREBOARD|".length()).trim();
        if (data.isEmpty()) {
            return;
        }

        String[] tokens = data.split(";");
        List<String> lineas = new ArrayList<>();

        for (String token : tokens) {
            if (token.isBlank()) continue;
            String[] kv = token.split("=", 2);
            String nombre = kv[0].trim();
            int puntos = 0;
            if (kv.length > 1) {
                try {
                    puntos = Integer.parseInt(kv[1].trim());
                } catch (NumberFormatException ignored) { }
            }
            lineas.add(nombre + " - " + puntos + " pts");
        }

        // Ordenamos de mayor a menor puntaje
        lineas.sort((a, b) -> {
            int pa = extraerPuntos(a);
            int pb = extraerPuntos(b);
            return Integer.compare(pb, pa); // descendente
        });

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n[Ranking global]\n");
        int pos = 1;
        for (String linea : lineas) {
            sb.append(pos).append(". ").append(linea).append("\n");
            pos++;
        }

        resultadoArea.appendText(sb.toString());
    }

    private int extraerPuntos(String linea) {
        // espera formato "Nombre - X pts"
        int idxDash = linea.lastIndexOf('-');
        int idxPts = linea.lastIndexOf("pts");
        if (idxDash == -1 || idxPts == -1 || idxPts <= idxDash) return 0;
        String numStr = linea.substring(idxDash + 1, idxPts).trim();
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void manejarRoundForceEnd(String msg) {
        // ROUND_FORCE_END|NombreJugador
        String nombre = msg.substring("ROUND_FORCE_END|".length()).trim();
        if (nombre.isEmpty()) {
            nombre = "Otro jugador";
        }

        // Podés mostrarlo en el resultadoArea o en un label
        resultadoArea.appendText("\n\n[Tutti Frutti] La ronda fue finalizada por " + nombre + ".\n");

        // Cortamos el timer y deshabilitamos inputs
        detenerTimer();
        camposCategorias.forEach(tf -> tf.setDisable(true));
        // si tenés botón de TUTTI y rendirse:
        // btnTuttiFrutti.setDisable(true);
        // btnRendirse.setDisable(true);

        // Si ESTE cliente todavía no envió sus respuestas, las mandamos como "rendirse"
        if (!rondaEnviada) {
            finalizarRonda(false); // esto llama enviarRespuestasAlServidor(false)
        }
    }

    private void manejarSalaState(String msg) {
        // formato: SALA_STATE|idSala|Santi;Agus;Lucas
        String[] parts = msg.split("\\|", -1);
        if (parts.length < 3) return;

        String jugadoresStr = parts[2];

        String[] tokens = jugadoresStr.split(";");
        StringBuilder sb = new StringBuilder("Jugadores conectados:\n");

        for (String nombre : tokens) {
            sb.append("- ").append(nombre).append("\n");
        }

        jugadoresArea.setText(sb.toString());
    }


}
