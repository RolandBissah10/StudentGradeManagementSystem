package services;

import models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class PatternSearchService {
    private StudentManager studentManager;
    private GradeManager gradeManager;

    // Cache compiled patterns for performance (US-8)
    private Map<String, Pattern> patternCache;

    // Search statistics
    private int totalSearches = 0;
    private int successfulSearches = 0;
    private Map<String, Integer> searchTypeCounts;

    public PatternSearchService(StudentManager studentManager, GradeManager gradeManager) {
        this.studentManager = studentManager;
        this.gradeManager = gradeManager;
        this.patternCache = new ConcurrentHashMap<>();
        this.searchTypeCounts = new HashMap<>();
    }

    // US-7: Email domain pattern search
    public Map<String, Object> searchByEmailDomain(String domainPattern) {
        recordSearch("email_domain");

        if (!domainPattern.contains("@")) {
            domainPattern = "@" + domainPattern;
        }

        String regex = ".*" + Pattern.quote(domainPattern) + "$";
        Pattern pattern = compilePattern("email:" + domainPattern, regex, true);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> matches = new ArrayList<>();
        int totalScanned = 0;

        for (Student student : studentManager.getStudents()) {
            totalScanned++;
            Matcher matcher = pattern.matcher(student.getEmail());

            // Use matches() for email domain (needs to match entire email)
            if (matcher.matches()) {
                Map<String, String> match = new HashMap<>();
                match.put("studentId", student.getStudentId());
                match.put("name", student.getName());
                match.put("email", student.getEmail());
                match.put("fullMatch", "Yes");
                matches.add(match);
            }
        }

        result.put("matches", matches);
        result.put("totalScanned", totalScanned);
        result.put("matchesFound", matches.size());
        result.put("pattern", pattern.pattern());
        result.put("searchType", "Email Domain");
        result.put("domainDistribution", analyzeEmailDomains(matches));

        successfulSearches++;
        return result;
    }

    // US-7: Phone area code pattern search
    public Map<String, Object> searchByPhoneAreaCode(String areaCode) {
        recordSearch("phone_area_code");

        // Support multiple phone formats
        String[] patterns = {
                ".*\\(" + areaCode + "\\).*",        // (555) 123-4567
                ".*" + areaCode + "-.*",            // 555-123-4567
                ".*\\+" + areaCode + "-.*",         // +1-555-123-4567
                "^" + areaCode + ".*"               // 5551234567
        };

        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> matches = new ArrayList<>();
        int totalScanned = 0;

        for (Student student : studentManager.getStudents()) {
            totalScanned++;
            String phone = student.getPhone();

            for (String patternStr : patterns) {
                Pattern pattern = compilePattern("phone:" + areaCode + ":" + patternStr, patternStr, false);
                Matcher matcher = pattern.matcher(phone);
                if (matcher.find()) {
                    Map<String, String> match = new HashMap<>();
                    match.put("studentId", student.getStudentId());
                    match.put("name", student.getName());
                    match.put("phone", phone);
                    match.put("pattern", patternStr);
                    matches.add(match);
                    break; // Found match, no need to check other patterns
                }
            }
        }

        result.put("matches", matches);
        result.put("totalScanned", totalScanned);
        result.put("matchesFound", matches.size());
        result.put("searchType", "Phone Area Code");
        result.put("areaCode", areaCode);

        successfulSearches++;
        return result;
    }

    // US-7: Student ID pattern with wildcards
    public Map<String, Object> searchByStudentIdPattern(String patternWithWildcards) {
        recordSearch("student_id_pattern");

        // Convert wildcards to regex: * = .*, ? = .
        String regex = "^" + patternWithWildcards
                .replace("*", ".*")
                .replace("?", ".");

        Pattern pattern = compilePattern("id:" + patternWithWildcards, regex, false);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> matches = new ArrayList<>();
        int totalScanned = 0;

        for (Student student : studentManager.getStudents()) {
            totalScanned++;
            Matcher matcher = pattern.matcher(student.getStudentId());
            if (matcher.matches()) {
                Map<String, String> match = new HashMap<>();
                match.put("studentId", student.getStudentId());
                match.put("name", student.getName());
                match.put("email", student.getEmail());
                match.put("type", student.getStudentType());
                match.put("pattern", patternWithWildcards);
                matches.add(match);
            }
        }

        result.put("matches", matches);
        result.put("totalScanned", totalScanned);
        result.put("matchesFound", matches.size());
        result.put("searchType", "Student ID Pattern");
        result.put("wildcardPattern", patternWithWildcards);
        result.put("regexPattern", regex);

        successfulSearches++;
        return result;
    }

    // US-7: Name pattern with regex
    public Map<String, Object> searchByNamePattern(String regexPattern) {
        recordSearch("name_pattern");

        try {
            Pattern pattern = compilePattern("name:" + regexPattern, regexPattern, true);

            Map<String, Object> result = new HashMap<>();
            List<Map<String, String>> matches = new ArrayList<>();
            int totalScanned = 0;

            for (Student student : studentManager.getStudents()) {
                totalScanned++;
                Matcher matcher = pattern.matcher(student.getName());

                // Use find() for partial matches in names
                if (matcher.find()) {
                    Map<String, String> match = new HashMap<>();
                    match.put("studentId", student.getStudentId());
                    match.put("name", student.getName());
                    match.put("email", student.getEmail());
                    match.put("matchedText", matcher.group());
                    matches.add(match);
                }
            }

            result.put("matches", matches);
            result.put("totalScanned", totalScanned);
            result.put("matchesFound", matches.size());
            result.put("searchType", "Name Pattern");
            result.put("regexPattern", regexPattern);
            result.put("complexity", analyzeRegexComplexity(regexPattern));

            successfulSearches++;
            return result;

        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Invalid regex pattern: " + e.getMessage());
            errorResult.put("regexPattern", regexPattern);
            errorResult.put("example", "Valid examples: ^J.* (names starting with J), .*son$ (names ending with son)");
            return errorResult;
        }
    }

    // US-7: Custom regex pattern on any field - FIXED VERSION
    public Map<String, Object> searchByCustomPattern(String regexPattern, String field) {
        recordSearch("custom_pattern");

        try {
            // First, let's validate the field
            String validField = validateField(field);
            if (validField == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Invalid field: " + field + ". Valid fields are: id, name, email, phone, type");
                return errorResult;
            }

            // Compile the pattern with debugging
            Pattern pattern;
            try {
                pattern = compilePattern("custom:" + field + ":" + regexPattern, regexPattern, true);
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Invalid regex pattern: " + e.getMessage());
                errorResult.put("example", "Valid examples: ^STU0.* (IDs starting with STU0), .*@.*\\.edu$ (email ending with .edu), ^[A-M].* (names starting with A-M)");
                return errorResult;
            }

            Map<String, Object> result = new HashMap<>();
            List<Map<String, String>> matches = new ArrayList<>();
            int totalScanned = 0;

            // Get all students
            List<Student> allStudents = studentManager.getStudents();

            // Debug: Show what we're looking for
            System.out.println("\n[DEBUG] Searching with pattern: " + regexPattern);
            System.out.println("[DEBUG] Field: " + field);
            System.out.println("[DEBUG] Total students: " + allStudents.size());

            for (Student student : allStudents) {
                totalScanned++;
                String textToMatch = getFieldValue(student, field);

                if (textToMatch == null || textToMatch.isEmpty()) {
                    continue;
                }

                // Create a fresh matcher for each check
                Matcher matcher = pattern.matcher(textToMatch);

                // First try matches() for full string match
                boolean fullMatch = matcher.matches();

                // Reset matcher for find()
                matcher.reset();
                boolean partialMatch = matcher.find();

                if (fullMatch || partialMatch) {
                    Map<String, String> match = new HashMap<>();
                    match.put("studentId", student.getStudentId());
                    match.put("name", student.getName());
                    match.put("field", field);
                    match.put("value", textToMatch);

                    // Get the matched text
                    if (fullMatch) {
                        match.put("matchedText", textToMatch);
                        match.put("fullMatch", "Yes");
                    } else {
                        // Reset and find again to get the matched text
                        matcher.reset();
                        matcher.find();
                        match.put("matchedText", matcher.group());
                        match.put("fullMatch", "No");
                    }

                    matches.add(match);

                    // Debug: Show first few matches
                    if (matches.size() <= 3) {
                        System.out.println("[DEBUG] Match found: " + student.getStudentId() + " - " +
                                student.getName() + " - " + textToMatch);
                    }
                }
            }

            System.out.println("[DEBUG] Total scanned: " + totalScanned);
            System.out.println("[DEBUG] Matches found: " + matches.size());

            result.put("matches", matches);
            result.put("totalScanned", totalScanned);
            result.put("matchesFound", matches.size());
            result.put("searchType", "Custom Pattern");
            result.put("field", field);
            result.put("regexPattern", regexPattern);
            result.put("complexity", analyzeRegexComplexity(regexPattern));

            successfulSearches++;
            return result;

        } catch (Exception e) {
            System.out.println("[DEBUG] Exception in searchByCustomPattern: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Search error: " + e.getMessage());
            errorResult.put("field", field);
            errorResult.put("regexPattern", regexPattern);
            errorResult.put("example", "Valid examples: ^STU0.* (IDs starting with STU0), .*@.*\\.edu$ (email ending with .edu), ^[A-M].* (names starting with A-M)");
            return errorResult;
        }
    }

    // Helper to validate field
    private String validateField(String field) {
        if (field == null) return null;

        String lowerField = field.toLowerCase();
        switch (lowerField) {
            case "id":
            case "name":
            case "email":
            case "phone":
            case "type":
                return lowerField;
            default:
                return null;
        }
    }

    // US-7: Grade pattern search
    public Map<String, Object> searchByGradePattern(String pattern) {
        recordSearch("grade_pattern");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> matches = new ArrayList<>();
        int totalScanned = 0;

        try {
            for (Student student : studentManager.getStudents()) {
                totalScanned++;
                List<Grade> grades = gradeManager.getGradesByStudent(student.getStudentId());

                boolean matchFound = false;
                String matchedGrade = "";

                for (Grade grade : grades) {
                    double gradeValue = grade.getGrade();
                    String letterGrade = getLetterGrade(gradeValue);

                    if (pattern.matches("[ABCDF][+-]?")) {
                        // Letter grade search
                        if (letterGrade.equals(pattern) ||
                                (pattern.length() == 1 && letterGrade.startsWith(pattern))) {
                            matchFound = true;
                            matchedGrade = letterGrade + " (" + gradeValue + "%)";
                            break;
                        }
                    } else if (pattern.contains(">")) {
                        // Greater than search
                        double threshold = Double.parseDouble(pattern.substring(1));
                        if (gradeValue > threshold) {
                            matchFound = true;
                            matchedGrade = gradeValue + "% > " + threshold + "%";
                            break;
                        }
                    } else if (pattern.contains("<")) {
                        // Less than search
                        double threshold = Double.parseDouble(pattern.substring(1));
                        if (gradeValue < threshold) {
                            matchFound = true;
                            matchedGrade = gradeValue + "% < " + threshold + "%";
                            break;
                        }
                    } else if (pattern.contains("-")) {
                        // Range search
                        String[] range = pattern.split("-");
                        double min = Double.parseDouble(range[0]);
                        double max = Double.parseDouble(range[1]);
                        if (gradeValue >= min && gradeValue <= max) {
                            matchFound = true;
                            matchedGrade = gradeValue + "% in range " + min + "-" + max + "%";
                            break;
                        }
                    }
                }

                if (matchFound) {
                    Map<String, String> match = new HashMap<>();
                    match.put("studentId", student.getStudentId());
                    match.put("name", student.getName());
                    match.put("email", student.getEmail());
                    match.put("type", student.getStudentType());
                    match.put("matchedGrade", matchedGrade);
                    match.put("pattern", pattern);
                    matches.add(match);
                }
            }

            result.put("matches", matches);
            result.put("totalScanned", totalScanned);
            result.put("matchesFound", matches.size());
            result.put("searchType", "Grade Pattern");
            result.put("pattern", pattern);

            successfulSearches++;

        } catch (Exception e) {
            result.put("error", "Invalid grade pattern: " + e.getMessage());
            result.put("example", "Valid patterns: >85, <60, 70-80, A, B+, C");
        }

        return result;
    }

    // Helper method to get field value
    private String getFieldValue(Student student, String field) {
        if (field == null) return null;

        switch (field.toLowerCase()) {
            case "id": return student.getStudentId();
            case "name": return student.getName();
            case "email": return student.getEmail();
            case "phone": return student.getPhone();
            case "type": return student.getStudentType();
            default: return null;
        }
    }

    // Helper method to get letter grade
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

    // Helper method to analyze email domains
    private Map<String, Integer> analyzeEmailDomains(List<Map<String, String>> matches) {
        Map<String, Integer> distribution = new HashMap<>();

        for (Map<String, String> match : matches) {
            String email = match.get("email");
            if (email.contains("@")) {
                String domain = email.substring(email.indexOf("@"));
                distribution.put(domain, distribution.getOrDefault(domain, 0) + 1);
            }
        }

        return distribution;
    }

    // Helper method to analyze regex complexity
    private String analyzeRegexComplexity(String regex) {
        if (regex.contains(".*") || regex.contains(".+")) {
            return "O(n) - Linear scan";
        } else if (regex.contains("|")) {
            return "O(n*m) - Multiple alternatives";
        } else if (regex.contains("(") && regex.contains(")")) {
            if (regex.contains("?:")) {
                return "O(n) - Non-capturing group";
            } else {
                return "O(n) - With capturing groups";
            }
        } else if (regex.contains("{") && regex.contains("}")) {
            return "O(n) - With quantifiers";
        } else {
            return "O(n) - Simple pattern";
        }
    }

    // Pattern compilation with caching
    private Pattern compilePattern(String key, String regex, boolean caseInsensitive) {
        return patternCache.computeIfAbsent(key, k -> {
            try {
                return caseInsensitive ?
                        Pattern.compile(regex, Pattern.CASE_INSENSITIVE) :
                        Pattern.compile(regex);
            } catch (Exception e) {
                System.out.println("[DEBUG] Failed to compile pattern: " + regex);
                System.out.println("[DEBUG] Error: " + e.getMessage());
                throw new IllegalArgumentException("Invalid regex: " + e.getMessage());
            }
        });
    }

    // Display search results
    public void displaySearchResults(Map<String, Object> searchResult) {
        if (searchResult.containsKey("error")) {
            System.out.println("\nERROR: " + searchResult.get("error"));
            if (searchResult.containsKey("example")) {
                System.out.println("Example: " + searchResult.get("example"));
            }
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> matches = (List<Map<String, String>>) searchResult.get("matches");
        int totalScanned = (int) searchResult.get("totalScanned");
        int matchesFound = (int) searchResult.get("matchesFound");
        String searchType = (String) searchResult.get("searchType");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("              SEARCH RESULTS - " + searchType);
        System.out.println("=".repeat(80));

        if (matches.isEmpty()) {
            System.out.println("\nNo matches found.");
            System.out.println("Total students scanned: " + totalScanned);
            return;
        }

        System.out.println("\nMATCH STATISTICS:");
        System.out.printf("Total Students Scanned: %d%n", totalScanned);
        System.out.printf("Matches Found: %d (%.1f%%)%n",
                matchesFound, (matchesFound * 100.0) / totalScanned);

        if (searchResult.containsKey("pattern")) {
            System.out.printf("Search Pattern: %s%n", searchResult.get("pattern"));
        }
        if (searchResult.containsKey("complexity")) {
            System.out.printf("Regex Complexity: %s%n", searchResult.get("complexity"));
        }

        System.out.println("\nMATCHED STUDENTS:");
        System.out.println("STU ID   | NAME           | TYPE    | MATCHED INFORMATION");
        System.out.println("----------------------------------------------------------------");

        for (Map<String, String> match : matches) {
            String studentId = match.get("studentId");
            String name = truncate(match.get("name"), 14);
            String type = match.getOrDefault("type", "N/A");

            // Display matched information based on search type
            String matchedInfo;
            if (match.containsKey("matchedText")) {
                matchedInfo = "Matched: '" + match.get("matchedText") + "'";
                if (match.containsKey("field")) {
                    matchedInfo += " in " + match.get("field");
                }
            } else if (match.containsKey("matchedGrade")) {
                matchedInfo = "Grade: " + match.get("matchedGrade");
            } else if (match.containsKey("email")) {
                matchedInfo = "Email: " + truncate(match.get("email"), 30);
            } else if (match.containsKey("phone")) {
                matchedInfo = "Phone: " + match.get("phone");
            } else {
                matchedInfo = "Matched";
            }

            System.out.printf("%-8s | %-14s | %-7s | %s%n",
                    studentId, name, type, matchedInfo);
        }

        // Display additional analysis
        if (searchResult.containsKey("domainDistribution")) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> domainDist = (Map<String, Integer>) searchResult.get("domainDistribution");
            if (!domainDist.isEmpty()) {
                System.out.println("\nEMAIL DOMAIN DISTRIBUTION:");
                domainDist.forEach((domain, count) -> {
                    double percentage = (count * 100.0) / matchesFound;
                    System.out.printf("  %-20s: %d students (%.1f%%)%n", domain, count, percentage);
                });
            }
        }
    }

    // Display pattern match details with highlighting
    public void displayPatternMatchDetails(Map<String, Object> searchResult, String field) {
        if (searchResult.containsKey("error") || !searchResult.containsKey("matches")) {
            displaySearchResults(searchResult);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> matches = (List<Map<String, String>>) searchResult.get("matches");
        String pattern = (String) searchResult.get("pattern");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("          PATTERN MATCH DETAILS WITH HIGHLIGHTING");
        System.out.println("=".repeat(80));
        System.out.println("Pattern: " + pattern);
        System.out.println("Field: " + field);
        System.out.println("Matches: " + matches.size());
        System.out.println();

        System.out.println("STU ID   | NAME           | ORIGINAL TEXT");
        System.out.println("         |                | MATCHED TEXT WITH HIGHLIGHT");
        System.out.println("----------------------------------------------------------------");

        Pattern compiledPattern;
        try {
            compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            compiledPattern = Pattern.compile(pattern);
        }

        for (Map<String, String> match : matches) {
            String studentId = match.get("studentId");
            String name = truncate(match.get("name"), 14);
            String originalText = match.get("value") != null ? match.get("value") :
                    match.get("email") != null ? match.get("email") :
                            match.get("phone") != null ? match.get("phone") : "";

            // Highlight the matched portion
            Matcher matcher = compiledPattern.matcher(originalText);
            String highlightedText = originalText;
            if (matcher.find()) {
                String matchedPart = matcher.group();
                highlightedText = originalText.replace(matchedPart,
                        "[" + matchedPart + "]");
            }

            System.out.printf("%-8s | %-14s | %s%n", studentId, name, originalText);
            System.out.printf("%-8s | %-14s | %s%n", "", "", highlightedText);
            System.out.println("         |                |");
        }
    }

    // Clear pattern cache
    public void clearPatternCache() {
        patternCache.clear();
        System.out.println("Pattern cache cleared.");
    }

    // Display search statistics
    public void displaySearchStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("              PATTERN SEARCH STATISTICS");
        System.out.println("=".repeat(80));

        System.out.printf("Total Searches: %d%n", totalSearches);
        System.out.printf("Successful Searches: %d (%.1f%%)%n",
                successfulSearches, totalSearches > 0 ? (successfulSearches * 100.0 / totalSearches) : 0);
        System.out.printf("Cached Patterns: %d%n", patternCache.size());

        if (!searchTypeCounts.isEmpty()) {
            System.out.println("\nSEARCH TYPE DISTRIBUTION:");
            searchTypeCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        double percentage = totalSearches > 0 ? (entry.getValue() * 100.0 / totalSearches) : 0;
                        System.out.printf("  %-20s: %d times (%.1f%%)%n",
                                entry.getKey(), entry.getValue(), percentage);
                    });
        }

        // Display cache contents
        if (!patternCache.isEmpty()) {
            System.out.println("\nCACHED PATTERNS:");
            patternCache.forEach((key, pattern) -> {
                String shortKey = key.length() > 30 ? key.substring(0, 27) + "..." : key;
                System.out.printf("  %-30s: %s%n", shortKey, pattern.pattern());
            });
        }
    }

    // Helper method to record search
    private void recordSearch(String searchType) {
        totalSearches++;
        searchTypeCounts.merge(searchType, 1, Integer::sum);
    }

    // Helper method to truncate text
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}