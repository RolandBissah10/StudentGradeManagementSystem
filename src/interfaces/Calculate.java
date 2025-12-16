package interfaces;

import java.util.Map;

public interface Calculate {
    double calculateGPA(String studentId);
    double calculateClassRank(String studentId);
    Map<String, Double> calculateSubjectAverages(String studentId);

    // Optional new methods
    double calculateWeightedGPA(String studentId, Map<String, Integer> creditHours);
    Map<String, Double> calculateSemesterGPAs(String studentId);
    Map<String, Integer> getClassGPADistribution();
}