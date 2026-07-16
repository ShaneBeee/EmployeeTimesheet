package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.CcaAsset;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.Expenditure;
import com.github.shanebeee.reconciled.model.ExpenseCategory;
import com.github.shanebeee.reconciled.model.KmTrip;
import com.github.shanebeee.reconciled.model.LogEntry;
import com.github.shanebeee.reconciled.storage.DataStorage;
import com.github.shanebeee.reconciled.view.TimePickerPanel;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnnualTaxReportGenerator {

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final DeviceRgb NAVY = new DeviceRgb(30, 41, 59);
    private static final DeviceRgb BLUE = new DeviceRgb(59, 130, 246);
    private static final DeviceRgb BLUE_LIGHT = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb GREEN = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb GREEN_LIGHT = new DeviceRgb(209, 250, 229);
    private static final DeviceRgb AMBER = new DeviceRgb(245, 158, 11);
    private static final DeviceRgb AMBER_LIGHT = new DeviceRgb(254, 243, 199);
    private static final DeviceRgb SLATE = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb RED = new DeviceRgb(239, 68, 68);

    public static void generate(int year, DataStorage storage, String outputPath) throws Exception {
        EmployeeInfo employee = storage.loadEmployeeInfo();
        List<Boss> allBosses = storage.loadBosses();
        List<Boss> selfBosses = allBosses.stream().filter(Boss::isSelfEmployed).toList();
        List<Expenditure> expenses = storage.loadExpenditures(String.valueOf(year));
        List<ExpenseCategory> cats = storage.loadExpenseCategories(String.valueOf(year));
        List<KmTrip> kmTrips = storage.loadKmTrips(String.valueOf(year));
        List<CcaAsset> ccaAssets = storage.loadCcaAssets();

        // Load all logs for the year
        List<LogEntry> allLogs = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String key = String.format("%d-%02d", year, m);
            allLogs.addAll(storage.loadLogs(key));
        }

        // ── Pre-compute income data ───────────────────────────────────────────
        // Per boss per month: [hours, hoursIncome, km, kmIncome, extras]
        record BossMonthData(double hours, double hoursIncome, double km, double kmIncome, double extras) {
        }
        Map<String, BossMonthData[]> bossMonthly = new LinkedHashMap<>();
        for (Boss boss : selfBosses) {
            BossMonthData[] months = new BossMonthData[12];
            for (int i = 0; i < 12; i++) months[i] = new BossMonthData(0, 0, 0, 0, 0);
            bossMonthly.put(boss.getId(), months);
        }

        double totalGstCollected = 0;
        for (LogEntry log : allLogs) {
            int mIdx = LocalDate.parse(log.getDate()).getMonthValue() - 1;
            if (log.getType() == LogEntry.EntryType.TIME) {
                for (Boss boss : selfBosses) {
                    double perc = 0;
                    if (log.getBossPercentages() != null) {
                        perc = log.getBossPercentages().getOrDefault(boss.getId(),
                            log.getBossPercentages().getOrDefault(boss.getName(), 0.0)) / 100.0;
                    }
                    if (perc > 0 && log.getStartTime() != null && log.getEndTime() != null) {
                        double hrs = Duration.between(
                            TimePickerPanel.parseTime(log.getStartTime()),
                            TimePickerPanel.parseTime(log.getEndTime())).toMinutes() / 60.0 * perc;
                        double rate = rateForDate(boss, log.getDate());
                        double income = hrs * rate;
                        BossMonthData old = bossMonthly.get(boss.getId())[mIdx];
                        bossMonthly.get(boss.getId())[mIdx] = new BossMonthData(
                            old.hours() + hrs, old.hoursIncome() + income,
                            old.km(), old.kmIncome(), old.extras());
                        totalGstCollected += income * (boss.getTaxRate() / 100.0);
                    }
                }
            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                for (Boss boss : selfBosses) {
                    if ((boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                        && log.getKilometers() != null) {
                        double km = log.getKilometers();
                        double kmInc = km * (boss.getKmRate() != null ? boss.getKmRate() : 0);
                        BossMonthData old = bossMonthly.get(boss.getId())[mIdx];
                        bossMonthly.get(boss.getId())[mIdx] = new BossMonthData(
                            old.hours(), old.hoursIncome(), old.km() + km, old.kmIncome() + kmInc, old.extras());
                        totalGstCollected += kmInc * (boss.getTaxRate() / 100.0);
                    }
                }
            } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                for (Boss boss : selfBosses) {
                    if ((boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                        && log.getUnits() != null && log.getCostPerUnit() != null) {
                        double ext = log.getUnits() * log.getCostPerUnit();
                        BossMonthData old = bossMonthly.get(boss.getId())[mIdx];
                        bossMonthly.get(boss.getId())[mIdx] = new BossMonthData(
                            old.hours(), old.hoursIncome(), old.km(), old.kmIncome(), old.extras() + ext);
                        totalGstCollected += ext * (boss.getTaxRate() / 100.0);
                    }
                }
            }
        }

        // ── Pre-compute expense data ──────────────────────────────────────────
        double totalExpenses = 0;
        double totalGstITC = 0;
        Map<String, Double> catTotals = new LinkedHashMap<>();
        for (ExpenseCategory cat : cats) catTotals.put(cat.getId(), 0.0);
        catTotals.put("__uncategorized__", 0.0);

        for (Expenditure exp : expenses) {
            totalExpenses += exp.getTotal();
            totalGstITC += exp.getGst();
            String catId = exp.getCategoryId() != null ? exp.getCategoryId() : "__uncategorized__";
            catTotals.merge(catId, exp.getTotal(), Double::sum);
        }

        // ── Pre-compute KM data ───────────────────────────────────────────────
        double totalKmDriven = kmTrips.stream().mapToDouble(KmTrip::getKm).sum();
        double totalKmBilled = 0;
        for (Boss boss : selfBosses) {
            for (BossMonthData[] months : bossMonthly.values()) {
                // sum km from this boss (already aggregated above)
            }
        }
        // Sum km billed across all bosses
        for (BossMonthData[] months : bossMonthly.values()) {
            for (BossMonthData m : months) totalKmBilled += m.km();
        }

        // ── Gross income & net ────────────────────────────────────────────────
        double grossIncome = 0;
        for (BossMonthData[] months : bossMonthly.values()) {
            for (BossMonthData m : months)
                grossIncome += m.hoursIncome() + m.kmIncome() + m.extras();
        }
        double totalCcaDeduction = ccaAssets.stream().mapToDouble(a -> a.deductionForYear(year)).sum();
        // Total claimable expenses (after partial-use rules)
        DeductionCalculator deductCalc = new DeductionCalculator(storage);
        double totalClaimableExpenses = 0;
        for (ExpenseCategory cat : cats) {
            double catTotal = catTotals.getOrDefault(cat.getId(), 0.0);
            totalClaimableExpenses += deductCalc.deductibleAmount(catTotal, cat, year);
        }
        totalClaimableExpenses += catTotals.getOrDefault("__uncategorized__", 0.0);
        double netIncome = grossIncome - totalClaimableExpenses - totalCcaDeduction;
        // GST ITCs include both regular expense GST and GST paid on capital assets purchased this year
        double totalCcaGstITC = ccaAssets.stream()
            .filter(a -> a.getPurchaseYear() == year)
            .mapToDouble(a -> a.getCost() * 0.05)
            .sum();
        double netGstOwing = totalGstCollected - totalGstITC - totalCcaGstITC;

        // ── Build PDF ─────────────────────────────────────────────────────────
        PdfWriter writer = new PdfWriter(outputPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.LETTER);
        doc.setMargins(50, 50, 50, 50);

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 1 — COVER
        // ═════════════════════════════════════════════════════════════════════
        addCoverPage(doc, employee, year);
        doc.add(new com.itextpdf.layout.element.AreaBreak());

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 2 — INCOME SUMMARY
        // ═════════════════════════════════════════════════════════════════════
        addSectionHeader(doc, "Income Summary", "Self-employed income for the year", BLUE, year);

        for (Boss boss : selfBosses) {
            BossMonthData[] months = bossMonthly.get(boss.getId());
            double bossTotal = 0;
            for (BossMonthData m : months) bossTotal += m.hoursIncome() + m.kmIncome() + m.extras();
            if (bossTotal == 0) continue;

            // Boss sub-header
            doc.add(new Paragraph(boss.getName())
                .setFontSize(12).setBold().setFontColor(NAVY)
                .setMarginTop(12).setMarginBottom(4));

            Table t = makeTable(new float[]{3, 1, 1, 1, 1, 1});
            addHeaderRow(t, new String[]{"Month", "Hours", "Labour $", "KM", "KM $", "Extras $"});

            for (int mi = 0; mi < 12; mi++) {
                BossMonthData m = months[mi];
                double rowTotal = m.hoursIncome() + m.kmIncome() + m.extras();
                if (rowTotal == 0 && m.hours() == 0 && m.km() == 0) continue;
                String month = Month.of(mi + 1).getDisplayName(TextStyle.FULL, Locale.CANADA);
                boolean alt = (mi % 2 == 0);
                addDataRow(t, alt, TextAlignment.LEFT, month);
                addDataRow(t, alt, TextAlignment.RIGHT, fmt2(m.hours()) + " hrs");
                addDataRow(t, alt, TextAlignment.RIGHT, fmtDollar(m.hoursIncome()));
                addDataRow(t, alt, TextAlignment.RIGHT, fmt2(m.km()) + " km");
                addDataRow(t, alt, TextAlignment.RIGHT, fmtDollar(m.kmIncome()));
                addDataRow(t, alt, TextAlignment.RIGHT, fmtDollar(m.extras()));
            }

            // Boss total row
            double bossHours = 0, bossHoursInc = 0, bossKm = 0, bossKmInc = 0, bossExt = 0;
            for (BossMonthData m : months) {
                bossHours += m.hours();
                bossHoursInc += m.hoursIncome();
                bossKm += m.km();
                bossKmInc += m.kmIncome();
                bossExt += m.extras();
            }
            addTotalRow(t, new String[]{
                "Total — " + boss.getName(),
                fmt2(bossHours) + " hrs", fmtDollar(bossHoursInc),
                fmt2(bossKm) + " km", fmtDollar(bossKmInc),
                fmtDollar(bossExt)
            });
            doc.add(t);
        }

        // GST collected
        doc.add(new Paragraph("\n"));
        Table gstTable = makeTable(new float[]{4, 1});
        addSubtotalRow(gstTable, "Gross Income (before GST)", fmtDollar(grossIncome), GREEN, GREEN_LIGHT);
        addSubtotalRow(gstTable, "GST / HST Collected", fmtDollar(totalGstCollected), BLUE, BLUE_LIGHT);
        doc.add(gstTable);
        doc.add(new com.itextpdf.layout.element.AreaBreak());

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 3 — EXPENSE SUMMARY
        // ═════════════════════════════════════════════════════════════════════
        addSectionHeader(doc, "Expense Summary", "CRA T2125 categorized business expenses", AMBER, year);

        Table expTable = makeTable(new float[]{1, 3, 1, 1, 1});
        addHeaderRow(expTable, new String[]{"T2125 Line", "Category", "Total Paid", "Claimable", "GST (ITC)"});

        DeductionCalculator calc = new DeductionCalculator(storage);
        double runningExpTotal = 0, runningClaimable = 0, runningITC = 0;
        int rowIdx = 0;
        for (ExpenseCategory cat : cats) {
            double catTotal = catTotals.getOrDefault(cat.getId(), 0.0);
            if (catTotal == 0) continue;
            double catClaimable = calc.deductibleAmount(catTotal, cat, year);
            double catITC = expenses.stream()
                .filter(e -> cat.getId().equals(e.getCategoryId()))
                .mapToDouble(Expenditure::getGst).sum();
            boolean alt = (rowIdx++ % 2 == 0);
            String claimLabel = calc.isPartial(cat, year)
                ? fmtDollar(catClaimable) + " (" + calc.percentLabel(cat, year) + ")"
                : fmtDollar(catClaimable);
            addDataRow(expTable, alt, TextAlignment.LEFT, safe(cat.getT2125Line()));
            addDataRow(expTable, alt, TextAlignment.LEFT, cat.getLabel());
            addDataRow(expTable, alt, TextAlignment.RIGHT, fmtDollar(catTotal));
            addDataRow(expTable, alt, TextAlignment.RIGHT, claimLabel);
            addDataRow(expTable, alt, TextAlignment.RIGHT, fmtDollar(catITC));
            runningExpTotal += catTotal;
            runningClaimable += catClaimable;
            runningITC += catITC;
        }

        // Uncategorized
        double uncatTotal = catTotals.getOrDefault("__uncategorized__", 0.0);
        if (uncatTotal > 0) {
            double uncatITC = expenses.stream()
                .filter(e -> e.getCategoryId() == null)
                .mapToDouble(Expenditure::getGst).sum();
            boolean alt = (rowIdx % 2 == 0);
            addDataRow(expTable, alt, TextAlignment.LEFT, "—");
            addDataRow(expTable, alt, TextAlignment.LEFT, "Uncategorized");
            addDataRow(expTable, alt, TextAlignment.RIGHT, fmtDollar(uncatTotal));
            addDataRow(expTable, alt, TextAlignment.RIGHT, fmtDollar(uncatTotal));
            addDataRow(expTable, alt, TextAlignment.RIGHT, fmtDollar(uncatITC));
            runningExpTotal += uncatTotal;
            runningClaimable += uncatTotal;
            runningITC += uncatITC;
        }

        addTotalRow(expTable, new String[]{"Total Expenses", "", fmtDollar(runningExpTotal), fmtDollar(runningClaimable), fmtDollar(runningITC)});
        doc.add(expTable);

        // GST reconciliation block
        doc.add(new Paragraph("\nGST / HST Reconciliation")
            .setFontSize(12).setBold().setFontColor(NAVY).setMarginTop(16));
        Table gstRecon = makeTable(new float[]{4, 1});
        addSubtotalRow(gstRecon, "GST Collected (from clients)", fmtDollar(totalGstCollected), BLUE, BLUE_LIGHT);
        addSubtotalRow(gstRecon, "GST Paid on Expenses (ITCs)", fmtDollar(totalGstITC), AMBER, AMBER_LIGHT);
        if (totalCcaGstITC > 0) {
            addSubtotalRow(gstRecon, "GST Paid on Capital Assets (ITCs)", fmtDollar(totalCcaGstITC), AMBER, AMBER_LIGHT);
        }
        DeviceRgb netGstColor = netGstOwing >= 0 ? RED : GREEN;
        DeviceRgb netGstBg = netGstOwing >= 0 ? new DeviceRgb(254, 226, 226) : GREEN_LIGHT;
        addSubtotalRow(gstRecon,
            netGstOwing >= 0 ? "Net GST Owing to CRA" : "Net GST Refund from CRA",
            fmtDollar(Math.abs(netGstOwing)), netGstColor, netGstBg);
        doc.add(gstRecon);
        doc.add(new com.itextpdf.layout.element.AreaBreak());

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 3 — CCA SCHEDULE
        // ═════════════════════════════════════════════════════════════════════
        addSectionHeader(doc, "CCA Schedule", "Capital Cost Allowance — depreciable assets", new DeviceRgb(139, 92, 246), year);

        if (ccaAssets.isEmpty()) {
            doc.add(new Paragraph("No CCA assets have been added.")
                .setFontColor(SLATE).setFontSize(10).setItalic().setMarginTop(8));
        } else {
            // Group assets by class
            Map<String, List<CcaAsset>> byClass = new java.util.LinkedHashMap<>();
            for (CcaAsset a : ccaAssets) {
                String cls = a.getAssetClass() != null ? a.getAssetClass() : "Unclassified";
                byClass.computeIfAbsent(cls, k -> new java.util.ArrayList<>()).add(a);
            }

            DeviceRgb PURPLE = new DeviceRgb(139, 92, 246);
            DeviceRgb PURPLE_LIGHT = new DeviceRgb(237, 233, 254);

            double totalOpeningUcc = 0;
            double totalDeduction = 0;
            double totalClosingUcc = 0;

            Table ccaTable = makeTable(new float[]{3, 1, 1, 1, 1, 1});
            addHeaderRow(ccaTable, new String[]{"Asset (Purchase Date)", "Class / Rate", "Cost", "Opening UCC", "Deduction", "Closing UCC"});

            int ccaRow = 0;
            for (Map.Entry<String, List<CcaAsset>> entry : byClass.entrySet()) {
                // Class sub-header spanning all columns
                ccaTable.addCell(new Cell(1, 6)
                    .add(new Paragraph(entry.getKey()).setFontSize(9).setBold().setFontColor(PURPLE))
                    .setBackgroundColor(PURPLE_LIGHT).setPadding(5).setBorder(Border.NO_BORDER));

                double classOpenUcc = 0, classDeduct = 0, classCloseUcc = 0;
                for (CcaAsset a : entry.getValue()) {
                    double deduction = a.deductionForYear(year);
                    double openingUcc = a.openingUccForYear(year);
                    double closingUcc = a.closingUccForYear(year);
                    boolean alt = (ccaRow++ % 2 == 0);

                    String assetLabel = (a.getDescription() != null ? a.getDescription() : "—")
                        + (a.getPurchaseDate() != null ? "\n" + a.getPurchaseDate() : "");
                    addDataRow(ccaTable, alt, TextAlignment.LEFT, assetLabel);
                    addDataRow(ccaTable, alt, TextAlignment.RIGHT,
                        a.getAssetClass() + " / " + String.format("%.0f%%", a.getClassRate() * 100));
                    addDataRow(ccaTable, alt, TextAlignment.RIGHT, fmtDollar(a.getCost()));
                    addDataRow(ccaTable, alt, TextAlignment.RIGHT, fmtDollar(openingUcc));
                    addDataRow(ccaTable, alt, TextAlignment.RIGHT, fmtDollar(deduction));
                    addDataRow(ccaTable, alt, TextAlignment.RIGHT, fmtDollar(closingUcc));

                    classOpenUcc += openingUcc;
                    classDeduct += deduction;
                    classCloseUcc += closingUcc;
                }

                // Class subtotal
                addTotalRow(ccaTable, new String[]{
                    entry.getKey() + " Subtotal", "",
                    "", fmtDollar(classOpenUcc), fmtDollar(classDeduct), fmtDollar(classCloseUcc)
                });

                totalOpeningUcc += classOpenUcc;
                totalDeduction += classDeduct;
                totalClosingUcc += classCloseUcc;
            }
            doc.add(ccaTable);

            // Grand total block
            doc.add(new Paragraph("\n"));
            Table ccaSummary = makeTable(new float[]{4, 1});
            addSubtotalRow(ccaSummary, "Total Opening UCC", fmtDollar(totalOpeningUcc), PURPLE, PURPLE_LIGHT);
            addSubtotalRow(ccaSummary, "Total CCA Deduction for " + year, fmtDollar(totalDeduction), PURPLE, PURPLE_LIGHT);
            addSubtotalRow(ccaSummary, "Total Closing UCC", fmtDollar(totalClosingUcc), PURPLE, PURPLE_LIGHT);
            doc.add(ccaSummary);

            // GST / ITC block — assets purchased in this tax year only
            double ccaGstThisYear = ccaAssets.stream()
                .filter(a -> a.getPurchaseYear() == year)
                .mapToDouble(a -> a.getCost() * 0.05)
                .sum();
            if (ccaGstThisYear > 0) {
                doc.add(new Paragraph("\nGST / HST on Capital Acquisitions")
                    .setFontSize(12).setBold().setFontColor(NAVY).setMarginTop(16));
                Table gstItcTable = makeTable(new float[]{4, 1});
                addSubtotalRow(gstItcTable,
                    "GST paid on assets purchased in " + year + " (claim as ITCs on GST return)",
                    fmtDollar(ccaGstThisYear), BLUE, BLUE_LIGHT);
                doc.add(gstItcTable);
                doc.add(new Paragraph(
                    "* Claim the above GST amount as an Input Tax Credit (ITC) on your "
                        + year + " GST/HST return. Do not include in T2125 business expenses.")
                    .setFontSize(9).setFontColor(SLATE).setItalic().setMarginTop(6));
            }

            doc.add(new Paragraph(
                "* Deductions shown are computed amounts for " + year + ".")
                .setFontSize(9).setFontColor(SLATE).setItalic().setMarginTop(10));
        }
        doc.add(new com.itextpdf.layout.element.AreaBreak());

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 4 — KILOMETRE SUMMARY
        // ═════════════════════════════════════════════════════════════════════
        addSectionHeader(doc, "Kilometre Summary", "Business kilometres driven and billed", GREEN, year);

        // Monthly KM trips table
        Table kmTable = makeTable(new float[]{2, 3, 1});
        addHeaderRow(kmTable, new String[]{"Date", "Purpose / Note", "KM"});
        int kmRow = 0;
        for (KmTrip trip : kmTrips.stream().sorted(Comparator.comparing(KmTrip::getDate)).toList()) {
            boolean alt = (kmRow++ % 2 == 0);
            addDataRow(kmTable, alt, TextAlignment.LEFT, trip.getDate());
            addDataRow(kmTable, alt, TextAlignment.LEFT, safe(trip.getNote()));
            addDataRow(kmTable, alt, TextAlignment.RIGHT, fmt2(trip.getKm()) + " km");
        }
        if (kmRow == 0) {
            Cell empty = new Cell(1, 3).add(new Paragraph("No km trips recorded for " + year).setFontColor(SLATE))
                .setBorder(Border.NO_BORDER).setPadding(8);
            kmTable.addCell(empty);
        }
        addTotalRow(kmTable, new String[]{"", "Total KM Driven", fmt2(totalKmDriven) + " km"});
        doc.add(kmTable);

        // KM billed per boss
        doc.add(new Paragraph("\nKilometres Billed to Clients")
            .setFontSize(12).setBold().setFontColor(NAVY).setMarginTop(16).setMarginBottom(4));
        Table kmBilledTable = makeTable(new float[]{3, 1, 1, 1});
        addHeaderRow(kmBilledTable, new String[]{"Client", "KM Billed", "Rate", "Income"});
        int kbRow = 0;
        for (Boss boss : selfBosses) {
            BossMonthData[] months = bossMonthly.get(boss.getId());
            double bossKm = 0, bossKmInc = 0;
            for (BossMonthData m : months) {
                bossKm += m.km();
                bossKmInc += m.kmIncome();
            }
            if (bossKm == 0) continue;
            boolean alt = (kbRow++ % 2 == 0);
            addDataRow(kmBilledTable, alt, TextAlignment.LEFT, boss.getName());
            addDataRow(kmBilledTable, alt, TextAlignment.RIGHT, fmt2(bossKm) + " km");
            addDataRow(kmBilledTable, alt, TextAlignment.RIGHT, "$" + fmt2(boss.getKmRate() != null ? boss.getKmRate() : 0) + "/km");
            addDataRow(kmBilledTable, alt, TextAlignment.RIGHT, fmtDollar(bossKmInc));
        }
        addTotalRow(kmBilledTable, new String[]{"Total KM Billed", fmt2(totalKmBilled) + " km", "", ""});
        doc.add(kmBilledTable);
        doc.add(new com.itextpdf.layout.element.AreaBreak());

        // ═════════════════════════════════════════════════════════════════════
        // PAGE 5 — NET SUMMARY
        // ═════════════════════════════════════════════════════════════════════
        addSectionHeader(doc, "Annual Net Summary", "Overview for tax year " + year, NAVY, year);

        doc.add(new Paragraph("\n"));
        Table summary = makeTable(new float[]{4, 1});

        addSubtotalRow(summary, "Gross Income (self-employed)", fmtDollar(grossIncome), GREEN, GREEN_LIGHT);
        addSubtotalRow(summary, "Total Expenses (paid)", fmtDollar(totalExpenses), AMBER, AMBER_LIGHT);
        addSubtotalRow(summary, "Total Expenses (claimable after partial use)", fmtDollar(totalClaimableExpenses), AMBER, AMBER_LIGHT);
        if (totalCcaDeduction > 0) {
            addSubtotalRow(summary, "CCA Deduction (Capital Cost Allowance)", fmtDollar(totalCcaDeduction), new DeviceRgb(139, 92, 246), new DeviceRgb(237, 233, 254));
        }
        DeviceRgb netColor = netIncome >= 0 ? GREEN : RED;
        DeviceRgb netBg = netIncome >= 0 ? GREEN_LIGHT : new DeviceRgb(254, 226, 226);
        addSubtotalRow(summary, "Estimated Net Income", fmtDollar(netIncome), netColor, netBg);

        doc.add(new Paragraph("\n"));
        doc.add(summary);

        doc.add(new Paragraph("\nGST / HST")
            .setFontSize(12).setBold().setFontColor(NAVY).setMarginTop(20));
        Table summaryGst = makeTable(new float[]{4, 1});
        addSubtotalRow(summaryGst, "GST Collected", fmtDollar(totalGstCollected), BLUE, BLUE_LIGHT);
        addSubtotalRow(summaryGst, "GST Paid on Expenses (ITCs)", fmtDollar(totalGstITC), AMBER, AMBER_LIGHT);
        if (totalCcaGstITC > 0) {
            addSubtotalRow(summaryGst, "GST Paid on Capital Assets (ITCs)", fmtDollar(totalCcaGstITC), AMBER, AMBER_LIGHT);
        }
        addSubtotalRow(summaryGst,
            netGstOwing >= 0 ? "Net GST Owing to CRA" : "Net GST Refund",
            fmtDollar(Math.abs(netGstOwing)), netGstColor, netGstBg);
        doc.add(summaryGst);

        doc.add(new Paragraph("\nKilometres")
            .setFontSize(12).setBold().setFontColor(NAVY).setMarginTop(20));
        Table summaryKm = makeTable(new float[]{4, 1});
        addSubtotalRow(summaryKm, "Total KM Driven", fmt2(totalKmDriven) + " km", NAVY, LIGHT);
        addSubtotalRow(summaryKm, "Total KM Billed", fmt2(totalKmBilled) + " km", BLUE, BLUE_LIGHT);
        doc.add(summaryKm);

        // Disclaimer
        doc.add(new Paragraph("\n* Estimated net income is before vehicle and home-office deductions, but includes CCA. "
            + "Consult a qualified tax professional for your final return.")
            .setFontSize(9).setFontColor(SLATE).setItalic().setMarginTop(24));

        doc.close();
    }

    // ── Page builders ─────────────────────────────────────────────────────────

    private static void addCoverPage(Document doc, EmployeeInfo employee, int year) {
        // Big year
        doc.add(new Paragraph(String.valueOf(year))
            .setFontSize(72).setBold().setFontColor(BLUE)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(100));

        doc.add(new Paragraph("Annual Tax Report")
            .setFontSize(28).setBold().setFontColor(NAVY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(8));

        // Divider
        Table div = new Table(UnitValue.createPercentArray(new float[]{1}))
            .setWidth(UnitValue.createPercentValue(60))
            .setHorizontalAlignment(HorizontalAlignment.CENTER)
            .setMarginTop(24).setMarginBottom(24);
        div.addCell(new Cell().setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(BLUE, 2)).add(new Paragraph("")));
        doc.add(div);

        if (employee != null) {
            if (employee.getFullName() != null && !employee.getFullName().isBlank()) {
                doc.add(new Paragraph(employee.getFullName())
                    .setFontSize(18).setBold().setFontColor(NAVY)
                    .setTextAlignment(TextAlignment.CENTER));
            }
            if (employee.getCompany() != null && !employee.getCompany().isBlank()) {
                doc.add(new Paragraph(employee.getCompany())
                    .setFontSize(13).setFontColor(SLATE)
                    .setTextAlignment(TextAlignment.CENTER));
            }
            if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
                doc.add(new Paragraph(employee.getEmail())
                    .setFontSize(11).setFontColor(SLATE)
                    .setTextAlignment(TextAlignment.CENTER));
            }
        }

        doc.add(new Paragraph("Generated " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
            .setFontSize(10).setFontColor(SLATE).setItalic()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40));

        // Table of contents hint
        doc.add(new Paragraph("\nContents")
            .setFontSize(12).setBold().setFontColor(NAVY)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(40));
        String[] sections = {"1. Income Summary", "2. Expense Summary", "3. CCA Schedule", "4. Kilometre Summary", "5. Annual Net Summary"};
        for (String s : sections) {
            doc.add(new Paragraph(s)
                .setFontSize(11).setFontColor(SLATE)
                .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private static void addSectionHeader(Document doc, String title, String subtitle, DeviceRgb color, int year) {
        Table header = makeTable(new float[]{1});
        Cell cell = new Cell()
            .setBackgroundColor(color)
            .setPadding(16)
            .setBorder(Border.NO_BORDER)
            .add(new Paragraph(title).setFontSize(20).setBold().setFontColor(WHITE).setMarginBottom(2))
            .add(new Paragraph(subtitle + "  ·  " + year).setFontSize(11).setFontColor(WHITE));
        header.addCell(cell);
        doc.add(header);
        doc.add(new Paragraph("\n").setMarginBottom(4));
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private static Table makeTable(float[] widths) {
        Table t = new Table(UnitValue.createPercentArray(widths));
        t.setWidth(UnitValue.createPercentValue(100));
        t.setMarginBottom(4);
        return t;
    }

    private static void addHeaderRow(Table t, String[] labels) {
        for (String label : labels) {
            t.addHeaderCell(new Cell()
                .add(new Paragraph(label).setBold().setFontColor(WHITE).setFontSize(10))
                .setBackgroundColor(NAVY)
                .setPadding(6)
                .setBorder(Border.NO_BORDER));
        }
    }

    private static void addDataRow(Table t, boolean alt, TextAlignment align, String value) {
        t.addCell(new Cell()
            .add(new Paragraph(value).setFontSize(9).setFontColor(NAVY).setTextAlignment(align))
            .setBackgroundColor(alt ? LIGHT : WHITE)
            .setPadding(5)
            .setBorder(Border.NO_BORDER)
            .setBorderBottom(new SolidBorder(new DeviceRgb(226, 232, 240), 0.5f)));
    }

    private static void addTotalRow(Table t, String[] values) {
        boolean first = true;
        for (String v : values) {
            TextAlignment align = first ? TextAlignment.LEFT : TextAlignment.RIGHT;
            t.addCell(new Cell()
                .add(new Paragraph(v).setBold().setFontSize(10).setFontColor(WHITE).setTextAlignment(align))
                .setBackgroundColor(NAVY)
                .setPadding(6)
                .setBorder(Border.NO_BORDER));
            first = false;
        }
    }

    private static void addSubtotalRow(Table t, String label, String value, DeviceRgb textColor, DeviceRgb bgColor) {
        t.addCell(new Cell()
            .add(new Paragraph(label).setFontSize(11).setBold().setFontColor(textColor))
            .setBackgroundColor(bgColor).setPadding(10).setBorder(Border.NO_BORDER));
        t.addCell(new Cell()
            .add(new Paragraph(value).setFontSize(12).setBold().setFontColor(textColor).setTextAlignment(TextAlignment.RIGHT))
            .setBackgroundColor(bgColor).setPadding(10).setBorder(Border.NO_BORDER));
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

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

    private static String fmtDollar(double v) {
        return String.format("$%.2f", v);
    }

    private static String fmt2(double v) {
        return String.format("%.2f", v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

}
