package uy.edu.tuttifrutti.infrastructure.net;

import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uy.edu.tuttifrutti.domain.config.GameConfig;
import uy.edu.tuttifrutti.domain.juego.Categoria;
import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.domain.juego.RoundSubmission;
import uy.edu.tuttifrutti.domain.juez.JudgeResult;
import uy.edu.tuttifrutti.domain.juez.MultiPlayerJudgeStrategy;
import uy.edu.tuttifrutti.domain.juez.SinglePlayerJudgeStrategy;


/**
 * Maneja un cliente conectado al GameServer.
 * Comandos soportados:
 *  - HELLO|nombre
 *  - LIST_SALAS
 *  - CREATE_SALA|nombreSala
 *  - JOIN_SALA|idSala
 */
public class ClientHandler implements Runnable {

    private String salaActualId;
    private final Socket socket;
    private final GameServer server;
    private final int clientId;

    private PrintWriter out;
    private BufferedReader in;

    private String nombreJugador = "desconocido";

    private final Random random = new Random();

    public ClientHandler(Socket socket, GameServer server, int clientId) {
        this.socket = socket;
        this.server = server;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true);

            server.logFromClient(clientId, "Handler iniciado");

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }

        } catch (IOException e) {
            server.logFromClient(clientId, "IOException: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            server.unregisterClient(this);
            server.logFromClient(clientId, "Conexión cerrada");
        }
    }

    private void handleLine(String line) {
        server.logFromClient(clientId, "Recibido: " + line);
        String[] parts = line.split("\\|", -1);
        String tipo = parts[0].trim().toUpperCase();

        switch (tipo) {
            case "HELLO" -> handleHello(parts);
            case "LIST_SALAS" -> handleListSalas();
            case "CREATE_SALA" -> handleCreateSala(parts);
            case "CREATE_SALA_CFG" -> handleCreateSalaCfg(parts);
            case "JOIN_SALA" -> handleJoinSala(parts);
            case "LEAVE_SALA" -> handleLeaveSala(parts); // nuevo comando
            case "START_ROUND" -> handleStartRound(parts);
            case "SUBMIT_RONDA" -> handleSubmitRonda(parts);
            default -> send("ERROR|Comando no reconocido: " + tipo);
        }
    }

    private void handleHello(String[] parts) {
        if (parts.length < 2) {
            send("ERROR|HELLO requiere el nombre del jugador");
            return;
        }
        this.nombreJugador = parts[1].trim();
        server.logFromClient(clientId, "Jugador se identificó como: " + nombreJugador);
        send("WELCOME|" + nombreJugador);
    }

    private void handleListSalas() {
        String lobbyMsg = server.buildLobbyStateMessage();
        send(lobbyMsg);
    }

    private void handleCreateSala(String[] parts) {
        String nombreSala;
        if (parts.length >= 2 && !parts[1].isBlank()) {
            nombreSala = parts[1].trim();
        } else {
            nombreSala = "Sala de " + nombreJugador;
        }
        // ahora el server recibe también el handler (this)
        String id = server.createSala(nombreSala, this);
        this.salaActualId = id;
        send("CREATE_SALA_OK|" + id);
        server.broadcastSalaState(id);
    }

    private void handleJoinSala(String[] parts) {
        if (parts.length < 2) {
            send("ERROR|JOIN_SALA requiere idSala");
            return;
        }
        String idSala = parts[1].trim();
        boolean ok = server.joinSala(idSala, this);
        if (ok) {
            this.salaActualId = idSala;
            send("JOIN_SALA_OK|" + idSala);
            server.broadcastSalaState(idSala);
        } else {
            send("ERROR|No se pudo unir a la sala " + idSala);
        }
    }


    private void handleStartRound(String[] parts) {
        // START_ROUND|idSala   (o sin idSala → usa salaActualId)
        String idSala;

        if (parts.length >= 2 && !parts[1].isBlank()) {
            idSala = parts[1].trim();
        } else {
            idSala = this.salaActualId;
        }

        if (idSala == null || idSala.isBlank()) {
            send("ERROR|START_ROUND sin sala asociada");
            return;
        }

        server.iniciarRoundEnSala(idSala);
    }


    private void handleSubmitRonda(String[] parts) {
        // NUEVO FORMATO:
        // SUBMIT_RONDA|idSala|nombreJugador|letra|FLAG|Categoria1=resp1;Categoria2=resp2;...
        if (parts.length < 6) {
            send("ERROR|SUBMIT_RONDA formato inválido");
            return;
        }

        String idSala = parts[1].trim();
        String nombreJugador = parts[2].trim();
        String letraStr = parts[3].trim();
        String flagStr = parts[4].trim();
        String respuestasStr = parts[5]; // puede contener ';' y '='

        if (letraStr.isEmpty()) {
            send("ERROR|SUBMIT_RONDA sin letra");
            return;
        }
        char letra = Character.toUpperCase(letraStr.charAt(0));

        boolean cerrarYa = "TUTTI".equalsIgnoreCase(flagStr) || "1".equals(flagStr);

        server.logFromClient(clientId,
                "Recibidas respuestas de " + nombreJugador + " en sala " + idSala +
                        " con letra " + letra + " (cerrarYa=" + cerrarYa + ")");

        // Parseamos respuestas: Categoria=texto;Categoria=texto;...
        String[] pares = respuestasStr.split(";");
        List<Categoria> categorias = new ArrayList<>();
        Map<Categoria, String> respuestasPorCategoria = new HashMap<>();

        for (String par : pares) {
            if (par.isBlank()) continue;
            String[] kv = par.split("=", 2);
            String nombreCat = kv[0].trim();
            String texto = kv.length > 1 ? kv[1].trim() : "";

            Categoria cat = new Categoria(nombreCat);
            categorias.add(cat);
            respuestasPorCategoria.put(cat, texto);
        }

        GameConfig config = GameConfig.configDefault(categorias);

        Jugador jugador = new Jugador(nombreJugador);
        Map<Jugador, Map<Categoria, String>> mapaRespuestas = new HashMap<>();
        mapaRespuestas.put(jugador, respuestasPorCategoria);

        RoundSubmission submission = new RoundSubmission(letra, mapaRespuestas);

        MultiPlayerJudgeStrategy judge = new MultiPlayerJudgeStrategy();
        JudgeResult result = judge.juzgar(submission, config);

        int puntajeRonda = result.getPuntajes().getOrDefault(jugador, 0);
        int puntajeTotal = server.agregarPuntaje(nombreJugador, puntajeRonda);

        server.broadcastScoreboard();

        StringBuilder sb = new StringBuilder();
        sb.append("Jugador: ").append(nombreJugador).append("\\n");
        sb.append("Puntaje de la ronda: ").append(puntajeRonda).append("\\n");
        sb.append("Puntaje total acumulado: ").append(puntajeTotal).append("\\n\\n");

        result.getEstados().getOrDefault(jugador, Map.of()).forEach((cat, estado) -> {
            sb.append("- ").append(cat.getNombre())
                    .append(": ").append(estado.name()).append("\\n");
        });

        String resumen = sb.toString();
        send("ROUND_RESULT|" + resumen);

        // 👇 SI ESTE SUBMIT VIENE DE UN TUTTI, AVISAMOS A TODOS LOS CLIENTES
        if (cerrarYa) {
            server.broadcast("ROUND_FORCE_END|" + nombreJugador);
        }
    }

    void send(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public String getNombreJugador() {
        return nombreJugador;
    }

    private void handleCreateSalaCfg(String[] parts) {
        // CREATE_SALA_CFG|nombreSala|maxJug|rondas|duracionSegundos|temasCsv|letrasCsv
        if (parts.length < 7) {
            send("ERROR|CREATE_SALA_CFG formato inválido");
            return;
        }

        String nombreSala = parts[1].trim();
        int maxJug;
        int rondas;
        int duracion;
        try {
            maxJug   = Integer.parseInt(parts[2].trim());
            rondas   = Integer.parseInt(parts[3].trim());
            duracion = Integer.parseInt(parts[4].trim());
        } catch (NumberFormatException e) {
            send("ERROR|CREATE_SALA_CFG parámetros numéricos inválidos");
            return;
        }

        String temasCsv   = parts[5].trim(); // ej: "Apellido,Animal"
        String letrasCsv  = parts[6].trim(); // ej: "A,B,C,D"

        String id = server.createSalaCfg(
                nombreSala,
                maxJug,
                rondas,
                duracion,
                temasCsv,
                letrasCsv,
                this
        );

        this.salaActualId = id;
        send("CREATE_SALA_OK|" + id);
    }

    private void handleLeaveSala(String[] parts) {
        // LEAVE_SALA|idSala (id opcional, normalmente usamos salaActualId)
        String idSala = (parts.length >= 2 && !parts[1].isBlank())
                ? parts[1].trim()
                : this.salaActualId;

        if (idSala == null || idSala.isBlank()) {
            send("ERROR|LEAVE_SALA sin sala asociada");
            return;
        }

        server.logFromClient(clientId, "Cliente solicita abandonar sala " + idSala);
        server.leaveSala(idSala, this);
        this.salaActualId = null;
        send("LEAVE_SALA_OK|" + idSala);
    }

}
