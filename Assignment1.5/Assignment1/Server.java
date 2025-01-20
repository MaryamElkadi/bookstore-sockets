import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
public class Server {
    private MongoDBConnection dbConnection;

    public Server() {
        this.dbConnection = new MongoDBConnection(); // Initialize MongoDB connection
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        ServerSocket socket = new ServerSocket(1125);
        System.out.println("Server started. Waiting for clients...");

        while (true) {
            Socket client = socket.accept();
            System.out.println("Client connected: " + client.getInetAddress().getHostAddress());
            Thread thread = new Handler(client, dbConnection);
            thread.start();
            // Handle client requests in a separate thread
            Thread object = new Handler(client, dbConnection);
            object.start();
        }
    }
}
