package uy.edu.tuttifrutti.infrastructure.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Random;

/**
 * Servidor de juego.
 * Mantiene una lista de salas en memoria y permite:
 *  - LIST_SALAS
 *  - CREATE_SALA
 *  - JOIN_SALA
 */
public class GameServer {

    private final int port;
    private final AtomicInteger clientIdSeq = new AtomicInteger(1);

    // Random para elegir letra de ronda
    private final Random random = new Random();


    // Puntajes acumulados por jugador (nombre → puntajeTotal)
    private final Map<String, Integer> puntajesPorJugador = new ConcurrentHashMap<>();


    // Clientes conectados (para poder hacer broadcast)
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Salas en memoria
    private final Map<String, SalaInfo> salas = new ConcurrentHashMap<>();

    public GameServer(int port) {
        this.port = port;
        inicializarSalasDummy();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[GameServer] Escuchando en puerto " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                int id = clientIdSeq.getAndIncrement();
                System.out.println("[GameServer] Nueva conexión (cliente #" + id + ") desde "
                        + socket.getInetAddress() + ":" + socket.getPort());
                ClientHandler handler = new ClientHandler(socket, this, id);
                clients.add(handler);
                Thread t = new Thread(handler, "client-" + id);
                t.start();
            }
        }
    }

    void unregisterClient(ClientHandler handler) {
        clients.remove(handler);
    }

    void logFromClient(int clientId, String message) {
        System.out.println("[GameServer] [cliente #" + clientId + "] " + message);
    }

    // ================== SALAS =====================

    private void inicializarSalasDummy() {
        crearSalaInterna("1", "Sala 001", 10, 3, 3, "a-b-c-d-e-f");
        crearSalaInterna("2", "Sala 002", 10, 1, 5, "a-b-c-d-e-f");
        crearSalaInterna("3", "Sala 003", 10, 5, 7, "a-b-c-d-e-f");
    }

    private void crearSalaInterna(String id, String nombre,
                                  int maxJug, int jugAct, int rondas,
                                  String letrasCsv) {
        SalaInfo info = new SalaInfo(id, nombre, maxJug, jugAct, rondas, letrasCsv);
        salas.put(id, info);
    }

    /** Crea una sala nueva con parámetros por defecto y devuelve su id. */
    public synchronized String createSala(String nombreSala, ClientHandler host) {
        String id = UUID.randomUUID().toString();
        int maxJugadores = 10;
        int jugadoresActuales = 0;
        int rondas = 5;
        String letrasCsv = "a-b-c-d-e-f";

        SalaInfo info = new SalaInfo(id, nombreSala, maxJugadores,
                jugadoresActuales, rondas, letrasCsv);

        // host entra en la sala
        if (host != null) {
            info.handlers.add(host);
            info.jugadoresActuales++;
        }

        salas.put(id, info);
        broadcastLobbyState();
        return id;
    }


    /** Intenta unir a un jugador a la sala indicada; true si se pudo. */
    public synchronized boolean joinSala(String idSala, ClientHandler handler) {
        SalaInfo info = salas.get(idSala);
        if (info == null) {
            return false;
        }
        if (info.jugadoresActuales >= info.maxJugadores) {
            return false;
        }
        if (!info.handlers.contains(handler)) {
            info.handlers.add(handler);
            info.jugadoresActuales++;
            broadcastLobbyState();
        }
        return true;
    }


    /** Construye el mensaje LOBBY_STATE con el formato acordado. */
    public synchronized String buildLobbyStateMessage() {
        if (salas.isEmpty()) {
            return "LOBBY_STATE|";
        }

        StringBuilder sb = new StringBuilder("LOBBY_STATE|");
        boolean first = true;
        for (SalaInfo info : salas.values()) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(info.id).append(",")
                    .append(info.nombre).append(",")
                    .append(info.maxJugadores).append(",")
                    .append(info.jugadoresActuales).append(",")
                    .append(info.rondas).append(",")
                    .append(info.letrasCsv);
        }
        return sb.toString();
    }

    /** Envía el lobby actual a todos los clientes. */
    public void broadcastLobbyState() {
        String msg = buildLobbyStateMessage();
        broadcast(msg);
    }

    public void broadcast(String msg) {
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    // Info de sala solo para el servidor
    private static class SalaInfo {
        final String id;
        final String nombre;
        final int maxJugadores;
        int jugadoresActuales;
        final int rondas;
        final String letrasCsv;

        // 👉 clientes (handlers) que están en esta sala
        final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();

        SalaInfo(String id, String nombre, int maxJugadores,
                 int jugadoresActuales, int rondas, String letrasCsv) {
            this.id = id;
            this.nombre = nombre;
            this.maxJugadores = maxJugadores;
            this.jugadoresActuales = jugadoresActuales;
            this.rondas = rondas;
            this.letrasCsv = letrasCsv;
        }
    }

    public int agregarPuntaje(String nombreJugador, int delta) {
        return puntajesPorJugador.merge(nombreJugador, delta, Integer::sum);
    }

    public int obtenerPuntaje(String nombreJugador) {
        return puntajesPorJugador.getOrDefault(nombreJugador, 0);
    }

    public void iniciarRoundEnSala(String idSala) {
        SalaInfo info = salas.get(idSala);
        if (info == null) {
            return;
        }

        char letra = (char) ('A' + random.nextInt(26));
        String msg = "ROUND_START|" + letra;

        logFromClient(0, "Iniciando ronda en sala " + idSala + " con letra " + letra);

        for (ClientHandler h : info.handlers) {
            h.send(msg);
        }
    }

    // Devuelve SCOREBOARD|Nombre1=10;Nombre2=7;...
    public synchronized String buildScoreboardMessage() {
        if (puntajesPorJugador.isEmpty()) {
            return "SCOREBOARD|";
        }

        StringBuilder sb = new StringBuilder("SCOREBOARD|");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : puntajesPorJugador.entrySet()) {
            if (!first) {
                sb.append(";");
            }
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    // Envía el scoreboard a TODOS los clientes conectados
    public void broadcastScoreboard() {
        String msg = buildScoreboardMessage();
        broadcast(msg);
    }


}
