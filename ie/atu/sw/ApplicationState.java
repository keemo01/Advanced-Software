package ie.atu.sw;

import java.util.Map;

public class ApplicationState {
    private static Map<String, float[]> wordEmbeddings;
    private static String embeddingsFilePath;

    // Checks if word embeddings are loaded
    public static boolean hasWordEmbeddings() {
        return wordEmbeddings != null && !wordEmbeddings.isEmpty();
    }

    // Sets the word embeddings and file path
    public static void setWordEmbeddings(Map<String, float[]> embeddings, String filePath) {
        wordEmbeddings = embeddings;
        embeddingsFilePath = filePath;
    }

    // Retrieves the word embeddings
    public static Map<String, float[]> getWordEmbeddings() {
        if (!hasWordEmbeddings()) {
            throw new IllegalStateException("Word embeddings are not loaded.");
        }
        return wordEmbeddings;
    }

    // Retrieves the file path of the loaded embeddings
    public static String getEmbeddingsFilePath() {
        return embeddingsFilePath;
    }
}
