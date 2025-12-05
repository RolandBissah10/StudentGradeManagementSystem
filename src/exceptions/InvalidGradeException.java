package exceptions;

public class InvalidGradeException extends Exception {
    public InvalidGradeException(double grade) {
        super("models.Grade must be between 0 and 100.\nYou entered: " + grade);
    }
}