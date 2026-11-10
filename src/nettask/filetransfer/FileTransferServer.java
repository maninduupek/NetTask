
package nettask.filetransfer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;

public class FileTransferServer {

    private static final int PORT = 6061;
    private static final String STORAGE_FOLDER = "shared_files";
    private static final long MAX_FILE_SIZE = 100 * 1024L * 1024L;

    // Start file transfer server
    public void startServer() {

        Path storagePath = Paths.get(STORAGE_FOLDER);

        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            System.out.println("Cannot create storage folder: "
                    + e.getMessage());
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("File Transfer Server started!");
            System.out.println(
                    "Waiting for file transfers on port " + PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                System.out.println(
                        "File transfer client connected: "
                        + socket.getInetAddress());

                Thread thread =
                        new Thread(() -> handleClient(socket));

                thread.start();
            }

        } catch (IOException e) {

            System.out.println(
                    "File Server Error: " + e.getMessage());
        }
    }

    // Handle client requests
    private void handleClient(Socket socket) {

        try (
            Socket clientSocket = socket;
            DataInputStream in = new DataInputStream(
                    clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    clientSocket.getOutputStream())
        ) {

            clientSocket.setSoTimeout(30000);

            String command = in.readUTF();

            switch (command) {

                case "UPLOAD":
                    receiveFile(in, out);
                    break;

                case "DOWNLOAD":
                    sendFile(in, out);
                    break;

                case "LIST":
                    sendFileList(out);
                    break;

                default:
                    out.writeBoolean(false);
                    out.writeUTF("Invalid command.");
                    out.flush();
                    break;
            }

        } catch (IOException e) {

            System.out.println(
                    "File transfer failed: " + e.getMessage());
        }
    }

    // Validate filename
    private String validateFileName(String fileName)
            throws IOException {

        if (fileName == null
                || fileName.isBlank()
                || fileName.equals(".")
                || fileName.equals("..")
                || fileName.contains("/")
                || fileName.contains("\\")) {

            throw new IOException("Invalid filename.");
        }

        return fileName;
    }

    // Receive uploaded file using temporary storage
    private synchronized void receiveFile(
            DataInputStream in,
            DataOutputStream out) throws IOException {

        String fileName;

        try {

            fileName = validateFileName(in.readUTF());

        } catch (IOException e) {

            out.writeBoolean(false);
            out.writeUTF("Invalid filename.");
            out.flush();
            return;
        }

        long fileSize = in.readLong();

        if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {

            out.writeBoolean(false);
            out.writeUTF("Invalid file size. Maximum is 100 MB.");
            out.flush();
            return;
        }

        Path storagePath = Paths.get(STORAGE_FOLDER)
                .toAbsolutePath().normalize();

        Path finalPath = storagePath.resolve(fileName).normalize();

        if (!finalPath.startsWith(storagePath)) {

            discardUploadData(in, fileSize);

            out.writeBoolean(false);
            out.writeUTF("Invalid file path.");
            out.flush();
            return;
        }

        // Check whether the filename already exists
        if (Files.exists(finalPath)) {

            discardUploadData(in, fileSize);

            out.writeBoolean(false);
            out.writeUTF(
                    "Upload rejected: A file with this name already exists.");
            out.flush();

            System.out.println(
                    "Duplicate upload rejected: " + fileName);

            return;
        }

        Path temporaryPath = null;

        try {

            // Create a unique temporary file in the same folder
            temporaryPath = Files.createTempFile(
                    storagePath,
                    ".nettask-upload-",
                    ".part"
            );

            // Receive file data into the temporary file
            try (OutputStream fileOut = Files.newOutputStream(
                    temporaryPath,
                    StandardOpenOption.WRITE)) {

                transferUploadData(in, fileOut, fileSize);
            }

            // Move temporary file to its final filename
            // The synchronized method prevents concurrent uploads
            // from this server instance from using the same filename.
            try {

                Files.move(
                        temporaryPath,
                        finalPath,
                        StandardCopyOption.ATOMIC_MOVE
                );

            } catch (AtomicMoveNotSupportedException e) {

                // Use a normal move if atomic move is unavailable
                Files.move(temporaryPath, finalPath);
            }

            out.writeBoolean(true);
            out.writeUTF(
                    "File uploaded successfully: " + fileName);
            out.flush();

            System.out.println(
                    "File uploaded successfully: " + fileName);

        } catch (FileAlreadyExistsException e) {

            out.writeBoolean(false);
            out.writeUTF(
                    "Upload rejected: A file with this name already exists.");
            out.flush();

        } catch (IOException e) {

            out.writeBoolean(false);
            out.writeUTF(
                    "Upload failed: " + e.getMessage());
            out.flush();

            System.out.println(
                    "Upload failed for " + fileName + ": "
                    + e.getMessage());

        } finally {

            // Delete temporary file if it still exists
            if (temporaryPath != null) {

                try {
                    Files.deleteIfExists(temporaryPath);
                } catch (IOException e) {
                    System.out.println(
                            "Could not remove temporary file: "
                            + e.getMessage());
                }
            }
        }
    }

    // Receive file data
    private void transferUploadData(
            DataInputStream in,
            OutputStream fileOut,
            long fileSize) throws IOException {

        byte[] buffer = new byte[8192];
        long remaining = fileSize;

        while (remaining > 0) {

            int bytesToRead =
                    (int) Math.min(buffer.length, remaining);

            int bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {

                throw new IOException(
                        "Incomplete file received.");
            }

            fileOut.write(buffer, 0, bytesRead);

            remaining -= bytesRead;
        }

        fileOut.flush();
    }

    // Discard incoming data for rejected uploads
    private void discardUploadData(
            DataInputStream in,
            long fileSize) throws IOException {

        byte[] buffer = new byte[8192];
        long remaining = fileSize;

        while (remaining > 0) {

            int bytesToRead =
                    (int) Math.min(buffer.length, remaining);

            int bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {

                throw new IOException(
                        "Incomplete incoming file data.");
            }

            remaining -= bytesRead;
        }
    }

    // Send file for download
    private void sendFile(
            DataInputStream in,
            DataOutputStream out) throws IOException {

        String fileName;

        try {

            fileName = validateFileName(in.readUTF());

        } catch (IOException e) {

            out.writeBoolean(false);
            out.writeUTF("Invalid filename.");
            out.flush();
            return;
        }

        Path storagePath = Paths.get(STORAGE_FOLDER)
                .toAbsolutePath().normalize();

        Path filePath = storagePath.resolve(fileName).normalize();

        if (!filePath.startsWith(storagePath)
                || !Files.isRegularFile(filePath)) {

            out.writeBoolean(false);
            out.writeUTF("File not found.");
            out.flush();
            return;
        }

        long fileSize = Files.size(filePath);

        if (fileSize > MAX_FILE_SIZE) {

            out.writeBoolean(false);
            out.writeUTF("File exceeds download size limit.");
            out.flush();
            return;
        }

        out.writeBoolean(true);
        out.writeLong(fileSize);

        Files.copy(filePath, out);
        out.flush();

        System.out.println("File downloaded: " + fileName);
    }

    // Send list of completed shared files
    private void sendFileList(DataOutputStream out)
            throws IOException {

        File folder = new File(STORAGE_FOLDER);

        File[] files = folder.listFiles(file ->
                file.isFile()
                && !file.getName().startsWith(".nettask-upload-")
                && !file.getName().endsWith(".part"));

        if (files == null) {

            out.writeBoolean(false);
            out.writeUTF("Unable to read shared files.");
            out.flush();
            return;
        }

        Arrays.sort(files,
                Comparator.comparing(
                        File::getName,
                        String.CASE_INSENSITIVE_ORDER));

        out.writeBoolean(true);
        out.writeInt(files.length);

        for (File file : files) {

            out.writeUTF(file.getName());
            out.writeLong(file.length());
        }

        out.flush();

        System.out.println(
                "Shared file list sent: "
                + files.length + " files.");
    }
}
