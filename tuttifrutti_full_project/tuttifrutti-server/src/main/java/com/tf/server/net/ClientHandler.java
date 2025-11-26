
package com.tf.server.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tf.common.dto.LoginRequest;
import com.tf.common.dto.LoginResponse;
import com.tf.server.core.PlayerManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private static final Gson gson = new Gson();
    private static final PlayerManager manager = new PlayerManager();
    private String registeredName = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                // ⭐⭐⭐ AGREGAR ESTO ⭐⭐⭐
                if (line.equals("HOLA_SERVIDOR")) {
                    System.out.println("Cliente envió test básico: " + line);
                    out.println("HOLA_CLIENTE");
                    continue; // evita que pase al código JSON
                }
                // ⭐⭐⭐ FIN DEL AGREGADO ⭐⭐⭐
                try {
                    JsonObject obj = gson.fromJson(line, JsonObject.class);
                    if (obj == null) continue;
                    String action = obj.has("action") ? obj.get("action").getAsString() : null;
                    if ("LOGIN".equalsIgnoreCase(action)) {
                        LoginRequest req = gson.fromJson(line, LoginRequest.class);
                        boolean ok = manager.registerPlayer(req.playerName, socket);
                        if (ok) {
                            registeredName = req.playerName;
                            out.println(gson.toJson(new LoginResponse(true, "Welcome, " + req.playerName)));
                        } else {
                            out.println(gson.toJson(new LoginResponse(false, "Name already taken or invalid.")));
                        }
                    } else {
                        // Other actions not implemented yet - echo for now
                        out.println("ECHO: " + line);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    out.println(gson.toJson(new LoginResponse(false, "Server error: " + ex.getMessage())));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (registeredName != null) {
                manager.unregisterPlayer(registeredName);
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
