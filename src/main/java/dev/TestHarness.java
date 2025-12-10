package dev;

import uy.edu.tuttifrutti.infrastructure.net.GameServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class TestHarness {
    public static void main(String[] args) throws Exception {
        int port = 5555;
        // Start server in background thread
        Thread serverThread = new Thread(() -> {
            GameServer server = new GameServer(port);
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "test-game-server");
        serverThread.setDaemon(true);
        serverThread.start();

        // wait a bit for server
        Thread.sleep(500);

        // Client 1 (host)
        Socket s1 = new Socket("localhost", port);
        PrintWriter out1 = new PrintWriter(new OutputStreamWriter(s1.getOutputStream()), true);
        BufferedReader in1 = new BufferedReader(new InputStreamReader(s1.getInputStream()));

        Thread reader1 = new Thread(() -> {
            try {
                String l;
                while ((l = in1.readLine()) != null) {
                    System.out.println("[C1 RECV] " + l);
                }
            } catch (Exception e) {
                System.out.println("[C1 RECV] closed: " + e.getMessage());
            }
        }, "reader-1");
        reader1.setDaemon(true);
        reader1.start();

        out1.println("HELLO|Host");
        System.out.println("c1 -> HELLO|Host");
        Thread.sleep(200);

        // create sala cfg
        out1.println("CREATE_SALA_CFG|Sala Prueba|6|5|45|Apellido,Animal|A,B,C");
        System.out.println("c1 -> CREATE_SALA_CFG|Sala Prueba|6|5|45|Apellido,Animal|A,B,C");

        // Client 2 list salas
        Socket s2 = new Socket("localhost", port);
        PrintWriter out2 = new PrintWriter(new OutputStreamWriter(s2.getOutputStream()), true);
        BufferedReader in2 = new BufferedReader(new InputStreamReader(s2.getInputStream()));

        Thread reader2 = new Thread(() -> {
            try {
                String l;
                while ((l = in2.readLine()) != null) {
                    System.out.println("[C2 RECV] " + l);
                }
            } catch (Exception e) {
                System.out.println("[C2 RECV] closed: " + e.getMessage());
            }
        }, "reader-2");
        reader2.setDaemon(true);
        reader2.start();

        out2.println("HELLO|Joiner");
        System.out.println("c2 -> HELLO|Joiner");
        Thread.sleep(200);

        out2.println("LIST_SALAS");
        System.out.println("c2 -> LIST_SALAS");

        // wait for server broadcasts
        Thread.sleep(1000);

        // Attempt to join the first sala reported via LOBBY_STATE: we don't parse it here, instead try to join a known id pattern
        // (the server returns CREATE_SALA_OK back to client1; reader1 will have printed it). We sleep and read that output from console.

        // Let the harness run a bit to capture broadcasts
        Thread.sleep(2000);

        // Close sockets
        s1.close(); s2.close();
        System.out.println("Test harness finished");
    }
}
