package models;

public class StudentManager {
    private Student[] students;
    private int studentCount;
    private static final int MAX_STUDENTS = 50;

    public StudentManager() {
        students = new Student[MAX_STUDENTS];
        studentCount = 0;
    }

    public void addStudent(Student student) {
        if (studentCount < MAX_STUDENTS) {
            students[studentCount] = student;
            studentCount++;
        } else {
            System.out.println("Maximum student capacity reached!");
        }
    }

    public Student findStudent(String studentId) {
        for (int i = 0; i < studentCount; i++) {
            if (students[i].getStudentId().equals(studentId)) {
                return students[i];
            }
        }
        return null;
    }

    public void viewAllStudents(GradeManager gradeManager) {
        System.out.println("\n=========================================");
        System.out.println("            STUDENT LISTING");
        System.out.println("=========================================");
        System.out.println();
        System.out.println("STU ID | NAME           | TYPE    | AVG GRADE | STATUS");
        System.out.println("---------------------------------------------------------");

        double classTotal = 0;
        int studentsWithGrades = 0;

        for (int i = 0; i < studentCount; i++) {
            Student student = students[i];
            double avgGrade = gradeManager.calculateOverallAverage(student.getStudentId());
            String status = avgGrade >= student.getPassingGrade() ? "Passing" : "Failing";

            // Count for class average
            if (avgGrade > 0) {
                classTotal += avgGrade;
                studentsWithGrades++;
            }

            System.out.printf("%-6s | %-14s | %-7s | %-9s | %s%n",
                    student.getStudentId(),
                    student.getName(),
                    student.getStudentType(),
                    String.format("%.1f%%", avgGrade),
                    status);

            int subjectCount = gradeManager.getGradeCountForStudent(student.getStudentId());
            System.out.printf("      | Enrolled Subjects: %d | Passing Grade: %.0f%%",
                    subjectCount,
                    student.getPassingGrade());

            if (student instanceof HonorsStudent) {
                HonorsStudent honorsStudent = (HonorsStudent) student;
                // Update honors eligibility based on current average
                honorsStudent.setHonorsEligible(avgGrade >= 85.0);
                System.out.print(" | Honors Eligible: " + (honorsStudent.checkHonorsEligibility() ? "Yes" : "No"));
            }
            System.out.println();
        }

        double classAverage = studentsWithGrades > 0 ? classTotal / studentsWithGrades : 0;

        System.out.println("\nTotal Students: " + studentCount);
        System.out.println("Average Class Grade: " + String.format("%.1f", classAverage) + "%");
    }

    public double getAverageClassGrade(GradeManager gradeManager) {
        double total = 0;
        int count = 0;

        for (int i = 0; i < studentCount; i++) {
            double avg = gradeManager.calculateOverallAverage(students[i].getStudentId());
            if (avg > 0) {
                total += avg;
                count++;
            }
        }

        return count > 0 ? total / count : 0.0;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public Student[] getStudents() {
        return students;
    }

    public int getStudentCountValue() {
        return studentCount;
    }


}