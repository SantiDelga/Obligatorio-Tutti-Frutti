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
        // Asegurarnos que use el estilo por defecto (texto blanco sobre fondo oscuro)
        resultadoArea.getStyleClass().remove("result-area-final");
        if (!resultadoArea.getStyleClass().contains("result-area")) {
            resultadoArea.getStyleClass().add("result-area");
        }
        camposCategorias.forEach(tf -> tf.setText(""));

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
            partida.sumarPuntajeRonda(result.getPuntajeTotal());

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
            e.printStackTrace();
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
        // 🟩 Si esta era la última ronda → mostrar el botón de reintentar
        else {
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

        if (hayMasRondas) {
            sb.append("\n➡ Pasando a la siguiente ronda...\n");
        } else {
            sb.append("\n✅ PARTIDA FINALIZADA\n");
            sb.append("PUNTAJE FINAL: ").append(partida.getPuntajeAcumulado()).append("\n");
        }

        resultadoArea.setText(sb.toString());
    }
}
