
package nettask.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class FileTransferHandler {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 6061;
    private static final long MAX_FILE_SIZE = 100 * 1024L * 1024L;

    // Upload without progress callback (backward compatibility)
    public String uploadFile(File file) throws IOException {
        return uploadFile(file, null);
    }

    // Upload a file with progress tracking
    public String uploadFile(File file, IntConsumer progress)
            throws IOException {

        if (file == null || !file.isFile()) {
            throw new IOException("Invalid file selected.");
        }

        long fileSize = file.length();

        if (fileSize <= 0) {
            throw new IOException("Cannot upload an empty file.");
        }

        if (fileSize > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds 100 MB.");
        }

        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            DataInputStream in = new DataInputStream(
                    socket.getInputStream());
            FileInputStream fileIn = new FileInputStream(file)
        ) {

            out.writeUTF("UPLOAD");
            out.writeUTF(file.getName());
            out.writeLong(fileSize);

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalSent = 0;

            reportProgress(progress, 0);

            while ((bytesRead = fileIn.read(buffer)) != -1) {

                out.write(buffer, 0, bytesRead);

                totalSent += bytesRead;

                int percentage =
                        (int) ((totalSent * 100) / fileSize);

                reportProgress(progress, percentage);
            }

            out.flush();

            boolean success = in.readBoolean();
            String response = in.readUTF();

            if (!success) {
                throw new IOException(response);
            }

            reportProgress(progress, 100);

            return response;
        }
    }

    // Download without progress callback (backward compatibility)
    public String downloadFile(
            String fileName, File destination) throws IOException {

        return downloadFile(fileName, destination, null);
    }

    // Download a file with progress tracking
    public String downloadFile(
            String fileName,
            File destination,
            IntConsumer progress) throws IOException {

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IOException("Invalid filename.");
        }

        if (destination == null) {
            throw new IOException("Invalid download location.");
        }

        File parent = destination.getAbsoluteFile().getParentFile();

        if (parent == null || !parent.exists()
                || !parent.isDirectory()) {

            throw new IOException("Download folder does not exist.");
        }

        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            DataInputStream in = new DataInputStream(
                    socket.getInputStream())
        ) {

            out.writeUTF("DOWNLOAD");
            out.writeUTF(fileName);
            out.flush();

            boolean success = in.readBoolean();

            if (!success) {
                throw new IOException(in.readUTF());
            }

            long fileSize = in.readLong();

            if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
                throw new IOException("Invalid file size.");
            }

            try (FileOutputStream fileOut =
                    new FileOutputStream(destination)) {

                byte[] buffer = new byte[8192];
                long remaining = fileSize;
                long totalReceived = 0;

                reportProgress(progress, 0);

                while (remaining > 0) {

                    int bytesToRead =
                            (int) Math.min(buffer.length, remaining);

                    int bytesRead =
                            in.read(buffer, 0, bytesToRead);

                    if (bytesRead == -1) {
                        throw new IOException(
                                "Incomplete file download.");
                    }

                    fileOut.write(buffer, 0, bytesRead);

                    remaining -= bytesRead;
                    totalReceived += bytesRead;

                    int percentage =
                            (int) ((totalReceived * 100) / fileSize);

                    reportProgress(progress, percentage);
                }
            }

            reportProgress(progress, 100);

            return "File downloaded successfully: "
                    + destination.getName();
        }
    }

    // Report progress safely
    private void reportProgress(
            IntConsumer progress, int percentage) {

        if (progress != null) {
            progress.accept(Math.min(100, Math.max(0, percentage)));
        }
    }

    // Get the list of files stored on the server
    public List<String> getSharedFiles() throws IOException {

        List<String> sharedFiles = new ArrayList<>();

        try (
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            DataInputStream in = new DataInputStream(
                    socket.getInputStream())
        ) {

            // Request shared file list
            out.writeUTF("LIST");
            out.flush();

            boolean success = in.readBoolean();

            if (!success) {
                throw new IOException(in.readUTF());
            }

            int fileCount = in.readInt();

            if (fileCount < 0 || fileCount > 10000) {
                throw new IOException("Invalid file list received.");
            }

            // Receive filenames and their sizes
            for (int i = 0; i < fileCount; i++) {

                String fileName = in.readUTF();
                long fileSize = in.readLong();

                String formattedSize = formatFileSize(fileSize);

                sharedFiles.add(
                        fileName + " (" + formattedSize + ")");
            }
        }

        return sharedFiles;
    }

    // Convert bytes into readable file sizes
    private String formatFileSize(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }

        return String.format("%.2f MB",
                bytes / (1024.0 * 1024.0));
    }
}
