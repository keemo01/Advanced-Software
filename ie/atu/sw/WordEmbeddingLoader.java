package ie.atu.sw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WordEmbeddingLoader {
    public Map<String, float[]> loadEmbeddings(String filePath) throws IOException {
        Map<String, float[]> embeddings = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Split by comma and space
                String[] parts = line.split("[,\\s]+");
                if (parts.length < 2) continue;

                // The first part is the word
                String word = parts[0].trim();
                
                // Parse the vector values
                float[] vector = new float[parts.length - 1];
                try {
                    for (int i = 1; i < parts.length; i++) {
                        String value = parts[i].trim();
                        // Remove any remaining commas
                        value = value.replace(",", "");
                        vector[i - 1] = Float.parseFloat(value);
                    }
                    embeddings.put(word, vector);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
            }
        }
        return embeddings;
    }
}