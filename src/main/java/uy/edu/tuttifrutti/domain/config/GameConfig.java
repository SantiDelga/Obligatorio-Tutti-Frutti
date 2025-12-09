package uy.edu.tuttifrutti.domain.config;

import java.util.List;
import uy.edu.tuttifrutti.domain.juego.Categoria;

public class GameConfig {

    private final int duracionSegundos;
    private final int tiempoGraciaSegundos;
    private final List<Categoria> categoriasActivas;
    private final int puntosValidaUnica;
    private final int puntosValidaDuplicada;

    public GameConfig(int duracionSegundos,
                      int tiempoGraciaSegundos,
                      List<Categoria> categoriasActivas,
                      int puntosValidaUnica,
                      int puntosValidaDuplicada) {
        this.duracionSegundos = duracionSegundos;
        this.tiempoGraciaSegundos = tiempoGraciaSegundos;
        this.categoriasActivas = List.copyOf(categoriasActivas);
        this.puntosValidaUnica = puntosValidaUnica;
        this.puntosValidaDuplicada = puntosValidaDuplicada;
    }

    public int getDuracionSegundos() {
        return duracionSegundos;
    }

    public int getTiempoGraciaSegundos() {
        return tiempoGraciaSegundos;
    }

    public List<Categoria> getCategoriasActivas() {
        return categoriasActivas;
    }

    public int getPuntosValidaUnica() {
        return puntosValidaUnica;
    }

    public int getPuntosValidaDuplicada() {
        return puntosValidaDuplicada;
    }

    public static GameConfig configDefault(List<Categoria> categorias) {
        // Cambiado: ahora por defecto las respuestas válidas dan 1 punto cada una
        return new GameConfig(60, 0, categorias, 1, 1);
    }
}
