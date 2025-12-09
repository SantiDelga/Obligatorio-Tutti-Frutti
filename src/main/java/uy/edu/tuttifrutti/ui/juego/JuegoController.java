package uy.edu.tuttifrutti.ui.juego;

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

import java.util.*;

public class JuegoController {

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
    @FXML
    private void onSalir() {
        // Cancelar timer si sigue corriendo
        if (timer != null) timer.stop();

        // Volver a la pantalla usando la instancia de SceneManager
        uy.edu.tuttifrutti.app.SceneManager.getInstance().showConfigSala(); // reemplaza showConfigSala() por el nombre real si es distinto
    }
    private final List<TextField> camposCategorias = new ArrayList<>();

    private Timeline timer;
    private int tiempoRestante;

    // Modelo de la partida
    private PartidaContext partida;
    private SinglePlayerGameService gameService;

    // Guardamos la binding para poder unbind/rebind cuando sea necesario
    private BooleanBinding todosValidosBinding;

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

        GameConfig config = partida.getGameConfig();

        // 2) Creamos un SinglePlayerGameService con el 1er jugador
        Jugador jugador = partida.getJugadores().get(0);
        gameService = new SinglePlayerGameService(jugador, config);

        // Aseguramos que el área de resultados use texto negro y la clase CSS adecuada
        try {
            if (!resultadoArea.getStyleClass().contains("result-area")) {
                resultadoArea.getStyleClass().add("result-area");
            }
            // Forzamos color en línea como respaldo
            resultadoArea.setStyle("-fx-text-fill: black; -fx-font-family: 'TTMilks';");
        } catch (Exception ex) {
            System.err.println("Warning: no se pudo setear estilo en resultadoArea: " + ex.getMessage());
        }

        // 3) Construimos dinámicamente las categorías
        construirCamposCategorias(config.getCategoriasActivas());

        // 4) Binding para habilitar / deshabilitar el botón TUTTI FRUTTI
        configurarHabilitadoTuttiFrutti();

