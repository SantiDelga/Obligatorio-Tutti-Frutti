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

    @FXML private Label letraLabel;
    @FXML private Label rondaLabel;
    @FXML private ProgressBar tiempoBar;
    @FXML private Button tuttiFruttiButton;
    @FXML private Button rendirseButton;
    @FXML private Button reintentarButton;
    @FXML private GridPane categoriasGrid;
    @FXML private TextArea resultadoArea;
    @FXML private TextArea jugadoresArea;

    private final List<TextField> camposCategorias = new ArrayList<>();
    private Timeline timer;
    private int tiempoRestante;

    private PartidaContext partida;
    private SinglePlayerGameService gameService;
    private BooleanBinding todosValidosBinding;

    private boolean esMultijugador;
    private MultiplayerClient multiplayerClient;
    private boolean rondaEnviada = false;

    @FXML
    public void initialize() {
        partida = SessionContext.getInstance().getPartidaActual();
        if (partida == null) throw new IllegalStateException("Error: PartidaContext es null. No abriste la partida desde la Config.");

        esMultijugador = (partida.getModo() == PartidaContext.ModoPartida.MULTIJUGADOR);
        GameConfig config = partida.getGameConfig();

        Jugador jugador = partida.getJugadores().get(0);
        gameService = new SinglePlayerGameService(jugador, config);

        construirCamposCategorias(config.getCategoriasActivas());
        configurarHabilitadoTuttiFrutti();

        if (esMultijugador) configurarMultijugador(); else iniciarNuevaRonda();
    }

    private void construirCamposCategorias(List<Categoria> categorias) {
        categoriasGrid.getChildren().clear();
        camposCategorias.clear();

        for (int i = 0; i < categorias.size(); i++) {
            Categoria cat = categorias.get(i);
            Label lbl = new Label(cat.getNombre()); lbl.getStyleClass().add("pill-label");
            TextField tf = new TextField(); tf.setPromptText("Escribe aquí..."); tf.getStyleClass().add("tutti-textfield");
            categoriasGrid.add(lbl, 0, i);
            categoriasGrid.add(tf, 1, i);
            camposCategorias.add(tf);
        }
    }

    private void configurarHabilitadoTuttiFrutti() {
        todosValidosBinding = Bindings.createBooleanBinding(
                this::camposValidos,
                camposCategorias.stream().map(TextField::textProperty).toArray(javafx.beans.Observable[]::new)
        );
        bindTuttiFrutti();
    }

    private boolean camposValidos() {
        String letraTxt = letraLabel.getText(); if (letraTxt == null || letraTxt.isBlank()) return false;
        char inicial = Character.toUpperCase(letraTxt.charAt(0));
        for (TextField tf : camposCategorias) {
            String t = tf.getText() == null ? "" : tf.getText().trim();
            if (t.isEmpty()) return false;
            if (Character.toUpperCase(t.charAt(0)) != inicial) return false;
        }
        return true;
    }

    private void bindTuttiFrutti() {
        if (todosValidosBinding != null) {
            if (tuttiFruttiButton.disableProperty().isBound()) tuttiFruttiButton.disableProperty().unbind();
            tuttiFruttiButton.disableProperty().bind(todosValidosBinding.not());
        }
    }
    private void unbindTuttiFrutti() { if (tuttiFruttiButton.disableProperty().isBound()) tuttiFruttiButton.disableProperty().unbind(); }

    private void iniciarNuevaRonda() {
        String letra = partida.sortearLetra(); if (letra == null || letra.isBlank()) letra = "A";
        prepararRondaConLetra(letra);
    }

    private void prepararRondaConLetra(String letra) {
        rondaLabel.setText("Ronda: " + partida.getRondaActual() + "/" + partida.getRondasTotales());
        if (letra == null || letra.isBlank()) letra = "A";
        partida.setLetraActual(letra);
        letraLabel.setText(letra.toUpperCase());

        resultadoArea.clear();
        resultadoArea.getStyleClass().remove("result-area-final");
        if (!resultadoArea.getStyleClass().contains("result-area")) resultadoArea.getStyleClass().add("result-area");

        camposCategorias.forEach(tf -> { tf.setDisable(false); tf.setText(""); });
        rondaEnviada = false; rendirseButton.setDisable(false); reintentarButton.setVisible(false); reintentarButton.setManaged(false);

        tiempoRestante = partida.getGameConfig().getDuracionSegundos(); tiempoBar.setProgress(1.0); iniciarTimer(); bindTuttiFrutti();
    }

    private void iniciarTimer() {
        if (timer != null) timer.stop();
        int duracion = partida.getGameConfig().getDuracionSegundos();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            tiempoRestante--; double progress = (double) tiempoRestante / duracion; tiempoBar.setProgress(progress);
            if (tiempoRestante <= 0) { timer.stop(); finalizarRonda(false); }
        }));
        timer.setCycleCount(duracion); timer.playFromStart();
    }

    private void detenerTimer() { try { if (timer != null) timer.stop(); } catch (Exception ignored) {} }

    @FXML private void onTuttiFrutti() { if (timer != null) timer.stop(); finalizarRonda(true); }
    @FXML private void onRendirse() { if (timer != null) timer.stop(); finalizarRonda(false); }
    @FXML private void onReintentar() { partida.reiniciar(); iniciarNuevaRonda(); bindTuttiFrutti(); rendirseButton.setDisable(false); }
    @FXML private void onSalir() { if (timer != null) timer.stop(); try { SessionContext.getInstance().setPartidaActual(null); } catch (Exception ignored) {} uy.edu.tuttifrutti.app.SceneManager.getInstance().showConfigSala(); }

    private void finalizarRonda(boolean fueTuttiFrutti) {
        if (esMultijugador && multiplayerClient != null) enviarRespuestasAlServidor(fueTuttiFrutti);
        LOGGER.fine("finalizarRonda called, fueTuttiFrutti=" + fueTuttiFrutti);

        Map<Categoria, String> respuestas = new HashMap<>(); List<Categoria> cats = partida.getGameConfig().getCategoriasActivas();
        for (int i = 0; i < cats.size(); i++) respuestas.put(cats.get(i), camposCategorias.get(i).getText());

        int numeroRondaActual = partida.getRondaActual(); boolean hayMasRondas = false;
        try {
            SinglePlayerRoundResult result = gameService.evaluarRonda(letraLabel.getText().charAt(0), respuestas);
            if (!esMultijugador) partida.sumarPuntajeRonda(result.getPuntajeTotal());
            hayMasRondas = partida.avanzarRonda();
            mostrarResultadoRonda(result, numeroRondaActual, hayMasRondas);
            if (!hayMasRondas) {
                unbindTuttiFrutti(); tuttiFruttiButton.setDisable(true); rendirseButton.setDisable(true);
                resultadoArea.getStyleClass().remove("result-area"); if (!resultadoArea.getStyleClass().contains("result-area-final")) resultadoArea.getStyleClass().add("result-area-final");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al llamar al juez IA", e);
            resultadoArea.setText("Ocurrió un error al llamar al juez IA.\nRevisa la consola y la configuración de la API.\n\nDetalle técnico: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        unbindTuttiFrutti(); tuttiFruttiButton.setDisable(true);

        if (hayMasRondas) {
            if (esMultijugador && multiplayerClient != null) {
                String salaId = SessionContext.getInstance().getSalaActualId(); if (salaId == null || salaId.isBlank()) salaId = "default";
                multiplayerClient.send("START_ROUND|" + salaId);
            } else {
                new Timer().schedule(new TimerTask() { public void run() { javafx.application.Platform.runLater(() -> { iniciarNuevaRonda(); bindTuttiFrutti(); }); } }, 800);
            }
        }
    }

    private void mostrarResultadoRonda(SinglePlayerRoundResult result, int numeroRonda, boolean hayMasRondas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resultados ronda ").append(numeroRonda).append(" - Letra: ").append(result.getLetra()).append("\n\n");
        Map<Categoria, JudgeResult.EstadoRespuesta> estadosActual = result.getJudgeResult().getEstados().get(gameService.getJugador());
        Map<Categoria, String> respuestasActual = result.getRespuestas();
        if (estadosActual != null) {
            for (Categoria categoria : result.getConfig().getCategoriasActivas()) {
                JudgeResult.EstadoRespuesta estado = estadosActual.get(categoria);
                String respuestaJugador = respuestasActual == null ? "" : respuestasActual.getOrDefault(categoria, "");
                String estadoTexto = estado == null ? "Vacío" : ((estado == JudgeResult.EstadoRespuesta.VALIDA_UNICA || estado == JudgeResult.EstadoRespuesta.VALIDA_DUPLICADA) ? "Válido" : (estado == JudgeResult.EstadoRespuesta.INVALIDA ? "No válido" : "Vacío"));
                sb.append(categoria.getNombre()).append(": ").append(respuestaJugador == null ? "" : respuestaJugador).append(" - ").append(estadoTexto).append("\n");
            }
        }
        sb.append("\nPuntaje de esta ronda: ").append(result.getPuntajeTotal()).append("\n");
        sb.append("Puntaje acumulado: ").append(partida.getPuntajeAcumulado()).append("\n");
        if (hayMasRondas) sb.append("\n➡ Pasando a la siguiente ronda...\n"); else { sb.append("\n✅ PARTIDA FINALIZADA\n"); sb.append("PUNTAJE FINAL: ").append(partida.getPuntajeAcumulado()).append("\n"); }
        resultadoArea.setText(sb.toString());
    }

    private void configurarMultijugador() {
        multiplayerClient = SessionContext.getInstance().getMultiplayerClient();
        if (multiplayerClient == null) { LOGGER.warning("MultiplayerClient es null; se comportará como singleplayer"); esMultijugador = false; iniciarNuevaRonda(); return; }

        SessionContext.getInstance().setServerMessageListener(msg -> {
            if (msg.startsWith("ROUND_START|")) Platform.runLater(() -> manejarRoundStart(msg));
            else if (msg.startsWith("ROUND_RESULT|")) Platform.runLater(() -> manejarRoundResult(msg));
            else if (msg.startsWith("SCOREBOARD|")) Platform.runLater(() -> manejarScoreboard(msg));
            else if (msg.startsWith("ROUND_FORCE_END|")) Platform.runLater(() -> manejarRoundForceEnd(msg));
            else if (msg.startsWith("CONFIG_SALA|")) Platform.runLater(() -> manejarConfigSala(msg));
            else if (msg.startsWith("SALA_STATE|")) Platform.runLater(() -> manejarSalaState(msg));
            else LOGGER.info("[Juego] Mensaje no manejado: " + msg);
        });

        String salaId = SessionContext.getInstance().getSalaActualId(); if (salaId == null || salaId.isBlank()) salaId = "default";
        multiplayerClient.send("START_ROUND|" + salaId);
    }

    private void manejarRoundStart(String msg) {
        String[] parts = msg.split("\\|", -1); if (parts.length < 2) return; String letraStr = parts[1].trim(); if (letraStr.isEmpty()) return; String letra = letraStr.substring(0,1).toUpperCase(); prepararRondaConLetra(letra);
    }

    private void manejarRoundResult(String msg) { String texto = msg.substring("ROUND_RESULT|".length()); if (texto == null) texto = ""; resultadoArea.appendText("\n\n[Servidor] " + texto + "\n"); }

    private void manejarConfigSala(String msg) {
        String[] parts = msg.split("\\|", -1); if (parts.length < 7) return; String id = parts[1]; String salaActual = SessionContext.getInstance().getSalaActualId(); if (salaActual == null || !salaActual.equals(id)) return;
        int rondas = partida.getRondasTotales(); int duracion = partida.getGameConfig().getDuracionSegundos(); try { rondas = Integer.parseInt(parts[3]); } catch (Exception ignored) {} try { duracion = Integer.parseInt(parts[4]); } catch (Exception ignored) {}
        String temasCsv = parts[5] == null ? "" : parts[5]; String letrasCsv = parts[6] == null ? "" : parts[6];
        List<Categoria> categorias = new ArrayList<>(); if (!temasCsv.isBlank()) for (String t : temasCsv.split(",")) if (!t.isBlank()) categorias.add(new Categoria(t.trim())); if (categorias.isEmpty()) categorias.addAll(partida.getGameConfig().getCategoriasActivas());
        GameConfig newConfig = new GameConfig(duracion, partida.getGameConfig().getTiempoGraciaSegundos(), categorias, partida.getGameConfig().getPuntosValidaUnica(), partida.getGameConfig().getPuntosValidaDuplicada());
        List<String> letras = new ArrayList<>(); if (!letrasCsv.isBlank()) for (String l : letrasCsv.split(",")) if (!l.isBlank()) letras.add(l.trim()); if (letras.isEmpty()) letras.addAll(partida.getLetrasDisponibles());
        aplicarConfigSala(newConfig, rondas, letras);
    }

    private void enviarRespuestasAlServidor(boolean fueTuttiFrutti) {
        String salaId = SessionContext.getInstance().getSalaActualId(); if (salaId == null || salaId.isBlank()) salaId = "default";
        String nombre = SessionContext.getInstance().getNombreJugadorActual(); if (nombre == null || nombre.isBlank()) nombre = "Anonimo";
        GameConfig config = partida.getGameConfig(); StringBuilder sb = new StringBuilder(); String letra = partida.getLetraActual(); if (letra == null || letra.isBlank()) letra = letraLabel.getText(); if (letra == null) letra = "A";
        List<Categoria> categorias = config.getCategoriasActivas(); for (int i = 0; i < categorias.size(); i++) { if (i>0) sb.append(";"); Categoria cat = categorias.get(i); String resp = camposCategorias.get(i).getText(); if (resp==null) resp = ""; resp = resp.replace("|"," ").replace(";"," "); sb.append(cat.getNombre()).append("=").append(resp); }
        String payload = sb.toString(); String flag = fueTuttiFrutti ? "TUTTI" : "RENDIRSE";
        if (multiplayerClient != null) multiplayerClient.send("SUBMIT_RONDA|"+salaId+"|"+nombre+"|"+letra+"|"+flag+"|"+payload); else LOGGER.info("No hay cliente multiplayer: no se envían respuestas al servidor");
        rondaEnviada = true;
    }

    private void manejarScoreboard(String msg) {
        String data = msg.substring("SCOREBOARD|".length()).trim(); if (data.isEmpty()) return; String[] tokens = data.split(";"); List<String> lineas = new ArrayList<>(); for (String token : tokens) { if (token.isBlank()) continue; String[] kv = token.split("=",2); String nombre = kv[0].trim(); int puntos=0; if (kv.length>1) { try { puntos = Integer.parseInt(kv[1].trim()); } catch (NumberFormatException ignored) {} } lineas.add(nombre+" - "+puntos+" pts"); }
        lineas.sort((a,b)->Integer.compare(extraerPuntos(b), extraerPuntos(a))); StringBuilder sb = new StringBuilder("\n\n[Ranking global]\n"); int pos=1; for (String l: lineas) { sb.append(pos++).append(". ").append(l).append("\n"); } resultadoArea.appendText(sb.toString());
    }

    private void manejarRoundForceEnd(String msg) { String nombre = msg.substring("ROUND_FORCE_END|".length()).trim(); if (nombre.isEmpty()) nombre = "Otro jugador"; resultadoArea.appendText("\n\n[Tutti Frutti] La ronda fue finalizada por "+nombre+".\n"); detenerTimer(); camposCategorias.forEach(tf->tf.setDisable(true)); if (!rondaEnviada) finalizarRonda(false); }

    private void manejarSalaState(String msg) {
        String[] parts = msg.split("\\|", -1); if (parts.length<3) return; String jugadoresStr = parts[2]; String[] toks = jugadoresStr.split(";"); StringBuilder sb = new StringBuilder("Jugadores conectados:\n"); for (String n: toks) { if (n==null||n.isBlank()) continue; sb.append("- ").append(n).append("\n"); } jugadoresArea.setText(sb.toString());
        if (parts.length>=8) {
            int rondas = partida.getRondasTotales(); int dur = partida.getGameConfig().getDuracionSegundos(); try { rondas = Integer.parseInt(parts[4]); } catch (Exception ignored) {} try { dur = Integer.parseInt(parts[5]); } catch (Exception ignored) {}
            String temasCsv = parts[6]==null?"":parts[6]; String letrasCsv = parts[7]==null?"":parts[7]; List<Categoria> categorias = new ArrayList<>(); if (!temasCsv.isBlank()) for (String t: temasCsv.split(",")) if (!t.isBlank()) categorias.add(new Categoria(t.trim())); if (categorias.isEmpty()) categorias.addAll(partida.getGameConfig().getCategoriasActivas());
            GameConfig newConfig = new GameConfig(dur, partida.getGameConfig().getTiempoGraciaSegundos(), categorias, partida.getGameConfig().getPuntosValidaUnica(), partida.getGameConfig().getPuntosValidaDuplicada()); List<String> letras = new ArrayList<>(); if (!letrasCsv.isBlank()) for (String l: letrasCsv.split(",")) if (!l.isBlank()) letras.add(l.trim()); if (letras.isEmpty()) letras.addAll(partida.getLetrasDisponibles()); aplicarConfigSala(newConfig, rondas, letras);
        }
    }

    private void aplicarConfigSala(GameConfig newConfig, int nuevasRondas, List<String> nuevasLetras) {
        PartidaContext ctxPartida = SessionContext.getInstance().getPartidaActual(); if (ctxPartida==null) return;
        PartidaContext nueva = new PartidaContext(ctxPartida.getModo(), newConfig, ctxPartida.getJugadores(), nuevasRondas, nuevasLetras);
        SessionContext.getInstance().setPartidaActual(nueva); this.partida = nueva;
        Platform.runLater(() -> { construirCamposCategorias(newConfig.getCategoriasActivas()); rondaLabel.setText("Ronda: " + nueva.getRondaActual() + "/" + nueva.getRondasTotales()); if (nueva.getLetraActual()!=null) letraLabel.setText(nueva.getLetraActual()); });
    }

    private int extraerPuntos(String linea) { int idxDash = linea.lastIndexOf('-'); int idxPts = linea.lastIndexOf("pts"); if (idxDash==-1||idxPts==-1||idxPts<=idxDash) return 0; String numStr = linea.substring(idxDash+1, idxPts).trim(); try { return Integer.parseInt(numStr); } catch (NumberFormatException e) { return 0; } }

}
