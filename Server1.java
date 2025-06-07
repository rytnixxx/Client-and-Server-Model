import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import javax.swing.*;

public class Server1 extends javax.swing.JFrame {
    private ServerSocket serverSocket;
    private final List<ServerClient1handler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Logger logger = Logger.getLogger(Server1.class.getName());
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread serverThread;

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea msg_area;
    private javax.swing.JButton msg_send;
    private javax.swing.JTextField msg_text;
    private javax.swing.JButton start_button;
    private javax.swing.JButton stop_button;

    // Constructor
    public Server1() {
        initComponents();
        setupServer();
    }

    // Sets up server button actions
    private void setupServer() {
        start_button.addActionListener(e -> startServer());
        stop_button.addActionListener(e -> stopServer());
        msg_send.addActionListener(e -> sendButtonActionPerformed());
        stop_button.setEnabled(false);
    }

    // Starts the server and accepts client connections
    private void startServer() {
        if (isRunning.get()) {
            appendToLog("Server is already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(7777);
                isRunning.set(true);
                SwingUtilities.invokeLater(() -> {
                    appendToLog("Server started on port 7777");
                    start_button.setEnabled(false);
                    stop_button.setEnabled(true);
                });

                while (!serverSocket.isClosed() && isRunning.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        SwingUtilities.invokeLater(() -> {
                            String clientIP = clientSocket.getInetAddress().getHostAddress();
                            String connectedToIP = clientSocket.getLocalAddress().getHostAddress();
                            int clientPort = clientSocket.getPort();
                            int serverPort = clientSocket.getLocalPort();

                            String connectionInfo = String.format(
                                "Client connected from: %s (Port: %d) → Server: %s (Port: %d)",
                                clientIP, clientPort, connectedToIP, serverPort
                            );
                            appendToLog(connectionInfo);
                        });

                        ServerClient1handler clientHandler = new ServerClient1handler(clientSocket, this);
                        clients.add(clientHandler);
                        new Thread(clientHandler).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed() && isRunning.get()) {
                            SwingUtilities.invokeLater(() -> 
                                appendToLog("Error accepting client: " + e.getMessage()));
                        }
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> 
                    appendToLog("Server error: " + e.getMessage()));
            } finally {
                isRunning.set(false);
                SwingUtilities.invokeLater(() -> {
                    start_button.setEnabled(true);
                    stop_button.setEnabled(false);
                });
            }
        });

        serverThread.start();
    }

    // Stops the server and disconnects all clients
    private void stopServer() {
        if (!isRunning.get()) {
            appendToLog("Server is not running");
            return;
        }

        isRunning.set(false);

        new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }

                synchronized (clients) {
                    for (ServerClient1handler client : new ArrayList<>(clients)) {
                        try {
                            client.getClientSocket().close();
                        } catch (IOException e) {
                            logger.warning("Error closing client socket: " + e.getMessage());
                        }
                    }
                    clients.clear();
                }

                if (serverThread != null) {
                    try {
                        serverThread.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    appendToLog("Server stopped successfully");
                    start_button.setEnabled(true);
                    stop_button.setEnabled(false);
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> 
                    appendToLog("Error stopping server: " + e.getMessage()));
            }
        }).start();
    }

    // Broadcasts message to all clients except sender
    public void broadcast(String message, ServerClient1handler excludeClient) {
        synchronized (clients) {
            for (ServerClient1handler client : new ArrayList<>(clients)) {
                if (client != excludeClient) {
                    try {
                        client.sendMessage(message);
                    } catch (IOException e) {
                        logger.warning("Broadcast failed to " + client.getClientName());
                        clients.remove(client);
                    }
                }
            }
        }
    }

    // Removes disconnected client from client list
    public void removeClient(ServerClient1handler client) {
        clients.remove(client);
        appendToLog(client.getClientName() + " was removed from active clients");
    }

    // Appends message to server log (GUI)
    public void appendToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            msg_area.append(message + "\n");
            msg_area.setCaretPosition(msg_area.getDocument().getLength());
        });
    }

    // Sends a message from the server to all clients
    private void sendButtonActionPerformed() {
        String msg = msg_text.getText().trim();
        if (!msg.isEmpty()) {
            broadcast("Server: " + msg, null);
            appendToLog("Server: " + msg);
            msg_text.setText("");
        } 
    }

    // Initializes GUI components
    private void initComponents() {
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        msg_area = new javax.swing.JTextArea();
        msg_send = new javax.swing.JButton();
        msg_text = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        start_button = new javax.swing.JButton();
        stop_button = new javax.swing.JButton();

        jButton1.setText("jButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat Server");

        msg_area.setBackground(new java.awt.Color(0, 0, 0));
        msg_area.setColumns(20);
        msg_area.setFont(new java.awt.Font("Arial", 0, 14));
        msg_area.setForeground(new java.awt.Color(255, 255, 255));
        msg_area.setRows(5);
        msg_area.setEditable(false);
        jScrollPane1.setViewportView(msg_area);

        msg_send.setBackground(new java.awt.Color(204, 0, 51));
        msg_send.setFont(new java.awt.Font("Cambria", 1, 16));
        msg_send.setForeground(new java.awt.Color(255, 255, 255));
        msg_send.setText("Send");

        msg_text.setBackground(new java.awt.Color(0, 0, 0));
        msg_text.setFont(new java.awt.Font("Arial", 0, 14));
        msg_text.setForeground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Cambria", 1, 36));
        jLabel1.setText("Server");

        start_button.setBackground(new java.awt.Color(0, 153, 51));
        start_button.setFont(new java.awt.Font("Cambria", 1, 16));
        start_button.setForeground(new java.awt.Color(255, 255, 255));
        start_button.setText("Start Server");

        stop_button.setBackground(new java.awt.Color(204, 0, 51));
        stop_button.setFont(new java.awt.Font("Cambria", 1, 16));
        stop_button.setForeground(new java.awt.Color(255, 255, 255));
        stop_button.setText("Stop Server");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(msg_text)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(msg_send, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 118, Short.MAX_VALUE)
                        .addComponent(start_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stop_button)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(start_button)
                    .addComponent(stop_button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(msg_send)
                    .addComponent(msg_text, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }

    // Main method to launch the server GUI
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Server1.class.getName()).log(Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new Server1().setVisible(true);
        });
    }
}
