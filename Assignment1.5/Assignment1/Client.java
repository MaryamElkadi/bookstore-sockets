import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    private
    Scanner scanner = new Scanner(System.in);

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Connected to server.");

            input = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            // Start client functionality
            startClient();
        } catch (UnknownHostException u) {
            System.out.println("Host unknown: " + u.getMessage());
        } catch (IOException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        } finally {
            // Close connections
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void startClient() {
        try {
            String choice;
            do {
                System.out.println("Press Enter to continue...");
                scanner.nextLine();

                displayMenu();
                choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        login();
                        break;
                    case "2":
                        register();
                        break;
                    case "3":
                        browseAndSearchBooks();
                        break;
                    case "4":
                        addBook();
                        break;
                    case "5":
                        removeBook();
                        break;
                    case "6":
                        submitRequest();
                        break;
                    case "7":
                        requestHistory();
                        break;
                    case "8":
                        respondToRequest();
                        break;
                    case "9":
                        displayRecommendations(out);
                        break;
                    case "10":
                        ReviewBooks();
                        break;
                    case "11":
                        displayLibraryStatistics();
                        break;
                    case "12":
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } while (!choice.equals("11"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close connections
            try {
                input.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n---- Bookstore Menu ----");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Browse and Search Books");
        System.out.println("4. Add Book");
        System.out.println("5. Remove Book");
        System.out.println("6. Submit Request");
        System.out.println("7. Request History");
        System.out.println("8. Respond to Request");
        System.out.println("9. Display Recommendations");
        System.out.println("10. Review Books");
        System.out.println("11. Library Overall Statistics");
        System.out.println("12. Exit");
        System.out.print("Enter your choice: ");
    }

    private void login() throws IOException {
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();

        out.writeUTF("LOGIN");
        out.writeUTF(username);
        out.writeUTF(password);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void register() throws IOException {
        System.out.println("Enter name: ");
        String name = scanner.nextLine().trim();
        System.out.println("Enter username: ");
        String username = scanner.nextLine().trim();
        System.out.println("Enter password: ");
        String password = scanner.nextLine().trim();

        out.writeUTF("REGISTER");
        out.writeUTF(name);
        out.writeUTF(username);
        out.writeUTF(password);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void browseAndSearchBooks() throws IOException {
        // Prompt the user to enter the search term and search criteria
        System.out.println("Enter search term: ");
        String searchTerm = scanner.nextLine().trim();

        System.out.println("Enter search by (title/author/genre): ");
        String searchBy = scanner.nextLine().trim();

        // Send the search term and search criteria to the server
        out.writeUTF("SEARCH");
        out.writeUTF(searchTerm);
        out.writeUTF(searchBy);
        out.flush();

        // Receive and display the server's response
        String response = input.readUTF();
        System.out.println(response);
    }



    private void addBook() throws IOException {
        System.out.println("Enter book title: ");
        String title = scanner.nextLine().trim();
        System.out.println("Enter book author: ");
        String author = scanner.nextLine().trim();
        System.out.println("Enter book genre: ");
        String genre = scanner.nextLine().trim();
        System.out.println("Enter book price: ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        System.out.println("Enter book quantity: ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());


        out.writeUTF("ADD_BOOK");
        out.writeUTF(title);
        out.writeUTF(author);
        out.writeUTF(genre);
        out.writeDouble(price);
        out.writeInt(quantity);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void removeBook() throws IOException {
        System.out.println("Enter book title to remove: ");
        String title = scanner.nextLine().trim();

        out.writeUTF("REMOVE_BOOK");
        out.writeUTF(title);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void submitRequest() throws IOException {
        System.out.println("Enter book title: ");
        String title = scanner.nextLine().trim();

        out.writeUTF("SUBMIT_REQUEST");
        out.writeUTF(title);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void requestHistory() throws IOException {
        out.writeUTF("REQUEST_HISTORY");
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }
    private void respondToRequest() throws IOException {
        System.out.println("Enter request ID: ");
        String requestId = scanner.nextLine().trim(); // Request ID is now a String (ObjectId)
        System.out.println("Enter your response (approved/rejected): ");
        String decision = scanner.nextLine().trim();

        out.writeUTF("RESPOND_REQUEST");
        out.writeUTF(requestId); // Send the requestId as a String
        out.writeUTF(decision);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }

    private void displayRecommendations(DataOutputStream out) {
        try {
            out.writeUTF("RECOMMENDATIONS");
            out.flush();

            System.out.print("Enter Type Recommendation by genre/mostly Borrowed/General: ");
            String type = scanner.nextLine().trim();
            out.writeUTF(type);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void ReviewBooks() throws IOException {
        System.out.println("Enter book title: ");
        String title = scanner.nextLine().trim();

        System.out.println("Enter review as an integer (1 to 5): ");
        int review = Integer.parseInt(scanner.nextLine().trim());

        out.writeUTF("SUBMIT_REVIEW");
        out.writeUTF(title);
        out.writeInt(review);
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }
    private void displayLibraryStatistics() throws IOException {
        out.writeUTF("LIBRARY_STATISTICS");
        out.flush();

        String response = input.readUTF();
        System.out.println(response);
    }



    public static void main(String[] args) {
        Client client = new Client("localhost", 1125); // Change "localhost" to server IP if needed
    }
}
