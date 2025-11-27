import java.text.SimpleDateFormat;
import java.util.Date;

public class Grade implements Gradable {
    private String gradeId;
    private String studentId;
    private Subject subject;
    private double grade;
    private String date;

    private static int gradeCounter = 0;

    public Grade(String studentId, Subject subject, double grade) {
        this.gradeId = generateGradeId();
        this.studentId = studentId;
        this.subject = subject;
        this.grade = grade;
        this.date = generateDate();
    }

    private String generateGradeId() {
        gradeCounter++;
        return String.format("GRD%03d", gradeCounter);
    }

    private String generateDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        return formatter.format(new Date());
    }

    @Override
    public boolean recordGrade(double grade) {
        if (validateGrade(grade)) {
            this.grade = grade;
            return true;
        }
        return false;
    }

    @Override
    public boolean validateGrade(double grade) {
        return grade >= 0 && grade <= 100;
    }

    public void displayGradeDetails() {
        System.out.println("Grade ID: " + gradeId);
        System.out.println("Student ID: " + studentId);
        System.out.println("Subject: " + subject.getSubjectName() + " (" + subject.getSubjectType() + ")");
        System.out.println("Grade: " + grade + "%");
        System.out.println("Date: " + date);
        System.out.println("Letter Grade: " + getLetterGrade());
    }

    public String getLetterGrade() {
        if (grade >= 90) return "A";
        else if (grade >= 80) return "B";
        else if (grade >= 70) return "C";
        else if (grade >= 60) return "D";
        else return "F";
    }

    // Getters
    public String getGradeId() { return gradeId; }
    public String getStudentId() { return studentId; }
    public Subject getSubject() { return subject; }
    public double getGrade() { return grade; }
    public String getDate() { return date; }

    public static int getGradeCounter() { return gradeCounter; }
}