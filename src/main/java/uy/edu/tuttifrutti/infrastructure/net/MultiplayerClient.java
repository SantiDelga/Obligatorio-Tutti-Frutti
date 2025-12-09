package uy.edu.tuttifrutti.infrastructure.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Cliente TCP para conectarse al GameServer.
 * Mantiene un hilo que escucha mensajes del servidor.
 */
public class MultiplayerClient {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    // callback para notificar mensajes al código de UI (JavaFX)
    private final Consumer<String> messageListener;

    public MultiplayerClient(String host, int port, Consumer<String> messageListener) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                true);
        this.messageListener = messageListener;

        Thread t = new Thread(this::listenLoop, "multi-client-listener");
        t.setDaemon(true);
        t.start();
    }

    private void listenLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (messageListener != null) {
                    messageListener.accept(line);
                }
            }
        } catch (IOException e) {
            if (messageListener != null) {
                messageListener.accept("ERROR|Conexión cerrada: " + e.getMessage());
            }
        }
    }

    public void send(String msg) {
        out.println(msg);
    }

    public void close() throws IOException {
        socket.close();
    }
}
