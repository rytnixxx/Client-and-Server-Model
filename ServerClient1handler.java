import java.io.*;
import java.net.*;
import java.util.logging.Logger;

public class ServerClient1handler implements Runnable {

    private final Socket clientSocket;
    private final Server1 server;
    private DataInputStream dis;
    private DataOutputStream dout;
    private String clientName;
    private static final Logger logger = Logger.getLogger(ServerClient1handler.class.getName());

    // Constructor to initialize the handler with socket and server reference
    public ServerClient1handler(Socket socket, Server1 server) {
        this.clientSocket = socket;
        this.server = server;
    }

    // Handles communication with the connected client
    @Override
    public void run() {
        try {
            dis = new DataInputStream(clientSocket.getInputStream());
            dout = new DataOutputStream(clientSocket.getOutputStream());

            clientName = dis.readUTF();
            server.appendToLog(clientName + " joined the chat.");
            server.broadcast("Server: " + clientName + " has joined the chat", this);

            String message;
            while (!(message = dis.readUTF()).equalsIgnoreCase("exit")) {
                if (containsSymbols(message)) {
                    sendMessage("Server: Special characters are not allowed.");
                    server.appendToLog("Blocked message from " + clientName + ": " + message);
                    continue;
                }

                server.appendToLog(clientName + ": " + message);
                server.broadcast(clientName + ": " + message, this);

                String response = generateAutoResponse(message);
                if (response != null) {
                    sendMessage("Server: " + response);
                    server.appendToLog("Server: " + response);
                }
            }

        } catch (IOException e) {
            logger.warning("Client error: " + e.getMessage());
        } finally {
            cleanupClient();
        }
    }

    // Checks if the message contains disallowed characters
    private boolean containsSymbols(String message) {
        return !message.matches("[a-zA-Z0-9\\s]+");
    }

    // Generates automated responses for certain keywords
    private String generateAutoResponse(String msg) {
        msg = msg.toLowerCase();
        if (msg.contains("hi") || msg.contains("hello")) return "Hello " + clientName + "!";
        if (msg.contains("how are you")) return "I'm a server, always running!";
        if (msg.contains("thanks") || msg.contains("thank you")) return "You're welcome.";
        return null;
    }

    // Sends a message to the connected client
    public void sendMessage(String message) throws IOException {
        dout.writeUTF(message);
        dout.flush();
    }

    // Cleans up resources when client disconnects
    private void cleanupClient() {
        try {
            if (clientName != null) {
                server.broadcast("Server: " + clientName + " has left the chat", this);
                server.appendToLog(clientName + " disconnected.");
            }
            server.removeClient(this);
            clientSocket.close();
        } catch (IOException e) {
            logger.warning("Cleanup error: " + e.getMessage());
        }
    }

    // Returns the client's socket
    public Socket getClientSocket() {
        return clientSocket;
    }

    // Returns the client's name
    public String getClientName() {
        return clientName;
    }
}
