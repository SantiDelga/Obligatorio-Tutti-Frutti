package uy.edu.tuttifrutti.ui.config;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import uy.edu.tuttifrutti.app.SceneManager;
import uy.edu.tuttifrutti.app.SessionContext;
import uy.edu.tuttifrutti.app.PartidaContext;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;


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

    // altura aproximada de cada “pastilla” de jugador (ajustá si hace falta)
    private static final double ALTURA_CELDA_JUGADOR = 44;

    // Lista de TODOS los posibles temas
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
        // llena la lista según el valor seleccionado en comboJugadores
        actualizarListaJugadores(comboJugadores.getValue());

        inicializarTemas();
        inicializarLetras();

        txtChatInfo.setText("Configura la sala y luego presiona EMPEZAR.\n" +
                "Máximo " + MAX_TEMAS_SELECCIONADOS + " temas.");
    }

    private void inicializarCombos() {
        // jugadores: 2..10
        comboJugadores.getItems().addAll(2,3,4,5,6,7,8,9,10);
        comboJugadores.getSelectionModel().select(Integer.valueOf(10));

        // 🔗 cuando cambia la cantidad de jugadores, actualizamos la lista
        comboJugadores.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                actualizarListaJugadores(newVal);
            }
        });

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

    /**
     * Llena la ListView con tantos jugadores como indique "cantidad".
     * Ej: cantidad = 2 -> "Jugador 1", "Jugador 2".
     */
    private void actualizarListaJugadores(int cantidad) {
        listaJugadores.getItems().clear();
        for (int i = 1; i <= cantidad; i++) {
            listaJugadores.getItems().add("Jugador " + i);
        }

        // Opcional: ajustar la altura de la lista para que no se vean slots vacíos
        listaJugadores.setPrefHeight(cantidad * ALTURA_CELDA_JUGADOR + 2);
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
                tb.setSelected(false);
                return;
            }
            temasSeleccionados++;
        } else {
            temasSeleccionados--;
        }

        if (temasSeleccionados >= MAX_TEMAS_SELECCIONADOS) {
            for (ToggleButton otro : toggleTemas) {
                if (!otro.isSelected()) {
                    otro.setDisable(true);
                }
            }
        } else {
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
        // 1) Temas seleccionados -> Categorias
        List<String> temas = new ArrayList<>();
        for (ToggleButton tb : toggleTemas) {
            if (tb.isSelected()) {
                temas.add(tb.getText());
            }
        }

        List<Categoria> categorias = new ArrayList<>();
        for (String tema : temas) {
            categorias.add(new Categoria(tema));
        }

        // 2) Letras seleccionadas
        List<String> letras = new ArrayList<>();
        for (ToggleButton tb : toggleLetras) {
            if (tb.isSelected()) {
                letras.add(tb.getText());
            }
        }

        // 3) Crear GameConfig con lo que definiste (podés ajustar puntos si querés)
        int duracionSegundos = comboTiempo.getValue();   // ej: 60
        int tiempoGracia = 0;                            // por ahora 0 en single
        int puntosValidaUnica = 10;
        int puntosValidaDuplicada = 5;

        GameConfig config = new GameConfig(
                duracionSegundos,
                tiempoGracia,
                categorias,
                puntosValidaUnica,
                puntosValidaDuplicada
        );

        // 4) Crear lista de jugadores dummy según comboJugadores
        int cantJugadores = comboJugadores.getValue();
        List<Jugador> jugadores = new ArrayList<>();
        for (int i = 1; i <= cantJugadores; i++) {
            jugadores.add(new Jugador("Jugador " + i));
        }

        // 5) Crear PartidaContext (modo singleplayer por ahora)
        PartidaContext partida = new PartidaContext(
                PartidaContext.ModoPartida.SINGLEPLAYER,
                config,
                jugadores,
                comboRondas.getValue(),
                letras
        );

        // 6) Guardar en SessionContext
        SessionContext.getInstance().setPartidaActual(partida);

        // 7) (Opcional) mostrar resumen en el chat
        txtChatInfo.setText("Config de la sala:\n" +
                "- Jugadores: " + cantJugadores + "\n" +
                "- Rondas: " + comboRondas.getValue() + "\n" +
                "- Tiempo: " + comboTiempo.getValue() + " s\n" +
                "- Temas (" + temas.size() + "): " + String.join(", ", temas) + "\n" +
                "- Letras: " + String.join(", ", letras) + "\n\n" +
                "Partida creada. Pasando a la pantalla de juego...");

        // 8) Ir a la pantalla de juego
        SceneManager.getInstance().showJuego();
    }

    // helper chiquito para armar el texto de temas
    private List<String> temasComoString(List<Categoria> categorias) {
        List<String> nombres = new ArrayList<>();
        for (Categoria c : categorias) {
            nombres.add(c.getNombre());
        }
        return nombres;
    }


    @FXML
    private void onSalir() {
        SceneManager.getInstance().showMenuPrincipal();
    }
}
