
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.logging.Logger;
import javax.swing.*;

public class Client1 extends javax.swing.JFrame {

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dout;
    private String clientName;
    private static final Logger logger = Logger.getLogger(Client1.class.getName());

    // Constructor initializes GUI and connects to server
    public Client1() {
        initComponents();
        connectToServer();
    }

    // Handles server connection logic
    private void connectToServer() {
        String serverIP = JOptionPane.showInputDialog(this, "Enter server IP:", "127.0.0.1");
        if (serverIP == null || serverIP.trim().isEmpty()) {
            serverIP = "127.0.0.1";
        }

        clientName = JOptionPane.showInputDialog(this, "Enter your name:", "Client");
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = "Client" + new Random().nextInt(1000);
        }

        String finalServerIP = serverIP;

        // Start new thread for connection and message handling
        new Thread(() -> {
            while (true) {
                try {
                    if (!isValidIP(finalServerIP)) {
                        SwingUtilities.invokeLater(()
                                -> msg_area.append("❌ Invalid IP address format. Use format like 127.0.0.1\n"));
                        return;
                    }

                    if (!isIPReachable(finalServerIP)) {
                        SwingUtilities.invokeLater(()
                                -> msg_area.append("⚠ IP address " + finalServerIP + " is not reachable\n"));
                        Thread.sleep(5000);
                        continue;
                    }

                    SwingUtilities.invokeLater(()
                            -> msg_area.append("🔄 Trying to connect to " + finalServerIP + "...\n"));

                    socket = new Socket();
                    socket.connect(new InetSocketAddress(finalServerIP, 7777), 2000);

                    dis = new DataInputStream(socket.getInputStream());
                    dout = new DataOutputStream(socket.getOutputStream());

                    dout.writeUTF(clientName);
                    SwingUtilities.invokeLater(()
                            -> msg_area.append("✅ Connected to " + finalServerIP + " as " + clientName + "\n"));

                    String msgin;
                    while (socket != null && !socket.isClosed() && (msgin = dis.readUTF()) != null) {
                        final String msg = msgin;
                        SwingUtilities.invokeLater(() -> msg_area.append(msg + "\n"));
                    }
                } catch (ConnectException e) {
                    SwingUtilities.invokeLater(()
                            -> msg_area.append("⚠ Server not running at " + finalServerIP + ". Retrying in 5 seconds...\n"));
                } catch (IOException e) {
                    SwingUtilities.invokeLater(()
                            -> msg_area.append("⚠ Connection error (" + finalServerIP + "): " + e.getMessage() + "\n"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    disconnect();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }).start();
    }

    // Validates IP format
    private boolean isValidIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if (i < 0 || i > 255) {
                    return false;
                }
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Checks if IP is reachable
    private boolean isIPReachable(String ip) {
        try {
            if (ip.startsWith("127.")) {
                return true;
            }
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(2000);
        } catch (IOException e) {
            return false;
        }
    }

    // Disconnects from server and closes streams
    private void disconnect() {
        try {
            if (dout != null) {
                dout.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.severe("Error disconnecting: " + e.getMessage());
        }
    }

    // Sends message to server on button click
    private void msg_sendActionPerformed(java.awt.event.ActionEvent evt) {
        String msg = msg_text.getText().trim();
        if (!msg.isEmpty()) {
            try {
                dout.writeUTF(msg);
                msg_area.append("You: " + msg + "\n");
                msg_text.setText("");
            } catch (IOException e) {
                logger.severe("Error sending message: " + e.getMessage());
                msg_area.append("⚠ Error sending message: " + e.getMessage() + "\n");
            }
        }
    }

    // Initializes GUI components
    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        msg_area = new javax.swing.JTextArea();
        msg_send = new javax.swing.JButton();
        msg_text = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat Client");

        jLabel1.setFont(new java.awt.Font("Cambria", 1, 36));
        jLabel1.setText("Client");

        msg_area.setBackground(new java.awt.Color(0, 0, 0));
        msg_area.setColumns(20);
        msg_area.setFont(new java.awt.Font("Arial", 0, 14));
        msg_area.setForeground(new java.awt.Color(255, 255, 255));
        msg_area.setRows(5);
        msg_area.setEditable(false);
        jScrollPane1.setViewportView(msg_area);

        msg_send.setBackground(new java.awt.Color(204, 0, 51));
        msg_send.setFont(new java.awt.Font("Cambria", 0, 16));
        msg_send.setForeground(new java.awt.Color(255, 255, 255));
        msg_send.setText("Send");
        msg_send.addActionListener(this::msg_sendActionPerformed);

        msg_text.setBackground(new java.awt.Color(0, 0, 0));
        msg_text.setFont(new java.awt.Font("Arial", 0, 14));
        msg_text.setForeground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(jScrollPane1)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(msg_text, javax.swing.GroupLayout.PREFERRED_SIZE, 326, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(msg_send, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGap(12, 12, 12)
                                                .addComponent(msg_send, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(msg_text, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    // Entry point of the application
    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> new Client1().setVisible(true));
    }

    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea msg_area;
    private javax.swing.JButton msg_send;
    private javax.swing.JTextField msg_text;
}
