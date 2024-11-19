package ie.atu.sw;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements the K-Means clustering algorithm for word embeddings.
 * It provides methods for clustering a set of word embeddings into a specified number of clusters.
 * The clustering process iterates over a predefined maximum number of iterations, updating centroids
 * and assigning words to the nearest centroid until convergence or the maximum iterations are reached.
 */
public class KMeansClustering {
    
    private final Map<String, float[]> wordEmbeddings;
    private final int numClusters;
    private final int maxIterations;

    /**
     * Constructs a KMeansClustering object.
     *
     * @param wordEmbeddings a map where the key is a word and the value is its embedding (a vector of floats).
     * @param numClusters the number of clusters to form.
     */
    public KMeansClustering(Map<String, float[]> wordEmbeddings, int numClusters) {
        this.wordEmbeddings = wordEmbeddings;
        this.numClusters = numClusters;
        this.maxIterations = 100; // Default max iterations
    }

    /**
     * Performs K-Means clustering on the word embeddings.
     *
     * @return a ClusteringResult object containing the clusters and centroids.
     */
    public ClusteringResult performClustering() {
        // Convert embeddings to a list for easier processing
        List<String> words = new ArrayList<>(wordEmbeddings.keySet());
        List<float[]> vectors = words.stream()
            .map(wordEmbeddings::get)
            .collect(Collectors.toList());

        // Initialize centroids randomly
        List<float[]> centroids = initializeCentroids(vectors);

        // Cluster assignment and iteration
        List<List<Integer>> clusters = new ArrayList<>(numClusters);
        List<List<Double>> clusterDistances = new ArrayList<>(numClusters);
        for (int i = 0; i < numClusters; i++) {
            clusters.add(new ArrayList<>());
            clusterDistances.add(new ArrayList<>());
        }

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Clear previous clusters
            clusters.forEach(List::clear);
            clusterDistances.forEach(List::clear);

            // Assign each word to the nearest centroid
            for (int i = 0; i < vectors.size(); i++) {
                int closestCentroidIndex = findClosestCentroid(vectors.get(i), centroids);
                double distance = calculateEuclideanDistance(vectors.get(i), centroids.get(closestCentroidIndex));
                
                clusters.get(closestCentroidIndex).add(i);
                clusterDistances.get(closestCentroidIndex).add(distance);
            }

            // Recalculate centroids
            List<float[]> newCentroids = calculateNewCentroids(vectors, clusters);

            // Check for convergence
            if (centroidsConverged(centroids, newCentroids)) {
                centroids = newCentroids;
                break;
            }
            centroids = newCentroids;
        }

