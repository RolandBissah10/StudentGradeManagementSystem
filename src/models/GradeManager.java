package models;

public class GradeManager {
    private Grade[] grades;
    private int gradeCount;
    private static final int MAX_GRADES = 200;

    public GradeManager() {
        grades = new Grade[MAX_GRADES];
        gradeCount = 0;
    }

    public void addGrade(Grade grade) {
        if (gradeCount < MAX_GRADES) {
            grades[gradeCount] = grade;
            gradeCount++;
        } else {
            System.out.println("Maximum grade capacity reached!");
        }
    }

    public void viewGradesByStudent(String studentId, Student student) {
        if (student == null) {
            System.out.println("models.Student not found!");
            return;
        }

        System.out.println("\nmodels.Student: " + studentId + " - " + student.getName());
        System.out.println("Type: " + student.getStudentType() + " models.Student");
        double overallAvg = calculateOverallAverage(studentId);
        System.out.println("Current Average: " + String.format("%.1f", overallAvg) + "%");
        System.out.println("Status: " + (overallAvg >= student.getPassingGrade() ? "PASSING ✓" : "FAILING"));

        System.out.println("\nGRADE HISTORY");
        System.out.println("GRD ID | DATE       | SUBJECT     | TYPE    | GRADE");
        System.out.println("---------------------------------------------------");

        boolean hasGrades = false;
        int gradeDisplayCount = 0;

        // Display grades in reverse chronological order (newest first)
        for (int i = gradeCount - 1; i >= 0; i--) {
            if (grades[i].getStudentId().equals(studentId)) {
                Grade grade = grades[i];
                System.out.printf("%-6s | %-10s | %-11s | %-7s | %.1f%%%n",
                        grade.getGradeId(),
                        grade.getDate(),
                        grade.getSubject().getSubjectName(),
                        grade.getSubject().getSubjectType(),
                        grade.getGrade());
                hasGrades = true;
                gradeDisplayCount++;

                // Limit display to prevent overflow, but still calculate all averages
                if (gradeDisplayCount >= 20) {
                    System.out.println("... (showing latest 20 grades)");
                    break;
                }
            }
        }

        if (!hasGrades) {
            System.out.println("\nNo grades recorded for this student.");
            return;
        }

        int totalGrades = getGradeCountForStudent(studentId);
        double coreAvg = calculateCoreAverage(studentId);
        double electiveAvg = calculateElectiveAverage(studentId);

        System.out.println("\nTotal Grades: " + totalGrades);
        System.out.println("Core Subjects Average: " + String.format("%.1f", coreAvg) + "%");
        System.out.println("Elective Subjects Average: " + String.format("%.1f", electiveAvg) + "%");
        System.out.println("Overall Average: " + String.format("%.1f", overallAvg) + "%");

        System.out.println("\nPerformance Summary:");
        if (coreAvg >= student.getPassingGrade()) {
            System.out.println("✓ Passing all core subjects");
        } else {
            System.out.println("✗ Not passing all core subjects");
        }

        if (overallAvg >= student.getPassingGrade()) {
            System.out.println("✓ Meeting passing grade requirement (" + student.getPassingGrade() + "%)");
        } else {
            System.out.println("✗ Not meeting passing grade requirement (" + student.getPassingGrade() + "%)");
        }

        if (student instanceof HonorsStudent) {
            HonorsStudent honorsStudent = (HonorsStudent) student;
            if (honorsStudent.checkHonorsEligibility()) {
                System.out.println("✓ Eligible for honors recognition");
            } else {
                System.out.println("✗ Not eligible for honors recognition (requires 85% average)");
            }
        }
    }

    public double calculateCoreAverage(String studentId) {
        double sum = 0;
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            Grade grade = grades[i];
            if (grade.getStudentId().equals(studentId) && grade.getSubject() instanceof CoreSubject) {
                sum += grade.getGrade();
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public double calculateElectiveAverage(String studentId) {
        double sum = 0;
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            Grade grade = grades[i];
            if (grade.getStudentId().equals(studentId) && grade.getSubject() instanceof ElectiveSubject) {
                sum += grade.getGrade();
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public double calculateOverallAverage(String studentId) {
        double sum = 0;
        int count = 0;

        for (int i = 0; i < gradeCount; i++) {
            Grade grade = grades[i];
            if (grade.getStudentId().equals(studentId)) {
                sum += grade.getGrade();
                count++;
            }
        }

        return count > 0 ? sum / count : 0.0;
    }

    public int getGradeCountForStudent(String studentId) {
        int count = 0;
        for (int i = 0; i < gradeCount; i++) {
            if (grades[i].getStudentId().equals(studentId)) {
                count++;
            }
        }
        return count;
    }

    public int getGradeCount() {
        return gradeCount;
    }

    public Grade[] getGrades() {
        return grades;
    }
}