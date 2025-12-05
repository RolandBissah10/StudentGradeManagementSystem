package interfaces;

import models.Student;

import java.util.List;

public interface Searchable {
    List<Student> searchByName(String name);
    List<Student> searchByGradeRange(double minGrade, double maxGrade);
    List<Student> searchByStudentType(String type);
}