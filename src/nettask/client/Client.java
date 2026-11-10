
package nettask.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        String serverAddress = "localhost";
        int port = 6060;

        try {
            Socket socket = new Socket(serverAddress, port);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            try (Scanner scanner = new Scanner(System.in)) {
				System.out.println("================================");
				System.out.println("       NetTask Client");
				System.out.println("================================");

				// Enter username
				System.out.print("Enter your username: ");
				String username = scanner.nextLine().trim();

				if (username.isEmpty()) {
				    System.out.println("Username cannot be empty.");
				    socket.close();
				    return;
				}

				// Send username to server
				out.println(username);

				// Receive welcome message
				String welcomeMessage = in.readLine();
				System.out.println(welcomeMessage);

				// Start a separate thread to receive messages
				Thread receiveThread = new Thread(() -> {

				    try {
				        String serverMessage;

				        while ((serverMessage = in.readLine()) != null) {
				            System.out.println("\n" + serverMessage);
				        }

				    } catch (IOException e) {
				        System.out.println("Disconnected from server.");
				    }
				});

				receiveThread.setDaemon(true);
				receiveThread.start();

				// Main thread sends messages
				while (true) {

				    System.out.print("Enter message (type exit to quit): ");

				    String message = scanner.nextLine();

				    if (message.equalsIgnoreCase("exit")) {
				        break;
				    }

				    if (!message.trim().isEmpty()) {
				        out.println(message);
				    }
				}
			}

            // Close connection
            socket.close();

            System.out.println("Disconnected from NetTask Server.");

        } catch (IOException e) {

            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}
