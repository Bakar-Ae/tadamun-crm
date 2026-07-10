package com.crm.backend.report;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/reports")

public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
        return ResponseEntity.ok(reportService.getAdvancedReport(from, to));
    }
}
