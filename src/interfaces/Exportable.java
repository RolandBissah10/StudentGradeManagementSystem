package interfaces;

import exceptions.ExportException;
import models.Student;

import java.util.List;

public interface Exportable {
    void exportSummaryReport(String studentId, String filename) throws ExportException;
    void exportDetailedReport(String studentId, String filename) throws ExportException;
    void exportSearchResults(List<Student> students, String filename) throws ExportException;
}