
package nettask.client;

import javax.swing.*;
import java.awt.*;

public class ClientUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextField usernameField;
    private JTextField serverField;
    private JTextField portField;
    private JLabel statusLabel;

    private volatile MessageHandler connectionHandler;

    public ClientUI() {

        setTitle("NetTask - Network File Transfer");
        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(24, 29, 39));
        mainPanel.setLayout(null);

        // Application title
        JLabel title = new JLabel("NetTask");
        title.setFont(new Font("Arial", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        title.setBounds(150, 40, 250, 50);
        mainPanel.add(title);

        // Subtitle
        JLabel subtitle = new JLabel(
                "Network Task & File Transfer System");
        subtitle.setForeground(Color.LIGHT_GRAY);
        subtitle.setBounds(105, 95, 300, 30);
        mainPanel.add(subtitle);

        // Username
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setBounds(70, 160, 150, 25);
        mainPanel.add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(70, 190, 350, 38);
        mainPanel.add(usernameField);

        // Server IP
        JLabel serverLabel = new JLabel("Server IP Address");
        serverLabel.setForeground(Color.WHITE);
        serverLabel.setBounds(70, 245, 200, 25);
        mainPanel.add(serverLabel);

        serverField = new JTextField("localhost");
        serverField.setBounds(70, 275, 350, 38);
        mainPanel.add(serverField);

        // Port
        JLabel portLabel = new JLabel("Port Number");
        portLabel.setForeground(Color.WHITE);
        portLabel.setBounds(70, 330, 150, 25);
        mainPanel.add(portLabel);

        portField = new JTextField("6060");
        portField.setBounds(70, 360, 350, 38);
        mainPanel.add(portField);

        // Connect button
        JButton connectButton = new JButton("CONNECT TO SERVER");
        connectButton.setBounds(70, 425, 350, 42);
        connectButton.setBackground(new Color(65, 105, 225));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        mainPanel.add(connectButton);

        // Status
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.ORANGE);
        statusLabel.setBounds(70, 480, 350, 25);
        mainPanel.add(statusLabel);

        // Connect button action
        connectButton.addActionListener(_ -> {

            String username = usernameField.getText().trim();
            String serverIP = serverField.getText().trim();
            String port = portField.getText().trim();

            // Validate fields
            if (username.isEmpty()
                    || serverIP.isEmpty()
                    || port.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please fill in all fields."
                );

                return;
            }

            final int portNumber;

            // Validate port number
            try {

                portNumber = Integer.parseInt(port);

                if (portNumber < 1 || portNumber > 65535) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid port number."
                );

                return;
            }

            // Disable button while connecting
            connectButton.setEnabled(false);

            statusLabel.setText("Status: Connecting...");
            statusLabel.setForeground(Color.YELLOW);

            // Establish TCP connection in background
            SwingWorker<Void, Void> worker =
                    new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {

                    // Create a new message handler
                    MessageHandler handler = new MessageHandler();

                    // Connect to server
                    handler.connect(
                            serverIP,
                            portNumber,
                            username
                    );

                    // IMPORTANT: Store successful connection
                    connectionHandler = handler;

                    return null;
                }

                @Override
                protected void done() {

                    try {

                        // Check background connection result
                        get();

                        // Retrieve successful connection
                        MessageHandler handler = connectionHandler;

                        if (handler == null || !handler.isConnected()) {
                            throw new IllegalStateException(
                                    "Connection was not established.");
                        }

                        statusLabel.setText(
                                "Status: Connected as " + username);

                        statusLabel.setForeground(Color.GREEN);

                        JOptionPane.showMessageDialog(
                                ClientUI.this,
                                "Successfully connected to NetTask Server!"
                        );

                        // Open chat interface
                        ChatUI chatWindow = new ChatUI(
                                username,
                                handler
                        );

                        chatWindow.setVisible(true);

                        // Close login window
                        dispose();

                    } catch (Exception ex) {

                        // Update status
                        statusLabel.setText(
                                "Status: Connection failed");

                        statusLabel.setForeground(Color.RED);

                        connectButton.setEnabled(true);

                        // Get actual error message
                        Throwable cause = ex.getCause();

                        String errorMessage = cause != null
                                ? cause.getMessage()
                                : ex.getMessage();

                        if (errorMessage == null) {
                            errorMessage = "Unable to connect to server.";
                        }

                        // Show error dialog
                        JOptionPane.showMessageDialog(
                                ClientUI.this,
                                "Connection failed: " + errorMessage,
                                "Login Error",
                                JOptionPane.ERROR_MESSAGE
                        );

                        // Allow another login attempt
                        usernameField.requestFocusInWindow();
                        usernameField.selectAll();
                    }
                }
            };

            worker.execute();
        });

        add(mainPanel);
    }

    // Main method
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            ClientUI window = new ClientUI();

            window.setVisible(true);
        });
    }
}
