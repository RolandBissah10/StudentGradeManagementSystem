package models;

import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;

class ScheduledTask {
    private String name;
    private String schedule;
    private LocalDateTime nextExecution;
    private LocalDateTime lastRun;
    private String status;
    private int threadCount;
    private String notificationEmail;
    private String notificationType;
    private ScheduledFuture<?> future;

    public ScheduledTask(String name, String schedule, LocalDateTime nextExecution,
                         int threadCount, String notificationEmail, String notificationType) {
        this.name = name;
        this.schedule = schedule;
        this.nextExecution = nextExecution;
        this.threadCount = threadCount;
        this.notificationEmail = notificationEmail;
        this.notificationType = notificationType;
        this.status = "SCHEDULED";
    }

    // Getters and setters
    public String getName() { return name; }
    public String getSchedule() { return schedule; }
    public LocalDateTime getNextExecution() { return nextExecution; }
    public LocalDateTime getLastRun() { return lastRun; }
    public void setLastRun(LocalDateTime lastRun) { this.lastRun = lastRun; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getThreadCount() { return threadCount; }
    public String getNotificationEmail() { return notificationEmail; }
    public String getNotificationType() { return notificationType; }
    public ScheduledFuture<?> getFuture() { return future; }
    public void setFuture(ScheduledFuture<?> future) { this.future = future; }

    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
        status = "CANCELLED";
    }
}
