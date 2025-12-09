package uy.edu.tuttifrutti.infrastructure.net;

public class GameServerMain {

    public static void main(String[] args) {
        int port = 55555; // o leer de args/env

        GameServer server = new GameServer(port);
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
