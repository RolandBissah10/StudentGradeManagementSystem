package exceptions;

public class ExportException extends Exception {
    public ExportException(String message) {
        super("Export failed: " + message);
    }
}