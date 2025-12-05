package test;

import models.GradeManager;
import models.StudentManager;
import org.junit.jupiter.api.Test;
import services.GPACalculator;

import static org.junit.jupiter.api.Assertions.*;

public class GPACalculatorTest {

    @Test
    public void testConvertToGPA() {
        GPACalculator calculator = new GPACalculator(new StudentManager(), new GradeManager());

        assertEquals(4.0, calculator.convertToGPA(95), 0.01);
        assertEquals(3.7, calculator.convertToGPA(91), 0.01);
        assertEquals(3.3, calculator.convertToGPA(88), 0.01);
        assertEquals(3.0, calculator.convertToGPA(85), 0.01);
        assertEquals(2.7, calculator.convertToGPA(81), 0.01);
        assertEquals(0.0, calculator.convertToGPA(55), 0.01);
    }

    @Test
    public void testGetLetterGrade() {
        GPACalculator calculator = new GPACalculator(new StudentManager(), new GradeManager());

        assertEquals("A", calculator.getLetterGrade(95));
        assertEquals("A-", calculator.getLetterGrade(91));
        assertEquals("B+", calculator.getLetterGrade(88));
        assertEquals("B", calculator.getLetterGrade(85));
        assertEquals("F", calculator.getLetterGrade(55));
    }
}