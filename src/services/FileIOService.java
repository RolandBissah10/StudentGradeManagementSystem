package services;

import models.*;
import exceptions.ExportException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileIOService {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    // Thread-safe map for file locking
    private static final ConcurrentHashMap<String, Object> fileLocks = new ConcurrentHashMap<>();

    public String exportToCSV(Student student, List<Grade> grades, String baseFilename, String reportType) throws ExportException, IOException {
        synchronized (getFileLock(baseFilename)) {
            // Create filename based on report type
            String filename = baseFilename + ".csv";
            Path filePath = Paths.get("reports", "csv", filename);
            ensureDirectoryExists(filePath.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // Write header based on report type
                if ("summary".equalsIgnoreCase(reportType)) {
                    writer.write("StudentID,Name,Email,Phone,Type,GPA,TotalGrades");
                    writer.newLine();

                    // Write student data
                    writer.write(String.format("%s,\"%s\",\"%s\",\"%s\",%s,%.2f,%d",
                            student.getStudentId(),
                            student.getName().replace("\"", "\"\""),
                            student.getEmail().replace("\"", "\"\""),
                            student.getPhone().replace("\"", "\"\""),
                            student.getStudentType(),
//                            student.getGPA(),
                            grades.size()));
                    writer.newLine();
                } else if ("detailed".equalsIgnoreCase(reportType)) {
                    writer.write("StudentID,Name,Email,Phone,Type,EnrollmentDate,Status");
                    writer.newLine();

                    // Write student data
                    writer.write(String.format("%s,\"%s\",\"%s\",\"%s\",%s,%s,%s",
                            student.getStudentId(),
                            student.getName().replace("\"", "\"\""),
                            student.getEmail().replace("\"", "\"\""),
                            student.getPhone().replace("\"", "\"\""),
                            student.getStudentType(),
                            student.getEnrollmentDateString(),
                            student.getStatus()));
                    writer.newLine();
                    writer.newLine();

                    // Write grades header
                    writer.write("GradeID,Subject,Type,Grade,Date,LetterGrade");
                    writer.newLine();

                    // Write grades
                    for (Grade grade : grades) {
                        writer.write(String.format("%s,\"%s\",%s,%.1f,%s,%s",
                                grade.getGradeId(),
                                grade.getSubject().getSubjectName().replace("\"", "\"\""),
                                grade.getSubject().getSubjectType(),
                                grade.getGrade(),
                                grade.getDate(),
                                grade.getLetterGrade()));
                        writer.newLine();
                    }
                } else if ("transcript".equalsIgnoreCase(reportType)) {
                    writer.write("UNIVERSITY TRANSCRIPT");
                    writer.newLine();
                    writer.write("Student: " + student.getName() + " (" + student.getStudentId() + ")");
                    writer.newLine();
                    writer.write("Email: " + student.getEmail());
                    writer.newLine();
                    writer.write("Enrollment Date: " + student.getEnrollmentDateString());
                    writer.newLine();
                    writer.write("-".repeat(50));
                    writer.newLine();
                    writer.write("SUBJECT                 TYPE     GRADE  LETTER  DATE");
                    writer.newLine();
                    writer.write("-".repeat(50));
                    writer.newLine();

                    for (Grade grade : grades) {
                        String subjectName = grade.getSubject().getSubjectName();
                        String subjectType = grade.getSubject().getSubjectType();
                        double gradeValue = grade.getGrade();
                        String letterGrade = grade.getLetterGrade();
                        String date = grade.getDate();

                        writer.write(String.format("%-20s   %-8s %6.1f   %-6s  %s",
                                subjectName.length() > 20 ? subjectName.substring(0, 17) + "..." : subjectName,
                                subjectType,
                                gradeValue,
                                letterGrade,
                                date));
                        writer.newLine();
                    }

                    writer.write("-".repeat(50));
                    writer.newLine();
//                    writer.write(String.format("GPA: %.2f   Total Credits: %d", student.getGPA(), grades.size()));
                    writer.newLine();
                }

                // Analytics report type would be similar but with more detailed calculations

            } catch (IOException e) {
                throw new ExportException("CSV export failed: " + e.getMessage());
            }

            return filePath.toString();
        }
    }

    public String exportToJSON(Student student, List<Grade> grades, String baseFilename, String reportType) throws ExportException, IOException {
        synchronized (getFileLock(baseFilename)) {
            String filename = baseFilename + ".json";
            Path filePath = Paths.get("reports", "json", filename);
            ensureDirectoryExists(filePath.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                StringBuilder json = new StringBuilder();
                json.append("{\n");

                if ("summary".equalsIgnoreCase(reportType)) {
                    json.append("  \"reportType\": \"summary\",\n");
                    json.append("  \"student\": {\n");
                    json.append(String.format("    \"id\": \"%s\",\n", student.getStudentId()));
                    json.append(String.format("    \"name\": \"%s\",\n", escapeJson(student.getName())));
                    json.append(String.format("    \"email\": \"%s\",\n", escapeJson(student.getEmail())));
                    json.append(String.format("    \"phone\": \"%s\",\n", escapeJson(student.getPhone())));
                    json.append(String.format("    \"type\": \"%s\",\n", student.getStudentType()));
//                    json.append(String.format("    \"gpa\": %.2f,\n", student.getGPA()));
                    json.append(String.format("    \"totalGrades\": %d\n", grades.size()));
                    json.append("  },\n");
                } else if ("detailed".equalsIgnoreCase(reportType)) {
                    json.append("  \"reportType\": \"detailed\",\n");
                    json.append("  \"student\": {\n");
                    json.append(String.format("    \"id\": \"%s\",\n", student.getStudentId()));
                    json.append(String.format("    \"name\": \"%s\",\n", escapeJson(student.getName())));
                    json.append(String.format("    \"email\": \"%s\",\n", escapeJson(student.getEmail())));
                    json.append(String.format("    \"phone\": \"%s\",\n", escapeJson(student.getPhone())));
                    json.append(String.format("    \"type\": \"%s\",\n", student.getStudentType()));
                    json.append(String.format("    \"enrollmentDate\": \"%s\",\n", student.getEnrollmentDateString()));
                    json.append(String.format("    \"status\": \"%s\",\n", student.getStatus()));
                    json.append(String.format("    \"age\": %d,\n", student.getAge()));
//                    json.append(String.format("    \"gpa\": %.2f\n", student.getGPA()));
                    json.append("  },\n");
                    json.append("  \"grades\": [\n");

                    String gradesJson = grades.stream()
                            .map(grade -> String.format(
                                    "    {\n" +
                                            "      \"id\": \"%s\",\n" +
                                            "      \"subject\": \"%s\",\n" +
                                            "      \"type\": \"%s\",\n" +
                                            "      \"grade\": %.1f,\n" +
                                            "      \"date\": \"%s\",\n" +
                                            "      \"letterGrade\": \"%s\",\n" +
                                            "      \"timestamp\": \"%s\"\n" +
                                            "    }",
                                    grade.getGradeId(),
                                    escapeJson(grade.getSubject().getSubjectName()),
                                    grade.getSubject().getSubjectType(),
                                    grade.getGrade(),
                                    grade.getDate(),
                                    grade.getLetterGrade(),
                                    grade.getTimestampString()))
                            .collect(Collectors.joining(",\n"));

                    json.append(gradesJson);
                    json.append("\n  ],\n");
                }

                json.append(String.format("  \"metadata\": {\n" +
                                "    \"exportDate\": \"%s\",\n" +
                                "    \"reportType\": \"%s\",\n" +
                                "    \"format\": \"JSON\"\n" +
                                "  }\n",
                        LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                        reportType));
                json.append("}");

                writer.write(json.toString());

            } catch (IOException e) {
                throw new ExportException("JSON export failed: " + e.getMessage());
            }

            return filePath.toString();
        }
    }

    public String exportToBinary(Student student, List<Grade> grades, String baseFilename, String reportType) throws ExportException, IOException {
        synchronized (getFileLock(baseFilename)) {
            String filename = baseFilename + ".dat";
            Path filePath = Paths.get("reports", "binary", filename);
            ensureDirectoryExists(filePath.getParent());

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(filePath)))) {

                Map<String, Object> report = new HashMap<>();

                // Add report type
                report.put("reportType", reportType);

                // Create serializable student data
                Map<String, String> studentData = new HashMap<>();
                studentData.put("id", student.getStudentId());
                studentData.put("name", student.getName());
                studentData.put("email", student.getEmail());
                studentData.put("phone", student.getPhone());
                studentData.put("type", student.getStudentType());
                studentData.put("enrollmentDate", student.getEnrollmentDateString());
                studentData.put("status", student.getStatus());
                studentData.put("age", String.valueOf(student.getAge()));
//                studentData.put("gpa", String.format("%.2f", student.getGPA()));

                report.put("student", studentData);

                // Create serializable grade data
                List<Map<String, Object>> gradeDataList = new ArrayList<>();
                for (Grade grade : grades) {
                    Map<String, Object> gradeData = new HashMap<>();
                    gradeData.put("id", grade.getGradeId());
                    gradeData.put("studentId", grade.getStudentId());
                    gradeData.put("subjectName", grade.getSubject().getSubjectName());
                    gradeData.put("subjectType", grade.getSubject().getSubjectType());
                    gradeData.put("subjectCode", grade.getSubject().getSubjectCode());
                    gradeData.put("grade", grade.getGrade());
                    gradeData.put("date", grade.getDate());
                    gradeData.put("letterGrade", grade.getLetterGrade());
                    gradeData.put("timestamp", grade.getTimestamp().toString());
                    gradeDataList.add(gradeData);
                }

                report.put("grades", gradeDataList);
                report.put("exportTimestamp", LocalDateTime.now().toString());
                report.put("totalGrades", grades.size());
                report.put("formatVersion", "2.0");

                oos.writeObject(report);
                oos.flush();

            } catch (IOException e) {
                System.err.println("Binary export error: " + e.getClass().getName() + ": " + e.getMessage());
                throw new ExportException("Binary export failed: " + e.getMessage());
            }

            return filePath.toString();
        }
    }

    public void exportAllFormats(Student student, List<Grade> grades, String baseFilename) throws ExportException, IOException {
        // This method is kept for backward compatibility
        exportAllFormats(student, grades, baseFilename, "detailed");
    }

    public void exportAllFormats(Student student, List<Grade> grades, String baseFilename, String reportType) throws ExportException, IOException {
        String filename = baseFilename;

        System.out.println("\nProcessing with NIO.2 Streaming...\n");

        long startTime = System.currentTimeMillis();
        int successfulExports = 0;

        try {
            exportToCSV(student, grades, filename, reportType);
            successfulExports++;
        } catch (ExportException | IOException e) {
            System.err.println("CSV export failed: " + e.getMessage());
        }

        try {
            exportToJSON(student, grades, filename, reportType);
            successfulExports++;
        } catch (ExportException | IOException e) {
            System.err.println("JSON export failed: " + e.getMessage());
        }

        try {
            exportToBinary(student, grades, filename, reportType);
            successfulExports++;
        } catch (ExportException | IOException e) {
            System.err.println("Binary export failed: " + e.getMessage());
        }

        long totalTime = System.currentTimeMillis() - startTime;

        if (successfulExports == 3) {
            System.out.println("All formats exported successfully in " + totalTime + "ms");
        } else if (successfulExports > 0) {
            System.out.println("Partially successful: " + successfulExports + "/3 formats exported");
        } else {
            System.out.println("All exports failed!");
        }
    }

    public List<Grade> importFromBinary(String filename) throws ExportException {
        Path filePath = Paths.get("reports/binary", filename + ".dat");

        if (!Files.exists(filePath)) {
            throw new ExportException("File not found: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {

            @SuppressWarnings("unchecked")
            Map<String, Object> report = (Map<String, Object>) ois.readObject();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> gradeDataList = (List<Map<String, Object>>) report.get("grades");

            List<Grade> grades = new ArrayList<>();

            for (Map<String, Object> gradeData : gradeDataList) {
                try {
                    String studentId = (String) gradeData.get("studentId");
                    String subjectName = (String) gradeData.get("subjectName");
                    String subjectType = (String) gradeData.get("subjectType");
                    String subjectCode = (String) gradeData.get("subjectCode");
                    double gradeValue;

                    // Handle different number types
                    Object gradeObj = gradeData.get("grade");
                    if (gradeObj instanceof Double) {
                        gradeValue = (Double) gradeObj;
                    } else if (gradeObj instanceof Integer) {
                        gradeValue = ((Integer) gradeObj).doubleValue();
                    } else if (gradeObj instanceof Float) {
                        gradeValue = ((Float) gradeObj).doubleValue();
                    } else {
                        gradeValue = Double.parseDouble(gradeObj.toString());
                    }

                    Subject subject;
                    if ("Core".equalsIgnoreCase(subjectType)) {
                        subject = new CoreSubject(subjectName, subjectCode != null ? subjectCode : getSubjectCode(subjectName));
                    } else {
                        subject = new ElectiveSubject(subjectName, subjectCode != null ? subjectCode : getSubjectCode(subjectName));
                    }

                    Grade grade = new Grade(studentId, subject, gradeValue);
                    grades.add(grade);

                } catch (Exception e) {
                    System.err.println("Warning: Could not import grade: " + e.getMessage());
                }
            }

            System.out.println("Binary Import completed: " + grades.size() + " grades loaded");
            return grades;

        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new ExportException("Binary import failed: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    // Helper method to get subject code
    private String getSubjectCode(String subjectName) {
        if (subjectName == null || subjectName.isEmpty()) {
            return "GEN";
        }

        String lowerName = subjectName.toLowerCase();
        if (lowerName.contains("math")) return "MATH";
        if (lowerName.contains("eng")) return "ENG";
        if (lowerName.contains("sci")) return "SCI";
        if (lowerName.contains("music")) return "MUS";
        if (lowerName.contains("art")) return "ART";
        if (lowerName.contains("phys") || lowerName.contains("pe")) return "PE";
        if (lowerName.contains("chem")) return "CHEM";
        if (lowerName.contains("bio")) return "BIO";
        if (lowerName.contains("hist")) return "HIST";
        if (lowerName.contains("geo")) return "GEO";

        return subjectName.substring(0, Math.min(3, subjectName.length())).toUpperCase();
    }

    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Object getFileLock(String filename) {
        return fileLocks.computeIfAbsent(filename, k -> new Object());
    }

    public void watchImportDirectory() {
        System.out.println("File watching service available (WatchService implementation)");
    }

    // Test method to verify binary export works
    public void testBinaryExport() {
        try {
            // Create a test student
            Student testStudent = new RegularStudent("Test Student", 18,
                    "test@email.com", "123-4567", "2024-01-01");

            // Create test grades
            List<Grade> testGrades = new ArrayList<>();
            testGrades.add(new Grade(testStudent.getStudentId(),
                    new CoreSubject("Mathematics", "MATH"), 85.5));
            testGrades.add(new Grade(testStudent.getStudentId(),
                    new ElectiveSubject("Music", "MUS"), 92.0));

            // Test binary export
            System.out.println("\n=== TESTING BINARY EXPORT ===");
            exportToBinary(testStudent, testGrades, "test_export", "detailed");
            System.out.println("Test binary export completed successfully!");

            // Test binary import
            System.out.println("\n=== TESTING BINARY IMPORT ===");
            List<Grade> importedGrades = importFromBinary("test_export_binary");
            System.out.println("Test binary import completed: " + importedGrades.size() + " grades loaded");

        } catch (Exception e) {
            System.err.println("Binary export test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}