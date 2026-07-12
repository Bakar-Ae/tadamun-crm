package com.crm.backend.report.export;

import com.crm.backend.report.AdvancedReportResponse;
import com.crm.backend.report.ReportBreakdownItem;
import com.crm.backend.report.ReportDailyActivity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExporterTest {

    @Test
    void excelExporterShouldCreateValidWorkbook() throws IOException {
        ReportExcelExporter exporter =
                new ReportExcelExporter("Africa/Mogadishu");

        ReportExportResult result = exporter.export(sampleReport());

        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                result.contentType()
        );

        assertTrue(result.fileName().endsWith(".xlsx"));
        assertTrue(result.content().length > 1000);

        try (
                XSSFWorkbook workbook = new XSSFWorkbook(
                        new ByteArrayInputStream(result.content())
                )
        ) {
            assertNotNull(workbook.getSheet("Report Overview"));
            assertNotNull(workbook.getSheet("Daily Activity"));

            assertEquals(
                    "Tadamun CRM Report",
                    workbook.getSheet("Report Overview")
                            .getRow(0)
                            .getCell(0)
                            .getStringCellValue()
            );
        }
    }

    @Test
    void pdfExporterShouldCreateReadableMultiPageDocument()
            throws IOException {
        ReportPdfExporter exporter =
                new ReportPdfExporter("Africa/Mogadishu");

        ReportExportResult result = exporter.export(sampleReport());

        assertEquals("application/pdf", result.contentType());
        assertTrue(result.fileName().endsWith(".pdf"));
        assertTrue(result.content().length > 1000);

        try (PDDocument document = Loader.loadPDF(result.content())) {
            assertTrue(document.getNumberOfPages() >= 2);

            String text = new PDFTextStripper().getText(document);

            assertTrue(text.contains("Tadamun CRM Report"));
            assertTrue(text.contains("Customers created"));
            assertTrue(text.contains("Daily Activity"));
        }
    }

    private static AdvancedReportResponse sampleReport() {
        return new AdvancedReportResponse(
                OffsetDateTime.parse("2026-06-30T21:00:00Z"),
                OffsetDateTime.parse("2026-07-03T21:00:00Z"),
                5,
                4,
                2,
                6,
                3,
                12,
                7,
                List.of(
                        new ReportBreakdownItem("NEW", 2),
                        new ReportBreakdownItem("CONVERTED", 2)
                ),
                List.of(
                        new ReportBreakdownItem("OPEN", 3),
                        new ReportBreakdownItem("COMPLETED", 3)
                ),
                List.of(
                        new ReportBreakdownItem("MEDIUM", 4),
                        new ReportBreakdownItem("HIGH", 2)
                ),
                List.of(
                        new ReportDailyActivity(
                                LocalDate.of(2026, 7, 1), 3
                        ),
                        new ReportDailyActivity(
                                LocalDate.of(2026, 7, 2), 4
                        ),
                        new ReportDailyActivity(
                                LocalDate.of(2026, 7, 3), 5
                        )
                )
        );
    }
}