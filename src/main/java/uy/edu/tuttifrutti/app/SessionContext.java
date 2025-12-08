package uy.edu.tuttifrutti.app;

import uy.edu.tuttifrutti.app.PartidaContext;

public class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String nombreJugadorActual;

    // 👉 Partida actual (single o multi)
    private PartidaContext partidaActual;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    // ---------- JUGADOR ACTUAL (LOGIN) ----------

    public String getNombreJugadorActual() {
        return nombreJugadorActual;
    }

    public void setNombreJugadorActual(String nombreJugadorActual) {
        this.nombreJugadorActual = nombreJugadorActual;
    }

    // ---------- PARTIDA ACTUAL ----------

    public PartidaContext getPartidaActual() {
        return partidaActual;
    }

    public void setPartidaActual(PartidaContext partidaActual) {
        this.partidaActual = partidaActual;
    }

    /** Por si querés limpiar todo al volver al menú, por ejemplo. */
    public void limpiarPartida() {
        this.partidaActual = null;
    }
}
