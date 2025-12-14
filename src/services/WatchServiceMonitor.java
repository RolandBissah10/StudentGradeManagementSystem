package services;

import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WatchServiceMonitor {
    private WatchService watchService;
    private ExecutorService executorService;
    private volatile boolean isRunning = false;
    private Path watchDirectory;

    public WatchServiceMonitor(String directoryPath) throws IOException {
        this.watchDirectory = Paths.get(directoryPath);
        if (!Files.exists(watchDirectory)) {
            Files.createDirectories(watchDirectory);
        }

        this.watchService = FileSystems.getDefault().newWatchService();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startMonitoring() {
        if (isRunning) {
            System.out.println("Watch service is already running.");
            return;
        }

        try {
            // Register for create and modify events
            watchDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            isRunning = true;
            executorService.submit(this::monitorDirectory);

            System.out.println("✓ Started monitoring directory: " + watchDirectory);
            System.out.println("  Watching for: CREATE, MODIFY events");
            System.out.println("  Auto-import will trigger for CSV files");

        } catch (IOException e) {
            System.out.println("Failed to start watch service: " + e.getMessage());
        }
    }

    private void monitorDirectory() {
        System.out.println("Watch service thread started: " + Thread.currentThread().getName());

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.poll(10, TimeUnit.SECONDS);

                if (key == null) {
                    continue; // No events
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // Handle overflow
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    // Get the filename
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = watchDirectory.resolve(filename);

                    // Process the event
                    processFileEvent(kind, fullPath);
                }

                // Reset the key
                boolean valid = key.reset();
                if (!valid) {
                    System.out.println("Watch key no longer valid for: " + watchDirectory);
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Watch service interrupted.");
                break;
            } catch (ClosedWatchServiceException e) {
                System.out.println("Watch service closed.");
                break;
            }
        }

        System.out.println("Watch service stopped.");
    }

    private void processFileEvent(WatchEvent.Kind<?> kind, Path filePath) {
        String filename = filePath.getFileName().toString();
        String eventType = kind.name();

        System.out.printf("[%s] %s: %s%n",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                eventType, filename);

        // Only process CSV files
        if (filename.toLowerCase().endsWith(".csv")) {
            handleCSVFile(filePath, eventType);
        } else if (filename.toLowerCase().endsWith(".json")) {
            handleJSONFile(filePath, eventType);
        }
    }

    private void handleCSVFile(Path filePath, String eventType) {
        try {
            if (eventType.equals("ENTRY_CREATE")) {
                System.out.println("  → New CSV file detected: " + filePath.getFileName());
                System.out.println("  → Would trigger auto-import (simulated)");

                // Simulate file processing delay
                Thread.sleep(1000);

                // In real implementation, you would:
                // 1. Parse the CSV file
                // 2. Validate the data
                // 3. Import into the system
                // 4. Move/rename the processed file

                // Simulate moving the file to processed directory
                Path processedDir = watchDirectory.resolve("processed");
                if (!Files.exists(processedDir)) {
                    Files.createDirectories(processedDir);
                }

                Path destination = processedDir.resolve(
                        filePath.getFileName().toString().replace(".csv",
                                "_processed_" + System.currentTimeMillis() + ".csv"));

                Files.move(filePath, destination, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("  → File processed and moved to: " + destination.getFileName());

            } else if (eventType.equals("ENTRY_MODIFY")) {
                System.out.println("  → CSV file modified: " + filePath.getFileName());
                System.out.println("  → Would trigger re-import (simulated)");
            }

        } catch (Exception e) {
            System.out.println("  → Error processing file: " + e.getMessage());
        }
    }

    private void handleJSONFile(Path filePath, String eventType) {
        System.out.println("  → JSON file detected: " + filePath.getFileName());
        System.out.println("  → Would trigger JSON import (simulated)");
    }

    public void stopMonitoring() {
        isRunning = false;

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing watch service: " + e.getMessage());
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Watch service stopped.");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void displayStatus() {
        System.out.println("\n=== WATCH SERVICE STATUS ===");
        System.out.println("Directory: " + watchDirectory);
        System.out.println("Status: " + (isRunning ? "RUNNING" : "STOPPED"));
        System.out.println("Watching for: CSV, JSON files");
        System.out.println("Events: CREATE, MODIFY");

        if (isRunning) {
            try {
                long fileCount = Files.list(watchDirectory)
                        .filter(p -> !Files.isDirectory(p))
                        .count();
                System.out.println("Files in directory: " + fileCount);

                long csvCount = Files.list(watchDirectory)
                        .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                        .count();
                System.out.println("CSV files: " + csvCount);

            } catch (IOException e) {
                System.out.println("Error scanning directory: " + e.getMessage());
            }
        }
    }
}