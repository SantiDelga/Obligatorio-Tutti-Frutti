package uy.edu.tuttifrutti.domain.juez;

import org.junit.jupiter.api.Test;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SinglePlayerJudgeStrategyTest {

    @Test
    void palabraValidaSumaPuntos() {
        Categoria animal = new Categoria("Animal");
        GameConfig config = GameConfig.configDefault(List.of(animal));

        Jugador jugador = new Jugador("Test");
        Map<Jugador, Map<Categoria, String>> respuestas = Map.of(
                jugador, Map.of(animal, "Araña")
        );
        RoundSubmission submission = new RoundSubmission('A', respuestas);

        SinglePlayerJudgeStrategy strategy = new SinglePlayerJudgeStrategy();
        JudgeResult result = strategy.juzgar(submission, config);

        assertEquals(config.getPuntosValidaUnica(), result.getPuntajes().get(jugador));
        assertEquals(JudgeResult.EstadoRespuesta.VALIDA_UNICA,
                result.getEstados().get(jugador).get(animal));
    }
}
