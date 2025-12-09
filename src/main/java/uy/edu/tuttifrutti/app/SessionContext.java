package uy.edu.tuttifrutti.app;

import uy.edu.tuttifrutti.domain.juego.Jugador;
import uy.edu.tuttifrutti.infrastructure.net.MultiplayerClient;

import java.io.IOException;
import java.util.function.Consumer;

public class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String nombreJugadorActual;

    private String salaActualId;

    public String getSalaActualId() {
        return salaActualId;
    }

    public void setSalaActualId(String salaActualId) {
        this.salaActualId = salaActualId;
    }


    // 👉 Partida actual (single o multi)
    private PartidaContext partidaActual;

    // 👉 Cliente TCP para multijugador
    private MultiplayerClient multiplayerClient;

    // Listener actual de mensajes del servidor (lo cambia cada pantalla si quiere)
    private Consumer<String> serverMessageListener;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    // -------------------------
    // Nombre del jugador
    // -------------------------
    public String getNombreJugadorActual() {
        return nombreJugadorActual;
    }

    public void setNombreJugadorActual(String nombreJugadorActual) {
        this.nombreJugadorActual = nombreJugadorActual;
    }

    // -------------------------
    // Partida actual
    // -------------------------
    public PartidaContext getPartidaActual() {
        return partidaActual;
    }

    public void setPartidaActual(PartidaContext partidaActual) {
        this.partidaActual = partidaActual;
    }

    public void limpiarPartida() {
        this.partidaActual = null;
    }

    // -------------------------
    // MultiplayerClient
    // -------------------------
    public MultiplayerClient getMultiplayerClient() {
        return multiplayerClient;
    }

    public void setMultiplayerClient(MultiplayerClient multiplayerClient) {
        this.multiplayerClient = multiplayerClient;
    }

    /**
     * Crea y conecta el cliente multiplayer.
     *
     * @param host      host del servidor (ej: "localhost")
     * @param port      puerto del servidor (ej: 55555)
     */
    public void conectarMultiplayer(String host, int port) throws IOException {
        this.multiplayerClient = new MultiplayerClient(host, port, this::onServerMessage);
    }

    public void setServerMessageListener(Consumer<String> listener) {
        this.serverMessageListener = listener;
    }

    private void onServerMessage(String msg) {
        if (serverMessageListener != null) {
            serverMessageListener.accept(msg);
        } else {
            System.out.println("[CLIENT] (sin listener) " + msg);
        }
    }
}
