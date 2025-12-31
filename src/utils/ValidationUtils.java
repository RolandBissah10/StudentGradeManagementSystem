package utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidationUtils {
    // Compile patterns once for performance (as per PDF requirement)
    public static final Pattern STUDENT_ID = Pattern.compile("^STU\\d{3}$");
    public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final Pattern PHONE = Pattern.compile(
            "^(\\(\\d{3}\\)\\s\\d{3}-\\d{4}|" +      // (123) 456-7890
                    "\\d{3}-\\d{3}-\\d{4}|" +                // 123-456-7890
                    "\\+\\d{1,3}-\\d{3}-\\d{3}-\\d{4}|" +    // +1-123-456-7890
                    "\\d{10})$"                              // 1234567890
    );
    public static final Pattern NAME = Pattern.compile("^[A-Za-z]+(['\\-\\s][A-Za-z]+)*$");
    public static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    public static final Pattern COURSE_CODE = Pattern.compile("^[A-Z]{3}\\d{3}$");
    public static final Pattern GRADE = Pattern.compile("^(100|[1-9]?\\d)$");
    public static final Pattern EMAIL_DOMAIN = Pattern.compile("@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    // For advanced pattern searches
    public static final Pattern WILDCARD_TO_REGEX = Pattern.compile("\\*|\\?");

    // Validation methods with detailed error messages
    public static ValidationResult validateStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return ValidationResult.failure("Student ID cannot be empty");
        }
        boolean valid = STUDENT_ID.matcher(studentId).matches();
        return valid ?
                ValidationResult.success() :
                ValidationResult.failure("Invalid Student ID format. Expected: STU### (STU followed by exactly 3 digits)",
                        "STU001, STU042, STU999");
    }

    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.failure("Email cannot be empty");
        }
        boolean valid = EMAIL.matcher(email).matches();
        return valid ?
                ValidationResult.success() :
                ValidationResult.failure("Invalid email format. Expected: username@domain.extension",
                        "john.smith@university.edu, jsmith@college.org");
    }

    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.failure("Phone number cannot be empty");
        }
        boolean valid = PHONE.matcher(phone).matches();
        return valid ?
                ValidationResult.success() :
                ValidationResult.failure("Invalid phone format. Accepted formats:\n" +
                                "- (123) 456-7890\n" +
                                "- 123-456-7890\n" +
                                "- +1-123-456-7890\n" +
                                "- 1234567890",
                        "(555) 123-4567, 555-123-4567, +1-555-123-4567");
    }

    public static ValidationResult validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.failure("Name cannot be empty");
        }
        boolean valid = NAME.matcher(name).matches();
        return valid ?
                ValidationResult.success() :
                ValidationResult.failure("Invalid name format. Only letters, spaces, hyphens, and apostrophes allowed",
                        "John Smith, Mary-Jane O'Connor, Jean-Luc");
    }

    public static ValidationResult validateDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return ValidationResult.failure("Date cannot be empty");
        }

        if (!DATE.matcher(date).matches()) {
            return ValidationResult.failure("Invalid date format. Expected: YYYY-MM-DD",
                    "2024-11-03, 2025-01-15");
        }

        try {
            LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate currentDate = LocalDate.now();

            if (parsedDate.isAfter(currentDate)) {
                return ValidationResult.failure("Date cannot be in the future");
            }

            if (parsedDate.isBefore(LocalDate.of(2000, 1, 1))) {
                return ValidationResult.failure("Date must be after 2000-01-01");
            }

            return ValidationResult.success();
        } catch (DateTimeParseException e) {
            return ValidationResult.failure("Invalid date. Please check the format: YYYY-MM-DD");
        }
    }

    public static ValidationResult validateCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            return ValidationResult.failure("Course code cannot be empty");
        }
        boolean valid = COURSE_CODE.matcher(courseCode).matches();
        return valid ?
                ValidationResult.success() :
                ValidationResult.failure("Invalid course code format. Expected: ABC123 (3 letters followed by 3 digits)",
                        "MAT101, ENG203, PHY301");
    }

    public static ValidationResult validateGrade(String gradeStr) {
        if (gradeStr == null || gradeStr.trim().isEmpty()) {
            return ValidationResult.failure("Grade cannot be empty");
        }

        if (!GRADE.matcher(gradeStr).matches()) {
            return ValidationResult.failure("Invalid grade. Must be between 0 and 100 (whole numbers only)",
                    "85, 92, 100");
        }

        int grade = Integer.parseInt(gradeStr);
        if (grade < 0 || grade > 100) {
            return ValidationResult.failure("Grade must be between 0 and 100");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateGrade(double grade) {
        if (grade < 0 || grade > 100) {
            return ValidationResult.failure("Grade must be between 0 and 100");
        }
        return ValidationResult.success();
    }

    // Advanced validation methods
    public static String extractDomainFromEmail(String email) {
        Matcher matcher = EMAIL_DOMAIN.matcher(email);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String convertWildcardToRegex(String pattern) {
        // Convert * to .* and ? to .
        return pattern.replace("*", ".*").replace("?", ".");
    }

    public static ValidationResult validateRegexPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.failure("Invalid regex pattern: " + e.getMessage());
        }
    }

    // Helper class for detailed validation results
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String examples;

        private ValidationResult(boolean valid, String errorMessage, String examples) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.examples = examples;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public static ValidationResult failure(String errorMessage, String examples) {
            return new ValidationResult(false, errorMessage, examples);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getExamples() {
            return examples;
        }

        public void displayError() {
            if (!valid) {
                System.out.println("\n✗ VALIDATION ERROR: " + errorMessage);
                if (examples != null) {
                    System.out.println("Examples: " + examples);
                }
            }
        }
    }
}