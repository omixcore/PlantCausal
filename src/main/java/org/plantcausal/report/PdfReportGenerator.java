package org.plantcausal.report;

/**
 * PDF report generation pipeline entry point.
 * Backend implementation can be connected to PDFBox/OpenPDF.
 */
public class PdfReportGenerator {

    public String generateReportPath(String reportName) {
        return reportName + ".pdf";
    }
}