        // 5) Arrancamos la primera ronda
        iniciarNuevaRonda();
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
            if (tuttiFruttiButton.disableProperty().isBound()) {
                tuttiFruttiButton.disableProperty().unbind();
            }
            tuttiFruttiButton.disableProperty().bind(todosValidosBinding.not());
        }
    }

    private void unbindTuttiFrutti() {
        try {
            if (tuttiFruttiButton.disableProperty().isBound()) {
                tuttiFruttiButton.disableProperty().unbind();
            }
        } catch (Exception ex) {
            // Si por alguna razón no podemos unbind, lo registramos en consola y seguimos
            System.err.println("Warning: no se pudo unbind del botón tuttiFrutti: " + ex.getMessage());
        }
    }

    // -----------------------------------------------------------------
    //                      LÓGICA DE UNA RONDA
    // -----------------------------------------------------------------
    private void iniciarNuevaRonda() {

        // Ronda actual
        rondaLabel.setText("Ronda: " + partida.getRondaActual() + "/" + partida.getRondasTotales());

        // Letra sorteada desde PartidaContext
        String letra = partida.sortearLetra();
        if (letra == null || letra.isBlank()) {
            letra = "A"; // fallback defensivo
        }
        letraLabel.setText(letra.toUpperCase());

        // Limpiar UI
        resultadoArea.clear();
        camposCategorias.forEach(tf -> tf.setText(""));

        // Timer
        tiempoRestante = partida.getGameConfig().getDuracionSegundos();
        tiempoBar.setProgress(1.0);
        iniciarTimer();

        // Restauramos binding del botón
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
        // limpiamos historial del servicio para empezar desde cero
        if (gameService != null) gameService.clearRoundHistory();
        iniciarNuevaRonda();
        // Restauramos binding y botones
        bindTuttiFrutti();
        rendirseButton.setDisable(false);
    }

    // -----------------------------------------------------------------
    //                      LÓGICA FINALIZAR RONDA
    // -----------------------------------------------------------------
    private void finalizarRonda(boolean fueTuttiFrutti) {

        Map<Categoria, String> respuestas = new HashMap<>();
        List<Categoria> cats = partida.getGameConfig().getCategoriasActivas();

        for (int i = 0; i < cats.size(); i++) {
            respuestas.put(cats.get(i), camposCategorias.get(i).getText());
        }

        int numeroRondaActual = partida.getRondaActual();

        boolean hayMasRondas = false;

        try {
            SinglePlayerRoundResult result =
                    gameService.evaluarRonda(letraLabel.getText().charAt(0), respuestas);

            // 1) Sumamos puntaje de esta ronda al acumulado de la partida
            partida.sumarPuntajeRonda(result.getPuntajeTotal());

            // 2) Avanzamos UNA vez y guardamos si hay más rondas
            hayMasRondas = partida.avanzarRonda();

            // 3) Mostrar resultado (ronda + acumulado + si es final o no)
            mostrarResultadoRonda(result, numeroRondaActual, hayMasRondas);

        } catch (Exception e) {
            e.printStackTrace();
            resultadoArea.setText(
                    "Ocurrió un error al llamar al juez IA.\n" +
                            "Revisa la consola y la configuración de la API.\n\n" +
                            "Detalle técnico: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            );
        }

        // ⛔ Deshabilito el botón después de usarlo (para evitar doble click)
        javafx.application.Platform.runLater(() -> {
            try {
                unbindTuttiFrutti();
            } catch (Exception ex) {
                System.err.println("Warning: fallo al unbind del botón tuttiFrutti en Platform.runLater: " + ex.getMessage());
            }
            // Ahora sí seteamos el estado del botón en el hilo de UI
            if (tuttiFruttiButton.disableProperty().isBound()) {
                try {
                    tuttiFruttiButton.disableProperty().unbind();
                } catch (Exception ex) {
                    System.err.println("Warning: no se pudo unbind antes de setDisable en Platform.runLater: " + ex.getMessage());
                }
            }
            tuttiFruttiButton.setDisable(true);
        });

        // Ahora usamos la variable hayMasRondas (ya establece si avanzamos) para decidir el flujo
        if (hayMasRondas) {
            // pequeña pausa opcional antes de iniciar la siguiente ronda
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        iniciarNuevaRonda();
                        // restauramos binding para que el boton vuelva a comportarse automáticamente
                        bindTuttiFrutti();
                    });
                }
            }, 800);
        }
        // 🟩 Si esta era la última ronda → mostrar el botón de reintentar y deshabilitar botones de juego
        else {
            // Última ronda: deshabilitamos botones de juego
            rendirseButton.setDisable(true);

            reintentarButton.setVisible(true);
            reintentarButton.setManaged(true);
        }
    }

    // -----------------------------------------------------------------
    //                      MOSTRAR RESULTADO
    // -----------------------------------------------------------------
    private void mostrarResultadoRonda(SinglePlayerRoundResult result,
                                       int numeroRonda,
                                       boolean hayMasRondas) {

        // Si hay más rondas, mostramos solamente el resultado de la ronda actual como antes
        if (hayMasRondas) {
            StringBuilder sb = new StringBuilder();
            sb.append("Resultados ronda ")
                    .append(numeroRonda)
                    .append(" - Letra: ")
                    .append(result.getLetra())
                    .append("\n\n");

            result.getJudgeResult().getEstados()
                    .get(gameService.getJugador())
                    .forEach((categoria, estado) -> {
                        sb.append(categoria.getNombre())
                                .append(": ")
                                .append(estado)
                                .append("\n");
                    });

            sb.append("\nPuntaje de esta ronda: ")
                    .append(result.getPuntajeTotal())
                    .append("\n");

            sb.append("Puntaje acumulado: ")
                    .append(partida.getPuntajeAcumulado())
                    .append("\n");

            sb.append("\n➡ Pasando a la siguiente ronda...\n");
            resultadoArea.setText(sb.toString());
            return;
        }

        // Si llegamos acá, la partida finalizó → mostramos resumen consolidado de todas las rondas
        List<SinglePlayerRoundResult> history = gameService.getRoundHistory();
        StringBuilder sbAll = new StringBuilder();

        int idx = 1;
        for (SinglePlayerRoundResult r : history) {
            sbAll.append("Ronda ").append(idx).append(" - Letra ").append(r.getLetra()).append("\n");

            Map<Categoria, JudgeResult.EstadoRespuesta> estados = r.getJudgeResult().getEstados().get(r.getJugador());
            Map<Categoria, Integer> puntos = r.getPuntosPorCategoria();
            Map<Categoria, String> respuestas = r.getRespuestas();

            for (Categoria cat : r.getConfig().getCategoriasActivas()) {
                String resp = respuestas.getOrDefault(cat, "");
                JudgeResult.EstadoRespuesta st = estados == null ? null : estados.get(cat);
                String valido = estadoToSiNo(st);
                int pts = puntos.getOrDefault(cat, 0);
                sbAll.append(cat.getNombre())
                        .append(": ")
                        .append(resp == null || resp.isBlank() ? "(vacío)" : resp)
                        .append(" -> ")
                        .append(valido)
                        .append(" (")
                        .append(pts)
                        .append(")\n");
            }

            sbAll.append("Puntaje: ").append(r.getPuntajeTotal()).append("\n\n");
            idx++;
        }

        sbAll.append("PUNTAJE FINAL: ").append(partida.getPuntajeAcumulado()).append("\n");
        resultadoArea.setText(sbAll.toString());
    }

    private String estadoToSiNo(JudgeResult.EstadoRespuesta estado) {
        if (estado == null) return "NO";
        switch (estado) {
            case VALIDA_UNICA:
            case VALIDA_DUPLICADA:
                return "SI";
            case VACIA:
                return "VACIA";
            case INVALIDA:
            default:
                return "NO";
        }
    }
}
