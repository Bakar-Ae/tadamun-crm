package com.crm.backend.report;

import com.crm.backend.report.export.ReportExportResult;
import com.crm.backend.report.export.ReportExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(
            ReportService reportService,
            ReportExportService reportExportService
    ) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportSummaryResponse> getSummaryReport() {
        return ResponseEntity.ok(reportService.getSummaryReport());
    }

    @GetMapping("/advanced")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<AdvancedReportResponse> getAdvancedReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return ResponseEntity.ok(
                reportService.getAdvancedReport(from, to)
        );
    }

    @GetMapping(
            value = "/advanced/export/excel",
            produces = EXCEL_CONTENT_TYPE
    )
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportAdvancedReportExcel(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return buildFileResponse(
                reportExportService.exportExcel(from, to)
        );
    }

    @GetMapping(
            value = "/advanced/export/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportAdvancedReportPdf(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return buildFileResponse(
                reportExportService.exportPdf(from, to)
        );
    }

    private static ResponseEntity<byte[]> buildFileResponse(
            ReportExportResult export
    ) {
        byte[] content = export.content();

        String contentDisposition = ContentDisposition
                .attachment()
                .filename(export.fileName())
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(export.contentType())
                )
                .contentLength(content.length)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition
                )
                .body(content);
    }
}