public abstract class Subject {
    private String subjectName;
    private String subjectCode;

    public Subject(String subjectName, String subjectCode) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
    }

    // Abstract methods
    public abstract void displaySubjectDetails();
    public abstract String getSubjectType();

    // Getters
    public String getSubjectName() { return subjectName; }
    public String getSubjectCode() { return subjectCode; }
}