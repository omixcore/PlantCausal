package org.plantcausal.report;

/**
 * PDF report generation entry point.
 * Backend adapter can be connected to PDF libraries.
 */
public class PdfCausalReportWriter {
    public String createReportPath(String output) {
        return output;
    }
}
