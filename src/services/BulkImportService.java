package services;

import exceptions.InvalidFileFormatException;
import exceptions.StudentNotFoundException;
import exceptions.InvalidGradeException;
import models.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BulkImportService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    public BulkImportService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    public void importGradesFromCSV(String filename) {
        String logFilename = "import_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt";
        int totalRows = 0;
        int successfulImports = 0;
        int failedImports = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("imports/" + filename + ".csv"));
             PrintWriter logWriter = new PrintWriter("imports/" + logFilename)) {

            logWriter.println("BULK IMPORT LOG");
            logWriter.println("File: " + filename + ".csv");
            logWriter.println("Started: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            logWriter.println("=========================================");
            logWriter.println();

            String line;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                totalRows++;

                if (rowNumber == 1) {
                    // Validate header
                    if (!line.equals("StudentID,SubjectName,SubjectType,Grade")) {
                        throw new InvalidFileFormatException(filename + ".csv");
                    }
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length != 4) {
                    logWriter.println("Row " + rowNumber + ": Invalid format - expected 4 columns, found " + parts.length);
                    failedImports++;
                    continue;
                }

                String studentId = parts[0].trim();
                String subjectName = parts[1].trim();
                String subjectType = parts[2].trim();
                String gradeStr = parts[3].trim();

                try {
                    // Validate student exists
                    Student student = studentManager.findStudent(studentId);
                    if (student == null) {
                        throw new StudentNotFoundException(studentId);
                    }

                    // Validate grade
                    double grade;
                    try {
                        grade = Double.parseDouble(gradeStr);
                    } catch (NumberFormatException e) {
                        throw new InvalidGradeException(-1);
                    }

                    if (grade < 0 || grade > 100) {
                        throw new InvalidGradeException(grade);
                    }

                    // Validate subject type and create subject
                    Subject subject;
                    if (subjectType.equalsIgnoreCase("Core")) {
                        subject = new CoreSubject(subjectName, getSubjectCode(subjectName));
                    } else if (subjectType.equalsIgnoreCase("Elective")) {
                        subject = new ElectiveSubject(subjectName, getSubjectCode(subjectName));
                    } else {
                        throw new IllegalArgumentException("Invalid subject type: " + subjectType);
                    }

                    // Create and add grade
                    Grade newGrade = new Grade(studentId, subject, grade);
                    gradeManager.addGrade(newGrade);

                    logWriter.println("Row " + rowNumber + ": SUCCESS - " + studentId + ", " + subjectName + ", " + grade);
                    successfulImports++;

                } catch (Exception e) {
                    logWriter.println("Row " + rowNumber + ": FAILED - " + e.getMessage());
                    failedImports++;
                }
            }

            logWriter.println();
            logWriter.println("IMPORT SUMMARY");
            logWriter.println("Total Rows: " + totalRows);
            logWriter.println("Successfully Imported: " + successfulImports);
            logWriter.println("Failed: " + failedImports);
            logWriter.println("Completed: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        } catch (IOException | InvalidFileFormatException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        // Display import summary to user
        System.out.println("\nIMPORT SUMMARY");
        System.out.println("Total Rows: " + totalRows);
        System.out.println("Successfully Imported: " + successfulImports);
        System.out.println("Failed: " + failedImports);
        System.out.println();
        System.out.println("✓ Import completed!");
        System.out.println(successfulImports + " grades added to system");
        System.out.println("See " + logFilename + " for details");
    }

    private String getSubjectCode(String subjectName) {
        switch (subjectName.toLowerCase()) {
            case "mathematics": return "MATH";
            case "english": return "ENG";
            case "science": return "SCI";
            case "music": return "MUS";
            case "art": return "ART";
            case "physical education": return "PE";
            default: return subjectName.substring(0, 3).toUpperCase();
        }
    }
}