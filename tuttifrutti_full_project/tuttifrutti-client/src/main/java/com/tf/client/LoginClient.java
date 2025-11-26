package com.tf.client;

import com.google.gson.Gson;
import com.tf.common.dto.LoginRequest;
import com.tf.common.dto.LoginResponse;

import java.io.*;
import java.net.Socket;

public class LoginClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public boolean connect() {
        try {
            socket = new Socket("localhost", 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public LoginResponse sendLogin(String playerName) {
        try {
            LoginRequest req = new LoginRequest(playerName);
            String json = gson.toJson(req);
            out.println(json);

            String response = in.readLine();

            return gson.fromJson(response, LoginResponse.class);

        } catch (Exception e) {
            return new LoginResponse(false, "Error: " + e.getMessage());
        }
    }
}
