package models;

public class RegularStudent extends Student {
    private double passingGrade = 50.0;

    public RegularStudent(String name, int age, String email, String phone, String enrollmentDate) {
        super(name, age, email, phone, enrollmentDate);
    }

    @Override
    public void displayStudentDetails() {
        System.out.println("Student ID: " + getStudentId());
        System.out.println("Name: " + getName());
        System.out.println("Type: " + getStudentType());
        System.out.println("Age: " + getAge());
        System.out.println("Email: " + getEmail());
        System.out.println("Phone: " + getPhone());
        System.out.println("Enrollment Date: " + getEnrollmentDateString());
        System.out.println("Passing Grade: " + getPassingGrade() + "%");
        System.out.println("Status: " + getStatus());
    }

    @Override
    public String getStudentType() {
        return "Regular";
    }

    @Override
    public double getPassingGrade() {
        return passingGrade;
    }
}