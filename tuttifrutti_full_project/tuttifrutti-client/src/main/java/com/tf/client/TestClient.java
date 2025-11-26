package com.tf.client;

import java.io.*;
import java.net.Socket;

public class TestClient {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5555);

            System.out.println("Cliente conectado al servidor!");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Enviar mensaje al servidor
            out.println("HOLA_SERVIDOR");

            // Leer respuesta
            String respuesta = in.readLine();
            System.out.println("Respuesta del servidor: " + respuesta);

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
