package services;

import exceptions.ExportException;
import interfaces.Exportable;
import models.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
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

    // ============================
    // NEW METHODS FOR BATCH REPORTS
    // ============================

    // 1. PDF Summary Report
    public void exportPdfSummary(String studentId, String filename) throws ExportException {
        ensureReportsDirectory("pdf");
        Path filePath = Paths.get("reports/pdf", filename + "_summary.pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float margin = 50;
                final float[] y = {750};

                // Helper function to add text with automatic endText()
                BiConsumer<String, Float> addText = (text, fontSize) -> {
                    try {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
                        cs.newLineAtOffset(margin, y[0]);
                        cs.showText(text);
                        cs.endText();
                        y[0] -= (fontSize + 5);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                // Title
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                cs.newLineAtOffset(margin, y[0]);
                cs.showText("STUDENT GRADE REPORT");
                cs.endText();
                y[0] -= 40;

                // Student Info
                addText.accept("Student ID: " + student.getStudentId(), 12f);
                addText.accept("Name: " + student.getName(), 12f);
                addText.accept("Type: " + student.getStudentType(), 12f);
                addText.accept("Email: " + student.getEmail(), 12f);
                y[0] -= 20;

                // Performance
                double overallAvg = gradeManager.calculateOverallAverage(studentId);
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                cs.newLineAtOffset(margin, y[0]);
                cs.showText("PERFORMANCE");
                cs.endText();
                y[0] -= 25;

                addText.accept(String.format("Overall Average: %.1f%%", overallAvg), 12f);
                addText.accept("Status: " + (overallAvg >= student.getPassingGrade() ?
                        "PASSING" : "FAILING"), 12f);

                // Footer
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 10);
                cs.newLineAtOffset(margin, 40);
                cs.showText("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
                cs.endText();
            }

            document.save(filePath.toFile());
            System.out.println("✓ PDF exported: " + filePath.getFileName());

        } catch (IOException | RuntimeException e) {
            throw new ExportException("PDF export failed: " + e.getMessage());
        }
    }

    // 2. Excel Spreadsheet (CSV format for Excel)
    public void exportExcelSpreadsheet(String studentId, String filename) throws ExportException {
        ensureReportsDirectory("excel");
        Path filePath = Paths.get("reports/excel", filename + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            Student student = studentManager.findStudent(studentId);
            if (student == null) {
                throw new ExportException("Student not found: " + studentId);
            }

            List<Grade> grades = gradeManager.getGradesByStudent(studentId);

            // Excel-friendly CSV with UTF-8 BOM for Excel compatibility
            writer.write("\uFEFF"); // UTF-8 BOM for Excel
            writer.newLine();

            // Student Information Sheet
            writer.write("STUDENT INFORMATION");
            writer.newLine();
            writer.write("Field,Value");
            writer.newLine();
            writer.write("Student ID," + student.getStudentId());
            writer.newLine();
            writer.write("Name,\"" + escapeCSV(student.getName()) + "\"");
            writer.newLine();
            writer.write("Email,\"" + escapeCSV(student.getEmail()) + "\"");
            writer.newLine();
            writer.write("Phone,\"" + escapeCSV(student.getPhone()) + "\"");
            writer.newLine();
            writer.write("Student Type," + student.getStudentType());
            writer.newLine();
            writer.write("Enrollment Date," + student.getEnrollmentDateString());
            writer.newLine();
            writer.write("Status," + student.getStatus());
            writer.newLine();
            writer.write("Passing Grade," + student.getPassingGrade());
            writer.newLine();
            writer.write("Overall Average," + String.format("%.2f", gradeManager.calculateOverallAverage(studentId)));
            writer.newLine();
            writer.newLine();

            // Grades Sheet
            writer.write("GRADE DETAILS");
            writer.newLine();
            writer.write("GradeID,StudentID,Subject,SubjectType,Grade,LetterGrade,Date,Timestamp");
            writer.newLine();

            for (Grade grade : grades) {
                writer.write(String.format("%s,%s,\"%s\",%s,%.2f,%s,%s,%s",
                        grade.getGradeId(),
                        grade.getStudentId(),
                        escapeCSV(grade.getSubject().getSubjectName()),
                        grade.getSubject().getSubjectType(),
                        grade.getGrade(),
                        grade.getLetterGrade(),
                        grade.getDate(),
                        grade.getTimestampString()));
                writer.newLine();
            }
            writer.newLine();

            // Statistics Sheet
            writer.write("STATISTICS");
            writer.newLine();
            writer.write("Metric,Value");
            writer.newLine();
            writer.write("Total Grades," + grades.size());
            writer.newLine();
            writer.write("Overall Average," + String.format("%.2f", gradeManager.calculateOverallAverage(studentId)));
            writer.newLine();
            writer.write("Core Subjects Average," + String.format("%.2f", gradeManager.calculateCoreAverage(studentId)));
            writer.newLine();
            writer.write("Elective Subjects Average," + String.format("%.2f", gradeManager.calculateElectiveAverage(studentId)));
            writer.newLine();

            // Grade Distribution
            Map<String, Long> distribution = getGradeDistribution(grades);
            writer.newLine();
            writer.write("GRADE DISTRIBUTION");
            writer.newLine();
            writer.write("Grade Range,Count,Percentage");
            writer.newLine();

            for (Map.Entry<String, Long> entry : distribution.entrySet()) {
                double percentage = grades.size() > 0 ? (entry.getValue() * 100.0) / grades.size() : 0;
                writer.write(String.format("%s,%d,%.2f%%",
                        entry.getKey(),
                        entry.getValue(),
                        percentage));
                writer.newLine();
            }
            writer.newLine();

            // Performance Indicators
            writer.write("PERFORMANCE INDICATORS");
            writer.newLine();
            writer.write("Indicator,Value,Status");
            writer.newLine();

            double overallAvg = gradeManager.calculateOverallAverage(studentId);
            writer.write(String.format("Passing Status,%.2f,%s",
                    overallAvg,
                    overallAvg >= student.getPassingGrade() ? "PASS" : "FAIL"));
            writer.newLine();

            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                writer.write(String.format("Honors Eligibility,%.2f,%s",
                        overallAvg,
                        honorsStudent.checkHonorsEligibility() ? "ELIGIBLE" : "NOT ELIGIBLE"));
                writer.newLine();
            }

            System.out.println("✓ Excel Spreadsheet exported: " + filePath.getFileName());
            System.out.println("  Format: CSV (Excel Compatible)");
            System.out.println("  Sheets: Student Info, Grades, Statistics, Distribution, Performance");
            System.out.println("  Size: " + Files.size(filePath) + " bytes");

        } catch (IOException e) {
            throw new ExportException("Excel export failed: " + e.getMessage());
        }
    }

    // 3. Batch Report Generation (All 3 formats)
    public void generateBatchReport(String studentId, String baseFilename) throws ExportException {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = baseFilename + "_" + studentId + "_" + timestamp;

        System.out.println("\n=== BATCH REPORT GENERATION ===");
        System.out.println("Student: " + studentId);
        System.out.println("Formats: PDF Summary, Detailed Text, Excel Spreadsheet");
        System.out.println("Timestamp: " + timestamp);

        // Generate all three formats
        try {
            System.out.println("\n1. Generating PDF Summary...");
            exportPdfSummary(studentId, filename);

            System.out.println("2. Generating Detailed Text Report...");
            exportDetailedReport(studentId, filename);

            System.out.println("3. Generating Excel Spreadsheet...");
            exportExcelSpreadsheet(studentId, filename);

            System.out.println("\n BATCH REPORT GENERATION COMPLETE");
            System.out.println("All 3 formats generated successfully:");
            System.out.println("  - PDF Summary:     reports/pdf/" + filename + "_summary.pdf");
            System.out.println("  - Detailed Text:   reports/text/" + filename + "_detailed.txt");
            System.out.println("  - Excel Spreadsheet: reports/excel/" + filename + ".csv");

        } catch (ExportException e) {
            throw new ExportException("Batch report generation failed: " + e.getMessage());
        }
    }

    // 4. Class-wide Batch Reports (All students)
    public void generateClassBatchReports() throws ExportException {
        List<Student> allStudents = studentManager.getStudents();
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);

        System.out.println("\n=== CLASS-WIDE BATCH REPORT GENERATION ===");
        System.out.println("Total Students: " + allStudents.size());
        System.out.println("Timestamp: " + timestamp);
        System.out.println("=".repeat(50));

        int successCount = 0;
        int failCount = 0;

        for (Student student : allStudents) {
            try {
                System.out.print("\nProcessing " + student.getStudentId() + " - " +
                        truncate(student.getName(), 20) + "... ");

                generateBatchReport(student.getStudentId(),
                        "class_report_" + timestamp);

                System.out.println("✓ Done");
                successCount++;

            } catch (ExportException e) {
                System.out.println("✗ Failed: " + e.getMessage());
                failCount++;
            }
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("CLASS BATCH REPORT SUMMARY");
        System.out.println("Successful: " + successCount + " students");
        System.out.println("Failed: " + failCount + " students");
        System.out.println("Total Reports Generated: " + (successCount * 3));
        System.out.println("Location: reports/{pdf,text,excel}/");

        if (failCount > 0) {
            throw new ExportException(failCount + " students failed to generate reports");
        }
    }

    // ============================
    // EXISTING METHODS (keep these)
    // ============================

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
        ensureReportsDirectory("search");
        Path filePath = Paths.get("reports/search", filename + ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write("STUDENT SEARCH RESULTS");
            writer.newLine();
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            writer.newLine();
            writer.write("Students Found: " + students.size());
            writer.newLine();
            writer.newLine();

            for (Student student : students) {
                writer.write(student.getStudentId() + " - " + student.getName() +
                        " (" + student.getStudentType() + ")");
                writer.newLine();
            }

        } catch (IOException e) {
            throw new ExportException("Search results export failed: " + e.getMessage());
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

    // ============================
    // HELPER METHODS
    // ============================

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