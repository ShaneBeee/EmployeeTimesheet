package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.LogEntry;
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

public class SummaryGenerator {

    public static void generateMonthlySummary(List<Boss> bosses, EmployeeInfo employee, List<LogEntry> logs, String yearMonth, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header Table
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100));
        headerTable.setMarginBottom(20);

        Cell titleCell = new Cell().add(new Paragraph("MONTHLY SUMMARY")
                .setFontSize(30)
                .setBold()
                .setFontColor(ColorConstants.DARK_GRAY))
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(titleCell);

        Cell infoCell = new Cell().add(new Paragraph("Month: " + yearMonth)
                .setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE);
        headerTable.addCell(infoCell);
        document.add(headerTable);

        // Employee Info Section
        document.add(new Paragraph("EMPLOYEE:").setBold().setFontColor(ColorConstants.GRAY));
        document.add(new Paragraph(safe(employee.getFullName())).setBold());
        document.add(new Paragraph(safe(employee.getCompany())).setMarginBottom(20));

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 1, 1, 1, 1}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setMarginBottom(20);

        // Styled Table Header
        String[] headers = {"Boss", "Description", "Units", "Rate", "Tax", "Total"};
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.DARK_GRAY)
                .setPadding(5));
        }

        double grandSubtotal = 0;
        double grandTax = 0;
        double grandTotal = 0;

        for (Boss boss : bosses) {
            double bossSubtotal = 0;
            double bossTax = 0;

            double totalHours = 0;
            double totalKm = 0;

            for (LogEntry log : logs) {
                if (log.getType() == LogEntry.EntryType.TIME) {
                    double perc = log.getBossPercentages().getOrDefault(boss.getId(), 0.0) / 100.0;
                    if (perc <= 0) {
                        perc = log.getBossPercentages().getOrDefault(boss.getName(), 0.0) / 100.0;
                    }
                    if (perc > 0) {
                        LocalTime start = com.github.shanebeee.reconciled.view.TimePickerPanel.parseTime(log.getStartTime());
                        LocalTime end = com.github.shanebeee.reconciled.view.TimePickerPanel.parseTime(log.getEndTime());
                        double hours = Duration.between(start, end).toMinutes() / 60.0;
                        totalHours += (hours * perc);
                    }
                } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                    if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid())) {
                        totalKm += (log.getKilometers() != null ? log.getKilometers() : 0);
                    }
                } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                    if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid())) {
                        double units = log.getUnits() != null ? log.getUnits() : 0;
                        double rate = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                        double sub = units * rate;
                        double tax = sub * (boss.getTaxRate() / 100.0);
                        double total = sub + tax;

                        table.addCell(new Cell().add(new Paragraph(boss.getName())).setPadding(5));
                        table.addCell(new Cell().add(new Paragraph(safe(log.getDescription()))).setPadding(5));
                        table.addCell(new Cell().add(new Paragraph(String.format("%.2f", units))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", rate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", tax))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                        table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", total))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));

                        bossSubtotal += sub;
                        bossTax += tax;
                    }
                }

                // Old kmEntries support
                if (log.getKmEntries() != null) {
                    for (LogEntry.KmEntry ke : log.getKmEntries()) {
                        if (boss.getId().equals(ke.getBossUuid()) || boss.getName().equals(ke.getBossUuid())) {
                            totalKm += ke.getKilometers();
                        }
                    }
                }
            }

            if (totalHours > 0) {
                double rate = boss.getHourlyRate();
                double sub = totalHours * rate;
                double tax = sub * (boss.getTaxRate() / 100.0);
                double total = sub + tax;

                table.addCell(new Cell().add(new Paragraph(boss.getName())).setPadding(5));
                table.addCell(new Cell().add(new Paragraph("Total Work Hours")).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalHours))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", rate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", tax))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", total))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));

                bossSubtotal += sub;
                bossTax += tax;
            }

            if (totalKm > 0) {
                double rate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                double sub = totalKm * rate;
                double tax = sub * (boss.getTaxRate() / 100.0);
                double total = sub + tax;

                table.addCell(new Cell().add(new Paragraph(boss.getName())).setPadding(5));
                table.addCell(new Cell().add(new Paragraph("Total Kilometers")).setPadding(5));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", totalKm))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", rate))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", tax))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", total))).setPadding(5).setTextAlignment(TextAlignment.RIGHT));

                bossSubtotal += sub;
                bossTax += tax;
            }

            grandSubtotal += bossSubtotal;
            grandTax += bossTax;
            grandTotal += (bossSubtotal + bossTax);
        }

        document.add(table);

        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        totalsTable.setWidth(UnitValue.createPercentValue(40));
        totalsTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);

        totalsTable.addCell(new Cell().add(new Paragraph("Subtotal")).setBorder(Border.NO_BORDER).setPadding(2));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", grandSubtotal))).setBorder(Border.NO_BORDER).setPadding(2).setTextAlignment(TextAlignment.RIGHT));

        totalsTable.addCell(new Cell().add(new Paragraph("Tax")).setBorder(Border.NO_BORDER).setPadding(2));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", grandTax))).setBorder(Border.NO_BORDER).setPadding(2).setTextAlignment(TextAlignment.RIGHT));

        totalsTable.addCell(new Cell().add(new Paragraph("TOTAL")).setBorder(Border.NO_BORDER).setPadding(2).setBold().setFontSize(16));
        totalsTable.addCell(new Cell().add(new Paragraph("$" + String.format("%.2f", grandTotal))).setBorder(Border.NO_BORDER).setPadding(2).setBold().setFontSize(16).setTextAlignment(TextAlignment.RIGHT).setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(34, 197, 94)));

        document.add(totalsTable);

        document.close();
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }

}
