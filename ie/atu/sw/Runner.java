package ie.atu.sw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.StructuredTaskScope;


/**
 * The Runner class provides a console-based interface for configuring 
 * and running a word clustering application using virtual threads.
 * 
 * <p>Main features include:</p>
 * <ul>
 *   <li>Loading word embeddings</li>
 *   <li>Searching for words</li>
 *   <li>Configuring threads and clusters</li>
 *   <li>Performing k-means clustering</li>
 * </ul>
 */
public class Runner {

// Global variables for configuring threads and clusters
    private static int numberOfThreads = 0;
    private static int numberOfClusters = 0;
    private static boolean isConfigured = false; // Monitors whether Option 4 has been used.


    /**
     * The main entry point for the application.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        // ExecutorService for virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (!exit) {
                // Displaying the menu
                System.out.println(ConsoleColour.WHITE);
                System.out.println("************************************************************");
                System.out.println("*     ATU - Dept. of Computer Science & Applied Physics    *");
                System.out.println("*                                                          *");
                System.out.println("*            Word Clustering with Virtual Threads          *");
                System.out.println("*                                                          *");
                System.out.println("************************************************************");
                System.out.println("(1) Specify a Word Embedding File");
                System.out.println("(2) Specify a Search Word");
                System.out.println("(3) Specify an Output File (default: ./out.txt)");
                System.out.println("(4) Configure Threads / Clusters");
                System.out.println("(5) Build Clusters");
                System.out.println("(0) Exit");
                System.out.print("Select Option [1-5, 0 to exit]> ");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume the newline character

                switch (choice) {
                    case 1:
                        handleEmbeddingsLoading(scanner, executor);
                        break;            
                    case 2:
                        handleWordSearch(scanner);
                        break;                
                    case 3:
                        System.out.println("Enter the output file path (default: ./out.txt):");
                        String outputFilePath = scanner.nextLine();
                        if (outputFilePath.isEmpty()) {
                            outputFilePath = "./out.txt"; // Default output file
                        }
                        // Save the application state's file path.
                        ApplicationState.setOutputFilePath(outputFilePath);
                        System.out.println("Output file path set to: " + outputFilePath);
                        break;
                    case 4:
                        configureThreadsAndClusters(scanner);
                        break;
                    case 5:
                        buildClusters();
                        break;
                    case 0:
                        exit = true;
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid option, please try again.");
                }

                if (choice != 0) {
                    try {
                        simulateProgress();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } // ExecutorService is automatically closed here

        scanner.close();
    }


    /**
     * Configures the number of threads and clusters for k-means clustering.
     *
     * @param scanner the Scanner instance for user input
     */
    private static void configureThreadsAndClusters(Scanner scanner) {
        System.out.println("Enter the number of threads:");
        numberOfThreads = scanner.nextInt();
        System.out.println("Enter the number of clusters:");
        numberOfClusters = scanner.nextInt();
        scanner.nextLine(); // consume the newline character

        isConfigured = true;
        System.out.println("Configuration saved: Threads = " + numberOfThreads + ", Clusters = " + numberOfClusters);
    }


    /**
     * Uses the k-means algorithm to make the clusters.
     *
     * <p>The user is prompted to configure threads and clusters first if they are not already configured.</p>
     */
    private static void buildClusters() {
        if (!isConfigured) {
            System.out.println("Please configure threads and clusters first using Option 4.");
            return;
        }

        System.out.println("Building clusters...");
        String outputFilePath = ApplicationState.getOutputFilePath();

        try {
            clusterAndSaveResults(outputFilePath);
            System.out.println("Clustering results saved to " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Failed to save clustering results: " + e.getMessage());
        }
    }

    private static void clusterAndSaveResults(String outputPath) throws IOException {
        if (!ApplicationState.hasWordEmbeddings()) {
            System.out.println("No word embeddings loaded. Please load embeddings first.");
            return;
        }
    
        // Get embeddings for words
        Map<String, float[]> wordEmbeddings = ApplicationState.getWordEmbeddings();
    
        // Perform clustering
        KMeansClustering clustering = new KMeansClustering(wordEmbeddings, numberOfClusters);
        KMeansClustering.ClusteringResult result = clustering.performClustering();
    
        // Write results to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write clustering results
            writer.write("Clustering results:\n");
            List<List<KMeansClustering.WordWithDistance>> clusters = result.getClusters();
            List<float[]> centroids = result.getCentroids();
    
            for (int i = 0; i < clusters.size(); i++) {
                writer.write("- Cluster " + (i + 1) + ": \n");
                for (KMeansClustering.WordWithDistance wordDetail : clusters.get(i)) {
                    writer.write(String.format("  - %s (Distance: %.6f)\n", 
                        wordDetail.getWord(), 
                        wordDetail.getDistance()));
                }
            }
    
            // Write cluster centroids
            writer.write("\nCluster centroids:\n");
            for (int i = 0; i < centroids.size(); i++) {
                writer.write("- Cluster " + (i + 1) + " Centroid (average vector): " 
                    + arrayToString(centroids.get(i)) + "\n");
            }
        }
    
        System.out.println("Clustering results saved to " + outputPath);
    }

