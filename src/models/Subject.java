package models;

public abstract class Subject {
    private String subjectName;
    private String subjectCode;

    public Subject(String subjectName, String subjectCode, String core) {
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
    }

    public abstract void displaySubjectDetails();
    public abstract String getSubjectType();


    public String getSubjectName() { return subjectName; }
    public String getSubjectCode() { return subjectCode; }
}