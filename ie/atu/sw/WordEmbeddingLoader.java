package ie.atu.sw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Word embeddings are loaded from a file by this class. 
 * It parses the word and its matching embedding vector after reading the file line by line.
 * and keeps them in a map with the word as the key and its embedding as the value.
 */
public class WordEmbeddingLoader {

    /**
     * Loads word embeddings from a specified file.
     *
     * @param filePath the file path where the word embeddings are located.
     * @return a map with a word as the key and its embedding (a float array) as the value.
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public Map<String, float[]> loadEmbeddings(String filePath) throws IOException {
        Map<String, float[]> embeddings = new HashMap<>();
        
        // Read the file line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Remove leading and trailing whitespace
                if (line.isEmpty()) continue; // Skip empty lines
                
                // Split the line by comma or space
                String[] parts = line.split("[,\\s]+");
                
                if (parts.length < 2) continue; // Skip lines with no word or vector
                
                // The first part is the word
                String word = parts[0].trim();
                
                // Parse the vector values from the remaining parts
                float[] vector = new float[parts.length - 1];
                try {
                    for (int i = 1; i < parts.length; i++) {
                        String value = parts[i].trim();
                        value = value.replace(",", ""); // Remove any remaining commas
                        vector[i - 1] = Float.parseFloat(value);
                    }
                    embeddings.put(word, vector); // Add the word and vector to the map
                } catch (NumberFormatException e) {
                    System.err.println("Skipping malformed line: " + line); // Log invalid lines
                    continue;
                }
            }
        }
        return embeddings;
    }
}
