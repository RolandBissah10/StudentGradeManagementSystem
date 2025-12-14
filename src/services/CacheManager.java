package services;

import models.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheManager {
    // Cache configuration
    private static final int MAX_CACHE_SIZE = 150;
    private static final long CACHE_TTL = 300000; // 5 minutes in milliseconds

    // Caches using ConcurrentHashMap for thread safety
    private ConcurrentHashMap<String, CacheEntry<Student>> studentCache;
    private ConcurrentHashMap<String, CacheEntry<List<Grade>>> studentGradesCache;
    private ConcurrentHashMap<String, CacheEntry<Double>> studentAverageCache;
    private ConcurrentHashMap<String, CacheEntry<Map<String, Double>>> subjectAverageCache;

    // LRU tracking
    private LinkedHashMap<String, Long> accessTimes;

    // Statistics
    private AtomicInteger cacheHits = new AtomicInteger(0);
    private AtomicInteger cacheMisses = new AtomicInteger(0);
    private AtomicInteger evictions = new AtomicInteger(0);

    private static class CacheEntry<T> {
        T data;
        long timestamp;
        long accessCount;

        CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.accessCount = 1;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }

        void recordAccess() {
            accessCount++;
        }
    }

    public CacheManager() {
        studentCache = new ConcurrentHashMap<>();
        studentGradesCache = new ConcurrentHashMap<>();
        studentAverageCache = new ConcurrentHashMap<>();
        subjectAverageCache = new ConcurrentHashMap<>();

        // LRU tracking with access order
        accessTimes = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };

        // Start background cleanup thread
        startCleanupThread();
    }

    // Student caching
    public Student getStudent(String studentId) {
        CacheEntry<Student> entry = studentCache.get(studentId);
        if (entry != null && !entry.isExpired()) {
            entry.recordAccess();
            cacheHits.incrementAndGet();
            updateAccessTime("student:" + studentId);
            return entry.data;
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    public void cacheStudent(Student student) {
        if (student == null) return;

        ensureCacheSpace();
        studentCache.put(student.getStudentId(), new CacheEntry<>(student));
        updateAccessTime("student:" + student.getStudentId());
    }

    // Student grades caching
    public List<Grade> getStudentGrades(String studentId) {
        CacheEntry<List<Grade>> entry = studentGradesCache.get(studentId);
        if (entry != null && !entry.isExpired()) {
            entry.recordAccess();
            cacheHits.incrementAndGet();
            updateAccessTime("grades:" + studentId);
            return new ArrayList<>(entry.data); // Return copy for safety
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    public void cacheStudentGrades(String studentId, List<Grade> grades) {
        if (grades == null) return;

        ensureCacheSpace();
        studentGradesCache.put(studentId, new CacheEntry<>(new ArrayList<>(grades)));
        updateAccessTime("grades:" + studentId);
    }

    // Student average caching
    public Double getStudentAverage(String studentId) {
        CacheEntry<Double> entry = studentAverageCache.get(studentId);
        if (entry != null && !entry.isExpired()) {
            entry.recordAccess();
            cacheHits.incrementAndGet();
            updateAccessTime("average:" + studentId);
            return entry.data;
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    public void cacheStudentAverage(String studentId, Double average) {
        if (average == null) return;

        ensureCacheSpace();
        studentAverageCache.put(studentId, new CacheEntry<>(average));
        updateAccessTime("average:" + studentId);
    }

    // Subject average caching
    public Map<String, Double> getSubjectAverages(String studentId) {
        CacheEntry<Map<String, Double>> entry = subjectAverageCache.get(studentId);
        if (entry != null && !entry.isExpired()) {
            entry.recordAccess();
            cacheHits.incrementAndGet();
            updateAccessTime("subjectAvg:" + studentId);
            return new HashMap<>(entry.data);
        }
        cacheMisses.incrementAndGet();
        return null;
    }

    public void cacheSubjectAverages(String studentId, Map<String, Double> averages) {
        if (averages == null) return;

        ensureCacheSpace();
        subjectAverageCache.put(studentId, new CacheEntry<>(new HashMap<>(averages)));
        updateAccessTime("subjectAvg:" + studentId);
    }

    // Cache invalidation
    public void invalidateStudent(String studentId) {
        studentCache.remove(studentId);
        studentGradesCache.remove(studentId);
        studentAverageCache.remove(studentId);
        subjectAverageCache.remove(studentId);

        // Remove from LRU tracking
        accessTimes.keySet().removeIf(key -> key.contains(studentId));
    }

    public void invalidateAll() {
        studentCache.clear();
        studentGradesCache.clear();
        studentAverageCache.clear();
        subjectAverageCache.clear();
        accessTimes.clear();
        evictions.set(0);
    }

    // Cache warming
    public void warmCache(List<Student> students) {
        System.out.println("Warming cache with " + students.size() + " students...");
        students.parallelStream().forEach(student -> {
            cacheStudent(student);
            // Note: Grades and averages would need to be loaded separately
        });
        System.out.println("✓ Cache warming complete");
    }

    // Statistics
    public void displayCacheStatistics() {
        System.out.println("\n=== CACHE STATISTICS ===");
        System.out.println("Cache Type           | Entries | Memory (est)");
        System.out.println("----------------------------------------------");

        System.out.printf("Student Cache        | %7d | %6.1f KB%n",
                studentCache.size(), estimateMemory(studentCache) / 1024.0);
        System.out.printf("Grades Cache         | %7d | %6.1f KB%n",
                studentGradesCache.size(), estimateMemory(studentGradesCache) / 1024.0);
        System.out.printf("Average Cache        | %7d | %6.1f KB%n",
                studentAverageCache.size(), estimateMemory(studentAverageCache) / 1024.0);
        System.out.printf("Subject Average Cache| %7d | %6.1f KB%n",
                subjectAverageCache.size(), estimateMemory(subjectAverageCache) / 1024.0);

        System.out.println("\nPerformance Metrics:");
        int totalRequests = cacheHits.get() + cacheMisses.get();
        if (totalRequests > 0) {
            double hitRate = (cacheHits.get() * 100.0) / totalRequests;
            System.out.printf("Hit Rate:            %6.1f%% (%d/%d)%n",
                    hitRate, cacheHits.get(), totalRequests);
            System.out.printf("Miss Rate:           %6.1f%% (%d/%d)%n",
                    100 - hitRate, cacheMisses.get(), totalRequests);
        }

        System.out.printf("Evictions:           %7d (LRU policy)%n", evictions.get());
        System.out.printf("Total Memory:        %6.1f KB%n",
                (estimateMemory(studentCache) + estimateMemory(studentGradesCache) +
                        estimateMemory(studentAverageCache) + estimateMemory(subjectAverageCache)) / 1024.0);

        System.out.println("\nLRU Status:");
        System.out.printf("Max Size:            %7d entries%n", MAX_CACHE_SIZE);
        System.out.printf("Current Size:        %7d entries%n", accessTimes.size());
        System.out.printf("TTL:                 %7d minutes%n", CACHE_TTL / 60000);
    }

    private void ensureCacheSpace() {
        if (accessTimes.size() >= MAX_CACHE_SIZE * 0.8) { // 80% threshold
            cleanupExpiredEntries();
        }
    }

    private void cleanupExpiredEntries() {
        int evicted = 0;

        // Clean expired entries from all caches
        evicted += cleanupCache(studentCache, "student:");
        evicted += cleanupCache(studentGradesCache, "grades:");
        evicted += cleanupCache(studentAverageCache, "average:");
        evicted += cleanupCache(subjectAverageCache, "subjectAvg:");

        evictions.addAndGet(evicted);

        if (evicted > 0) {
            System.out.println("Cleaned up " + evicted + " expired cache entries");
        }
    }

    private <T> int cleanupCache(ConcurrentHashMap<String, CacheEntry<T>> cache, String prefix) {
        int evicted = 0;
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, CacheEntry<T>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            cache.remove(key);
            accessTimes.remove(prefix + key);
            evicted++;
        }

        return evicted;
    }

    private void updateAccessTime(String key) {
        accessTimes.put(key, System.currentTimeMillis());
    }

    private long estimateMemory(ConcurrentHashMap<?, ?> map) {
        // Rough estimation: 100 bytes per entry
        return map.size() * 100L;
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CACHE_TTL / 2); // Clean every half TTL
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("Cache-Cleanup-Thread");
        cleanupThread.start();
    }
}