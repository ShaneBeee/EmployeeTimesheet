package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.LogEntry;
import com.github.shanebeee.reconciled.view.TimePickerPanel;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class InvoiceGenerator {

    public enum InvoiceMode {
        STANDARD,        // single page, summary only
        WITH_BREAKDOWN   // page 1 = standard invoice, page 2 = work log detail
    }

    private static final DeviceRgb NAVY  = new DeviceRgb(30,  41,  59);
    private static final DeviceRgb BLUE  = new DeviceRgb(59,  130, 246);
    private static final DeviceRgb SLATE = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb AMBER = new DeviceRgb(245, 158, 11);

    // ── Public entry point ────────────────────────────────────────────────────

    public static void generateInvoice(Boss boss, EmployeeInfo employee, List<LogEntry> logs,
                                       String startDate, String endDate, int invoiceNum,
                                       String outputPath, InvoiceMode mode) throws Exception {
        PdfWriter   writer = new PdfWriter(outputPath);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf);
        doc.setMargins(50, 50, 50, 50);

        // Pre-compute totals (needed for both pages)
        Totals totals = computeTotals(boss, logs);

        // Page 1 — Invoice
        addInvoicePage(doc, boss, employee, logs, startDate, endDate, invoiceNum, totals);

        // Page 2 — Work Log Detail (only for WITH_BREAKDOWN)
        if (mode == InvoiceMode.WITH_BREAKDOWN) {
            doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            addBreakdownPage(doc, boss, employee, logs, startDate, endDate, invoiceNum, totals);
        }

        doc.close();
    }

    // ── Page 1: Invoice ───────────────────────────────────────────────────────

    private static void addInvoicePage(Document doc, Boss boss, EmployeeInfo employee,
                                       List<LogEntry> logs, String startDate, String endDate,
                                       int invoiceNum, Totals t) {
        // Header row
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

        Cell titleCell = new Cell()
            .add(new Paragraph("INVOICE").setFontSize(30).setBold().setFontColor(NAVY))
            .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(titleCell);

        Cell infoCell = new Cell()
            .add(new Paragraph("Invoice #: " + invoiceNum).setTextAlignment(TextAlignment.RIGHT).setFontColor(NAVY))
            .add(new Paragraph("Date Range: " + startDate + " to " + endDate).setTextAlignment(TextAlignment.RIGHT).setFontColor(SLATE).setFontSize(10))
            .setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(infoCell);
        doc.add(headerTable);

        // Divider
        doc.add(makeDivider(BLUE));

        // Addresses
        Table addrTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        addrTable.setWidth(UnitValue.createPercentValue(100)).setMarginTop(16).setMarginBottom(24);

        Cell fromCell = new Cell()
            .add(new Paragraph("FROM").setBold().setFontSize(9).setFontColor(SLATE))
            .add(new Paragraph(safe(employee.getFullName())).setBold().setFontColor(NAVY))
            .add(new Paragraph(safe(employee.getCompany())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(employee.getAddress())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(employee.getAddress2())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(employee.getEmail())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(employee.getPhoneNumber())).setFontColor(SLATE).setFontSize(10))
            .setBorder(Border.NO_BORDER);
        addrTable.addCell(fromCell);

        Cell toCell = new Cell()
            .add(new Paragraph("BILL TO").setBold().setFontSize(9).setFontColor(SLATE))
            .add(new Paragraph(safe(boss.getName())).setBold().setFontColor(NAVY))
            .add(new Paragraph(safe(boss.getCompany())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(boss.getAddress())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(boss.getAddress2())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(boss.getEmail())).setFontColor(SLATE).setFontSize(10))
            .add(new Paragraph(safe(boss.getPhoneNumber())).setFontColor(SLATE).setFontSize(10))
            .setBorder(Border.NO_BORDER);
        addrTable.addCell(toCell);
        doc.add(addrTable);

        // Line items table
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
        for (String h : new String[]{"Description", "Units", "Rate", "Total"}) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(NAVY).setPadding(6).setBorder(Border.NO_BORDER));
        }

        if (t.totalHours > 0) {
            String displayRate = String.format("$%.2f/hr", t.totalHoursCost / t.totalHours);
            addRow(table, false, "Total Work Hours",
                String.format("%.2f hrs", t.totalHours), displayRate,
                String.format("$%.2f", t.totalHoursCost));
        }
        if (t.totalKm > 0) {
            double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
            addRow(table, true, "Total Kilometers",
                String.format("%.2f km", t.totalKm),
                String.format("$%.2f/km", kmRate),
                String.format("$%.2f", t.kmCost));
        }
        // Extras as individual line items on standard invoice too
        boolean alt = (t.totalKm > 0);
        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.EXTRA
                && (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))) {
                double units = log.getUnits() != null ? log.getUnits() : 0;
                double cost  = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                addRow(table, alt, safe(log.getDescription()),
                    String.format("%.2f", units),
                    String.format("$%.2f", cost),
                    String.format("$%.2f", units * cost));
                alt = !alt;
            }
        }
        doc.add(table);

        // Totals block
        addTotalsBlock(doc, boss, t);
    }

    // ── Page 2: Work Log Detail ───────────────────────────────────────────────

    private static void addBreakdownPage(Document doc, Boss boss, EmployeeInfo employee,
                                         List<LogEntry> logs, String startDate, String endDate,
                                         int invoiceNum, Totals t) {
        // Banner — clearly NOT an invoice
        Table banner = new Table(UnitValue.createPercentArray(new float[]{1}));
        banner.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);
        banner.addCell(new Cell()
            .add(new Paragraph("WORK LOG DETAIL").setFontSize(20).setBold().setFontColor(ColorConstants.WHITE))
            .add(new Paragraph("Supplementary breakdown for Invoice #" + invoiceNum
                + "  ·  " + startDate + " to " + endDate)
                .setFontSize(10).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(SLATE).setPadding(14).setBorder(Border.NO_BORDER));
        doc.add(banner);

        // From / To compact row
        Table addrTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        addrTable.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);
        addrTable.addCell(new Cell()
            .add(new Paragraph(safe(employee.getFullName())).setBold().setFontSize(10).setFontColor(NAVY))
            .setBorder(Border.NO_BORDER));
        addrTable.addCell(new Cell()
            .add(new Paragraph(safe(boss.getName())).setBold().setFontSize(10)
                .setFontColor(NAVY).setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER));
        doc.add(addrTable);
        doc.add(makeDivider(SLATE));

        // Detail table
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100)).setMarginTop(12).setMarginBottom(16);
        for (String h : new String[]{"Date", "Description", "Units", "Rate", "Total"}) {
            table.addHeaderCell(new Cell()
                .add(new Paragraph(h).setBold().setFontColor(ColorConstants.WHITE).setFontSize(10))
                .setBackgroundColor(NAVY).setPadding(6).setBorder(Border.NO_BORDER));
        }

        DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MMM d");
        int rowIdx = 0;
        for (LogEntry log : logs.stream().sorted(Comparator.comparing(LogEntry::getDate)).toList()) {
            boolean alt = (rowIdx++ % 2 == 0);

            if (log.getType() == LogEntry.EntryType.TIME) {
                double perc = 0;
                if (log.getBossPercentages() != null) {
                    perc = log.getBossPercentages().getOrDefault(boss.getId(),
                        log.getBossPercentages().getOrDefault(boss.getName(), 0.0)) / 100.0;
                }
                if (perc <= 0) continue;

                LocalTime startT = TimePickerPanel.parseTime(log.getStartTime());
                LocalTime endT   = TimePickerPanel.parseTime(log.getEndTime());
                double billable  = Duration.between(startT, endT).toMinutes() / 60.0 * perc;
                double rate      = rateForDate(boss, log.getDate());

                String desc = TimePickerPanel.formatTime(log.getStartTime())
                    + " – " + TimePickerPanel.formatTime(log.getEndTime());
                if (perc < 1.0) desc += String.format(" (%.0f%%)", perc * 100);
                if (log.getDescription() != null && !log.getDescription().isBlank())
                    desc += "\n" + log.getDescription();

                String dateStr = LocalDate.parse(log.getDate()).format(displayFmt);
                addDetailRow(table, alt, dateStr, desc,
                    String.format("%.2f hrs", billable),
                    String.format("$%.2f/hr", rate),
                    String.format("$%.2f", billable * rate));

            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                if (!boss.getId().equals(log.getBossUuid()) && !boss.getName().equals(log.getBossUuid())) continue;
                double km    = log.getKilometers() != null ? log.getKilometers() : 0;
                double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                if (km <= 0) continue;
                String dateStr = LocalDate.parse(log.getDate()).format(displayFmt);
                String desc = "Kilometres";
                if (log.getDescription() != null && !log.getDescription().isBlank())
                    desc += " — " + log.getDescription();
                addDetailRow(table, alt, dateStr, desc,
                    String.format("%.2f km", km),
                    String.format("$%.2f/km", kmRate),
                    String.format("$%.2f", km * kmRate));

            } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                if (!boss.getId().equals(log.getBossUuid()) && !boss.getName().equals(log.getBossUuid())) continue;
                double units = log.getUnits() != null ? log.getUnits() : 0;
                double cost  = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                String dateStr = LocalDate.parse(log.getDate()).format(displayFmt);
                addDetailRow(table, alt, dateStr, safe(log.getDescription()),
                    String.format("%.2f", units),
                    String.format("$%.2f", cost),
                    String.format("$%.2f", units * cost));
            }

            // Legacy km entries
            if (log.getKmEntries() != null) {
                for (LogEntry.KmEntry ke : log.getKmEntries()) {
                    if (!boss.getId().equals(ke.getBossUuid()) && !boss.getName().equals(ke.getBossUuid())) continue;
                    double km    = ke.getKilometers();
                    double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                    if (km <= 0) continue;
                    boolean a = (rowIdx++ % 2 == 0);
                    String dateStr = LocalDate.parse(log.getDate()).format(displayFmt);
                    addDetailRow(table, a, dateStr, "Kilometres",
                        String.format("%.2f km", km),
                        String.format("$%.2f/km", kmRate),
                        String.format("$%.2f", km * kmRate));
                }
            }
        }
        doc.add(table);

        // Summary totals (same as page 1, for reference)
        doc.add(new Paragraph("Summary").setFontSize(11).setBold().setFontColor(NAVY).setMarginBottom(4));
        doc.add(makeDivider(new DeviceRgb(226, 232, 240)));
        addTotalsBlock(doc, boss, t);

        // Footer note
        doc.add(new Paragraph("This page is a supplementary work log detail. It is not a separate invoice.")
            .setFontSize(9).setFontColor(SLATE).setItalic()
            .setMarginTop(20).setTextAlignment(TextAlignment.CENTER));
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private static void addTotalsBlock(Document doc, Boss boss, Totals t) {
        Table totals = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        totals.setWidth(UnitValue.createPercentValue(38))
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setMarginTop(8);

        addTotalRow(totals, "Subtotal", String.format("$%.2f", t.subtotal), false);
        addTotalRow(totals, String.format("GST/HST (%.0f%%)", boss.getTaxRate()),
            String.format("$%.2f", t.tax), false);
        addTotalRow(totals, "TOTAL DUE", String.format("$%.2f", t.total), true);
        doc.add(totals);
    }

    private static void addRow(Table t, boolean alt, String desc, String units, String rate, String total) {
        DeviceRgb bg = alt ? LIGHT : new DeviceRgb(255, 255, 255);
        t.addCell(cell(desc,  bg, TextAlignment.LEFT));
        t.addCell(cell(units, bg, TextAlignment.RIGHT));
        t.addCell(cell(rate,  bg, TextAlignment.RIGHT));
        t.addCell(cell(total, bg, TextAlignment.RIGHT));
    }

    private static void addDetailRow(Table t, boolean alt, String date, String desc,
                                     String units, String rate, String total) {
        DeviceRgb bg = alt ? LIGHT : new DeviceRgb(255, 255, 255);
        t.addCell(cell(date,  bg, TextAlignment.LEFT));
        t.addCell(cell(desc,  bg, TextAlignment.LEFT));
        t.addCell(cell(units, bg, TextAlignment.RIGHT));
        t.addCell(cell(rate,  bg, TextAlignment.RIGHT));
        t.addCell(cell(total, bg, TextAlignment.RIGHT));
    }

    private static Cell cell(String text, DeviceRgb bg, TextAlignment align) {
        return new Cell()
            .add(new Paragraph(text).setFontSize(9).setFontColor(NAVY).setTextAlignment(align))
            .setBackgroundColor(bg).setPadding(5).setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(new DeviceRgb(226, 232, 240), 0.5f));
    }

    private static void addTotalRow(Table t, String label, String value, boolean bold) {
        int size = bold ? 13 : 10;
        DeviceRgb color = bold ? BLUE : NAVY;
        Paragraph labelP = new Paragraph(label).setFontSize(size).setFontColor(NAVY);
        Paragraph valueP = new Paragraph(value).setFontSize(size).setFontColor(color).setTextAlignment(TextAlignment.RIGHT);
        if (bold) { labelP.setBold(); valueP.setBold(); }
        t.addCell(new Cell().add(labelP).setBorder(Border.NO_BORDER).setPadding(4));
        t.addCell(new Cell().add(valueP).setBorder(Border.NO_BORDER).setPadding(4));
    }

    private static Table makeDivider(DeviceRgb color) {
        Table div = new Table(UnitValue.createPercentArray(new float[]{1}));
        div.setWidth(UnitValue.createPercentValue(100));
        div.addCell(new Cell().setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(color, 1.5f))
            .add(new Paragraph("")));
        return div;
    }

    // ── Totals computation ────────────────────────────────────────────────────

    public record Totals(double totalHours, double totalHoursCost, double totalKm,
                  double kmCost, double extrasCost, double subtotal, double tax, double total) {}

    public static Totals computeTotals(Boss boss, List<LogEntry> logs) {
        double hours = 0, hoursCost = 0, km = 0, extras = 0;
        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.TIME) {
                double perc = 0;
                if (log.getBossPercentages() != null) {
                    perc = log.getBossPercentages().getOrDefault(boss.getId(),
                        log.getBossPercentages().getOrDefault(boss.getName(), 0.0)) / 100.0;
                }
                if (perc > 0 && log.getStartTime() != null && log.getEndTime() != null) {
                    double h = Duration.between(
                        TimePickerPanel.parseTime(log.getStartTime()),
                        TimePickerPanel.parseTime(log.getEndTime())).toMinutes() / 60.0 * perc;
                    double rate = rateForDate(boss, log.getDate());
                    hours     += h;
                    hoursCost += h * rate;
                }
            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                    km += log.getKilometers() != null ? log.getKilometers() : 0;
            } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                    extras += (log.getUnits() != null ? log.getUnits() : 0)
                        * (log.getCostPerUnit() != null ? log.getCostPerUnit() : 0);
            }
            if (log.getKmEntries() != null) {
                for (LogEntry.KmEntry ke : log.getKmEntries()) {
                    if (boss.getId().equals(ke.getBossUuid()) || boss.getName().equals(ke.getBossUuid()))
                        km += ke.getKilometers();
                }
            }
        }
        double kmCost   = km * (boss.getKmRate() != null ? boss.getKmRate() : 0);
        double subtotal = hoursCost + kmCost + extras;
        double tax      = subtotal * (boss.getTaxRate() / 100.0);
        return new Totals(hours, hoursCost, km, kmCost, extras, subtotal, tax, subtotal + tax);
    }

    private static double rateForDate(Boss boss, String logDate) {
        if (boss.getRateHistory() == null || boss.getRateHistory().isEmpty())
            return boss.getHourlyRate();
        LocalDate date = LocalDate.parse(logDate);
        return boss.getRateHistory().stream()
            .filter(rc -> LocalDate.parse(rc.date()).isAfter(date))
            .min(Comparator.comparing(rc -> LocalDate.parse(rc.date())))
            .map(Boss.RateChange::rate)
            .orElse(boss.getHourlyRate());
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
