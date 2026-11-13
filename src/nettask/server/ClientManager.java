package nettask.server;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {

    // Store connected clients and their usernames
    private final Map<Socket, String> connectedClients =
            new ConcurrentHashMap<>();

    // Store output streams for sending messages
    private final Map<Socket, PrintWriter> clientWriters =
            new ConcurrentHashMap<>();

    // Register a client only if the username is available
    public synchronized boolean addClient(
            Socket socket, String username, PrintWriter writer) {

        for (String existingUsername : connectedClients.values()) {

            if (existingUsername.equalsIgnoreCase(username)) {
                System.out.println(
                        "Username already in use: " + username);
                return false;
            }
        }

        connectedClients.put(socket, username);
        clientWriters.put(socket, writer);

        System.out.println("Client registered: " + username);
        System.out.println("Total connected clients: "
                + getConnectedClientCount());

        return true;
    }

    // Remove a disconnected client
    public synchronized String removeClient(Socket socket) {

        String username = connectedClients.remove(socket);
        clientWriters.remove(socket);

        if (username != null) {
            System.out.println("Client disconnected: " + username);
        }

        System.out.println("Total connected clients: "
                + getConnectedClientCount());

        return username;
    }

    // Get connected client count
    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    // Get usernames of connected clients
    public List<String> getConnectedUsernames() {
        return new ArrayList<>(connectedClients.values());
    }

    // Display connected clients
    public void displayConnectedClients() {

        System.out.println("Currently connected clients:");

        for (String username : getConnectedUsernames()) {
            System.out.println("- " + username);
        }
    }

    // Broadcast a message to all connected clients
    public void broadcastMessage(String message) {

        for (PrintWriter writer : clientWriters.values()) {
            writer.println(message);
        }
    }

    // Send updated online user list to all clients
    public void broadcastUserList() {

        List<String> usernames = getConnectedUsernames();

        String userList = "USERS:" + String.join(",", usernames);

        broadcastMessage(userList);

        System.out.println("Updated online users: " + userList);
    }
}