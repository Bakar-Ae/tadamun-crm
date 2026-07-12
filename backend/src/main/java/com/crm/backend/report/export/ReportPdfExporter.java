package com.crm.backend.report.export;

import com.crm.backend.report.AdvancedReportResponse;
import com.crm.backend.report.ReportBreakdownItem;
import com.crm.backend.report.ReportDailyActivity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ReportPdfExporter {

    private static final String CONTENT_TYPE = "application/pdf";
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 48;
    private static final float CONTENT_WIDTH =
            PAGE_SIZE.getWidth() - (MARGIN * 2);

    private static final int ACTIVITY_ROWS_PER_PAGE = 34;

    private static final PDFont REGULAR_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDFont BOLD_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final Color BRAND_COLOR =
            new Color(31, 78, 121);

    private static final Color TEXT_COLOR =
            new Color(31, 41, 55);

    private static final Color MUTED_COLOR =
            new Color(100, 116, 139);

    private static final Color BORDER_COLOR =
            new Color(203, 213, 225);

    private static final Color WHITE = Color.WHITE;

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM uuuu");

    private final ZoneId appTimeZone;

    public ReportPdfExporter(
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

        int activityPageCount = Math.max(
                1,
                (report.dailyActivity().size()
                        + ACTIVITY_ROWS_PER_PAGE - 1)
                        / ACTIVITY_ROWS_PER_PAGE
        );

        int totalPages = 1 + activityPageCount;

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            writeSummaryPage(
                    document,
                    report,
                    startDate,
                    endDate,
                    totalPages
            );

            writeDailyActivityPages(
                    document,
                    report.dailyActivity(),
                    startDate,
                    endDate,
                    totalPages
            );

            document.save(output);

            String fileName = "tadamun-report-"
                    + startDate
                    + "-to-"
                    + endDate
                    + ".pdf";

            return new ReportExportResult(
                    fileName,
                    CONTENT_TYPE,
                    output.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not generate PDF report",
                    exception
            );
        }
    }

    private static void writeSummaryPage(
            PDDocument document,
            AdvancedReportResponse report,
            LocalDate startDate,
            LocalDate endDate,
            int totalPages
    ) throws IOException {
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);

        try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {

            float y = PAGE_SIZE.getHeight() - MARGIN;

            drawText(
                    content,
                    "Tadamun CRM Report",
                    MARGIN,
                    y,
                    BOLD_FONT,
                    20,
                    BRAND_COLOR
            );

            y -= 27;

            drawText(
                    content,
                    "Period: "
                            + DISPLAY_DATE.format(startDate)
                            + " to "
                            + DISPLAY_DATE.format(endDate),
                    MARGIN,
                    y,
                    REGULAR_FONT,
                    10,
                    MUTED_COLOR
            );

            y -= 30;

            y = drawSection(content, y, "Summary");

            y = drawRow(content, y, "Customers created",
                    report.customersCreated());

            y = drawRow(content, y, "Leads created",
                    report.leadsCreated());

            y = drawRow(content, y, "Lead conversions",
                    report.leadConversions());

            y = drawRow(content, y, "Tasks created",
                    report.tasksCreated());

            y = drawRow(content, y, "Task completions",
                    report.taskCompletions());

            y = drawRow(content, y, "Activities recorded",
                    report.activitiesRecorded());

            y = drawRow(content, y, "Customer activities",
                    report.customerActivities());

            y -= 10;

            y = drawBreakdown(
                    content,
                    y,
                    "Lead Status",
                    report.leadStatusBreakdown()
            );

            y -= 10;

            y = drawBreakdown(
                    content,
                    y,
                    "Task Status",
                    report.taskStatusBreakdown()
            );

            y -= 10;

            drawBreakdown(
                    content,
                    y,
                    "Task Priority",
                    report.taskPriorityBreakdown()
            );

            drawFooter(content, 1, totalPages);
        }
    }

    private static void writeDailyActivityPages(
            PDDocument document,
            List<ReportDailyActivity> activities,
            LocalDate startDate,
            LocalDate endDate,
            int totalPages
    ) throws IOException {
        int pageCount = Math.max(
                1,
                (activities.size() + ACTIVITY_ROWS_PER_PAGE - 1)
                        / ACTIVITY_ROWS_PER_PAGE
        );

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int fromIndex = Math.min(
                    pageIndex * ACTIVITY_ROWS_PER_PAGE,
                    activities.size()
            );

            int toIndex = Math.min(
                    fromIndex + ACTIVITY_ROWS_PER_PAGE,
                    activities.size()
            );

            writeDailyActivityPage(
                    document,
                    activities.subList(fromIndex, toIndex),
                    startDate,
                    endDate,
                    pageIndex + 2,
                    totalPages
            );
        }
    }

    private static void writeDailyActivityPage(
            PDDocument document,
            List<ReportDailyActivity> activities,
            LocalDate startDate,
            LocalDate endDate,
            int pageNumber,
            int totalPages
    ) throws IOException {
        PDPage page = new PDPage(PAGE_SIZE);
        document.addPage(page);

        try (PDPageContentStream content =
                     new PDPageContentStream(document, page)) {

            float y = PAGE_SIZE.getHeight() - MARGIN;

            drawText(
                    content,
                    "Daily Activity",
                    MARGIN,
                    y,
                    BOLD_FONT,
                    18,
                    BRAND_COLOR
            );

            y -= 25;

            drawText(
                    content,
                    DISPLAY_DATE.format(startDate)
                            + " to "
                            + DISPLAY_DATE.format(endDate),
                    MARGIN,
                    y,
                    REGULAR_FONT,
                    10,
                    MUTED_COLOR
            );

            y -= 30;

            content.setNonStrokingColor(BRAND_COLOR);
            content.addRect(MARGIN, y - 17, CONTENT_WIDTH, 22);
            content.fill();

            drawText(
                    content,
                    "Date",
                    MARGIN + 8,
                    y - 11,
                    BOLD_FONT,
                    10,
                    WHITE
            );

            drawText(
                    content,
                    "Activity Count",
                    PAGE_SIZE.getWidth() - MARGIN - 100,
                    y - 11,
                    BOLD_FONT,
                    10,
                    WHITE
            );

            y -= 30;

            if (activities.isEmpty()) {
                drawText(
                        content,
                        "No activity was recorded for this period.",
                        MARGIN,
                        y,
                        REGULAR_FONT,
                        10,
                        MUTED_COLOR
                );
            } else {
                for (ReportDailyActivity activity : activities) {
                    y = drawActivityRow(content, y, activity);
                }
            }

            drawFooter(content, pageNumber, totalPages);
        }
    }

    private static float drawBreakdown(
            PDPageContentStream content,
            float y,
            String title,
            List<ReportBreakdownItem> items
    ) throws IOException {
        y = drawSection(content, y, title);

        for (ReportBreakdownItem item : items) {
            y = drawRow(
                    content,
                    y,
                    humanize(item.key()),
                    item.count()
            );
        }

        return y;
    }

    private static float drawSection(
            PDPageContentStream content,
            float y,
            String title
    ) throws IOException {
        content.setNonStrokingColor(BRAND_COLOR);
        content.addRect(MARGIN, y - 17, CONTENT_WIDTH, 22);
        content.fill();

        drawText(
                content,
                title,
                MARGIN + 8,
                y - 11,
                BOLD_FONT,
                10,
                WHITE
        );

        return y - 30;
    }

    private static float drawRow(
            PDPageContentStream content,
            float y,
            String label,
            long value
    ) throws IOException {
        drawText(
                content,
                label,
                MARGIN,
                y,
                REGULAR_FONT,
                10,
                TEXT_COLOR
        );

        String valueText = Long.toString(value);

        float valueX = PAGE_SIZE.getWidth()
                - MARGIN
                - textWidth(BOLD_FONT, valueText, 10);

        drawText(
                content,
                valueText,
                valueX,
                y,
                BOLD_FONT,
                10,
                TEXT_COLOR
        );

        drawLine(content, y - 6);

        return y - 18;
    }

    private static float drawActivityRow(
            PDPageContentStream content,
            float y,
            ReportDailyActivity activity
    ) throws IOException {
        drawText(
                content,
                DISPLAY_DATE.format(activity.date()),
                MARGIN,
                y,
                REGULAR_FONT,
                10,
                TEXT_COLOR
        );

        String count = Long.toString(activity.count());

        float countX = PAGE_SIZE.getWidth()
                - MARGIN
                - textWidth(BOLD_FONT, count, 10);

        drawText(
                content,
                count,
                countX,
                y,
                BOLD_FONT,
                10,
                TEXT_COLOR
        );

        drawLine(content, y - 6);

        return y - 18;
    }

    private static void drawLine(
            PDPageContentStream content,
            float y
    ) throws IOException {
        content.setStrokingColor(BORDER_COLOR);
        content.setLineWidth(0.5f);
        content.moveTo(MARGIN, y);
        content.lineTo(PAGE_SIZE.getWidth() - MARGIN, y);
        content.stroke();
    }

    private static void drawFooter(
            PDPageContentStream content,
            int pageNumber,
            int totalPages
    ) throws IOException {
        String footer = "Page " + pageNumber + " of " + totalPages;

        float x = (
                PAGE_SIZE.getWidth()
                        - textWidth(REGULAR_FONT, footer, 9)
        ) / 2;

        drawText(
                content,
                footer,
                x,
                25,
                REGULAR_FONT,
                9,
                MUTED_COLOR
        );
    }

    private static void drawText(
            PDPageContentStream content,
            String text,
            float x,
            float y,
            PDFont font,
            float fontSize,
            Color color
    ) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(safeText(text));
        content.endText();
    }

    private static float textWidth(
            PDFont font,
            String text,
            float fontSize
    ) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }

    private static String safeText(String value) {
        return value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
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
}