package com.github.shanebeee.et.util;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.LogEntry;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public class InvoiceGenerator {

    public static void generateInvoice(Boss boss, EmployeeInfo employee, List<LogEntry> logs, String startDate, String endDate, int invoiceNum, String outputPath, boolean itemized) throws Exception {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header Table
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setMarginBottom(20);

        Cell titleCell = new Cell().add(new Paragraph(itemized ? "ITEMIZED INVOICE" : "INVOICE")
                .setFontSize(30)
                .setBold()
                .setFontColor(ColorConstants.DARK_GRAY))
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(titleCell);

        Cell infoCell = new Cell().add(new Paragraph("Invoice #: " + invoiceNum)
                .setTextAlignment(TextAlignment.RIGHT))
            .add(new Paragraph("Date Range: " + startDate + " to " + endDate)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(infoCell);
        document.add(headerTable);

        // Addresses Table
        Table addrTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        addrTable.setWidth(UnitValue.createPercentValue(100));
        addrTable.setMarginBottom(30);

        Cell fromCell = new Cell().add(new Paragraph("FROM:").setBold().setFontColor(ColorConstants.GRAY))
            .add(new Paragraph(safe(employee.getFullName())).setBold())
            .add(new Paragraph(safe(employee.getCompany())))
            .add(new Paragraph(safe(employee.getAddress())))
            .add(new Paragraph(safe(employee.getAddress2())))
            .add(new Paragraph(safe(employee.getEmail())))
            .add(new Paragraph(safe(employee.getPhoneNumber())))
            .setBorder(Border.NO_BORDER);
        addrTable.addCell(fromCell);

        Cell toCell = new Cell().add(new Paragraph("TO:").setBold().setFontColor(ColorConstants.GRAY))
            .add(new Paragraph(safe(boss.getName())).setBold())
            .add(new Paragraph(safe(boss.getCompany())))
            .add(new Paragraph(safe(boss.getAddress())))
            .add(new Paragraph(safe(boss.getAddress2())))
            .add(new Paragraph(safe(boss.getEmail())))
            .add(new Paragraph(safe(boss.getPhoneNumber())))
            .setBorder(Border.NO_BORDER);
        addrTable.addCell(toCell);
        document.add(addrTable);

        Table table = new Table(itemized
            ? UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1})
            : UnitValue.createPercentArray(new float[]{4, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Styled Table Header
        String[] headers = itemized
            ? new String[]{"Date", "Description", "Units", "Rate", "Total"}
            : new String[]{"Description", "Units", "Rate", "Total"};
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(5));
        }

        double totalHours = 0;
        double totalKm = 0;
        double extraEntriesTotal = 0;

        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.TIME) {
                double perc = log.getBossPercentages().getOrDefault(boss.getId(), 0.0) / 100.0;
                if (perc <= 0) {
                    // Fallback for old logs using names
                    perc = log.getBossPercentages().getOrDefault(boss.getName(), 0.0) / 100.0;
                }
                if (perc > 0) {
                    LocalTime startT = LocalTime.parse(log.getStartTime());
                    LocalTime endT = LocalTime.parse(log.getEndTime());
                    double hours = Duration.between(startT, endT).toMinutes() / 60.0;
                    double billableHours = hours * perc;
                    totalHours += billableHours;

                    if (itemized) {
                        table.addCell(new Cell().add(new Paragraph(log.getDate())).setPadding(5));
                        String desc = "Time: " + com.github.shanebeee.et.view.TimePickerPanel.formatTime(log.getStartTime()) + " - " + com.github.shanebeee.et.view.TimePickerPanel.formatTime(log.getEndTime());
                        if (perc < 1.0) desc += String.format(" (%.0f%%)", perc * 100);
                        table.addCell(new Cell().add(new Paragraph(desc)).setPadding(5));
                        table.addCell(new Cell().add(new Paragraph(String.format("%.2f", billableHours))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f/hr", boss.getHourlyRate()))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", billableHours * boss.getHourlyRate()))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    }
                }
            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid())) {
                    double km = (log.getKilometers() != null ? log.getKilometers() : 0);
                    totalKm += km;

                    if (itemized && km > 0) {
                        double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                        table.addCell(new Cell().add(new Paragraph(log.getDate())).setPadding(5));
                        table.addCell(new Cell().add(new Paragraph("Kilometers")).setPadding(5));
                        table.addCell(new Cell().add(new Paragraph(String.format("%.2f", km))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f/km", kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", km * kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    }
                }
            } else if (log.getType() == LogEntry.EntryType.EXTRA && (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))) {
                double units = log.getUnits() != null ? log.getUnits() : 0;
                double cost = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                double lineTotal = units * cost;
                extraEntriesTotal += lineTotal;

                if (itemized) {
                    table.addCell(new Cell().add(new Paragraph(log.getDate())).setPadding(5));
                    table.addCell(new Cell().add(new Paragraph(log.getDescription())).setPadding(5));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", units))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", cost))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", lineTotal))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                }
            }

            // Old kmEntries support
            if (log.getKmEntries() != null) {
                for (LogEntry.KmEntry ke : log.getKmEntries()) {
                    if (boss.getId().equals(ke.getBossUuid()) || boss.getName().equals(ke.getBossUuid())) {
                        double km = ke.getKilometers();
                        totalKm += km;
                        if (itemized && km > 0) {
                            double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                            table.addCell(new Cell().add(new Paragraph(log.getDate())).setPadding(5));
                            table.addCell(new Cell().add(new Paragraph("Kilometers")).setPadding(5));
                            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", km))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                            table.addCell(new Cell().add(new Paragraph(String.format("$%.2f/km", kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                            table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", km * kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        }
                    }
                }
            }
        }

        if (!itemized) {
            if (totalHours > 0) {
                table.addCell(new Cell().add(new Paragraph("Total Work Hours")).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalHours))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f/hr", boss.getHourlyRate()))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", totalHours * boss.getHourlyRate()))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
            }

            if (totalKm > 0) {
                double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                table.addCell(new Cell().add(new Paragraph("Total Kilometers")).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalKm))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f/km", kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", totalKm * kmRate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
            }

            for (LogEntry log : logs) {
                if (log.getType() == LogEntry.EntryType.EXTRA && (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))) {
                    double units = log.getUnits() != null ? log.getUnits() : 0;
                    double cost = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                    double lineTotal = units * cost;
                    table.addCell(new Cell().add(new Paragraph(log.getDescription())).setPadding(5));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.2f", units))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", cost))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", lineTotal))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                }
            }
        }

        document.add(table);

        double hoursCost = totalHours * boss.getHourlyRate();
        double kmCost = totalKm * (boss.getKmRate() != null ? boss.getKmRate() : 0);
        double subtotal = hoursCost + kmCost + extraEntriesTotal;
        double tax = subtotal * (boss.getTaxRate() / 100.0);
        double total = subtotal + tax;

        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        totalsTable.setWidth(UnitValue.createPercentValue(40));
        totalsTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);

        totalsTable.addCell(new Cell().add(new Paragraph("Subtotal")).setBorder(Border.NO_BORDER).setPadding(2));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", subtotal))).setBorder(Border.NO_BORDER).setPadding(2).setTextAlignment(TextAlignment.RIGHT));

        totalsTable.addCell(new Cell().add(new Paragraph("Tax (" + boss.getTaxRate() + "%)")).setBorder(Border.NO_BORDER).setPadding(2));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", tax))).setBorder(Border.NO_BORDER).setPadding(2).setTextAlignment(TextAlignment.RIGHT));

        totalsTable.addCell(new Cell().add(new Paragraph("TOTAL")).setBorder(Border.NO_BORDER).setPadding(2).setBold().setFontSize(16));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", total))).setBorder(Border.NO_BORDER).setPadding(2).setBold().setFontSize(16).setTextAlignment(TextAlignment.RIGHT).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(59, 130, 246)));

        document.add(totalsTable);

        document.close();
    }

    public static void generateInvoice(Boss boss, EmployeeInfo employee, List<LogEntry> logs, String startDate, String endDate, int invoiceNum, String outputPath) throws Exception {
        generateInvoice(boss, employee, logs, startDate, endDate, invoiceNum, outputPath, false);
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }

}
