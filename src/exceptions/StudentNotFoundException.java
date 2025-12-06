package exceptions;

public class StudentNotFoundException extends Exception {
    public StudentNotFoundException(String studentId) {
        super("Student with ID '" + studentId + "' not found in the system.");
    }

    public StudentNotFoundException(String message, String availableIds) {
        super(message + "\nAvailable student IDs: " + availableIds);
    }
}