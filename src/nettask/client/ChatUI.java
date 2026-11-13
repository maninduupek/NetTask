package nettask.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ChatUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private JTextArea chatArea;
    private JTextField messageField;

    private JButton sendButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton sharedFilesButton;

    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private JLabel transferStatusLabel;
    private JProgressBar transferProgressBar;

    private MessageHandler messageHandler;
    private FileTransferHandler fileTransferHandler;

    private String username;

    private volatile boolean running = true;
    private volatile boolean connectionAvailable = true;

    public ChatUI(String username, MessageHandler messageHandler) {

        this.username = username;
        this.messageHandler = messageHandler;
        this.fileTransferHandler = new FileTransferHandler(
                messageHandler != null ? messageHandler.getServerAddress() : "localhost",
                6061
        );

        setTitle("NetTask - " + username);
        setSize(950, 650);
        setMinimumSize(new Dimension(750, 500));
        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeChatWindow();
            }
        });

        createInterface();
        startMessageListener();
    }

    private void createInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(25, 25, 35));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titleLabel = new JLabel(
                "NetTask - Multi Client Chat",
                SwingConstants.CENTER
        );

        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("Logged in as: " + username);
        userLabel.setForeground(new Color(100, 220, 160));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(35, 35, 50));
        headerPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(userLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setBackground(new Color(35, 35, 45));
        chatArea.setForeground(Color.WHITE);
        chatArea.setCaretColor(Color.WHITE);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // Online users
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);

        userList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        userList.setBackground(new Color(35, 35, 45));
        userList.setForeground(Color.WHITE);
        userList.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel usersPanel = new JPanel(new BorderLayout(5, 5));
        usersPanel.setBackground(new Color(30, 30, 40));
        usersPanel.setPreferredSize(new Dimension(180, 0));

        JLabel onlineLabel = new JLabel(
                "ONLINE USERS",
                SwingConstants.CENTER
        );

        onlineLabel.setForeground(new Color(100, 220, 160));
        onlineLabel.setFont(new Font("Arial", Font.BOLD, 14));

        usersPanel.add(onlineLabel, BorderLayout.NORTH);
        usersPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(25, 25, 35));

        centerPanel.add(chatScrollPane, BorderLayout.CENTER);
        centerPanel.add(usersPanel, BorderLayout.EAST);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Message input
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));

        sendButton = createButton("Send");

        sendButton.addActionListener(_ -> sendMessage());
        messageField.addActionListener(_ -> sendMessage());

        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBackground(new Color(25, 25, 35));

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // File transfer buttons
        uploadButton = createButton("Upload File");
        downloadButton = createButton("Download File");
        sharedFilesButton = createButton("Shared Files");

        uploadButton.addActionListener(_ -> uploadFile());
        downloadButton.addActionListener(_ -> downloadFile());
        sharedFilesButton.addActionListener(_ -> showSharedFiles());

        JPanel fileButtonPanel = new JPanel(
                new GridLayout(1, 3, 8, 0)
        );

        fileButtonPanel.setBackground(new Color(25, 25, 35));

        fileButtonPanel.add(uploadButton);
        fileButtonPanel.add(downloadButton);
        fileButtonPanel.add(sharedFilesButton);

        // Transfer progress
        transferStatusLabel = new JLabel("Ready");
        transferStatusLabel.setForeground(Color.WHITE);

        transferProgressBar = new JProgressBar(0, 100);
        transferProgressBar.setStringPainted(true);
        transferProgressBar.setValue(0);

        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBackground(new Color(25, 25, 35));

        progressPanel.add(transferStatusLabel, BorderLayout.NORTH);
        progressPanel.add(transferProgressBar, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(new Color(25, 25, 35));

        bottomPanel.add(messagePanel, BorderLayout.NORTH);
        bottomPanel.add(fileButtonPanel, BorderLayout.CENTER);
        bottomPanel.add(progressPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        appendMessage("Welcome to NetTask, " + username + "!");
    }

    private JButton createButton(String text) {

        JButton button = new JButton(text);

        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(new Color(65, 110, 190));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        return button;
    }

    // Send chat message
    private void sendMessage() {

        String message = messageField.getText().trim();

        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Message cannot be empty.",
                    "Empty Message",
                    JOptionPane.WARNING_MESSAGE
            );
            messageField.requestFocusInWindow();
            return;
        }

        if (connectionAvailable
                && messageHandler != null
                && messageHandler.isConnected()) {

            messageHandler.sendMessage(message);
            messageField.setText("");

        } else {

            appendMessage("SERVER: Not connected to server.");
            handleConnectionLost();
        }

    }

    // Receive messages from server
    private void startMessageListener() {

        Thread listenerThread = new Thread(() -> {

            while (running && connectionAvailable) {

                try {

                    String message = messageHandler.receiveMessage();

                    if (message == null) {
                        handleConnectionLost();
                        break;
                    }

                    if (message.startsWith("USERS:")) {

                        String users = message.substring(6);
                        updateUserList(users);

                    } else {

                        appendMessage(message);
                    }

                } catch (IOException e) {

                    if (running) {
                        handleConnectionLost();
                    }

                    break;
                }
            }

        });

        listenerThread.setName("NetTask-MessageListener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Handle connection loss
    private void handleConnectionLost() {

        if (!connectionAvailable) {
            return;
        }

        connectionAvailable = false;

        SwingUtilities.invokeLater(() -> {

            if (messageField != null) {
                messageField.setEnabled(false);
            }

            if (sendButton != null) {
                sendButton.setEnabled(false);
            }

            if (transferStatusLabel != null) {
                transferStatusLabel.setText("Server connection lost.");
            }

            appendMessage("SERVER: Connection lost. Please reconnect.");
        });
    }

    // Update online users
    private void updateUserList(String users) {

        SwingUtilities.invokeLater(() -> {

            userListModel.clear();

            if (users == null || users.trim().isEmpty()) {
                return;
            }

            String[] usernames = users.split(",");

            for (String name : usernames) {

                if (!name.trim().isEmpty()) {
                    userListModel.addElement(name.trim());
                }
            }
        });
    }

    // Display messages with timestamps
    private void appendMessage(String message) {

        SwingUtilities.invokeLater(() -> {

            if (chatArea != null) {

                String time = LocalTime.now().format(TIME_FORMAT);
                String displayMessage;

                if (message.startsWith("SERVER:")) {

                    displayMessage = "[" + time + "] [SERVER] "
                            + message.substring(7).trim();

                } else {

                    displayMessage = "[" + time + "] " + message;
                }

                chatArea.append(displayMessage + "\n");

                chatArea.setCaretPosition(
                        chatArea.getDocument().getLength()
                );
            }
        });
    }

    // Upload file
    private void uploadFile() {

        if (messageHandler == null || !messageHandler.isConnected()) {
            showError("Connection Error", "Not connected to the server.");
            return;
        }

        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();

        if (selectedFile == null || !selectedFile.isFile()) {
            showError("Upload Error", "Please choose a valid file.");
            return;
        }

        setTransferButtonsEnabled(false);
        startTransfer("Uploading " + selectedFile.getName());

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                fileTransferHandler.uploadFile(
                        selectedFile,
                        progress -> publish(progress)
                );

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {

                int progress = chunks.get(chunks.size() - 1);
                updateTransferProgress(progress);
            }

            @Override
            protected void done() {

                try {

                    get();

                    finishTransfer("Upload completed successfully!");

                    appendMessage(
                            "File uploaded: " + selectedFile.getName()
                    );

                } catch (Exception e) {

                    finishTransfer("Upload failed.");

                    showError("Upload Error", getErrorMessage(e));
                }

                setTransferButtonsEnabled(true);
            }
        };

        worker.execute();
    }

    // Download file manually
    private void downloadFile() {

        if (messageHandler == null || !messageHandler.isConnected()) {
            showError("Connection Error", "Not connected to the server.");
            return;
        }

        String fileName = JOptionPane.showInputDialog(
                this,
                "Enter the exact file name to download:"
        );

        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }

        downloadSelectedFile(fileName.trim());
    }

    // Display shared files
    private void showSharedFiles() {

        if (messageHandler == null || !messageHandler.isConnected()) {
            showError("Connection Error", "Not connected to the server.");
            return;
        }

        setTransferButtonsEnabled(false);
        startTransfer("Loading shared files...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<String> doInBackground() throws Exception {

                return fileTransferHandler.getSharedFiles();
            }

            @Override
            protected void done() {

                try {

                    List<String> files = get();

                    finishTransfer("Shared files loaded.");

                    if (files == null || files.isEmpty()) {

                        JOptionPane.showMessageDialog(
                                ChatUI.this,
                                "No shared files available."
                        );

                        return;
                    }

                    String[] fileArray = files.toArray(new String[0]);

                    String selectedFile =
                            (String) JOptionPane.showInputDialog(
                                    ChatUI.this,
                                    "Select a file to download:",
                                    "Shared Files",
                                    JOptionPane.PLAIN_MESSAGE,
                                    null,
                                    fileArray,
                                    fileArray[0]
                            );

                    if (selectedFile != null) {

                        String fileName = selectedFile.replaceFirst(
                                " \\([0-9]+(?:\\.[0-9]+)? (?:B|KB|MB)\\)$",
                                ""
                        );

                        downloadSelectedFile(fileName);
                    }

                } catch (Exception e) {

                    finishTransfer("Unable to load shared files.");

                    showError(
                            "Shared Files Error",
                            getErrorMessage(e)
                    );

                } finally {

                    setTransferButtonsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    // Download selected file
    private void downloadSelectedFile(String fileName) {

        if (fileName == null || fileName.trim().isEmpty()) {
            showError("Download Error", "Please enter a valid file name.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));

        int result = chooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destination = chooser.getSelectedFile();

        if (destination.exists()) {
            showError(
                    "Download Error",
                    "A file with this name already exists. Choose a different location."
            );
            return;
        }

        setTransferButtonsEnabled(false);
        startTransfer("Downloading " + fileName);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                fileTransferHandler.downloadFile(
                        fileName,
                        destination,
                        progress -> publish(progress)
                );

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {

                int progress = chunks.get(chunks.size() - 1);
                updateTransferProgress(progress);
            }

            @Override
            protected void done() {

                try {

                    get();

                    finishTransfer("Download completed successfully!");

                    appendMessage("File downloaded: " + fileName);

                    JOptionPane.showMessageDialog(
                            ChatUI.this,
                            "File downloaded successfully!\n"
                                    + destination.getAbsolutePath()
                    );

                } catch (Exception e) {

                    finishTransfer("Download failed.");

                    showError("Download Error", getErrorMessage(e));

                } finally {

                    setTransferButtonsEnabled(true);
                }
            }
        };

        worker.execute();
    }

    // Start transfer progress
    private void startTransfer(String message) {

        transferStatusLabel.setText(message);
        transferProgressBar.setValue(0);
    }

    // Update progress percentage
    private void updateTransferProgress(int progress) {

        transferProgressBar.setValue(progress);

        transferStatusLabel.setText(
                "Transfer progress: " + progress + "%"
        );
    }

    // Finish transfer
    private void finishTransfer(String message) {

        transferStatusLabel.setText(message);
        transferProgressBar.setValue(100);
    }

    // Enable or disable file buttons
    private void setTransferButtonsEnabled(boolean enabled) {

        uploadButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        sharedFilesButton.setEnabled(enabled);
    }

    // Display error
    private void showError(String title, String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                title,
                JOptionPane.ERROR_MESSAGE
        );
    }

    // Get actual exception message
    private String getErrorMessage(Exception e) {

        Throwable cause = e;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() != null
                ? cause.getMessage()
                : e.toString();
    }

    // Close chat and disconnect from server
    private void closeChatWindow() {

        if (!running) {
            dispose();
            return;
        }

        running = false;

        if (messageHandler != null) {
            messageHandler.disconnect();
        }

        dispose();
    }
}