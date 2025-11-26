
package com.tf.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private int port = 5555;
    private boolean running = false;
    private ExecutorService pool = Executors.newCachedThreadPool();

    public void start() throws IOException {
        running = true;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);
        while (running) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client));
        }
        serverSocket.close();
    }

    public void stop() {
        running = false;
        pool.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        new GameServer().start();
    }
}
