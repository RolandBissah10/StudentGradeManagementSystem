package exceptions;

public class InvalidPatternException extends Exception {
    private final String pattern;

    public InvalidPatternException(String pattern, String message) {
        super("Invalid regex pattern '" + pattern + "': " + message);
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}