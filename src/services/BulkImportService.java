package services;

import exceptions.InvalidFileFormatException;
import exceptions.StudentNotFoundException;
import exceptions.InvalidGradeException;
import models.*;
import utils.ValidationUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BulkImportService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public BulkImportService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    public void importGradesFromCSV(String filename) {
        String logFilename = "import_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        AtomicInteger totalRows = new AtomicInteger();
        int successfulImports = 0;
        int failedImports = 0;

        Path filePath = Paths.get("imports", filename + ".csv");

        if (!Files.exists(filePath)) {
            System.out.println("✗ ERROR: File not found: " + filePath);
            return;
        }

        try (Stream<String> lines = Files.lines(filePath);
             PrintWriter logWriter = new PrintWriter(Files.newBufferedWriter(Paths.get("imports", logFilename)))) {

            logWriter.println("BULK IMPORT LOG");
            logWriter.println("File: " + filename + ".csv");
            logWriter.println("Started: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            logWriter.println("Method: NIO.2 Streaming");
            logWriter.println("=========================================");
            logWriter.println();

            // Use stream for memory-efficient processing
            totalRows.set((int) lines.skip(1) // Skip header
                    .map(line -> processCSVRow(line, logWriter, totalRows.incrementAndGet()))
                    .filter(result -> result)
                    .count());

            successfulImports = totalRows.get();

            logWriter.println();
            logWriter.println("IMPORT SUMMARY");
            logWriter.println("Total Rows Processed: " + (totalRows.get() + failedImports));
            logWriter.println("Successfully Imported: " + successfulImports);
            logWriter.println("Failed: " + failedImports);
            logWriter.println("Memory Efficient: Yes (streaming)");
            logWriter.println("Completed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        // Display import summary to user
        System.out.println("\n=== IMPORT SUMMARY ===");
        System.out.println("Total Rows: " + (totalRows.get() + failedImports));
        System.out.println("Successfully Imported: " + successfulImports);
        System.out.println("Failed: " + failedImports);
        System.out.println("Method: NIO.2 Streaming (memory efficient)");
        System.out.println("\n✓ Import completed!");
        System.out.println(successfulImports + " grades added to system");
        System.out.println("See " + logFilename + " for details");
    }

    private boolean processCSVRow(String line, PrintWriter logWriter, int rowNumber) {
        String[] parts = line.split(",");
        if (parts.length != 4) {
            logWriter.println("Row " + rowNumber + ": Invalid format - expected 4 columns, found " + parts.length);
            return false;
        }

        String studentId = parts[0].trim();
        String subjectName = parts[1].trim();
        String subjectType = parts[2].trim();
        String gradeStr = parts[3].trim();

        try {
            // Validate student ID format
            ValidationUtils.ValidationResult studentIdResult = ValidationUtils.validateStudentId(studentId);
            if (!studentIdResult.isValid()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid student ID format: " + studentId);
                logWriter.println("  Reason: " + studentIdResult.getErrorMessage());
                return false;
            }

            // Validate student exists
            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new StudentNotFoundException(studentId);
            }

            // Validate grade
            ValidationUtils.ValidationResult gradeResult = ValidationUtils.validateGrade(gradeStr);
            if (!gradeResult.isValid()) {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid grade: " + gradeStr);
                logWriter.println("  Reason: " + gradeResult.getErrorMessage());
                return false;
            }

            double grade = Double.parseDouble(gradeStr);

            // Additional grade validation
            if (grade < 0 || grade > 100) {
                logWriter.println("Row " + rowNumber + ": FAILED - Grade must be between 0 and 100: " + grade);
                return false;
            }

            // Validate subject type and create subject
            Subject subject;
            if (subjectType.equalsIgnoreCase("Core")) {
                subject = new CoreSubject(subjectName, getSubjectCode(subjectName));
            } else if (subjectType.equalsIgnoreCase("Elective")) {
                subject = new ElectiveSubject(subjectName, getSubjectCode(subjectName));
            } else {
                logWriter.println("Row " + rowNumber + ": FAILED - Invalid subject type: " + subjectType);
                logWriter.println("  Must be 'Core' or 'Elective'");
                return false;
            }

            // Create and add grade
            Grade newGrade = new Grade(studentId, subject, grade);
            gradeManager.addGrade(newGrade);

            logWriter.println("Row " + rowNumber + ": SUCCESS - " + studentId + ", " + subjectName + ", " + grade);
            return true;

        } catch (StudentNotFoundException e) {
            logWriter.println("Row " + rowNumber + ": FAILED - " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            logWriter.println("Row " + rowNumber + ": FAILED - Invalid number format for grade: " + gradeStr);
            return false;
        } catch (Exception e) {
            logWriter.println("Row " + rowNumber + ": FAILED - Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private String getSubjectCode(String subjectName) {
        switch (subjectName.toLowerCase()) {
            case "mathematics": return "MATH";
            case "english": return "ENG";
            case "science": return "SCI";
            case "music": return "MUS";
            case "art": return "ART";
            case "physical education": return "PE";
            default: return subjectName.substring(0, Math.min(3, subjectName.length())).toUpperCase();
        }
    }
}