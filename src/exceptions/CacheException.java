package exceptions;

public class CacheException extends Exception {
    public CacheException(String message) {
        super("Cache operation failed: " + message);
    }
}