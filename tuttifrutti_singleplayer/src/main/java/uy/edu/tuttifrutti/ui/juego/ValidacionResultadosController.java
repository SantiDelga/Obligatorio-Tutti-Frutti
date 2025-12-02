package uy.edu.tuttifrutti.ui.juego;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class ValidacionResultadosController {

    @FXML
    private TableView<ResultadoItem> resultTable;

    @FXML
    private TableColumn<ResultadoItem, String> colCategoria;

    @FXML
    private TableColumn<ResultadoItem, String> colRespuesta;

    @FXML
    private TableColumn<ResultadoItem, String> colValida;

    private final ObservableList<ResultadoItem> data = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Mapear columnas a propiedades del modelo
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colRespuesta.setCellValueFactory(new PropertyValueFactory<>("respuestaUsuario"));
        colValida.setCellValueFactory(new PropertyValueFactory<>("valida"));

        // Forzar texto en negro en las celdas
        colCategoria.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
                }
            }
        });
        colRespuesta.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
                }
            }
        });
        colValida.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        });

        // Datos de ejemplo (placeholder)
        data.addAll(
                new ResultadoItem("Animal", "Araña", "SI"),
                new ResultadoItem("Color", "Amarillo", "SI"),
                new ResultadoItem("País", "Argentina", "SI"),
                new ResultadoItem("Fruta", "Manzana", "SI"),
                new ResultadoItem("Objeto", "Avión", "NO")
        );
        resultTable.setItems(data);
    }

    @FXML
    private void onCerrar() {
        Stage st = (Stage) resultTable.getScene().getWindow();
        st.close();
    }

    /**
     * Método público que usará el juez (AI) para actualizar las validaciones.
     * Reemplaza completamente la lista de resultados y refresca la tabla.
     * Ejemplo: AI llamará a controller.actualizarValidacion(nuevosResultados);
     */
    public void actualizarValidacion(List<ResultadoItem> nuevosResultados) {
        data.setAll(nuevosResultados);
        // La TableView observará los cambios automáticamente
    }
}
