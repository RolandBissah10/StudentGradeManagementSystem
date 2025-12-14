package services;

import exceptions.ExportException;
import interfaces.Exportable;
import models.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator implements Exportable {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ReportGenerator(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
    }

    @Override
    public void exportSummaryReport(String studentId, String filename) throws ExportException {
        ensureReportsDirectory();
        Path filePath = Paths.get("reports/text", filename + "_summary.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            writer.write("STUDENT GRADE SUMMARY REPORT");
            writer.newLine();
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            writer.newLine();
            writer.write("=========================================");
            writer.newLine();
            writer.newLine();
            writer.write("Student ID: " + student.getStudentId());
            writer.newLine();
            writer.write("Name: " + student.getName());
            writer.newLine();
            writer.write("Type: " + student.getStudentType() + " Student");
            writer.newLine();
            writer.write("Email: " + student.getEmail());
            writer.newLine();
            writer.write("Phone: " + student.getPhone());
            writer.newLine();
            writer.write("Enrollment Date: " + student.getEnrollmentDateString());
            writer.newLine();
            writer.write("Status: " + student.getStatus());
            writer.newLine();
            writer.newLine();

            double overallAvg = gradeManager.calculateOverallAverage(studentId);
            double coreAvg = gradeManager.calculateCoreAverage(studentId);
            double electiveAvg = gradeManager.calculateElectiveAverage(studentId);

            writer.write("PERFORMANCE SUMMARY");
            writer.newLine();
            writer.write("Overall Average: " + String.format("%.1f%%", overallAvg));
            writer.newLine();
            writer.write("Core Subjects Average: " + String.format("%.1f%%", coreAvg));
            writer.newLine();
            writer.write("Elective Subjects Average: " + String.format("%.1f%%", electiveAvg));
            writer.newLine();
            writer.write("Passing Grade Required: " + student.getPassingGrade() + "%");
            writer.newLine();
            writer.write("Passing Status: " + (overallAvg >= student.getPassingGrade() ? "PASSING ✓" : "FAILING ✗"));
            writer.newLine();

            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                writer.write("Honors Eligible: " + (honorsStudent.checkHonorsEligibility() ? "YES ✓" : "NO ✗"));
                writer.newLine();
            }

            writer.newLine();
            writer.write("Report generated using NIO.2 with buffered writing");
            writer.newLine();
            writer.write("File size: " + Files.size(filePath) + " bytes");

            System.out.println("✓ Summary report exported: " + filePath.getFileName());

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }

    @Override
    public void exportDetailedReport(String studentId, String filename) throws ExportException {
        ensureReportsDirectory();
        Path filePath = Paths.get("reports/text", filename + "_detailed.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            writer.write("DETAILED GRADE REPORT");
            writer.newLine();
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            writer.newLine();
            writer.write("=========================================");
            writer.newLine();
            writer.newLine();
            writer.write("STUDENT INFORMATION");
            writer.newLine();
            writer.write("ID: " + student.getStudentId());
            writer.newLine();
            writer.write("Name: " + student.getName());
            writer.newLine();
            writer.write("Type: " + student.getStudentType());
            writer.newLine();
            writer.write("Age: " + student.getAge());
            writer.newLine();
            writer.write("Email: " + student.getEmail());
            writer.newLine();
            writer.write("Phone: " + student.getPhone());
            writer.newLine();
            writer.write("Enrollment Date: " + student.getEnrollmentDateString());
            writer.newLine();
            writer.write("Passing Grade: " + student.getPassingGrade() + "%");
            writer.newLine();
            writer.write("Status: " + student.getStatus());
            writer.newLine();
            writer.newLine();

            writer.write("GRADE HISTORY");
            writer.newLine();
            writer.write("---------------------------------------------------");
            writer.newLine();
            writer.write(String.format("%-8s | %-10s | %-15s | %-8s | %-7s | %s%n",
                    "GRD ID", "DATE", "SUBJECT", "TYPE", "GRADE", "LETTER"));
            writer.write("---------------------------------------------------");
            writer.newLine();

            // Use new collection-based method
            List<Grade> grades = gradeManager.getGradesByStudent(studentId);
            int studentGradeCount = grades.size();

            // Sort by date (newest first)
            grades.sort(Comparator.comparing(Grade::getTimestamp).reversed());

            for (Grade grade : grades) {
                writer.write(String.format("%-8s | %-10s | %-15s | %-8s | %6.1f%% | %-5s%n",
                        grade.getGradeId(),
                        grade.getDate(),
                        truncate(grade.getSubject().getSubjectName(), 15),
                        grade.getSubject().getSubjectType(),
                        grade.getGrade(),
                        grade.getLetterGrade()));
            }

            writer.newLine();
            writer.write("STATISTICS");
            writer.newLine();
            writer.write("Total Grades: " + studentGradeCount);
            writer.newLine();
            writer.write("Overall Average: " + String.format("%.1f%%", gradeManager.calculateOverallAverage(studentId)));
            writer.newLine();
            writer.write("Core Subjects Average: " + String.format("%.1f%%", gradeManager.calculateCoreAverage(studentId)));
            writer.newLine();
            writer.write("Elective Subjects Average: " + String.format("%.1f%%", gradeManager.calculateElectiveAverage(studentId)));
            writer.newLine();

            // Add grade distribution
            writer.newLine();
            writer.write("GRADE DISTRIBUTION");
            writer.newLine();
            Map<String, Long> distribution = getGradeDistribution(grades);
            distribution.forEach((category, count) -> {
                try {
                    writer.write(category + ": " + count + " grades");
                    writer.newLine();
                } catch (IOException e) {
                    // Ignore for this iteration
                }
            });

            System.out.println("✓ Detailed report exported: " + filePath.getFileName());
            System.out.println("  Grades: " + studentGradeCount + " | File size: " + Files.size(filePath) + " bytes");

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }

    private Map<String, Long> getGradeDistribution(List<Grade> grades) {
        return grades.stream()
                .collect(Collectors.groupingBy(
                        grade -> {
                            double g = grade.getGrade();
                            if (g >= 90) return "A (90-100)";
                            else if (g >= 80) return "B (80-89)";
                            else if (g >= 70) return "C (70-79)";
                            else if (g >= 60) return "D (60-69)";
                            else return "F (0-59)";
                        },
                        Collectors.counting()
                ));
    }

    @Override
    public void exportSearchResults(List<Student> students, String filename) throws ExportException {
        ensureReportsDirectory();
        Path filePath = Paths.get("reports/text", filename + "_search.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write("STUDENT SEARCH RESULTS");
            writer.newLine();
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            writer.newLine();
            writer.write("=========================================");
            writer.newLine();
            writer.newLine();
            writer.write(String.format("%-8s | %-20s | %-10s | %-25s | %-15s | %s%n",
                    "ID", "NAME", "TYPE", "EMAIL", "PHONE", "AVERAGE"));
            writer.write("---------------------------------------------------------------------------");
            writer.newLine();

            for (Student student : students) {
                double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                writer.write(String.format("%-8s | %-20s | %-10s | %-25s | %-15s | %6.1f%%%n",
                        student.getStudentId(),
                        truncate(student.getName(), 20),
                        student.getStudentType(),
                        truncate(student.getEmail(), 25),
                        truncate(student.getPhone(), 15),
                        avg));
            }

            writer.newLine();
            writer.write("Total students found: " + students.size());
            writer.newLine();
            writer.write("Export format: Plain text (UTF-8)");
            writer.newLine();
            writer.write("Generated with NIO.2 API");

            System.out.println("✓ Search results exported: " + filePath.getFileName());
            System.out.println("  Students: " + students.size() + " | File size: " + Files.size(filePath) + " bytes");

        } catch (IOException e) {
            throw new ExportException("Cannot write to file: " + e.getMessage());
        }
    }

    // New method for exporting to multiple formats
    public void exportToMultipleFormats(String studentId, String baseFilename) throws ExportException {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = baseFilename + "_" + timestamp;

        System.out.println("\n=== MULTI-FORMAT EXPORT ===");
        System.out.println("Student: " + studentId);
        System.out.println("Base filename: " + filename);

        // Export to all formats
        exportSummaryReport(studentId, filename + "_summary");
        exportDetailedReport(studentId, filename + "_detailed");
        exportToCSV(studentId, filename + "_data");
        exportToJSON(studentId, filename + "_data");

        System.out.println("✓ All format exports completed!");
    }

    // New method: CSV export
    public void exportToCSV(String studentId, String filename) throws ExportException {
        ensureReportsDirectory("csv");
        Path filePath = Paths.get("reports/csv", filename + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            List<Grade> grades = gradeManager.getGradesByStudent(studentId);

            // Write CSV header
            writer.write("StudentID,Name,Email,Phone,Type,EnrollmentDate,Status,OverallAverage");
            writer.newLine();

            // Write student data
            writer.write(String.format("%s,\"%s\",\"%s\",\"%s\",%s,%s,%s,%.1f",
                    student.getStudentId(),
                    escapeCSV(student.getName()),
                    escapeCSV(student.getEmail()),
                    escapeCSV(student.getPhone()),
                    student.getStudentType(),
                    student.getEnrollmentDateString(),
                    student.getStatus(),
                    gradeManager.calculateOverallAverage(studentId)));
            writer.newLine();
            writer.newLine();

            // Write grades header
            writer.write("GradeID,Subject,SubjectType,Grade,Date,LetterGrade,Timestamp");
            writer.newLine();

            // Write grades
            for (Grade grade : grades) {
                writer.write(String.format("%s,\"%s\",%s,%.1f,%s,%s,%s",
                        grade.getGradeId(),
                        escapeCSV(grade.getSubject().getSubjectName()),
                        grade.getSubject().getSubjectType(),
                        grade.getGrade(),
                        grade.getDate(),
                        grade.getLetterGrade(),
                        grade.getTimestampString()));
                writer.newLine();
            }

            System.out.println("✓ CSV export completed: " + filePath.getFileName());
            System.out.println("  Format: Comma-Separated Values");
            System.out.println("  Size: " + Files.size(filePath) + " bytes");

        } catch (IOException e) {
            throw new ExportException("CSV export failed: " + e.getMessage());
        }
    }

    // New method: JSON export
    public void exportToJSON(String studentId, String filename) throws ExportException {
        ensureReportsDirectory("json");
        Path filePath = Paths.get("reports/json", filename + ".json");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            List<Grade> grades = gradeManager.getGradesByStudent(studentId);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\n  \"metadata\": {");
            json.append("\n    \"exportDate\": \"").append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append("\",");
            json.append("\n    \"format\": \"JSON\",");
            json.append("\n    \"version\": \"1.0\"");
            json.append("\n  },");
            json.append("\n  \"student\": {");
            json.append("\n    \"id\": \"").append(student.getStudentId()).append("\",");
            json.append("\n    \"name\": \"").append(escapeJSON(student.getName())).append("\",");
            json.append("\n    \"email\": \"").append(escapeJSON(student.getEmail())).append("\",");
            json.append("\n    \"phone\": \"").append(escapeJSON(student.getPhone())).append("\",");
            json.append("\n    \"type\": \"").append(student.getStudentType()).append("\",");
            json.append("\n    \"enrollmentDate\": \"").append(student.getEnrollmentDateString()).append("\",");
            json.append("\n    \"status\": \"").append(student.getStatus()).append("\",");
            json.append("\n    \"passingGrade\": ").append(student.getPassingGrade());
            json.append("\n  },");
            json.append("\n  \"grades\": [");

            // Add grades
            for (int i = 0; i < grades.size(); i++) {
                Grade grade = grades.get(i);
                json.append("\n    {");
                json.append("\n      \"id\": \"").append(grade.getGradeId()).append("\",");
                json.append("\n      \"subject\": \"").append(escapeJSON(grade.getSubject().getSubjectName())).append("\",");
                json.append("\n      \"type\": \"").append(grade.getSubject().getSubjectType()).append("\",");
                json.append("\n      \"grade\": ").append(grade.getGrade()).append(",");
                json.append("\n      \"date\": \"").append(grade.getDate()).append("\",");
                json.append("\n      \"letterGrade\": \"").append(grade.getLetterGrade()).append("\",");
                json.append("\n      \"timestamp\": \"").append(grade.getTimestampString()).append("\"");
                json.append("\n    }");
                if (i < grades.size() - 1) {
                    json.append(",");
                }
            }

            json.append("\n  ],");
            json.append("\n  \"statistics\": {");
            json.append("\n    \"totalGrades\": ").append(grades.size()).append(",");
            json.append("\n    \"overallAverage\": ").append(gradeManager.calculateOverallAverage(studentId)).append(",");
            json.append("\n    \"coreAverage\": ").append(gradeManager.calculateCoreAverage(studentId)).append(",");
            json.append("\n    \"electiveAverage\": ").append(gradeManager.calculateElectiveAverage(studentId));
            json.append("\n  }");
            json.append("\n}");

            writer.write(json.toString());

            System.out.println("✓ JSON export completed: " + filePath.getFileName());
            System.out.println("  Format: JavaScript Object Notation");
            System.out.println("  Size: " + Files.size(filePath) + " bytes");

        } catch (IOException e) {
            throw new ExportException("JSON export failed: " + e.getMessage());
        }
    }

    // New method: Batch export for multiple students
    public void exportBatchReports(List<String> studentIds, String baseFilename) throws ExportException {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String batchDir = "batch_" + timestamp;

        System.out.println("\n=== BATCH REPORT GENERATION ===");
        System.out.println("Students: " + studentIds.size());
        System.out.println("Directory: reports/batch/" + batchDir);

        int successCount = 0;
        int failCount = 0;

        for (String studentId : studentIds) {
            try {
                Student student = studentManager.findStudent(studentId);
                if (student != null) {
                    String studentFilename = baseFilename + "_" + studentId;
                    exportToMultipleFormats(studentId, studentFilename);
                    successCount++;
                } else {
                    System.out.println("✗ Student not found: " + studentId);
                    failCount++;
                }
            } catch (ExportException e) {
                System.out.println("✗ Failed to export " + studentId + ": " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\n=== BATCH EXPORT SUMMARY ===");
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failCount);
        System.out.println("Total: " + (successCount + failCount));

        if (failCount > 0) {
            throw new ExportException(failCount + " students failed to export");
        }
    }

    // Helper methods
    private void ensureReportsDirectory() {
        ensureReportsDirectory("text");
    }

    private void ensureReportsDirectory(String subdirectory) {
        try {
            Path dir = Paths.get("reports", subdirectory);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create reports directory: " + e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String escapeCSV(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }

    private String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}