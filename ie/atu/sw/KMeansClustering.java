package ie.atu.sw;

import java.util.*;
import java.util.stream.Collectors;

public class KMeansClustering {
    private final Map<String, float[]> wordEmbeddings;
    private final int numClusters;
    private final int maxIterations;

    public KMeansClustering(Map<String, float[]> wordEmbeddings, int numClusters) {
        this.wordEmbeddings = wordEmbeddings;
        this.numClusters = numClusters;
        this.maxIterations = 100; // Default max iterations
    }

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

    // Initialize centroids by selecting random vectors from the list
    private List<float[]> initializeCentroids(List<float[]> vectors) {
        Random random = new Random();
        List<float[]> centroids = new ArrayList<>();
        for (int i = 0; i < numClusters; i++) {
            int randomIndex = random.nextInt(vectors.size());
            centroids.add(vectors.get(randomIndex)); // Randomly choose a vector as a centroid
        }
        return centroids;
    }

    // Find the closest centroid to a given vector
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

    // Calculate Euclidean distance between two vectors
    private double calculateEuclideanDistance(float[] v1, float[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
    }

    // Recalculate centroids by averaging the vectors in each cluster
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

    // Check if centroids have converged
    private boolean centroidsConverged(List<float[]> oldCentroids, List<float[]> newCentroids) {
        for (int i = 0; i < oldCentroids.size(); i++) {
            if (calculateEuclideanDistance(oldCentroids.get(i), newCentroids.get(i)) > 0.0001) {
                return false; // If centroids have changed by more than a small threshold
            }
        }
        return true;
    }

    // Prepare and return the clustering result
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

    // Inner class to hold word with its distance from centroid
    public static class WordWithDistance {
        private final String word;
        private final double distance;

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

    // Updated ClusteringResult to use WordWithDistance
    public static class ClusteringResult {
        private final List<List<WordWithDistance>> clusters;
        private final List<float[]> centroids;

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
