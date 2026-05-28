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

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public class SummaryGenerator {
    public static void generateMonthlySummary(List<Boss> bosses, EmployeeInfo employee, List<LogEntry> logs, String yearMonth, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("MONTHLY INVOICE SUMMARY").setFontSize(24).setBold());
        document.add(new Paragraph("Month: " + yearMonth));
        document.add(new Paragraph("\nEMPLOYEE:"));
        document.add(new Paragraph(safe(employee.getFullName())));
        document.add(new Paragraph(safe(employee.getCompany())));

        Table table = new Table(UnitValue.createPointArray(new float[]{150, 150, 80, 80, 100, 100}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell("Boss");
        table.addHeaderCell("Description");
        table.addHeaderCell("Units");
        table.addHeaderCell("Rate");
        table.addHeaderCell("Tax");
        table.addHeaderCell("Total");

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
                        LocalTime start = LocalTime.parse(log.getStartTime());
                        LocalTime end = LocalTime.parse(log.getEndTime());
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

                        table.addCell(boss.getName());
                        table.addCell(safe(log.getDescription()));
                        table.addCell(String.format("%.2f", units));
                        table.addCell(String.format("$%.2f", rate));
                        table.addCell(String.format("$%.2f", tax));
                        table.addCell(String.format("$%.2f", total));

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

                table.addCell(boss.getName());
                table.addCell("Total Work Hours");
                table.addCell(String.format("%.2f", totalHours));
                table.addCell(String.format("$%.2f", rate));
                table.addCell(String.format("$%.2f", tax));
                table.addCell(String.format("$%.2f", total));

                bossSubtotal += sub;
                bossTax += tax;
            }

            if (totalKm > 0) {
                double rate = boss.getKmRate() != null ? boss.getKmRate() : 0;
                double sub = totalKm * rate;
                double tax = sub * (boss.getTaxRate() / 100.0);
                double total = sub + tax;

                table.addCell(boss.getName());
                table.addCell("Total Kilometers");
                table.addCell(String.format("%.2f", totalKm));
                table.addCell(String.format("$%.2f", rate));
                table.addCell(String.format("$%.2f", tax));
                table.addCell(String.format("$%.2f", total));

                bossSubtotal += sub;
                bossTax += tax;
            }

            grandSubtotal += bossSubtotal;
            grandTax += bossTax;
            grandTotal += (bossSubtotal + bossTax);
        }

        document.add(table);
        document.add(new Paragraph("\nSubtotal: $" + String.format("%.2f", grandSubtotal)).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
        document.add(new Paragraph("Tax: $" + String.format("%.2f", grandTax)).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
        document.add(new Paragraph("TOTAL: $" + String.format("%.2f", grandTotal)).setBold().setFontSize(16).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));

        document.close();
    }

    private static String safe(String str) {
        return str == null ? "" : str;
    }
}
