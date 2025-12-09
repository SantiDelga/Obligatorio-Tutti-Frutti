package uy.edu.tuttifrutti.infrastructure.ai;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;
import uy.edu.tuttifrutti.domain.juez.JudgeLogEntry;
import uy.edu.tuttifrutti.domain.juez.JudgeResult;
import uy.edu.tuttifrutti.domain.juez.JudgeStrategy;

import java.util.*;

/**
 * Estrategia de juez que delega en ChatGPT.
 */
public class OpenAiJudgeStrategy implements JudgeStrategy {

    private final OpenAiJudgeClient client = new OpenAiJudgeClient();

    @Override
    public JudgeResult juzgar(RoundSubmission submission, GameConfig config) {

        // En singleplayer solo hay un jugador
        Map<Jugador, Map<Categoria, String>> mapaRespuestas = submission.getRespuestas();
        Jugador jugador = mapaRespuestas.keySet().iterator().next();
        Map<Categoria, String> respuestasJugador = mapaRespuestas.get(jugador);

        // 👇 Llamamos a ChatGPT
        String respuestaIA = client.juzgarRonda(config, submission.getLetra(), respuestasJugador);

        // 👇 Parseamos respuesta IA para obtener puntajes y estados
        return parsear(respuestaIA, jugador, respuestasJugador);
    }


    private JudgeResult parsear(String textoIA,
                                Jugador jugador,
                                Map<Categoria, String> respuestasJugador) {

        Map<Jugador, Integer> puntajes = new HashMap<>();
        Map<Jugador, Map<Categoria, JudgeResult.EstadoRespuesta>> estados = new HashMap<>();
        List<JudgeLogEntry> logs = new ArrayList<>();

        Map<Categoria, JudgeResult.EstadoRespuesta> estadosJugador = new HashMap<>();

        String[] lineas = textoIA.split("\\n");

        for (String linea : lineas) {
            String l = linea.trim();

            if (l.startsWith("- Categoria:")) {
                // Esperamos algo parecido a:
                // - Categoria: Animal | Respuesta: Araña | Estado: SI | Motivo: ...
                try {
                    String[] partes = l.split("\\|");
                    String nombreCat = partes[0].split(":", 2)[1].trim();
                    String estadoTxt = "";
                    if (partes.length > 2) {
                        estadoTxt = partes[2].split(":", 2)[1].trim();
                    }

                    Categoria cat = respuestasJugador.keySet().stream()
                            .filter(c -> c.getNombre().equalsIgnoreCase(nombreCat))
                            .findFirst()
                            .orElse(null);

                    if (cat != null) {
                        JudgeResult.EstadoRespuesta estadoEnum;

                        String e = estadoTxt.toUpperCase();
                        if (e.equals("SI") || e.startsWith("VALIDA")) {
                            estadoEnum = JudgeResult.EstadoRespuesta.VALIDA_UNICA;
                            logs.add(new JudgeLogEntry(jugador, cat, "IA marcó SI/VALIDA"));
                        } else if (e.equals("VACIA")) {
                            estadoEnum = JudgeResult.EstadoRespuesta.VACIA;
                            logs.add(new JudgeLogEntry(jugador, cat, "IA marcó VACIA"));
                        } else {
                            estadoEnum = JudgeResult.EstadoRespuesta.INVALIDA;
                            logs.add(new JudgeLogEntry(jugador, cat, "IA marcó NO/INVALIDA"));
                        }

                        estadosJugador.put(cat, estadoEnum);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // No aplicamos fallback automático: la IA debe confirmar existencia y pertenencia. Si duda o marca NO,
        // la respuesta será considerada INVALIDA.

        // Devolvemos puntajes vacíos; la lógica de puntaje por categoría la realiza SinglePlayerGameService
        puntajes.put(jugador, 0);
        estados.put(jugador, estadosJugador);

        return new JudgeResult(puntajes, estados, logs);
    }
}
