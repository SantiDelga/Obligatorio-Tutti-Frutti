package uy.edu.tuttifrutti.domain.juez;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;

public class SinglePlayerJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeResult juzgar(RoundSubmission submission, GameConfig config) {
        Map<Jugador, Map<Categoria, String>> respuestas = submission.getRespuestas();
        Map<Jugador, Integer> puntajes = new HashMap<>();
        Map<Jugador, Map<Categoria, JudgeResult.EstadoRespuesta>> estadosPorJugador = new HashMap<>();
        List<JudgeLogEntry> logs = new ArrayList<>();

        char letra = Character.toUpperCase(submission.getLetra());

        for (Map.Entry<Jugador, Map<Categoria, String>> entry : respuestas.entrySet()) {
            Jugador jugador = entry.getKey();
            Map<Categoria, String> porCategoria = entry.getValue();
            Map<Categoria, JudgeResult.EstadoRespuesta> estados = new HashMap<>();
            int puntaje = 0;

            for (Categoria cat : config.getCategoriasActivas()) {
                String texto = porCategoria.getOrDefault(cat, "").trim();
                JudgeResult.EstadoRespuesta estado;
                if (texto.isEmpty()) {
                    estado = JudgeResult.EstadoRespuesta.VACIA;
                    logs.add(new JudgeLogEntry(jugador, cat, "Campo vacío"));
                } else if (Character.toUpperCase(texto.charAt(0)) != letra) {
                    estado = JudgeResult.EstadoRespuesta.INVALIDA;
                    logs.add(new JudgeLogEntry(jugador, cat, "No empieza con la letra " + letra));
                } else {
                    // En esta implementación simple asumimos que toda palabra con la letra correcta es válida
                    estado = JudgeResult.EstadoRespuesta.VALIDA_UNICA;
                    puntaje += config.getPuntosValidaUnica();
                    logs.add(new JudgeLogEntry(jugador, cat, "Palabra válida (no se chequea diccionario en esta versión)."));
                }
                estados.put(cat, estado);
            }

            estadosPorJugador.put(jugador, estados);
            puntajes.put(jugador, puntaje);
        }

        return new JudgeResult(puntajes, estadosPorJugador, logs);
    }
}
