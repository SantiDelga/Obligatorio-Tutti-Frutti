package uy.edu.tuttifrutti.ui.juego;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Modelo de datos para cada fila de validación.
 * Tiene propiedades JavaFX para permitir actualización en UI.
 */
public class ResultadoItem {

    private final StringProperty categoria;
    private final StringProperty respuestaUsuario;
    private final StringProperty valida; // "SI" o "NO"

    public ResultadoItem(String categoria, String respuestaUsuario, String valida) {
        this.categoria = new SimpleStringProperty(categoria);
        this.respuestaUsuario = new SimpleStringProperty(respuestaUsuario);
        this.valida = new SimpleStringProperty(valida);
    }

    public String getCategoria() {
        return categoria.get();
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria.set(categoria);
    }

    public String getRespuestaUsuario() {
        return respuestaUsuario.get();
    }

    public StringProperty respuestaUsuarioProperty() {
        return respuestaUsuario;
    }

    public void setRespuestaUsuario(String respuestaUsuario) {
        this.respuestaUsuario.set(respuestaUsuario);
    }

    public String getValida() {
        return valida.get();
    }

    public StringProperty validaProperty() {
        return valida;
    }

    public void setValida(String valida) {
        this.valida.set(valida);
    }
}

