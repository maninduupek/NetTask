
package nettask.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageHandler {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Establish connection with the server
    public boolean connect(String serverIP, int port, String username)
            throws IOException {

        try {
            socket = new Socket(serverIP, port);

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(socket.getOutputStream(), true);

            // Send username to server
            out.println(username);

            // Receive server response
            String response = in.readLine();

            if (response == null) {
                throw new IOException("Server disconnected.");
            }

            if (response.startsWith("ERROR:")) {
                throw new IOException(response.substring(6).trim());
            }

            return true;

        } catch (IOException e) {

            disconnect();
            throw e;
        }
    }

    // Send a message to the server
    public void sendMessage(String message) {

        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    // Receive a message from the server
    public String receiveMessage() throws IOException {

        if (in != null) {
            return in.readLine();
        }

        return null;
    }

    // Close the connection
    public void disconnect() {

        try {
            if (in != null) {
                in.close();
                in = null;
            }

            if (out != null) {
                out.close();
                out = null;
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {

            System.out.println("Error closing connection: "
                    + e.getMessage());

        } finally {
            socket = null;
        }
    }

    // Check connection status
    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }
}
