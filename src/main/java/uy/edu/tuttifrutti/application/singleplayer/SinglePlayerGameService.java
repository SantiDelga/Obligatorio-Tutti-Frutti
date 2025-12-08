package uy.edu.tuttifrutti.application.singleplayer;

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
        int puntaje = result.getPuntajes().getOrDefault(jugador, 0);
        return new SinglePlayerRoundResult(letra, config, jugador, respuestasPorCategoria, result, puntaje);
    }

    public List<Categoria> getCategorias() {
        return config.getCategoriasActivas();
    }
}
