package uy.edu.tuttifrutti.application.singleplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;
import uy.edu.tuttifrutti.domain.juez.JudgeResult;
import uy.edu.tuttifrutti.domain.juez.JudgeStrategy;
import uy.edu.tuttifrutti.infrastructure.ai.OpenAiJudgeStrategy;

public class SinglePlayerGameService {

    private final GameConfig config;
    private final JudgeStrategy judgeStrategy;
    private final Jugador jugador;

    // historial de resultados por ronda
    private final List<SinglePlayerRoundResult> roundHistory = new ArrayList<>();

    public SinglePlayerGameService(Jugador jugador, GameConfig config) {
        this.jugador = jugador;
        this.config = config;
        // ✅ ahora usamos la estrategia que llama a OpenAI
        this.judgeStrategy = new OpenAiJudgeStrategy();
        // si quisieras volver al juez local:
        // this.judgeStrategy = new SinglePlayerJudgeStrategy();
    }

    public GameConfig getConfig() {
        return config;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public SinglePlayerRoundResult evaluarRonda(char letra, Map<Categoria, String> respuestasPorCategoria) {
        Map<Jugador, Map<Categoria, String>> map = new HashMap<>();
        map.put(jugador, respuestasPorCategoria);
        RoundSubmission submission = new RoundSubmission(letra, map);
        JudgeResult result = judgeStrategy.juzgar(submission, config);

        // calculamos puntaje por categoría según los estados devueltos por el juez
        Map<Categoria, JudgeResult.EstadoRespuesta> estadosJugador = result.getEstados().get(jugador);
        Map<Categoria, Integer> puntosPorCategoria = new HashMap<>();
        int puntajeTotal = 0;

        if (estadosJugador != null) {
            for (Categoria cat : config.getCategoriasActivas()) {
                JudgeResult.EstadoRespuesta estado = estadosJugador.get(cat);
                int puntos = 0;
                if (estado != null) {
                    switch (estado) {
                        case VALIDA_UNICA:
                            puntos = config.getPuntosValidaUnica();
                            break;
                        case VALIDA_DUPLICADA:
                            puntos = config.getPuntosValidaDuplicada();
                            break;
                        default:
                            puntos = 0;
                    }
                }
                puntosPorCategoria.put(cat, puntos);
                puntajeTotal += puntos;
            }
        }

        SinglePlayerRoundResult roundResult = new SinglePlayerRoundResult(letra, config, jugador, respuestasPorCategoria, result, puntajeTotal, puntosPorCategoria);
        // guardamos en el historial
        roundHistory.add(roundResult);
        return roundResult;
    }

    public List<Categoria> getCategorias() {
        return config.getCategoriasActivas();
    }

    public List<SinglePlayerRoundResult> getRoundHistory() {
        return List.copyOf(roundHistory);
    }

    public void clearRoundHistory() {
        roundHistory.clear();
    }
}
