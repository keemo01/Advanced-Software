package ie.atu.sw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KMeansClustering {
    private final int numClusters;
    private List<float[]> centroids;
    private int[] assignedCluster;

    public KMeansClustering(int numClusters) {
        this.numClusters = numClusters;
        this.centroids = new ArrayList<>(numClusters);
    }

    public void cluster(List<float[]> vectors, int numThreads) {
        int n = vectors.size();
        assignedCluster = new int[n];
        boolean[] changed = new boolean[1];  // Use an array to make it effectively final
    
        // Randomly initialize centroids
        Random rand = new Random();
        for (int i = 0; i < numClusters; i++) {
            centroids.add(vectors.get(rand.nextInt(vectors.size())));
        }
    
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        do {
            changed[0] = false;
    
            // Step 1: Assign each point to the nearest centroid
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final int idx = i;
                tasks.add(() -> {
                    float[] vector = vectors.get(idx);
                    int closestCluster = findClosestCentroid(vector);
                    if (assignedCluster[idx] != closestCluster) {
                        assignedCluster[idx] = closestCluster;
                        changed[0] = true;
                    }
                    return null;
                });
            }
    
            try {
                executor.invokeAll(tasks);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    
            // Step 2: Recalculate centroids
            recalculateCentroids(vectors);
        } while (changed[0]); // Repeat until no points change clusters
    
        executor.shutdown();
    }
    

    private int findClosestCentroid(float[] vector) {
        float minDistance = Float.MAX_VALUE;
        int closestCluster = -1;
        for (int i = 0; i < numClusters; i++) {
            float distance = calculateDistance(vector, centroids.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestCluster = i;
            }
        }
        return closestCluster;
    }

    private void recalculateCentroids(List<float[]> vectors) {
        List<float[]> newCentroids = new ArrayList<>(numClusters);
        int[] clusterSizes = new int[numClusters];
        for (int i = 0; i < numClusters; i++) {
            newCentroids.add(new float[vectors.get(0).length]);
        }

        for (int i = 0; i < vectors.size(); i++) {
            int cluster = assignedCluster[i];
            float[] vector = vectors.get(i);
            for (int j = 0; j < vector.length; j++) {
                newCentroids.get(cluster)[j] += vector[j];
            }
            clusterSizes[cluster]++;
        }

        for (int i = 0; i < numClusters; i++) {
            if (clusterSizes[i] > 0) {
                for (int j = 0; j < newCentroids.get(i).length; j++) {
                    newCentroids.get(i)[j] /= clusterSizes[i];
                }
            }
        }

        centroids = newCentroids;
    }

    private float calculateDistance(float[] vector1, float[] vector2) {
        float distance = 0;
        for (int i = 0; i < vector1.length; i++) {
            distance += Math.pow(vector1[i] - vector2[i], 2);
        }
        return (float) Math.sqrt(distance);
    }

    public int getAssignedCluster(int idx) {
        return assignedCluster[idx];
    }
}
