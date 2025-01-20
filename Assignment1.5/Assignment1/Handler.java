import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Handler extends Thread {
    private Socket client;
    private MongoDBConnection dbConnection;
    private String Username;
    private
    Scanner scanner = new Scanner(System.in);
    private boolean isAdmin;
    public Handler(Socket clientSocket, MongoDBConnection dbConnection) {
        this.client = clientSocket;
        this.dbConnection = dbConnection;
        this.isAdmin = false; // By default, assume regular user
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            dbConnection.testConnection();


            // Receive and process client requests
            while (true) {
                String request = in.readUTF();
                if (request.equals("LOGIN")) {
                    handleLogin(in, out);
                } else if (request.equals("REGISTER")) {
                    handleRegister(in, out);
                } else if (request.equals("SEARCH")) {
                    handleSearch(in,out);
                } else if (request.equals("ADD_BOOK") && isAdmin) {
                    handleAddBook(in, out);
                } else if (request.equals("REMOVE_BOOK") && isAdmin) {
                    handleRemoveBook(in, out);
                } else if (request.equals("SUBMIT_REQUEST")) {
                    handleSubmitRequest(in,out);
                } else if (request.equals("RESPOND_REQUEST") && isAdmin) {
                    handleRespondRequest(in, out);
                } else if (request.equals("REQUEST_HISTORY")) {
                    handleRequestHistory(out);
                } else if (request.equals("RECOMMENDATIONS")) {
                    String type = "";
                    getRecommendations(type);
                } else if(request.equals("SUBMIT_REVIEW")) {
                    handleReviewBook(in,out);
                }
                else if (request.equals("LIBRARY_STATISTICS") && isAdmin) {
                    handleLibraryStatistics(out);
                }
                else if (request.equals("end")) {
                    break;
                }
            }
            // Close the connection
            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void handleLogin(DataInputStream in, DataOutputStream out) throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        // Implement user authentication logic using MongoDBConnection
        int responseCode;
        if (dbConnection.authenticateUser(username, password)) {
            Username = username;
            isAdmin = dbConnection.isAdmin(username); // Check if the user is admin
            responseCode = 200; // Login successful, send 200 OK
            out.writeUTF("Login successful!");
        } else {
            // Check the reason for authentication failure and send appropriate error code
            if (dbConnection.isUserExists(username)) {
                responseCode = 401; // Unauthorized, send 401 Unauthorized
                out.writeUTF("Invalid password.");
            } else {
                responseCode = 404; // Not found, send 404 Not Found
                out.writeUTF("Username not found.");
            }
        }
        out.writeInt(responseCode);
        out.flush();
    }

    private void handleRegister(DataInputStream in, DataOutputStream out) throws IOException {
        String name = in.readUTF();
        String username = in.readUTF();
        String password = in.readUTF();

        // Implement user registration logic using MongoDBConnection
        int responseCode;
        if (dbConnection.registerUser(name, username, password)) {
            Username = username;
            isAdmin = false; // Newly registered users are not admins
            responseCode = 200; // Registration successful, send 200 OK
            out.writeUTF("Registration successful!");
        } else {
            // Check the reason for registration failure and send appropriate error code
            if (dbConnection.isUserExists(username)) {
                responseCode = 409; // Username already exists, send 409 Conflict
                out.writeUTF("Username already exists. Please choose another one.");
            } else {
                responseCode = 500; // Internal server error, send 500 Internal Server Error
                out.writeUTF("Error occurred during registration.");
            }
        }
        out.writeInt(responseCode);
        out.flush();
    }

    private void handleSearch(DataInputStream in, DataOutputStream out) throws IOException {
        String searchTerm = in.readUTF();
        String searchBy = in.readUTF();

        // Perform search based on the specified criteria
        List<String> searchResults = dbConnection.searchBooks(searchTerm, searchBy);

        // Send search results to the client
        out.writeUTF("Search Results: " + searchResults.toString());
        out.flush();
    }


    private void handleAddBook(DataInputStream in, DataOutputStream out) throws IOException {
        String title = in.readUTF();
        String author = in.readUTF();
        String genre = in.readUTF();
        double price = in.readDouble();
        int quantity = in.readInt();

        if (dbConnection.addBook(title, author, genre, price, quantity)) {
            out.writeUTF("Book added successfully with details and review!");
        } else {
            out.writeUTF("Failed to add book with details and review.");
        }
        out.flush();
    }

    private void handleReviewBook(DataInputStream in, DataOutputStream out) throws IOException {
        String title = in.readUTF();
        int review = in.readInt(); // Read the review as an integer

        // Store review information in the database and calculate overall rating
        if (dbConnection.addReviewToBook(title, review)) {
            out.writeUTF("Review added!");
        } else {
            out.writeUTF("Failed to add review to the book.");
        }
        out.flush();
    }

    private void handleRemoveBook(DataInputStream in, DataOutputStream out) throws IOException {
        String title = in.readUTF();

        // Remove book using MongoDBConnection
        if (dbConnection.removeBook(title)) {
            out.writeUTF("Book removed successfully!");
        } else {
            out.writeUTF("Book not found in inventory.");
        }
        out.flush();
    }

    private void handleSubmitRequest(DataInputStream in, DataOutputStream out) throws IOException {
        String bookTitle = in.readUTF(); // Read book title from client

        // Submit request using MongoDBConnection
        if (dbConnection.submitRequest(Username, bookTitle)) {
            out.writeUTF("Request submitted successfully!");
        } else {
            out.writeUTF("Failed to submit request.");
        }
        out.flush();
    }

    private void handleRespondRequest(DataInputStream in, DataOutputStream out) throws IOException {
        String requestId = in.readUTF(); // Read requestId as a String
        String decision = in.readUTF(); // Read decision from client
        // Respond to request using MongoDBConnection
        if (dbConnection.respondToRequest(requestId, decision)) {
            // Fetch the request document from the database based on the request ID
            Document request = dbConnection.getRequestById(requestId);
            if (request != null) {
                if (decision.equals("approved")) {
                    // Extract the book title from the request document
                    String bookTitle = request.getString("bookTitle");
                    if (bookTitle != null) {
                        if (dbConnection.updateBooksForUser(Username, bookTitle)) {
                            out.writeUTF("Request response processed successfully and book added to user's books.");
                            out.flush();
                        } else {
                            out.writeUTF("Failed to process request response.");
                        }
                    } else {
                        out.writeUTF("Book title not found in the request.");
                    }
                } else {
                    out.writeUTF("Request response processed successfully.");
                }
            } else {
                out.writeUTF("Request not found.");
            }
        } else {
            out.writeUTF("Failed to process request response.");
        }
        out.flush();
    }


    private void handleRequestHistory(DataOutputStream out) throws IOException {
        // Retrieve request history using MongoDBConnection for the given username
        List<Document> requestHistory = dbConnection.getRequestHistory();
        out.writeUTF("Request history: " + requestHistory.toString());
        out.flush();
    }

    // handleLibraryStatistics Method
    private void handleLibraryStatistics(DataOutputStream out) throws IOException {
        int borrowedBooks = dbConnection.getBorrowedBooksCount();
        int availableBooks = dbConnection.getAvailableBooksCount();
        int pendingRequests = dbConnection.getRequestsCount("pending");
        int acceptedRequests = dbConnection.getRequestsCount("approved");
        int rejectedRequests = dbConnection.getRequestsCount("rejected");

        String statistics = "Library Statistics:\n" +
                "Borrowed Books: " + borrowedBooks + "\n" +
                "Available Books: " + availableBooks + "\n" +
                "Pending Requests: " + pendingRequests + "\n" +
                "Accepted Requests: " + acceptedRequests + "\n" +
                "Rejected Requests: " + rejectedRequests;

        out.writeUTF(statistics);
        out.flush();
    }

    public List<String> getRecommendations(String type) {
        List<String> recommendedBooks = new ArrayList<>();

        switch (type) {
            case "genre":
                // Assuming you have a method to get all genres from the database
                List<String> genres = dbConnection.getAllGenres();
                // Display genres to the user and let them choose
                // Then get books based on the chosen genre
                System.out.println("Available Genres:");
                for (String genre : genres) {
                    System.out.println(genre);
                }
                System.out.print("Enter Genre: ");
                String chosenGenre = scanner.nextLine().trim();
                recommendedBooks.addAll(dbConnection.getBooksByGenre(chosenGenre));
                break;
            case "mostly Borrowed":
                // Get mostly borrowed genres and fetch books of those genres
                List<String> mostlyBorrowedGenres = dbConnection.getMostlyBorrowedGenres();
                for (String genre : mostlyBorrowedGenres) {
                    recommendedBooks.addAll(dbConnection.getBooksByGenre(genre));
                }
                break;
            case "General":
                // Fetch general recommendations, i.e., all books
                recommendedBooks.addAll(dbConnection.getAllBooks());
                break;
            default:
                System.out.println("Invalid recommendation type.");
        }

        return recommendedBooks;
    }

}
