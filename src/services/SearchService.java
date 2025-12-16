package services;

import interfaces.Searchable;
import models.*;
import utils.ValidationUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class SearchService implements Searchable {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    // Cache for compiled patterns (US-8)
    private Map<String, Pattern> patternCache;

    // Search statistics
    private int totalSearches = 0;
    private Map<String, Integer> searchTypeCounts;

    public SearchService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.patternCache = new HashMap<>();
        this.searchTypeCounts = new HashMap<>();
    }

    @Override
    public List<Student> searchByName(String name) {
        recordSearch("name");

        // Use stream for case-insensitive partial match
        return studentManager.getStudents().stream()
                .filter(student -> student.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> searchByGradeRange(double minGrade, double maxGrade) {
        recordSearch("grade_range");

        // Validate range
        if (minGrade < 0 || maxGrade > 100 || minGrade > maxGrade) {
            System.out.println("Invalid grade range! Must be 0-100 with min <= max");
            return new ArrayList<>();
        }

        // Use stream with parallel processing for large datasets
        return studentManager.getStudents().parallelStream()
                .filter(student -> {
                    double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                    return avg >= minGrade && avg <= maxGrade;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> searchByStudentType(String type) {
        recordSearch("student_type");

        if (!type.equalsIgnoreCase("Regular") && !type.equalsIgnoreCase("Honors")) {
            System.out.println("Invalid student type! Must be 'Regular' or 'Honors'");
            return new ArrayList<>();
        }

        return studentManager.getStudents().stream()
                .filter(student -> student.getStudentType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public List<Student> searchByStudentId(String studentId) {
        recordSearch("student_id");

        return studentManager.getStudents().stream()
                .filter(student -> student.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

    // New method: Search by email domain (US-7)
    public List<Student> searchByEmailDomain(String domain) {
        recordSearch("email_domain");

        String regex = ".*@" + Pattern.quote(domain) + "$";
        Pattern pattern = compilePattern("email_domain:" + domain, regex, true);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getEmail()).matches())
                .collect(Collectors.toList());
    }

    // New method: Search by phone area code (US-7)
    public List<Student> searchByPhoneAreaCode(String areaCode) {
        recordSearch("phone_area_code");

        String regex = ".*" + Pattern.quote(areaCode) + ".*";
        Pattern pattern = compilePattern("phone_area:" + areaCode, regex, false);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getPhone()).matches())
                .collect(Collectors.toList());
    }

    // New method: Search by name pattern with regex (US-7)
    public List<Student> searchByNamePattern(String regexPattern) {
        recordSearch("name_pattern");

        try {
            Pattern pattern = compilePattern("name_pattern:" + regexPattern, regexPattern, true);

            return studentManager.getStudents().stream()
                    .filter(student -> pattern.matcher(student.getName()).matches())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("Invalid regex pattern: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // New method: Search by student ID pattern with wildcards (US-7)
    public List<Student> searchByStudentIdPattern(String pattern) {
        recordSearch("id_pattern");

        // Convert wildcard pattern to regex (* = .*, ? = .)
        String regex = pattern.replace("*", ".*").replace("?", ".");
        Pattern compiledPattern = compilePattern("id_pattern:" + pattern, regex, false);

        return studentManager.getStudents().stream()
                .filter(student -> compiledPattern.matcher(student.getStudentId()).matches())
                .collect(Collectors.toList());
    }

    // New method: Advanced search with multiple criteria (US-7)
    public List<Student> searchByMultipleCriteria(Map<String, String> criteria) {
        recordSearch("multi_criteria");

        List<Student> results = new ArrayList<>(studentManager.getStudents());

        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            String field = entry.getKey();
            String value = entry.getValue();

            switch (field.toLowerCase()) {
                case "name":
                    results = intersect(results, searchByName(value));
                    break;
                case "id":
                    results = intersect(results, searchByStudentId(value));
                    break;
                case "type":
                    results = intersect(results, searchByStudentType(value));
                    break;
                case "email_domain":
                    results = intersect(results, searchByEmailDomain(value));
                    break;
                case "grade_min":
                    double min = Double.parseDouble(value);
                    results = intersect(results, searchByGradeRange(min, 100));
                    break;
                case "grade_max":
                    double max = Double.parseDouble(value);
                    results = intersect(results, searchByGradeRange(0, max));
                    break;
                case "phone_area":
                    results = intersect(results, searchByPhoneAreaCode(value));
                    break;
            }
        }

        return results;
    }

    // New method: Custom regex search on any field (US-7)
    public List<Student> searchByCustomRegex(String regex, String field) {
        recordSearch("custom_regex");

        try {
            Pattern pattern = compilePattern("custom:" + field + ":" + regex, regex, true);

            return studentManager.getStudents().stream()
                    .filter(student -> {
                        switch (field.toLowerCase()) {
                            case "id": return pattern.matcher(student.getStudentId()).matches();
                            case "name": return pattern.matcher(student.getName()).matches();
                            case "email": return pattern.matcher(student.getEmail()).matches();
                            case "phone": return pattern.matcher(student.getPhone()).matches();
                            case "type": return pattern.matcher(student.getStudentType()).matches();
                            default: return false;
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("Invalid regex: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // New method: Search students with specific letter grades (US-7)
    public List<Student> searchByLetterGrade(String letterGrade) {
        recordSearch("letter_grade");

        if (!letterGrade.matches("[ABCDF][+-]?")) {
            System.out.println("Invalid letter grade! Use A, B, C, D, F with optional + or -");
            return new ArrayList<>();
        }

        return studentManager.getStudents().stream()
                .filter(student -> {
                    double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                    String studentLetterGrade = getLetterGrade(avg);
                    return studentLetterGrade.startsWith(letterGrade.toUpperCase());
                })
                .collect(Collectors.toList());
    }

    // New method: Search by enrollment date range - FIXED HERE
    public List<Student> searchByEnrollmentDateRange(String startDate, String endDate) {
        recordSearch("enrollment_date");

        // Use the ValidationResult object properly
        ValidationUtils.ValidationResult startDateResult = ValidationUtils.validateDate(startDate);
        ValidationUtils.ValidationResult endDateResult = ValidationUtils.validateDate(endDate);

        if (!startDateResult.isValid() || !endDateResult.isValid()) {
            System.out.println("Invalid date format! Use YYYY-MM-DD");
            if (!startDateResult.isValid()) {
                startDateResult.displayError();
            }
            if (!endDateResult.isValid()) {
                endDateResult.displayError();
            }
            return new ArrayList<>();
        }

        return studentManager.getStudents().stream()
                .filter(student -> {
                    String enrollmentDate = student.getEnrollmentDateString();
                    return enrollmentDate.compareTo(startDate) >= 0 &&
                            enrollmentDate.compareTo(endDate) <= 0;
                })
                .collect(Collectors.toList());
    }

    // New method: Search students with honors eligibility
    public List<Student> searchByHonorsEligibility(boolean eligible) {
        recordSearch("honors_eligibility");

        return studentManager.getStudents().stream()
                .filter(student -> {
                    if (student instanceof HonorsStudent) {
                        HonorsStudent honorsStudent = (HonorsStudent) student;
                        double avg = gradeManager.calculateOverallAverage(student.getStudentId());
                        honorsStudent.setHonorsEligible(avg >= 85.0);
                        return honorsStudent.checkHonorsEligibility() == eligible;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    // New method: Complex search with AND/OR logic (US-7)
    public List<Student> searchWithLogic(String query) {
        recordSearch("complex_query");

        // Simple query parser for AND/OR logic
        if (query.contains(" AND ")) {
            String[] parts = query.split(" AND ");
            List<Student> result1 = parseSearchTerm(parts[0].trim());
            List<Student> result2 = parseSearchTerm(parts[1].trim());
            return intersect(result1, result2);

        } else if (query.contains(" OR ")) {
            String[] parts = query.split(" OR ");
            List<Student> result1 = parseSearchTerm(parts[0].trim());
            List<Student> result2 = parseSearchTerm(parts[1].trim());
            return union(result1, result2);

        } else {
            return parseSearchTerm(query.trim());
        }
    }

    private List<Student> parseSearchTerm(String term) {
        if (term.contains(":")) {
            String[] parts = term.split(":", 2);
            String field = parts[0].trim();
            String value = parts[1].trim();

            switch (field.toLowerCase()) {
                case "email": return searchByEmailDomain(value);
                case "phone": return searchByPhoneAreaCode(value);
                case "name": return searchByNamePattern(value);
                case "id": return searchByStudentIdPattern(value);
                case "type": return searchByStudentType(value);
                case "grade": return searchByLetterGrade(value);
                default: return searchByCustomRegex(value, field);
            }
        } else {
            return searchByName(term);
        }
    }

    public void displaySearchResults(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("No students found matching your criteria.");
            return;
        }

        System.out.println("\n=== SEARCH RESULTS ===");
        System.out.println("Found: " + students.size() + " students");
        System.out.println();
        System.out.printf("%-8s | %-20s | %-10s | %-25s | %-15s | %s%n",
                "STU ID", "NAME", "TYPE", "EMAIL", "PHONE", "AVG GRADE");
        System.out.println("---------------------------------------------------------------------------");

        for (Student student : students) {
            double avg = gradeManager.calculateOverallAverage(student.getStudentId());
            System.out.printf("%-8s | %-20s | %-10s | %-25s | %-15s | %6.1f%%%n",
                    student.getStudentId(),
                    truncate(student.getName(), 20),
                    student.getStudentType(),
                    truncate(student.getEmail(), 25),
                    truncate(student.getPhone(), 15),
                    avg);
        }

        displaySearchStatistics(students);
    }

    // Enhanced display with pattern matching highlights
    public void displaySearchResultsWithHighlight(List<Student> students, Pattern pattern, String field) {
        if (students.isEmpty()) {
            System.out.println("No students found matching your criteria.");
            return;
        }

        System.out.println("\n=== SEARCH RESULTS WITH PATTERN HIGHLIGHT ===");
        System.out.println("Pattern: " + pattern.pattern());
        System.out.println("Field: " + field);
        System.out.println("Found: " + students.size() + " students");
        System.out.println();

        System.out.printf("%-8s | %-20s | %-10s | %-30s%n",
                "STU ID", "NAME", "TYPE", "MATCHED TEXT");
        System.out.println("----------------------------------------------------------");

        for (Student student : students) {
            String textToMatch = getFieldValue(student, field);
            Matcher matcher = pattern.matcher(textToMatch);

            if (matcher.find()) {
                String matchedPart = matcher.group();
                String displayText = textToMatch.replace(matchedPart,
                        "[" + matchedPart + "]");

                System.out.printf("%-8s | %-20s | %-10s | %s%n",
                        student.getStudentId(),
                        truncate(student.getName(), 20),
                        student.getStudentType(),
                        truncate(displayText, 30));
            }
        }
    }

    private String getFieldValue(Student student, String field) {
        switch (field.toLowerCase()) {
            case "id": return student.getStudentId();
            case "name": return student.getName();
            case "email": return student.getEmail();
            case "phone": return student.getPhone();
            case "type": return student.getStudentType();
            default: return student.getStudentId();
        }
    }

    private void displaySearchStatistics(List<Student> results) {
        System.out.println("\n=== SEARCH STATISTICS ===");
        System.out.println("Total searches performed: " + totalSearches);

        if (!searchTypeCounts.isEmpty()) {
            System.out.println("\nSearch type distribution:");
            searchTypeCounts.forEach((type, count) ->
                    System.out.printf("  %-20s: %d times%n", type, count));
        }

        // Show pattern cache statistics
        System.out.println("\nPattern cache: " + patternCache.size() + " compiled patterns");

        // Show student distribution in results
        Map<String, Long> typeDistribution = results.stream()
                .collect(Collectors.groupingBy(Student::getStudentType, Collectors.counting()));

        if (!typeDistribution.isEmpty()) {
            System.out.println("\nResult distribution:");
            typeDistribution.forEach((type, count) ->
                    System.out.printf("  %-20s: %d students%n", type + " students", count));
        }
    }

    // Helper methods
    private Pattern compilePattern(String key, String regex, boolean caseInsensitive) {
        return patternCache.computeIfAbsent(key, k -> {
            try {
                return caseInsensitive ?
                        Pattern.compile(regex, Pattern.CASE_INSENSITIVE) :
                        Pattern.compile(regex);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
            }
        });
    }

    private List<Student> intersect(List<Student> list1, List<Student> list2) {
        Set<Student> set1 = new HashSet<>(list1);
        return list2.stream()
                .filter(set1::contains)
                .collect(Collectors.toList());
    }

    private List<Student> union(List<Student> list1, List<Student> list2) {
        Set<Student> union = new HashSet<>(list1);
        union.addAll(list2);
        return new ArrayList<>(union);
    }

    private String getLetterGrade(double percentage) {
        if (percentage >= 93) return "A";
        else if (percentage >= 90) return "A-";
        else if (percentage >= 87) return "B+";
        else if (percentage >= 83) return "B";
        else if (percentage >= 80) return "B-";
        else if (percentage >= 77) return "C+";
        else if (percentage >= 73) return "C";
        else if (percentage >= 70) return "C-";
        else if (percentage >= 67) return "D+";
        else if (percentage >= 60) return "D";
        else return "F";
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private void recordSearch(String searchType) {
        totalSearches++;
        searchTypeCounts.merge(searchType, 1, Integer::sum);
    }

    // Cache management methods
    public void clearPatternCache() {
        patternCache.clear();
        System.out.println("Pattern cache cleared.");
    }

    public void displayCacheStatistics() {
        System.out.println("\n=== SEARCH SERVICE CACHE ===");
        System.out.println("Cached patterns: " + patternCache.size());
        System.out.println("Total searches: " + totalSearches);

        if (!patternCache.isEmpty()) {
            System.out.println("\nCached patterns:");
            patternCache.forEach((key, pattern) ->
                    System.out.printf("  %-30s: %s%n", key, pattern.pattern()));
        }
    }
}