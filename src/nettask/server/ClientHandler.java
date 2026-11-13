package nettask.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ClientManager clientManager;

    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
    }

    @Override
    public void run() {

        boolean registered = false;

        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true)
        ) {

            // Receive username from client
            String username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                out.println("ERROR: Username is required.");
                return;
            }

            username = username.trim();

            // Check and register username
            if (!clientManager.addClient(socket, username, out)) {
                out.println("ERROR: Username already taken. Please choose another username.");
                return;
            }

            registered = true;

            System.out.println(
                    "Client handler started for: " + username);

            // Send welcome message
            out.println("Welcome to NetTask, " + username + "!");

            // Notify everyone that a user joined
            clientManager.broadcastMessage(
                    "SERVER: " + username + " joined NetTask!");

            // Update online users list
            clientManager.broadcastUserList();

            // Display connected clients
            clientManager.displayConnectedClients();

            String message;

            // Receive and broadcast messages
            while ((message = in.readLine()) != null) {

                System.out.println(username + ": " + message);

                clientManager.broadcastMessage(
                        username + ": " + message);
            }

        } catch (IOException e) {

            System.out.println("Client connection error: "
                    + e.getMessage());

        } finally {

            if (registered) {

                String disconnectedUser = clientManager.removeClient(socket);

                if (disconnectedUser != null && !disconnectedUser.isBlank()) {
                    clientManager.broadcastMessage(
                            "SERVER: " + disconnectedUser + " disconnected.");
                }

                clientManager.broadcastUserList();
            }

            try {
                socket.close();
                System.out.println("Client connection closed.");

            } catch (IOException e) {
                System.out.println("Error closing socket.");
            }
        }
    }
}