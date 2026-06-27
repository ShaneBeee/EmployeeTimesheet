package com.github.shanebeee.et.util;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.Expenditure;
import com.github.shanebeee.et.model.ExpenseCategory;
import com.github.shanebeee.et.model.KmOdometer;
import com.github.shanebeee.et.model.KmTrip;
import com.github.shanebeee.et.storage.DataStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CsvExporter {

    private final DataStorage storage;

    public CsvExporter(DataStorage storage) {
        this.storage = storage;
    }

    /**
     * Exports expenses and KM log for the given year as a zip containing two CSV files.
     * Returns the output zip file.
     */
    public File export(int year, String outputPath) throws IOException {
        if (!outputPath.endsWith(".zip")) outputPath += ".zip";
        File zipFile = new File(outputPath);

        EmployeeInfo employee   = storage.loadEmployeeInfo();
        List<Expenditure>     expenses   = storage.loadExpenditures(String.valueOf(year));
        List<ExpenseCategory> categories = storage.loadExpenseCategories(String.valueOf(year));
        List<KmTrip>          trips      = storage.loadKmTrips(String.valueOf(year));
        KmOdometer            odometer   = storage.loadKmOdometer(String.valueOf(year));

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // ── Expenses CSV ──────────────────────────────────────────────────
            zos.putNextEntry(new ZipEntry("Expenses_" + year + ".csv"));
            PrintWriter expWriter = new PrintWriter(zos, false, StandardCharsets.UTF_8);
            writeExpensesCsv(expWriter, employee, expenses, categories, year);
            expWriter.flush();
            zos.closeEntry();

            // ── KM Log CSV ────────────────────────────────────────────────────
            zos.putNextEntry(new ZipEntry("KilometreLog_" + year + ".csv"));
            PrintWriter kmWriter = new PrintWriter(zos, false, StandardCharsets.UTF_8);
            writeKmCsv(kmWriter, employee, trips, odometer, year);
            kmWriter.flush();
            zos.closeEntry();
        }

        return zipFile;
    }

    // ── Expenses CSV ──────────────────────────────────────────────────────────

    private void writeExpensesCsv(PrintWriter w, EmployeeInfo employee,
                                   List<Expenditure> expenses,
                                   List<ExpenseCategory> categories, int year) {
        // Header block
        w.println(csv("Employee Timesheet — Expense Report"));
        w.println(csv("Year", String.valueOf(year)));
        if (employee != null && employee.getFullName() != null)
            w.println(csv("Name", employee.getFullName()));
        if (employee != null && employee.getCompany() != null && !employee.getCompany().isBlank())
            w.println(csv("Company", employee.getCompany()));
        w.println(csv("Generated", LocalDate.now().toString()));
        w.println();

        // Column headers
        w.println(csv("Date", "Category", "T2125 Line", "Description", "Subtotal", "GST", "Total"));

        // Group by category
        double grandTotal = 0;
        for (ExpenseCategory cat : categories) {
            List<Expenditure> catExpenses = expenses.stream()
                .filter(e -> cat.getId().equals(e.getCategoryId()))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
            if (catExpenses.isEmpty()) continue;

            double catTotal = 0;
            for (Expenditure exp : catExpenses) {
                w.println(csv(
                    exp.getDate(),
                    cat.getLabel(),
                    safe(cat.getT2125Line()),
                    safe(exp.getDescription()),
                    String.format("%.2f", exp.getSubtotal()),
                    String.format("%.2f", exp.getGst()),
                    String.format("%.2f", exp.getTotal())
                ));
                catTotal += exp.getTotal();
            }
            // Category subtotal
            w.println(csv("", cat.getLabel() + " Subtotal", "", "", "", "",
                String.format("%.2f", catTotal)));
            w.println();
            grandTotal += catTotal;
        }

        // Uncategorized
        List<Expenditure> uncat = expenses.stream()
            .filter(e -> e.getCategoryId() == null)
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .toList();
        if (!uncat.isEmpty()) {
            double ucTotal = 0;
            for (Expenditure exp : uncat) {
                w.println(csv(
                    exp.getDate(),
                    "Uncategorized",
                    "",
                    safe(exp.getDescription()),
                    String.format("%.2f", exp.getSubtotal()),
                    String.format("%.2f", exp.getGst()),
                    String.format("%.2f", exp.getTotal())
                ));
                ucTotal += exp.getTotal();
            }
            w.println(csv("", "Uncategorized Subtotal", "", "", "", "",
                String.format("%.2f", ucTotal)));
            w.println();
            grandTotal += ucTotal;
        }

        // Grand total
        w.println(csv("GRAND TOTAL", "", "", "", "", "", String.format("%.2f", grandTotal)));
    }

    // ── KM Log CSV ────────────────────────────────────────────────────────────

    private void writeKmCsv(PrintWriter w, EmployeeInfo employee,
                             List<KmTrip> trips, KmOdometer odometer, int year) {
        // Header block
        w.println(csv("Employee Timesheet — Kilometre Log"));
        w.println(csv("Year", String.valueOf(year)));
        if (employee != null && employee.getFullName() != null)
            w.println(csv("Name", employee.getFullName()));
        w.println(csv("Generated", LocalDate.now().toString()));
        w.println();

        // Odometer summary
        if (odometer != null) {
            w.println(csv("Odometer Summary"));
            w.println(csv("Year Start (km)", String.format("%.1f", odometer.getStartKm())));
            w.println(csv("Year End (km)",   String.format("%.1f", odometer.getEndKm())));
            double totalKm    = odometer.getEndKm() - odometer.getStartKm();
            double businessKm = trips.stream().mapToDouble(KmTrip::getKm).sum();
            double businessPct = totalKm > 0 ? (businessKm / totalKm) * 100 : 0;
            w.println(csv("Total KM Driven", String.format("%.1f", totalKm)));
            w.println(csv("Business KM",     String.format("%.1f", businessKm)));
            w.println(csv("Business Use %",  String.format("%.1f%%", businessPct)));
            w.println();
        }

        // Trip log
        w.println(csv("Date", "KM", "Source", "Note"));
        List<KmTrip> sorted = trips.stream()
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .toList();
        double total = 0;
        for (KmTrip trip : sorted) {
            w.println(csv(
                trip.getDate(),
                String.format("%.2f", trip.getKm()),
                trip.getSourceLogId() != null ? "Auto (Work Log)" : "Manual",
                safe(trip.getNote())
            ));
            total += trip.getKm();
        }
        w.println();
        w.println(csv("Total Business KM", String.format("%.2f", total)));
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    /** Escapes and joins values as a CSV row. */
    private static String csv(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values[i]));
        }
        return sb.toString();
    }

    /** Wraps a value in quotes if it contains commas, quotes, or newlines. */
    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
