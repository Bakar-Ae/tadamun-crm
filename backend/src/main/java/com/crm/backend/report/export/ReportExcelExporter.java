package com.crm.backend.report.export;

import com.crm.backend.report.AdvancedReportResponse;
import com.crm.backend.report.ReportBreakdownItem;
import com.crm.backend.report.ReportDailyActivity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ReportExcelExporter {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM uuuu");

    private final ZoneId appTimeZone;

    public ReportExcelExporter(
            @Value("${app.time-zone:Africa/Mogadishu}") String appTimeZone
    ) {
        this.appTimeZone = ZoneId.of(appTimeZone);
    }

    public ReportExportResult export(AdvancedReportResponse report) {
        Objects.requireNonNull(report, "Report is required");

        LocalDate startDate = report.from()
                .atZoneSameInstant(appTimeZone)
                .toLocalDate();

        LocalDate endDate = report.to()
                .minusNanos(1)
                .atZoneSameInstant(appTimeZone)
                .toLocalDate();

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            Styles styles = createStyles(workbook);

            createOverviewSheet(
                    workbook,
                    report,
                    startDate,
                    endDate,
                    styles
            );

            createDailyActivitySheet(
                    workbook,
                    report.dailyActivity(),
                    styles
            );

            workbook.write(output);

            String fileName = "tadamun-report-"
                    + startDate
                    + "-to-"
                    + endDate
                    + ".xlsx";

            return new ReportExportResult(
                    fileName,
                    CONTENT_TYPE,
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not generate Excel report",
                    exception
            );
        }
    }

    private static void createOverviewSheet(
            Workbook workbook,
            AdvancedReportResponse report,
            LocalDate startDate,
            LocalDate endDate,
            Styles styles
    ) {
        Sheet sheet = workbook.createSheet("Report Overview");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Tadamun CRM Report");
        titleCell.setCellStyle(styles.title());

        Cell titleFillCell = titleRow.createCell(1);
        titleFillCell.setCellStyle(styles.title());

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        Row periodRow = sheet.createRow(1);
        periodRow.createCell(0).setCellValue("Period");
        periodRow.createCell(1).setCellValue(
                DISPLAY_DATE.format(startDate)
                        + " - "
                        + DISPLAY_DATE.format(endDate)
        );

        int rowNumber = 3;

        rowNumber = writeSectionTitle(
                sheet,
                rowNumber,
                "Summary",
                styles.section()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Customers created",
                report.customersCreated(), styles.count()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Leads created",
                report.leadsCreated(), styles.count()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Lead conversions",
                report.leadConversions(), styles.count()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Tasks created",
                report.tasksCreated(), styles.count()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Task completions",
                report.taskCompletions(), styles.count()
        );

        rowNumber = writeMetric(
                sheet, rowNumber, "Activities recorded",
                report.activitiesRecorded(), styles.count()
        );

        rowNumber++;

        rowNumber = writeBreakdown(
                sheet,
                rowNumber,
                "Lead Status",
                report.leadStatusBreakdown(),
                styles
        );

        rowNumber++;

        rowNumber = writeBreakdown(
                sheet,
                rowNumber,
                "Task Status",
                report.taskStatusBreakdown(),
                styles
        );

        rowNumber++;

        writeBreakdown(
                sheet,
                rowNumber,
                "Task Priority",
                report.taskPriorityBreakdown(),
                styles
        );

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.createFreezePane(0, 3);
    }

    private static void createDailyActivitySheet(
            Workbook workbook,
            List<ReportDailyActivity> dailyActivity,
            Styles styles
    ) {
        Sheet sheet = workbook.createSheet("Daily Activity");

        Row header = sheet.createRow(0);

        Cell dateHeader = header.createCell(0);
        dateHeader.setCellValue("Date");
        dateHeader.setCellStyle(styles.section());

        Cell countHeader = header.createCell(1);
        countHeader.setCellValue("Activity Count");
        countHeader.setCellStyle(styles.section());

        int rowNumber = 1;

        for (ReportDailyActivity activity : dailyActivity) {
            Row row = sheet.createRow(rowNumber++);

            row.createCell(0).setCellValue(activity.date().toString());

            Cell countCell = row.createCell(1);
            countCell.setCellValue(activity.count());
            countCell.setCellStyle(styles.count());
        }

        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.createFreezePane(0, 1);
    }

    private static int writeBreakdown(
            Sheet sheet,
            int rowNumber,
            String title,
            List<ReportBreakdownItem> items,
            Styles styles
    ) {
        rowNumber = writeSectionTitle(
                sheet,
                rowNumber,
                title,
                styles.section()
        );

        for (ReportBreakdownItem item : items) {
            rowNumber = writeMetric(
                    sheet,
                    rowNumber,
                    humanize(item.key()),
                    item.count(),
                    styles.count()
            );
        }

        return rowNumber;
    }

    private static int writeSectionTitle(
            Sheet sheet,
            int rowNumber,
            String title,
            CellStyle style
    ) {
        Row row = sheet.createRow(rowNumber);

        Cell firstCell = row.createCell(0);
        firstCell.setCellValue(title);
        firstCell.setCellStyle(style);

        Cell secondCell = row.createCell(1);
        secondCell.setCellStyle(style);

        return rowNumber + 1;
    }

    private static int writeMetric(
            Sheet sheet,
            int rowNumber,
            String label,
            long value,
            CellStyle countStyle
    ) {
        Row row = sheet.createRow(rowNumber);

        row.createCell(0).setCellValue(label);

        Cell countCell = row.createCell(1);
        countCell.setCellValue(value);
        countCell.setCellStyle(countStyle);

        return rowNumber + 1;
    }

    private static Styles createStyles(Workbook workbook) {
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);

        CellStyle sectionStyle = workbook.createCellStyle();
        sectionStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setColor(IndexedColors.WHITE.getIndex());
        sectionStyle.setFont(sectionFont);

        CellStyle countStyle = workbook.createCellStyle();
        countStyle.setDataFormat(
                workbook.createDataFormat().getFormat("#,##0")
        );

        return new Styles(
                titleStyle,
                sectionStyle,
                countStyle
        );
    }

    private static String humanize(String value) {
        String[] words = value
                .toLowerCase(Locale.ROOT)
                .split("_");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return result.toString();
    }

    private record Styles(
            CellStyle title,
            CellStyle section,
            CellStyle count
    ) {
    }
}