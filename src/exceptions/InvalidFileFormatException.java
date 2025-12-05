package exceptions;

public class InvalidFileFormatException extends Exception {
    public InvalidFileFormatException(String filename) {
        super("Invalid file format for: " + filename + "\nRequired format: StudentID,SubjectName,SubjectType,models.Grade");
    }
}