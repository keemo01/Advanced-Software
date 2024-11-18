package ie.atu.sw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.StructuredTaskScope;

public class Runner {

    // Global variables for threads and clusters configuration
    private static int numberOfThreads = 0;
    private static int numberOfClusters = 0;
    private static boolean isConfigured = false; // Tracks if Option 4 has been executed

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
                        // Save the file path to the application state
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

    private static void configureThreadsAndClusters(Scanner scanner) {
        System.out.println("Enter the number of threads:");
        numberOfThreads = scanner.nextInt();
        System.out.println("Enter the number of clusters:");
        numberOfClusters = scanner.nextInt();
        scanner.nextLine(); // consume the newline character

        isConfigured = true;
        System.out.println("Configuration saved: Threads = " + numberOfThreads + ", Clusters = " + numberOfClusters);
    }

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
        // Simulate clustering logic
        String clusteringResults = performClustering(numberOfThreads, numberOfClusters);

        // Write results to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(clusteringResults);
        }
    }

    private static String performClustering(int threads, int clusters) {
        // Mock implementation of clustering logic
        StringBuilder result = new StringBuilder();
        result.append("Clustering completed with ").append(threads).append(" threads\n");
        result.append("Clusters formed: ").append(clusters).append("\n");
        result.append("Cluster results:\n");

        // Mock cluster data
        for (int i = 1; i <= clusters; i++) {
            result.append("Cluster ").append(i).append(": [Sample Data]\n");
        }

        return result.toString();
    }

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
            
            // Get the result and update the application state
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

    private static void handleWordSearch(Scanner scanner) {
        if (!ApplicationState.hasWordEmbeddings()) {
            System.out.println("No word embeddings loaded. Please load embeddings using Option 1 first.");
            return;
        }
    
        System.out.println("Enter the search word:");
        final String searchWordInput = scanner.nextLine().trim();
    
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var future = scope.fork(() -> {
                Map<String, float[]> wordEmbeddings = ApplicationState.getWordEmbeddings();
                String foundWord = searchWordInput;
                float[] embedding = null;
                
                // Try exact match first
                embedding = wordEmbeddings.get(searchWordInput);
                
                // If no exact match, try case-insensitive search
                if (embedding == null) {
                    String lowerSearchWord = searchWordInput.toLowerCase();
                    for (Map.Entry<String, float[]> entry : wordEmbeddings.entrySet()) {
                        if (entry.getKey().toLowerCase().equals(lowerSearchWord)) {
                            embedding = entry.getValue();
                            foundWord = entry.getKey();
                            break;
                        }
                    }
                }
                
                return embedding != null ? 
                    new SearchResult(foundWord, embedding) : 
                    new SearchResult(searchWordInput, null);
            });
            
            scope.join();
            scope.throwIfFailed();
            
            SearchResult result = future.get();
            if (result.embedding() != null) {
                System.out.println("Word embedding for '" + result.word() + "': " + arrayToString(result.embedding()));
                System.out.println("Vector dimension: " + result.embedding().length);
            } else {
                System.out.println("The word '" + result.word() + "' was not found in the embeddings.");
            }
            
        } catch (InterruptedException e) {
            System.err.println("Search was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
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