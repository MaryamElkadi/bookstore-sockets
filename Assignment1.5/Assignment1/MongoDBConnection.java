import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MongoDBConnection {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> usersCollection;
    private MongoCollection<Document> booksCollection;
    private MongoCollection<Document> requestsCollection;
    private MongoDBConnection dbConnection;
    private Scanner scanner = new Scanner(System.in);

    public MongoDBConnection() {
        String connectionString = "mongodb+srv://amrmoh:VjKZx2ZlZs0vASho@onlinebookstore.gg5c6cr.mongodb.net/?retryWrites=true&w=majority&appName=OnlineBookstore";
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase("OnlineBookstore");
        this.usersCollection = database.getCollection("users");
        this.booksCollection = database.getCollection("books");
        this.requestsCollection = database.getCollection("requests");
    }

    public void testConnection() {
        try {
            // Send a ping to confirm a successful connection
            database.runCommand(new Document("ping", 1));
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean authenticateUser(String username, String password) {
        Document user = usersCollection.find(Filters.eq("username", username)).first();
        if (user != null) {
            String storedPassword = user.getString("password");
            return storedPassword.equals(password);
        }
        return false;
    }
    public boolean registerUser(String name, String username, String password) {
        Document newUser = new Document()
                .append("name", name)
                .append("username", username)
                .append("password", password);

        try {
            usersCollection.insertOne(newUser);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getAllBooks() {
        List<String> books = new ArrayList<>();
        for (Document doc : booksCollection.find()) {
            books.add(doc.getString("title"));
        }
        return books;
    }

    public List<String> searchBooks(String searchTerm, String searchBy) {
        List<String> searchResults = new ArrayList<>();
        MongoCollection<Document> booksCollection = database.getCollection("books");

        // Create a query filter based on the searchBy field and searchTerm
        Document queryFilter = new Document();
        if (searchBy.equalsIgnoreCase("title")) {
            queryFilter.append("title", searchTerm);
        } else if (searchBy.equalsIgnoreCase("author")) {
            queryFilter.append("author", searchTerm);
        } else if (searchBy.equalsIgnoreCase("genre")) {
            queryFilter.append("genre", searchTerm);
        } else {
            System.out.println("There is No Book with this name");
            return searchResults;
        }

        // Perform the query on the booksCollection
        FindIterable<Document> queryResult = booksCollection.find(queryFilter);

        // Iterate over the query result and add matching book titles to the searchResults list
        for (Document book : queryResult) {
            searchResults.add(book.getString("title"));
        }

        return searchResults;
    }

    public boolean addBook(String title, String author, String genre, double price, int quantity) {
        Document newBook = new Document()
                .append("title", title)
                .append("author", author)
                .append("genre", genre)
                .append("price", price)
                .append("quantity", quantity)
                .append("clients bought the book", new ArrayList<String>()) // Initialize empty list of clients
                .append("reviews", 0);

        try {
            booksCollection.insertOne(newBook);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean removeBook(String title) {
        try {
            booksCollection.deleteOne(new Document("title", title));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean submitRequest(String username , String bookTitle) {
        Document newRequest = new Document().append("bookTitle", bookTitle).append("status", "pending").append("username", username);
        try {
            requestsCollection.insertOne(newRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean respondToRequest(String requestId, String decision) {
        try {
            // Parse requestId to ObjectId
            ObjectId objectId = new ObjectId(requestId);

            // Update the status of the request in the database
            Document query = new Document("_id", objectId); // Use objectId in the query
            Document update = new Document("$set", new Document("status", decision));
            requestsCollection.updateOne(query, update);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean updateBooksForUser(String username, String bookTitle) {
        try {
            // Find the user's document
            Document user = usersCollection.find(Filters.eq("username", username)).first();
            if (user != null) {
                // Add the approved book to the user's list of books
                List<String> books = user.get("books", List.class);
                if (books == null) {
                    books = new ArrayList<>();
                }
                books.add(bookTitle);
                // Update the user's document with the new list of books
                usersCollection.updateOne(Filters.eq("username", username), Updates.set("books", books));
                return true;
            } else {
                System.out.println("User not found.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public Document getRequestById(String requestId) {
        try {
            // Parse requestId to ObjectId
            ObjectId objectId = new ObjectId(requestId);

            // Find the request document
            Document request = requestsCollection.find(Filters.eq("_id", objectId)).first();
            return request;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public List<Document> getRequestHistory() {
        List<Document> history = new ArrayList<>();
        for (Document doc : requestsCollection.find()) {
            history.add(doc);
        }
        return history;
    }


    public boolean addReviewToBook(String title, int review) {
        try {
            // Find the document corresponding to the book title
            Document book = booksCollection.find(Filters.eq("title", title)).first();
            if (book != null) {
                // Update the review field in the document
                List<Integer> reviews = book.get("reviews", List.class);
                if (reviews == null) {
                    reviews = new ArrayList<>();
                }
                reviews.add(review);
                // Update the document in the collection with the updated list of reviews
                booksCollection.updateOne(Filters.eq("title", title), Updates.set("reviews", reviews));
                // Calculate overall rating
                double sum = 0.0;
                for (int r : reviews) {
                    sum += r;
                }
                double avg = sum / reviews.size();
                // Update the overall rating field in the document
                booksCollection.updateOne(Filters.eq("title", title), Updates.set("overallRating", avg));

                return true;
            } else {
                // Book not found
                System.out.println("Book not found in the database.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Document> getBooksByGenreSortedByRating(String genre) {
        List<Document> sortedBooks = new ArrayList<>();
        try {
            // Find books by genre and sort them by rating in descending order
            FindIterable<Document> result = booksCollection.find(Filters.eq("genre", genre))
                    .sort(Sorts.descending("review"));

            // Iterate over the results and add them to the list
            MongoCursor<Document> cursor = result.iterator();
            while (cursor.hasNext()) {
                sortedBooks.add(cursor.next());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sortedBooks;
    }

    public List<String> getAllGenres() {
        List<String> genres = new ArrayList<>();

        try {
            // Retrieve distinct genres from the books collection
            List<String> distinctGenres = booksCollection.distinct("genre", String.class).into(new ArrayList<>());

            // Add distinct genres to the list
            genres.addAll(distinctGenres);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return genres;
    }

    public List<String> getBooksByGenre(String genre) {
        List<String> books = new ArrayList<>();

        try {
            // Query the books collection for documents matching the specified genre
            Document query = new Document("genre", genre);
            FindIterable<Document> result = booksCollection.find(query);

            // Iterate over the query result and extract book titles
            MongoCursor<Document> cursor = result.iterator();
            while (cursor.hasNext()) {
                Document book = cursor.next();
                books.add(book.getString("title"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return books;
    }

    public boolean isUserExists(String username) {
        Document user = usersCollection.find(Filters.eq("username", username)).first();
        return user != null;
    }
    public int getBorrowedBooksCount() {
        try {
            // Count the number of borrowed books, where the quantity is less than 0 (indicating borrowed copies)
            return (int) booksCollection.countDocuments(Filters.lt("quantity", 0));
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 to indicate an error
        }
    }

    public int getAvailableBooksCount() {
        try {
            // Count the number of available books, where the quantity is greater than 0
            return (int) booksCollection.countDocuments(Filters.gt("quantity", 0));
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 to indicate an error
        }
    }

    public int getRequestsCount(String status) {
        try {
            if (status.equals("pending")) {
                return (int) requestsCollection.countDocuments(Filters.eq("status", "pending"));
            } else if (status.equals("approved")) {
                // Count both "accepted" and "rejected" requests
                return (int) requestsCollection.countDocuments(Filters.in("status", "approved"));
            }else if(status.equals("rejected")) {
                return (int) requestsCollection.countDocuments(Filters.in("status", "rejected"));
            }
            else {
                System.err.println("Invalid status: " + status);
                return -1; // Return -1 to indicate an error
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 to indicate an error
        }
    }



    public List<String> getUserPreferredGenres(String username) {
        List<String> preferredGenres = new ArrayList<>();
        try {
            // Retrieve user's preferred genres from the users collection
            Document user = usersCollection.find(Filters.eq("username", username)).first();
            if (user != null && user.containsKey("preferredGenres")) {
                preferredGenres = user.getList("preferredGenres", String.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return preferredGenres;
    }

    public List<String> getMostlyBorrowedGenres() {
        List<String> mostlyBorrowedGenres = new ArrayList<>();
        try {
            // Aggregate query to find mostly borrowed genres
            List<Document> results = requestsCollection.aggregate(Arrays.asList(
                    Aggregates.group("$bookGenre", Accumulators.sum("count", 1L)),
                    Aggregates.sort(Sorts.descending("count")),
                    Aggregates.limit(3) // Get top 3 mostly borrowed genres
            )).into(new ArrayList<>());

            for (Document doc : results) {
                mostlyBorrowedGenres.add(doc.getString("_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mostlyBorrowedGenres;
    }



    public boolean isAdmin(String username) {
        Document user = usersCollection.find(Filters.eq("username", username)).first();
        return user != null && user.getBoolean("isAdmin", false);
    }
}