        // Prepare clustering results
        return createClusteringResult(words, vectors, clusters, centroids, clusterDistances);
    }

    /**
     * Initializes the centroids by randomly selecting vectors from the list of embeddings.
     *
     * @param vectors a list of word embedding vectors.
     * @return a list of randomly chosen centroids.
     */
    private List<float[]> initializeCentroids(List<float[]> vectors) {
        Random random = new Random();
        List<float[]> centroids = new ArrayList<>();
        for (int i = 0; i < numClusters; i++) {
            int randomIndex = random.nextInt(vectors.size());
            centroids.add(vectors.get(randomIndex)); // Randomly choose a vector as a centroid
        }
        return centroids;
    }

    /**
     * Finds the index of the closest centroid to a given vector.
     *
     * @param vector the word embedding vector.
     * @param centroids the list of centroids.
     * @return the index of the closest centroid.
     */
    private int findClosestCentroid(float[] vector, List<float[]> centroids) {
        int closestCentroidIndex = -1;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < centroids.size(); i++) {
            double distance = calculateEuclideanDistance(vector, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestCentroidIndex = i;
            }
        }
        return closestCentroidIndex;
    }

    /**
     * Calculates the Euclidean distance between two vectors.
     *
     * @param v1 the first vector.
     * @param v2 the second vector.
     * @return the Euclidean distance between the two vectors.
     */
    private double calculateEuclideanDistance(float[] v1, float[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Recalculates the centroids by averaging the vectors in each cluster.
     *
     * @param vectors the list of word embedding vectors.
     * @param clusters a list of clusters, each containing indices of vectors.
     * @return a list of new centroids.
     */
    private List<float[]> calculateNewCentroids(List<float[]> vectors, List<List<Integer>> clusters) {
        List<float[]> newCentroids = new ArrayList<>();
        for (List<Integer> cluster : clusters) {
            float[] newCentroid = new float[vectors.get(0).length];
            for (int index : cluster) {
                for (int i = 0; i < vectors.get(index).length; i++) {
                    newCentroid[i] += vectors.get(index)[i];
                }
            }
            for (int i = 0; i < newCentroid.length; i++) {
                newCentroid[i] /= cluster.size(); // Average the vectors
            }
            newCentroids.add(newCentroid);
        }
        return newCentroids;
    }

    /**
     * Checks if the centroids have converged (i.e., if the change in centroids is smaller than a threshold).
     *
     * @param oldCentroids the previous centroids.
     * @param newCentroids the new centroids.
     * @return {@code true} if the centroids have converged, {@code false} otherwise.
     */
    private boolean centroidsConverged(List<float[]> oldCentroids, List<float[]> newCentroids) {
        for (int i = 0; i < oldCentroids.size(); i++) {
            if (calculateEuclideanDistance(oldCentroids.get(i), newCentroids.get(i)) > 0.0001) {
                return false; // If centroids have changed by more than a small threshold
            }
        }
        return true;
    }

    /**
     * Prepares and returns the clustering result, which includes the clusters and centroids.
     *
     * @param words the list of words.
     * @param vectors the list of word embedding vectors.
     * @param clusters the list of clusters.
     * @param centroids the list of centroids.
     * @param clusterDistances the distances of the words from their respective centroids.
     * @return a ClusteringResult object containing the clusters and centroids.
     */
    private ClusteringResult createClusteringResult(
            List<String> words, 
            List<float[]> vectors, 
            List<List<Integer>> clusters, 
            List<float[]> centroids,
            List<List<Double>> clusterDistances) {
        
        List<List<WordWithDistance>> clusterWordDetails = new ArrayList<>();
        List<float[]> clusterCentroids = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            List<WordWithDistance> currentClusterWords = new ArrayList<>();
            for (int j = 0; j < clusters.get(i).size(); j++) {
                int index = clusters.get(i).get(j);
                String word = words.get(index);
                double distance = clusterDistances.get(i).get(j);
                currentClusterWords.add(new WordWithDistance(word, distance));
            }
            clusterWordDetails.add(currentClusterWords);
            clusterCentroids.add(centroids.get(i));
        }

        return new ClusteringResult(clusterWordDetails, clusterCentroids);
    }

    /**
     * Inner class to hold a word along with its distance from the centroid.
     */
    public static class WordWithDistance {
        private final String word;
        private final double distance;

        /**
         * Constructs a WordWithDistance object.
         *
         * @param word the word.
         * @param distance the distance of the word from the centroid.
         */
        public WordWithDistance(String word, double distance) {
            this.word = word;
            this.distance = distance;
        }

        public String getWord() {
            return word;
        }

        public double getDistance() {
            return distance;
        }
    }

    /**
     * Inner class representing the result of the clustering operation.
     * It contains the clusters of words and their corresponding centroids.
     */
    public static class ClusteringResult {
        private final List<List<WordWithDistance>> clusters;
        private final List<float[]> centroids;

        /**
         * Constructs a ClusteringResult object.
         *
         * @param clusters the list of clusters, where each cluster is a list of words with distances.
         * @param centroids the list of centroids corresponding to the clusters.
         */
        public ClusteringResult(List<List<WordWithDistance>> clusters, List<float[]> centroids) {
            this.clusters = clusters;
            this.centroids = centroids;
        }

        public List<List<WordWithDistance>> getClusters() {
            return clusters;
        }

        public List<float[]> getCentroids() {
            return centroids;
        }
    }
}
