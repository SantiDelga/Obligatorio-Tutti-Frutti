package uy.edu.tuttifrutti.app;

public class SessionContext {

    private static final SessionContext INSTANCE = new SessionContext();

    private String nombreJugadorActual;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public String getNombreJugadorActual() {
        return nombreJugadorActual;
    }

    public void setNombreJugadorActual(String nombreJugadorActual) {
        this.nombreJugadorActual = nombreJugadorActual;
    }
}
