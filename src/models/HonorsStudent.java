package models;

public class HonorsStudent extends Student {
    private double passingGrade = 60.0;
    private boolean honorsEligible;
    private double gpa;

    public HonorsStudent(String name, int age, String email, String phone, String enrollmentDate) {
        super(name, age, email, phone, enrollmentDate);
        this.honorsEligible = false;
        this.gpa = 0.0;
    }

    @Override
    public void displayStudentDetails() {
        System.out.println("=".repeat(50));
        System.out.println("HONORS STUDENT DETAILS");
        System.out.println("=".repeat(50));
        System.out.println("Student ID: " + getStudentId());
        System.out.println("Name: " + getName());
        System.out.println("Type: " + getStudentType());
        System.out.println("Age: " + getAge());
        System.out.println("Email: " + getEmail());
        System.out.println("Phone: " + getPhone());
        System.out.println("Enrollment Date: " + getEnrollmentDateString());
        System.out.println("GPA: " + String.format("%.2f", getGpa()));
        System.out.println("Passing Grade: " + getPassingGrade() + "%");

        // Honors eligibility with explanation
        String eligibility = checkHonorsEligibility() ? "✓ Yes" : "✗ No";
        String explanation = checkHonorsEligibility() ? "(≥85% average)" : "(<85% average)";
        System.out.println("Honors Eligible: " + eligibility + " " + explanation);

        System.out.println("Status: " + getStatus());
        System.out.println("=".repeat(50));
    }

    @Override
    public String getStudentType() {
        return "Honors";
    }

    @Override
    public double getPassingGrade() {
        return passingGrade;
    }

    @Override
    public double getGpa() {
        return gpa;
    }

    @Override
    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public boolean checkHonorsEligibility() {
        return honorsEligible;
    }

    public void setHonorsEligible(boolean honorsEligible) {
        this.honorsEligible = honorsEligible;
    }

    @Override
    public String getStatus() {
        double averageGrade = getAverageGrade();
        boolean isPassing = averageGrade >= passingGrade;
        boolean isHonorsEligible = honorsEligible;

        if (isHonorsEligible && isPassing) {
            return "PASSING WITH HONORS ELIGIBILITY";
        } else if (isPassing) {
            return "PASSING (but not honors eligible)";
        } else {
            return "FAILING";
        }
    }

    @Override
    public String getDetailedStatus() {
        double averageGrade = getAverageGrade();
        return String.format("Average: %.1f%% | GPA: %.2f | Passing: %s | Honors Eligible: %s",
                averageGrade,
                gpa,
                (averageGrade >= passingGrade ? "Yes (≥60%)" : "No (<60%)"),
                (honorsEligible ? "Yes (≥85%)" : "No (<85%)"));
    }
}