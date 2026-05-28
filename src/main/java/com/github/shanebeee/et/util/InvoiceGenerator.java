package com.github.shanebeee.et.util;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.LogEntry;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public class InvoiceGenerator {
    public static void generateInvoice(Boss boss, EmployeeInfo employee, List<LogEntry> logs, String startDate, String endDate, int invoiceNum, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("INVOICE").setFontSize(24).setBold());
        document.add(new Paragraph("Invoice #: " + invoiceNum));
        document.add(new Paragraph("Date Range: " + startDate + " to " + endDate));
        document.add(new Paragraph("\nFROM:"));
        document.add(new Paragraph(safe(employee.getFullName())));
        document.add(new Paragraph(safe(employee.getCompany())));
        document.add(new Paragraph(safe(employee.getEmail())));
        document.add(new Paragraph(safe(employee.getPhoneNumber())));

        document.add(new Paragraph("\nTO:"));
        document.add(new Paragraph(safe(boss.getName())));
        document.add(new Paragraph(safe(boss.getCompany())));
        document.add(new Paragraph(safe(boss.getAddress())));

        Table table = new Table(UnitValue.createPointArray(new float[]{200, 100, 100, 100}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell("Description");
        table.addHeaderCell("Units");
        table.addHeaderCell("Rate");
        table.addHeaderCell("Total");

        double totalHours = 0;
        double totalKm = 0;

        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.TIME) {
                double perc = log.getBossPercentages().getOrDefault(boss.getId(), 0.0) / 100.0;
                if (perc <= 0) {
                    // Fallback for old logs using names
                    perc = log.getBossPercentages().getOrDefault(boss.getName(), 0.0) / 100.0;
                }
                if (perc > 0) {
                    LocalTime start = LocalTime.parse(log.getStartTime());
                    LocalTime end = LocalTime.parse(log.getEndTime());
                    double hours = Duration.between(start, end).toMinutes() / 60.0;
                    double billableHours = hours * perc;
                    totalHours += billableHours;
                }
            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid())) {
                    totalKm += (log.getKilometers() != null ? log.getKilometers() : 0);
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
            table.addCell("Total Work Hours");
            table.addCell(String.format("%.2f", totalHours));
            table.addCell(String.format("$%.2f/hr", boss.getHourlyRate()));
            table.addCell(String.format("$%.2f", totalHours * boss.getHourlyRate()));
        }

        if (totalKm > 0) {
            double kmRate = boss.getKmRate() != null ? boss.getKmRate() : 0;
            table.addCell("Total Kilometers");
            table.addCell(String.format("%.2f", totalKm));
            table.addCell(String.format("$%.2f/km", kmRate));
            table.addCell(String.format("$%.2f", totalKm * kmRate));
        }

        double extraEntriesTotal = 0;
        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.EXTRA && (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))) {
                double units = log.getUnits() != null ? log.getUnits() : 0;
                double cost = log.getCostPerUnit() != null ? log.getCostPerUnit() : 0;
                double lineTotal = units * cost;
                table.addCell(log.getDescription());
                table.addCell(String.format("%.2f", units));
                table.addCell(String.format("$%.2f", cost));
                table.addCell(String.format("$%.2f", lineTotal));
                extraEntriesTotal += lineTotal;
            }
        }

        document.add(table);
        double hoursCost = totalHours * boss.getHourlyRate();
        double kmCost = totalKm * (boss.getKmRate() != null ? boss.getKmRate() : 0);
        double subtotal = hoursCost + kmCost + extraEntriesTotal;
        double tax = subtotal * (boss.getTaxRate() / 100.0);
        double total = subtotal + tax;

        document.add(new Paragraph("\nSubtotal: $" + String.format("%.2f", subtotal)).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
        document.add(new Paragraph("Tax (" + boss.getTaxRate() + "%): $" + String.format("%.2f", tax)).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
        document.add(new Paragraph("TOTAL: $" + String.format("%.2f", total)).setBold().setFontSize(16).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));

        document.close();
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }
}
