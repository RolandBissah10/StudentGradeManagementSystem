package exceptions;

public class ConcurrentOperationException extends Exception {
    public ConcurrentOperationException(String message) {
        super("Concurrent operation failed: " + message);
    }
}