package uy.edu.tuttifrutti.domain.juez;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;

/**
 * Estrategia de juez pensada para MULTIJUGADOR.
 *
 * - Toma todas las respuestas de todos los jugadores de la ronda.
 * - Para cada categoría:
 *      - Agrupa por palabra (normalizada).
 *      - Si la palabra es válida y solo 1 jugador la escribió → VALIDA_UNICA.
 *      - Si la palabra es válida y 2+ jugadores la escribieron → VALIDA_DUPLICADA.
 * - VACIA si no respondió nada.
 * - INVALIDA si no empieza con la letra correcta.
 *
 * Los puntajes se calculan con los valores de GameConfig:
 *  - puntosValidaUnica
 *  - puntosValidaDuplicada
 */
public class MultiPlayerJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeResult juzgar(RoundSubmission submission, GameConfig config) {
        Map<Jugador, Map<Categoria, String>> respuestas = submission.getRespuestas();

        // Mapa: jugador -> (categoria -> estado)
        Map<Jugador, Map<Categoria, JudgeResult.EstadoRespuesta>> estadosPorJugador = new HashMap<>();

        // Mapa: jugador -> puntaje total
        Map<Jugador, Integer> puntajesPorJugador = new HashMap<>();

        // Logs para trazabilidad
        List<JudgeLogEntry> logs = new ArrayList<>();

        char letra = Character.toUpperCase(submission.getLetra());

        // 1) Construimos un índice global por categoría y palabra
        //    cat -> (palabraNormalizada -> lista de jugadores que la escribieron)
        Map<Categoria, Map<String, List<Jugador>>> indicePorCategoria = new HashMap<>();

        for (Map.Entry<Jugador, Map<Categoria, String>> entryJugador : respuestas.entrySet()) {
            Jugador jugador = entryJugador.getKey();
            Map<Categoria, String> respuestasJugador = entryJugador.getValue();

            for (Map.Entry<Categoria, String> entryCat : respuestasJugador.entrySet()) {
                Categoria cat = entryCat.getKey();
                String texto = entryCat.getValue();
                if (texto == null) {
                    texto = "";
                }
                String trimmed = texto.trim();

                // Palabra vacía, igual se registra para determinar VACIA luego
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Si no arranca con la letra, igual la registramos,
                // pero se considerará INVALIDA luego para ese jugador.
                String normalizada = normalizarPalabra(trimmed);

                indicePorCategoria
                        .computeIfAbsent(cat, c -> new HashMap<>())
                        .computeIfAbsent(normalizada, p -> new ArrayList<>())
                        .add(jugador);
            }
        }

        // 2) Para cada jugador y categoría, determinamos estado y puntaje
        for (Map.Entry<Jugador, Map<Categoria, String>> entryJugador : respuestas.entrySet()) {
            Jugador jugador = entryJugador.getKey();
            Map<Categoria, String> respuestasJugador = entryJugador.getValue();

            Map<Categoria, JudgeResult.EstadoRespuesta> estadosJugador = new HashMap<>();
            int puntajeJugador = 0;

            for (Map.Entry<Categoria, String> entryCat : respuestasJugador.entrySet()) {
                Categoria cat = entryCat.getKey();
                String textoOriginal = entryCat.getValue();
                if (textoOriginal == null) {
                    textoOriginal = "";
                }
                String resp = textoOriginal.trim();

                JudgeResult.EstadoRespuesta estado;

                if (resp.isEmpty()) {
                    estado = JudgeResult.EstadoRespuesta.VACIA;
                    logs.add(new JudgeLogEntry(jugador, cat, "Respuesta vacía."));
                } else if (Character.toUpperCase(resp.charAt(0)) != letra) {
                    estado = JudgeResult.EstadoRespuesta.INVALIDA;
                    logs.add(new JudgeLogEntry(jugador, cat,
                            "Respuesta no empieza con la letra " + letra + ": " + resp));
                } else {
                    // Palabra no vacía y con letra correcta: vemos cuántos la repitieron
                    String normalizada = normalizarPalabra(resp);
                    List<Jugador> jugadoresConEsaPalabra =
                            indicePorCategoria
                                    .getOrDefault(cat, Map.of())
                                    .getOrDefault(normalizada, List.of());

                    int cantidadJugadores = jugadoresConEsaPalabra.size();

                    if (cantidadJugadores <= 1) {
                        estado = JudgeResult.EstadoRespuesta.VALIDA_UNICA;
                        puntajeJugador += config.getPuntosValidaUnica();
                        logs.add(new JudgeLogEntry(jugador, cat,
                                "Palabra válida y única: " + resp));
                    } else {
                        estado = JudgeResult.EstadoRespuesta.VALIDA_DUPLICADA;
                        puntajeJugador += config.getPuntosValidaDuplicada();
                        logs.add(new JudgeLogEntry(jugador, cat,
                                "Palabra válida pero duplicada (" + cantidadJugadores +
                                        " jugadores): " + resp));
                    }
                }

                estadosJugador.put(cat, estado);
            }

            estadosPorJugador.put(jugador, estadosJugador);
            puntajesPorJugador.put(jugador, puntajeJugador);
        }

        return new JudgeResult(puntajesPorJugador, estadosPorJugador, logs);
    }

    /**
     * Normaliza una palabra para comparar duplicados:
     * - pasa a minúsculas
     * - recorta espacios
     * - reemplaza múltiples espacios internos por uno solo
     */
    private String normalizarPalabra(String s) {
        String trimmed = s.trim().toLowerCase();
        return trimmed.replaceAll("\\s+", " ");
    }
}
