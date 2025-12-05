package interfaces;

import java.util.Map;

public interface Calculate {
    double calculateGPA(String studentId);
    double calculateClassRank(String studentId);
    Map<String, Double> calculateSubjectAverages(String studentId);
}