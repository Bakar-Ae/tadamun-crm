package com.crm.backend.report.export;

import com.crm.backend.report.AdvancedReportResponse;
import com.crm.backend.report.ReportService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ReportExportService {

    private final ReportService reportService;
    private final ReportExcelExporter excelExporter;
    private final ReportPdfExporter pdfExporter;

    public ReportExportService(
            ReportService reportService,
            ReportExcelExporter excelExporter,
            ReportPdfExporter pdfExporter
    ) {
        this.reportService = reportService;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
    }

    public ReportExportResult exportExcel(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        AdvancedReportResponse report =
                reportService.getAdvancedReport(from, to);

        return excelExporter.export(report);
    }

    public ReportExportResult exportPdf(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        AdvancedReportResponse report =
                reportService.getAdvancedReport(from, to);

        return pdfExporter.export(report);
    }
}