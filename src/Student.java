public abstract class Student {
    private String studentId;
    private String name;
    private int age;
    private String email;
    private String phone;
    private String status;

    private static int studentCounter = 0;

    public Student(String name, int age, String email, String phone) {
        this.studentId = generateStudentId();
        this.name = name;
        this.age = age;
        this.email = email;
        this.phone = phone;
        this.status = "Active";
    }

    private String generateStudentId() {
        studentCounter++;
        return String.format("STU%03d", studentCounter);
    }


    public abstract void displayStudentDetails();
    public abstract String getStudentType();
    public abstract double getPassingGrade();


    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public static int getStudentCounter() { return studentCounter; }
}