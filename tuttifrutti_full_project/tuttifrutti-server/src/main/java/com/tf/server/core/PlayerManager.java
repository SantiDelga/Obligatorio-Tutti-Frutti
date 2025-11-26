
package com.tf.server.core;

import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private final Map<String, Socket> players = Collections.synchronizedMap(new HashMap<>());

    public synchronized boolean registerPlayer(String name, Socket socket) {
        if (name == null || name.isBlank()) return false;
        if (players.containsKey(name)) return false;
        players.put(name, socket);
        System.out.println("Player registered: " + name);
        return true;
    }

    public synchronized void unregisterPlayer(String name) {
        if (name == null) return;
        players.remove(name);
        System.out.println("Player unregistered: " + name);
    }

    public synchronized boolean isRegistered(String name) {
        return players.containsKey(name);
    }

    public synchronized int count() {
        return players.size();
    }
}
