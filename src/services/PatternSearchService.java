package services;

import models.*;
import utils.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class PatternSearchService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    // Cache compiled patterns for performance
    private Map<String, Pattern> patternCache;

    public PatternSearchService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.patternCache = new ConcurrentHashMap<>();
    }

    public List<Student> searchByEmailDomain(String domainPattern) {
        String regex = ".*" + Pattern.quote(domainPattern) + "$";
        Pattern pattern = compilePattern("email:" + domainPattern, regex);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getEmail()).matches())
                .collect(Collectors.toList());
    }

    public List<Student> searchByPhoneAreaCode(String areaCode) {
        String regex = ".*" + Pattern.quote(areaCode) + ".*";
        Pattern pattern = compilePattern("phone:" + areaCode, regex);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getPhone()).matches())
                .collect(Collectors.toList());
    }

    public List<Student> searchByStudentIdPattern(String patternWithWildcards) {
        String regex = patternWithWildcards.replace("*", ".*").replace("?", ".");
        Pattern pattern = compilePattern("id:" + patternWithWildcards, regex);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getStudentId()).matches())
                .collect(Collectors.toList());
    }

    public List<Student> searchByNamePattern(String regexPattern) {
        Pattern pattern = compilePattern("name:" + regexPattern, regexPattern);

        return studentManager.getStudents().stream()
                .filter(student -> pattern.matcher(student.getName()).matches())
                .collect(Collectors.toList());
    }

    public List<Student> searchByCustomPattern(String regexPattern, String field) {
        Pattern pattern = compilePattern("custom:" + field + ":" + regexPattern, regexPattern);

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
    }

    public List<Student> searchByGradePattern(String pattern) {
        // Search students whose grades match a pattern
        // Example: ">85" for grades above 85, "70-80" for grades between 70 and 80

        List<Student> results = new ArrayList<>();

        for (Student student : studentManager.getStudents()) {
            List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());

            boolean matches = grades.stream().anyMatch(grade -> {
                double gradeValue = grade.getGrade();

                if (pattern.startsWith(">")) {
                    double threshold = Double.parseDouble(pattern.substring(1));
                    return gradeValue > threshold;
                } else if (pattern.startsWith("<")) {
                    double threshold = Double.parseDouble(pattern.substring(1));
                    return gradeValue < threshold;
                } else if (pattern.contains("-")) {
                    String[] range = pattern.split("-");
                    double min = Double.parseDouble(range[0]);
                    double max = Double.parseDouble(range[1]);
                    return gradeValue >= min && gradeValue <= max;
                } else if (pattern.equals("A") || pattern.equals("B") || pattern.equals("C") ||
                        pattern.equals("D") || pattern.equals("F")) {
                    return grade.getLetterGrade().startsWith(pattern);
                }

                return false;
            });

            if (matches) {
                results.add(student);
            }
        }

        return results;
    }

    public Map<String, List<Student>> searchByMultiplePatterns(Map<String, String> patterns) {
        Map<String, List<Student>> results = new HashMap<>();

        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            String field = entry.getKey();
            String pattern = entry.getValue();

            List<Student> fieldResults = switch (field.toLowerCase()) {
                case "email" -> searchByEmailDomain(pattern);
                case "phone" -> searchByPhoneAreaCode(pattern);
                case "id" -> searchByStudentIdPattern(pattern);
                case "name" -> searchByNamePattern(pattern);
                case "grade" -> searchByGradePattern(pattern);
                default -> searchByCustomPattern(pattern, field);
            };

            results.put(field, fieldResults);
        }

        return results;
    }

    public List<Student> searchByComplexQuery(String query) {
        // Support queries like: "email:@university.edu AND grade>85"
        // Or: "name:John* OR email:*@college.org"

        try {
            if (query.contains(" AND ")) {
                String[] parts = query.split(" AND ");
                return intersectResults(
                        searchByQueryPart(parts[0]),
                        searchByQueryPart(parts[1])
                );
            } else if (query.contains(" OR ")) {
                String[] parts = query.split(" OR ");
                return unionResults(
                        searchByQueryPart(parts[0]),
                        searchByQueryPart(parts[1])
                );
            } else {
                return searchByQueryPart(query);
            }
        } catch (Exception e) {
            System.out.println("Invalid query syntax: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Student> searchByQueryPart(String queryPart) {
        if (queryPart.contains(":")) {
            String[] parts = queryPart.split(":", 2);
            String field = parts[0].trim();
            String pattern = parts[1].trim();

            return searchByCustomPattern(pattern, field);
        } else if (queryPart.contains(">")) {
            return searchByGradePattern(queryPart);
        } else if (queryPart.contains("<")) {
            return searchByGradePattern(queryPart);
        } else if (queryPart.contains("-")) {
            return searchByGradePattern(queryPart);
        } else {
            // Default to name search
            return searchByNamePattern(".*" + Pattern.quote(queryPart) + ".*");
        }
    }

    private List<Student> intersectResults(List<Student> list1, List<Student> list2) {
        Set<Student> set1 = new HashSet<>(list1);
        return list2.stream()
                .filter(set1::contains)
                .collect(Collectors.toList());
    }

    private List<Student> unionResults(List<Student> list1, List<Student> list2) {
        Set<Student> union = new HashSet<>(list1);
        union.addAll(list2);
        return new ArrayList<>(union);
    }

    public Map<String, Object> analyzePatternMatches(String regexPattern, String field) {
        Map<String, Object> analysis = new HashMap<>();
        Pattern pattern = compilePattern("analyze:" + regexPattern, regexPattern);

        List<Student> allStudents = studentManager.getStudents();
        List<Student> matches = new ArrayList<>();
        List<String> matchedTexts = new ArrayList<>();

        for (Student student : allStudents) {
            String textToMatch = switch (field.toLowerCase()) {
                case "id" -> student.getStudentId();
                case "name" -> student.getName();
                case "email" -> student.getEmail();
                case "phone" -> student.getPhone();
                default -> student.getStudentId();
            };

            Matcher matcher = pattern.matcher(textToMatch);
            if (matcher.matches()) {
                matches.add(student);
                if (matcher.groupCount() > 0) {
                    matchedTexts.add(matcher.group(0));
                }
            }
        }

        analysis.put("totalScanned", allStudents.size());
        analysis.put("matchesFound", matches.size());
        analysis.put("matchPercentage", allStudents.size() > 0 ?
                (matches.size() * 100.0 / allStudents.size()) : 0);
        analysis.put("matchedStudents", matches);
        analysis.put("matchedTexts", matchedTexts);
        analysis.put("pattern", regexPattern);
        analysis.put("field", field);

        // Complexity analysis
        analysis.put("complexity", analyzeRegexComplexity(regexPattern));

        return analysis;
    }

    private String analyzeRegexComplexity(String regex) {
        // Simple complexity analysis
        if (regex.contains(".*") || regex.contains(".+")) {
            return "O(n) - Linear scan";
        } else if (regex.contains("|")) {
            return "O(n*m) - Multiple alternatives";
        } else if (regex.contains("(") && regex.contains(")")) {
            return "O(n) - With capturing groups";
        } else {
            return "O(n) - Simple pattern";
        }
    }

    private Pattern compilePattern(String key, String regex) {
        return patternCache.computeIfAbsent(key, k -> {
            try {
                return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                System.out.println("Invalid regex pattern: " + regex);
                throw new IllegalArgumentException("Invalid regex: " + e.getMessage());
            }
        });
    }

    public void clearPatternCache() {
        patternCache.clear();
        System.out.println("Pattern cache cleared.");
    }

    public void displaySearchStatistics() {
        System.out.println("\n=== PATTERN SEARCH STATISTICS ===");
        System.out.println("Cached patterns: " + patternCache.size());
        System.out.println("Available search methods:");
        System.out.println("  - Email domain search");
        System.out.println("  - Phone area code search");
        System.out.println("  - Student ID pattern (with wildcards)");
        System.out.println("  - Name regex search");
        System.out.println("  - Grade pattern search (>85, 70-80, A, B, etc.)");
        System.out.println("  - Complex queries (AND/OR combinations)");
        System.out.println("  - Custom field regex search");
    }
}