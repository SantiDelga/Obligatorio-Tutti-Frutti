package com.tf.ui.controllers;

import com.google.gson.Gson;
import com.tf.common.dto.LoginRequest;
import com.tf.common.dto.LoginResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {

    @FXML private TextField txtName;
    @FXML private Label errorLabel;

    private final Gson gson = new Gson();

    @FXML
    public void onLoginButtonClicked() {

        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Ingrese un nombre");
            return;
        }

        errorLabel.setText("Conectando...");

        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 5555);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Enviar JSON
                LoginRequest req = new LoginRequest(name);
                out.println(gson.toJson(req));

                // Recibir respuesta
                String response = in.readLine();
                LoginResponse resp = gson.fromJson(response, LoginResponse.class);

                Platform.runLater(() -> {
                    if (resp != null && resp.success) {
                        errorLabel.setText("✔ " + resp.message);

                        // Luego pasarás al lobby aquí
                        // SceneManager.changeScene("lobby.fxml");
                    } else {
                        errorLabel.setText(resp != null ? resp.message : "Respuesta inválida");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() ->
                        errorLabel.setText("Error al conectar: " + ex.getMessage())
                );
            }
        }).start();
    }
}
