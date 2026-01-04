package models;

public class ElectiveSubject extends Subject {
    private boolean mandatory = false;

    public ElectiveSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode, "Core");
    }

    @Override
    public void displaySubjectDetails() {
        System.out.println("Subject: " + getSubjectName() + " (" + getSubjectCode() + ")");
        System.out.println("Type: Elective");
        System.out.println("Mandatory: " + (isMandatory() ? "Yes" : "No"));
    }

    @Override
    public String getSubjectType() {
        return "Elective";
    }

    public boolean isMandatory() {
        return mandatory;
    }
}