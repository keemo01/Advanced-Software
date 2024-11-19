package ie.atu.sw;

import java.util.Map;

/**
 * The application's state, including file locations and word embeddings, is stored in this class.
 * It offers ways to set and retrieve the embeddings file path, word embeddings,
 * and the path to the output file. Additionally, it verifies whether the word embeddings have been loaded.
 */
public class ApplicationState {
    
    private static Map<String, float[]> wordEmbeddings;
    private static String embeddingsFilePath;

    private static String outputFilePath = "./out.txt"; // Default path

    /**
     * Verifies whether the word embeddings have been loaded.
     *
     * @return {@code true} if the word embeddings are loaded, {@code false} otherwise.
     */
    public static boolean hasWordEmbeddings() {
        return wordEmbeddings != null && !wordEmbeddings.isEmpty();
    }

    /**
     * Sets the file path and word embeddings.
     *
     * @param embeddings a map where the key is the word and the value is the corresponding embedding vector.
     * @param filePath the file path from which the word embeddings were loaded.
     */
    public static void setWordEmbeddings(Map<String, float[]> embeddings, String filePath) {
        wordEmbeddings = embeddings;
        embeddingsFilePath = filePath;
    }

    /**
     * Retrieves the word embeddings.
     *
     * @return a map of word embeddings.
     * @throws IllegalStateException if the word embeddings have not been loaded.
     */
    public static Map<String, float[]> getWordEmbeddings() {
        if (!hasWordEmbeddings()) {
            throw new IllegalStateException("Word embeddings are not loaded.");
        }
        return wordEmbeddings;
    }

    /**
     * Obtains the loaded embeddings' file path.
     *
     * @return the file path from which the embeddings were loaded.
     */
    public static String getEmbeddingsFilePath() {
        return embeddingsFilePath;
    }

    /**
     * Specifies the output file path for logs or results.
     *
     * @param path the path to the output file.
     */
    public static void setOutputFilePath(String path) {
        outputFilePath = path;
    }

    /**
     * Returns the path to the output file containing the logs or results.
     *
     * @return the output file path.
     */
    public static String getOutputFilePath() {
        return outputFilePath;
    }
}
