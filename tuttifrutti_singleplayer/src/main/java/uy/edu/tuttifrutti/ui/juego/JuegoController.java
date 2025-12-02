package uy.edu.tuttifrutti.ui.juego;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import uy.edu.tuttifrutti.app.SessionContext;
import uy.edu.tuttifrutti.application.singleplayer.SinglePlayerGameService;
import uy.edu.tuttifrutti.application.singleplayer.SinglePlayerRoundResult;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JuegoController {

    @FXML
    private Label letraLabel;

    @FXML
    private ProgressBar tiempoBar;

    @FXML
    private Button tuttiFruttiButton;

    @FXML
    private Button rendirseButton;

    @FXML
    private GridPane categoriasGrid;

    @FXML
    private TableView<ResultadoItem> resultTable;

    @FXML
    private TableColumn<ResultadoItem,String> colCategoriaRight;

    @FXML
    private TableColumn<ResultadoItem,String> colRespuestaRight;

    @FXML
    private TableColumn<ResultadoItem,String> colValidaRight;

    private final ObservableList<TextField> camposCategorias = FXCollections.observableArrayList();
    private final ObservableList<ResultadoItem> resultadosData = FXCollections.observableArrayList();

    private Timeline timeline;
    private int duracionSegundos;
    private int tiempoRestante;

    private SinglePlayerGameService gameService;
    private char letraActual;

    @FXML
    private void initialize() {
        // Config por defecto: 60s, sin gracia, 10 puntos por válida, 5 por duplicada (no aplica en single)
        List<Categoria> categorias = List.of(
                new Categoria("Animal"),
                new Categoria("País"),
                new Categoria("Color"),
                new Categoria("Fruta"),
                new Categoria("Objeto")
        );
        GameConfig config = GameConfig.configDefault(categorias);

        String nombre = SessionContext.getInstance().getNombreJugadorActual();
        if (nombre == null || nombre.isBlank()) {
            nombre = "Jugador 1";
        }
        Jugador jugador = new Jugador(nombre);
        gameService = new SinglePlayerGameService(jugador, config);

        construirCamposCategorias(config.getCategoriasActivas());

        BooleanBinding todosValidos = Bindings.createBooleanBinding(
                this::camposValidos,
                camposCategorias.stream().map(TextField::textProperty).toArray(observable -> new javafx.beans.Observable[observable])
        );
        tuttiFruttiButton.disableProperty().bind(todosValidos.not());

        // Inicializar tabla de resultados en la derecha con placeholders
        colCategoriaRight.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("categoria"));
        colRespuestaRight.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("respuestaUsuario"));
        colValidaRight.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("valida"));

        // Forzar texto negro y legible en la tabla derecha
        colCategoriaRight.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-size: 12px;");
                }
            }
        });
        colRespuestaRight.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-size: 12px;");
                }
            }
        });
        colValidaRight.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        });

        // Placeholder inicial: los mismos nombres de categorías con respuesta vacía y valida placeholder
        resultadosData.addAll(
                new ResultadoItem("Animal", "", "SI"),
                new ResultadoItem("Color", "", "SI"),
                new ResultadoItem("País", "", "SI"),
                new ResultadoItem("Fruta", "", "SI"),
                new ResultadoItem("Objeto", "", "SI")
        );
        resultTable.setItems(resultadosData);

        iniciarNuevaRonda();
    }

    private void construirCamposCategorias(List<Categoria> categorias) {
        categoriasGrid.getChildren().clear();
        camposCategorias.clear();
        int row = 0;
        for (Categoria categoria : categorias) {
            Label label = new Label(categoria.getNombre());
            label.getStyleClass().add("pill-label");
            TextField tf = new TextField();
            tf.setPromptText("Escribe aquí");
            tf.getStyleClass().add("tutti-textfield");

            categoriasGrid.add(label, 0, row);
            categoriasGrid.add(tf, 1, row);

            camposCategorias.add(tf);
            row++;
        }
    }

    private void iniciarNuevaRonda() {
        // Letra random de A-Z
        letraActual = (char) ('A' + new Random().nextInt(26));
        letraLabel.setText(String.valueOf(letraActual));

        camposCategorias.forEach(tf -> tf.setText(""));
        // Actualizar tabla derecha con las respuestas vacías
        for (int i = 0; i < resultadosData.size(); i++) {
            resultadosData.get(i).setRespuestaUsuario("");
            resultadosData.get(i).setValida("SI"); // placeholder
        }

        duracionSegundos = gameService.getConfig().getDuracionSegundos();
        tiempoRestante = duracionSegundos;
        tiempoBar.setProgress(1.0);

        iniciarTimer();
    }

    private void iniciarTimer() {
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    tiempoRestante--;
                    double progress = (double) tiempoRestante / duracionSegundos;
                    tiempoBar.setProgress(progress);
                    if (tiempoRestante <= 0) {
                        timeline.stop();
                        onTiempoTerminado();
                    }
                })
        );
        timeline.setCycleCount(duracionSegundos);
        timeline.playFromStart();
    }

    private boolean camposValidos() {
        if (letraLabel.getText() == null || letraLabel.getText().isBlank()) {
            return false;
        }
        char inicial = Character.toUpperCase(letraLabel.getText().charAt(0));
        for (TextField tf : camposCategorias) {
            String t = tf.getText().trim();
            if (t.isEmpty()) {
                return false;
            }
            if (Character.toUpperCase(t.charAt(0)) != inicial) {
                return false;
            }
        }
        return true;
    }

    private void onTiempoTerminado() {
        enviarRespuestasYMostrarResultado(false);
    }

    @FXML
    private void onTuttiFrutti() {
        if (timeline != null) {
            timeline.stop();
        }
        // Generar resultados y actualizar la tabla lateral de resultados (sin ventana emergente)
        List<Categoria> categorias = gameService.getConfig().getCategoriasActivas();
        List<ResultadoItem> resultados = new java.util.ArrayList<>();
        for (int i = 0; i < categorias.size(); i++) {
            String cat = categorias.get(i).getNombre();
            String resp = camposCategorias.get(i).getText();
            String valida = "SI"; // placeholder; el juez AI actualizará este campo posteriormente
            resultados.add(new ResultadoItem(cat, resp, valida));
        }
        // Reemplaza los datos en la tabla lateral
        resultadosData.setAll(resultados);
    }

    /**
     * Método público que permitirá al juez AI actualizar las validaciones en la tabla lateral.
     * Reemplaza la lista completa de resultados.
     */
    public void actualizarValidacion(List<ResultadoItem> nuevos) {
        resultadosData.setAll(nuevos);
    }

    @FXML
    private void onRendirse() {
        if (timeline != null) {
            timeline.stop();
        }
        enviarRespuestasYMostrarResultado(false);
    }

    private void enviarRespuestasYMostrarResultado(boolean esTuttiFrutti) {
        // Mantenemos este método por compatibilidad interna si se invoca desde otro lugar
        String resultados = generarResultadosTexto(esTuttiFrutti);
        //resultadoArea.setText(resultados);
    }

    private String generarResultadosTexto(boolean esTuttiFrutti) {
        Map<Categoria, String> respuestas = new HashMap<>();
        List<Categoria> categorias = gameService.getConfig().getCategoriasActivas();
        for (int i = 0; i < categorias.size(); i++) {
            Categoria c = categorias.get(i);
            String texto = camposCategorias.get(i).getText();
            respuestas.put(c, texto);
        }

        SinglePlayerRoundResult result = gameService.evaluarRonda(letraActual, respuestas);

        StringBuilder sb = new StringBuilder();
        sb.append("Resultados para ").append(gameService.getJugador().getNombre())
                .append(" (Letra: ").append(letraActual).append(")\n\n");

        result.getJudgeResult().getEstados().get(gameService.getJugador()).forEach((cat, estado) -> {
            sb.append(cat.getNombre()).append(": ").append(estado).append("\n");
        });

        sb.append("\nPuntaje total: ").append(result.getPuntajeTotal()).append("\n");
        if (!esTuttiFrutti) {
            sb.append("(La ronda terminó por tiempo o rendición)\n");
        }

        return sb.toString();
    }


    @FXML
    private void onReintentar() {
        iniciarNuevaRonda();
    }
}
