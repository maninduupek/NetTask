
package nettask.server;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import nettask.filetransfer.FileTransferServer;

public class Server {

    private static final int PORT = 6060;

    public static void main(String[] args) {

        ClientManager clientManager = new ClientManager();

        System.out.println("================================");
        System.out.println("          NetTask Server");
        System.out.println("================================");

        // Start the chat server first
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Chat Server started successfully!");
            System.out.println("Chat Server Port: " + PORT);

            // Start the file transfer server only after
            // the chat server successfully binds its port
            FileTransferServer fileTransferServer =
                    new FileTransferServer();

            Thread fileServerThread = new Thread(
                    () -> fileTransferServer.startServer()
            );

            fileServerThread.setName("FileTransferServer");
            fileServerThread.setDaemon(true);
            fileServerThread.start();

            System.out.println("--------------------------------");
            System.out.println("Waiting for clients...");

            // Accept multiple chat clients
            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println("New chat client connected: "
                        + socket.getInetAddress());

                ClientHandler handler =
                        new ClientHandler(socket, clientManager);

                Thread clientThread = new Thread(handler);
                clientThread.start();
            }

        } catch (BindException e) {

            System.out.println("ERROR: Chat port "
                    + PORT + " is already in use!");

            System.out.println(
                    "Please stop the previous NetTask server."
            );

        } catch (IOException e) {

            System.out.println("Chat Server Error: "
                    + e.getMessage());
        }
    }
}
