
package com.tf.client.net;

import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private String host = "localhost";
    private int port = 5555;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String msg) {
        out.println(msg);
    }

    public String receive() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        ClientConnection c = new ClientConnection();
        c.connect();
        c.send("hello server");
        System.out.println(c.receive());
        c.close();
    }
}
