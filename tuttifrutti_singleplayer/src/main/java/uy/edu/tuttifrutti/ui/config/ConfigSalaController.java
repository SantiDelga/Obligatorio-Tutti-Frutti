package uy.edu.tuttifrutti.ui.config;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import uy.edu.tuttifrutti.app.SceneManager;

import java.util.*;

public class ConfigSalaController {

    @FXML
    private ComboBox<Integer> comboJugadores;

    @FXML
    private ListView<String> listaJugadores;

    @FXML
    private ComboBox<Integer> comboRondas;

    @FXML
    private ComboBox<Integer> comboTiempo; // segundos

    @FXML
    private GridPane gridTemas;

    @FXML
    private Label lblTemasSeleccionados;

    @FXML
    private FlowPane flowLetras;

    @FXML
    private TextArea txtChatInfo;

    private static final int MAX_TEMAS_MOSTRAR = 20;
    private static final int MAX_TEMAS_SELECCIONADOS = 10;

    // Lista de TODOS los posibles temas; podés agregar más
    private static final List<String> TODOS_LOS_TEMAS = List.of(
            "Nombre", "Apellido", "Animal", "País", "Ciudad",
            "Color", "Fruta", "Comida", "Marca", "Deporte",
            "Película", "Serie", "Cantante", "Objeto", "Profesión",
            "Juego", "Instrumento", "Personaje", "Flor", "Bebida",
            "Superhéroe", "Libro", "Aplicación", "Lugar turístico"
    );

    // Letras del abecedario español
    private static final List<String> LETRAS_ESPANOL = List.of(
            "A","B","C","D","E","F","G","H","I","J","K","L","M",
            "N","Ñ","O","P","Q","R","S","T","U","V","W","X","Y","Z"
    );

    private final List<ToggleButton> toggleTemas = new ArrayList<>();
    private int temasSeleccionados = 0;

    private final List<ToggleButton> toggleLetras = new ArrayList<>();

    @FXML
    private void initialize() {
        inicializarCombos();
        inicializarJugadoresDummy();
        inicializarTemas();
        inicializarLetras();

        txtChatInfo.setText("Configura la sala y luego presiona EMPEZAR.\n" +
                "Máximo " + MAX_TEMAS_SELECCIONADOS + " temas.");
    }

    private void inicializarCombos() {
        comboJugadores.getItems().addAll(2,3,4,5,6,7,8,9,10);
        comboJugadores.getSelectionModel().select(Integer.valueOf(10));

        comboRondas.getItems().addAll(3,5,8,10);
        comboRondas.getSelectionModel().select(Integer.valueOf(8));

        comboTiempo.getItems().addAll(30,45,60,90,120);
        comboTiempo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + " segundos");
            }
        });
        comboTiempo.setButtonCell(comboTiempo.getCellFactory().call(null));
        comboTiempo.getSelectionModel().select(Integer.valueOf(60));
    }

    private void inicializarJugadoresDummy() {
        // Por ahora datos de ejemplo; luego se llenará con jugadores reales
        listaJugadores.getItems().clear();
        for (int i = 1; i <= 6; i++) {
            listaJugadores.getItems().add("Jugador " + i);
        }
    }

    private void inicializarTemas() {
        gridTemas.getChildren().clear();
        toggleTemas.clear();
        temasSeleccionados = 0;

        List<String> copia = new ArrayList<>(TODOS_LOS_TEMAS);
        Collections.shuffle(copia);
        List<String> temasParaMostrar = copia.subList(0,
                Math.min(MAX_TEMAS_MOSTRAR, copia.size()));

        int col = 0;
        int row = 0;

        for (String tema : temasParaMostrar) {
            ToggleButton tb = new ToggleButton(tema);
            tb.getStyleClass().add("tema-toggle");
            tb.setOnAction(e -> manejarClickTema(tb));
            gridTemas.add(tb, col, row);
            toggleTemas.add(tb);

            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }

        actualizarLabelTemasSeleccionados();
    }

    private void manejarClickTema(ToggleButton tb) {
        if (tb.isSelected()) {
            if (temasSeleccionados >= MAX_TEMAS_SELECCIONADOS) {
                // No permitir seleccionar más
                tb.setSelected(false);
                return;
            }
            temasSeleccionados++;
        } else {
            temasSeleccionados--;
        }

        // Si llegamos al máximo, deshabilitar los no seleccionados
        if (temasSeleccionados >= MAX_TEMAS_SELECCIONADOS) {
            for (ToggleButton otro : toggleTemas) {
                if (!otro.isSelected()) {
                    otro.setDisable(true);
                }
            }
        } else {
            // Volver a habilitar todos
            for (ToggleButton otro : toggleTemas) {
                otro.setDisable(false);
            }
        }

        actualizarLabelTemasSeleccionados();
    }

    private void actualizarLabelTemasSeleccionados() {
        lblTemasSeleccionados.setText(
                "Temas seleccionados: " + temasSeleccionados +
                        " / " + MAX_TEMAS_SELECCIONADOS
        );
    }

    private void inicializarLetras() {
        flowLetras.getChildren().clear();
        toggleLetras.clear();

        for (String letra : LETRAS_ESPANOL) {
            ToggleButton tb = new ToggleButton(letra);
            tb.getStyleClass().add("letra-toggle");
            tb.setSelected(true); // por defecto todas activas
            toggleLetras.add(tb);
            flowLetras.getChildren().add(tb);
        }
    }

    @FXML
    private void onEmpezar() {
        // Acá podrías construir un objeto GameConfig con:
        // - comboJugadores.getValue()
        // - comboRondas.getValue()
        // - comboTiempo.getValue()
        // - temas seleccionados
        // - letras seleccionadas
        //
        // Por ahora, simplemente mostramos algo en el chat:
        List<String> temas = new ArrayList<>();
        for (ToggleButton tb : toggleTemas) {
            if (tb.isSelected()) {
                temas.add(tb.getText());
            }
        }

        List<String> letras = new ArrayList<>();
        for (ToggleButton tb : toggleLetras) {
            if (tb.isSelected()) {
                letras.add(tb.getText());
            }
        }

        txtChatInfo.setText("Config de la sala:\n" +
                "- Jugadores máx: " + comboJugadores.getValue() + "\n" +
                "- Rondas: " + comboRondas.getValue() + "\n" +
                "- Tiempo: " + comboTiempo.getValue() + " s\n" +
                "- Temas (" + temas.size() + "): " + String.join(", ", temas) + "\n" +
                "- Letras: " + String.join(", ", letras) + "\n\n" +
                "Acá luego se puede notificar al servidor y pasar a la pantalla de juego.");
    }

    @FXML
    private void onSalir() {
        // Por ahora volvemos al menú principal
        SceneManager.getInstance().showMenuPrincipal();
    }
}
