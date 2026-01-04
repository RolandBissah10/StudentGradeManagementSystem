package models;

public class CoreSubject extends Subject {
    private boolean mandatory = true;

    public CoreSubject(String subjectName, String subjectCode) {
        super(subjectName, subjectCode, "Core");
    }

    @Override
    public void displaySubjectDetails() {
        System.out.println("Subject: " + getSubjectName() + " (" + getSubjectCode() + ")");
        System.out.println("Type: Core");
        System.out.println("Mandatory: " + (isMandatory() ? "Yes" : "No"));
    }

    @Override
    public String getSubjectType() {
        return "Core";
    }
    public boolean isMandatory() {
        return mandatory;
    }
}