    /**
     * Handles the loading of word embeddings.
     *
     * @param scanner  the Scanner instance for user input
     * @param executor the ExecutorService for managing virtual threads
     */
    private static void handleEmbeddingsLoading(Scanner scanner, ExecutorService executor) {
        System.out.println("Enter the path to the word embedding file:");
        String embeddingsFile = scanner.nextLine();
        System.out.println("You selected: " + embeddingsFile);
    
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Submit the loading task to the scope
            var future = scope.fork(() -> {
                WordEmbeddingLoader loader = new WordEmbeddingLoader();
                return loader.loadEmbeddings(embeddingsFile);
            });
            
            // Wait for the task to complete
            scope.join();
            scope.throwIfFailed(); // Propagate any exceptions
            
            // Update the application state after obtaining the result.
            Map<String, float[]> wordEmbeddings = future.get();
            ApplicationState.setWordEmbeddings(wordEmbeddings, embeddingsFile);
            System.out.println("Word embeddings loaded successfully with " + wordEmbeddings.size() + " entries.");
            
        } catch (Exception e) {
            System.err.println("Error loading embeddings: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Handles searching for a word in the loaded word embeddings.
     *
     * <p>If no embeddings are loaded, the user is prompted to load them first.</p>
     *
     * @param scanner the Scanner instance for user input
     */
    private static void handleWordSearch(Scanner scanner) {
        // Check if word embeddings are loaded. If not, notify the user and exit the method.
        if (!ApplicationState.hasWordEmbeddings()) {
            System.out.println("No word embeddings loaded. Please load embeddings using Option 1 first.");
            return;
        }
    
        // Prompt the user to enter the word they want to search for.
        System.out.println("Enter the search word:");
        final String searchWordInput = scanner.nextLine().trim(); // Trim spaces from user input.
    
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Use structured concurrency to perform the search in a separate thread.
            var future = scope.fork(() -> {
                // Retrieve the loaded word embeddings from the application state.
                Map<String, float[]> wordEmbeddings = ApplicationState.getWordEmbeddings();
                String foundWord = searchWordInput; // Default the found word to the user's input.
                float[] embedding = null; // This will hold the vector for the word, if found.
    
                // Attempt to find an exact match for the input word.
                embedding = wordEmbeddings.get(searchWordInput);
    
                // If no exact match is found, perform a case-insensitive search.
                if (embedding == null) {
                    String lowerSearchWord = searchWordInput.toLowerCase(); // Convert input to lowercase.
                    for (Map.Entry<String, float[]> entry : wordEmbeddings.entrySet()) {
                        // Compare words in a case-insensitive manner.
                        if (entry.getKey().toLowerCase().equals(lowerSearchWord)) {
                            embedding = entry.getValue(); // Assign the matched vector.
                            foundWord = entry.getKey(); // Update to the exact word found in embeddings.
                            break; // Stop searching once a match is found.
                        }
                    }
                }
    
                // Return the search result, either with a matching embedding or null if not found.
                return embedding != null ? 
                    new SearchResult(foundWord, embedding) : 
                    new SearchResult(searchWordInput, null);
            });
    
            // Wait for the search task to complete and handle any exceptions that occur.
            scope.join();
            scope.throwIfFailed();
    
            // Retrieve the result of the search.
            SearchResult result = future.get();
    
            // Check if a word embedding was found.
            if (result.embedding() != null) {
                System.out.println("Word embedding for '" + result.word() + "': " + arrayToString(result.embedding()));
                System.out.println("Vector dimension: " + result.embedding().length); // Display the vector's dimensions.
            } else {
                System.out.println("The word '" + result.word() + "' was not found in the embeddings.");
            }
    
        } catch (InterruptedException e) {
            // Handle cases where the thread is interrupted during execution.
            System.err.println("Search was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt(); // Restore the interrupt status.
        } catch (Exception e) {
            // Handle any other unexpected exceptions during the search.
            System.err.println("Error while searching for the word: " + e.getMessage());
        }
    }

    private record SearchResult(String word, float[] embedding) {}

    private static String arrayToString(float[] array) {
        if (array == null || array.length == 0) return "[]";
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(String.format("%.6f", array[i]));
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static void simulateProgress() throws InterruptedException {
        System.out.print(ConsoleColour.GREEN);
        int size = 100;
        for (int i = 0; i < size; i++) {
            printProgress(i + 1, size);
            Thread.sleep(10);
        }
    }

    public static void printProgress(int index, int total) {
        if (index > total) return;
        int size = 50;
        char done = '█';
        char todo = '░';
        
        int complete = (100 * index) / total;
        int completeLen = size * complete / 100;
        
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < size; i++) {
            sb.append((i < completeLen) ? done : todo);
        }

        System.out.print("\r" + sb + "] " + complete + "%");

        if (index == total) {
            System.out.println("\n");
        }
    }
}