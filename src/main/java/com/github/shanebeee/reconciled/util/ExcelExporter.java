package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.ExpenseCategory;
import com.github.shanebeee.reconciled.model.Expenditure;
import com.github.shanebeee.reconciled.model.KmOdometer;
import com.github.shanebeee.reconciled.model.KmTrip;
import com.github.shanebeee.reconciled.storage.DataStorage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExcelExporter {

    private final DataStorage storage;

    public ExcelExporter(DataStorage storage) {
        this.storage = storage;
    }

    public File export(int year, String outputPath) throws IOException, InterruptedException {
        List<Expenditure>     expenses = storage.loadExpenditures(String.valueOf(year));
        expenses.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        List<ExpenseCategory> cats     = storage.loadExpenseCategories(String.valueOf(year));
        List<KmTrip>          trips    = storage.loadKmTrips(String.valueOf(year));
        trips.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        KmOdometer odometer = storage.loadKmOdometer(String.valueOf(year));
        List<com.github.shanebeee.reconciled.model.Boss> allBosses = storage.loadBosses();
        List<com.github.shanebeee.reconciled.model.Boss> selfEmployedBosses = allBosses.stream()
            .filter(com.github.shanebeee.reconciled.model.Boss::isSelfEmployed).toList();
        List<com.github.shanebeee.reconciled.model.CcaAsset> ccaAssets = storage.loadCcaAssets();

        double businessKm = trips.stream().mapToDouble(KmTrip::getKm).sum();
        double totalKm    = odometer.totalKm();
        double pct        = (totalKm > 0 && businessKm > 0)
            ? Math.min(100.0, businessKm / totalKm * 100.0) : 0.0;

        // Ensure venv with openpyxl
        String venvDir    = System.getProperty("user.home") + File.separator
            + "ShaneApps" + File.separator + "Reconciled" + File.separator + "python_venv";
        String venvPython = venvDir + File.separator + "bin" + File.separator + "python3";
        if (!new File(venvPython).exists()) run(new String[]{"python3", "-m", "venv", venvDir});
        String marker = venvDir + File.separator + "lib_marker_openpyxl";
        if (!new File(marker).exists()) {
            run(new String[]{venvPython, "-m", "pip", "install", "openpyxl", "--quiet"});
            new File(marker).createNewFile();
        }

        StringBuilder py = new StringBuilder();
        py.append("from openpyxl import Workbook\n");
        py.append("from openpyxl.styles import Font, PatternFill, Alignment, Border, Side\n");
        py.append("wb = Workbook()\n\n");

        // ── Shared styles ─────────────────────────────────────────────────────
        py.append("hdr_font   = Font(name='Arial', bold=True, color='FFFFFF', size=11)\n");
        py.append("hdr_fill   = PatternFill('solid', start_color='1E3A5F')\n");
        py.append("hdr_align  = Alignment(horizontal='center', vertical='center')\n");
        py.append("thin       = Side(style='thin', color='D1D5DB')\n");
        py.append("border     = Border(left=thin, right=thin, top=thin, bottom=thin)\n");
        py.append("alt_fill   = PatternFill('solid', start_color='F8FAFC')\n");
        py.append("sub_font   = Font(name='Arial', bold=True, size=10)\n");
        py.append("sub_fill   = PatternFill('solid', start_color='DBEAFE')\n");
        py.append("tot_fill   = PatternFill('solid', start_color='1E3A5F')\n");
        py.append("tot_font   = Font(name='Arial', bold=True, size=11, color='FFFFFF')\n");
        py.append("data_font  = Font(name='Arial', size=10)\n\n");

        // ── Load employee info once (reused by all sheets) ────────────────────
        com.github.shanebeee.reconciled.model.EmployeeInfo info = storage.loadEmployeeInfo();
        String addr = info.getAddress() != null && !info.getAddress().isBlank()
            ? info.getAddress() + (info.getAddress2() != null && !info.getAddress2().isBlank()
                ? ", " + info.getAddress2() : "")
            : null;
        String contactLine = "";
        if (info.getPhoneNumber() != null && !info.getPhoneNumber().isBlank()) contactLine += info.getPhoneNumber();
        if (info.getEmail() != null && !info.getEmail().isBlank()) {
            if (!contactLine.isEmpty()) contactLine += "   |   ";
            contactLine += info.getEmail();
        }

        // ── Sheet 1: Expenses ─────────────────────────────────────────────────
        py.append("ws = wb.active\n");
        py.append("ws.title = 'Expenses'\n");
        py.append(String.format("ws['A1'] = 'Expenses \u2014 %d'\n", year));
        py.append("ws['A1'].font = Font(name='Arial', bold=True, size=14, color='1E3A5F')\n");
        py.append("ws.merge_cells('A1:E1')\n");
        py.append("ws['A1'].alignment = Alignment(horizontal='left')\n\n");

        int infoRow = 2;
        if (info.getFullName() != null && !info.getFullName().isBlank()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", infoRow, pyStr(info.getFullName())));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow)); infoRow++;
        }
        if (info.getCompany() != null && !info.getCompany().isBlank()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(info.getCompany())));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow)); infoRow++;
        }
        if (addr != null) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(addr)));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow)); infoRow++;
        }
        if (!contactLine.isEmpty()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(contactLine)));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow)); infoRow++;
        }

        int headerRow = infoRow + 1;
        py.append(String.format("for ci,h in enumerate(['Date','Description','Category','Subtotal ($)','GST ($)','Total ($)','Claimable ($)'],1):\n"));
        py.append(String.format("  c=ws.cell(row=%d,column=ci,value=h)\n", headerRow));
        py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");
        int row = headerRow + 1;
        List<Integer> subtotalRows = new ArrayList<>();

        Map<String, List<Expenditure>> byCat = new LinkedHashMap<>();
        for (ExpenseCategory cat : cats) byCat.put(cat.getId(), new ArrayList<>());
        byCat.put("__none__", new ArrayList<>());
        for (Expenditure e : expenses) {
            ExpenseCategory ec = storage.resolveCategory(e, cats);
            byCat.computeIfAbsent(ec != null ? ec.getId() : "__none__", k -> new ArrayList<>()).add(e);
        }

        for (ExpenseCategory cat : cats) {
            List<Expenditure> catExps = byCat.getOrDefault(cat.getId(), List.of());
            if (catExps.isEmpty()) continue;
            String hex = cat.getColor() != null ? cat.getColor().replace("#", "") : "94A3B8";
            String pctStr  = new DeductionCalculator(storage).percentLabel(cat, year);
            String catLabel = cat.getLabel().toUpperCase()
                + (cat.getT2125Line() != null && !cat.getT2125Line().isBlank()
                   ? " \u2014 Line " + cat.getT2125Line() : "")
                + " (" + pctStr + " claimable)";
            py.append(String.format("cf=PatternFill('solid',start_color='%s')\n", hex));
            py.append(String.format("c=ws.cell(row=%d,column=1,value=%s)\n", row, pyStr(catLabel)));
            py.append(String.format("ws.merge_cells(start_row=%d,start_column=1,end_row=%d,end_column=7)\n", row, row));
            py.append(String.format("c.font=Font(name='Arial',bold=True,size=10,color='FFFFFF');c.fill=cf;c.border=border\n"));
            py.append(String.format("for ci in range(2,8):\n  ws.cell(row=%d,column=ci).fill=cf\n  ws.cell(row=%d,column=ci).border=border\n", row, row));
            row++;
            int dataStart = row; int ri = 0;
            for (Expenditure e : catExps) {
                boolean alt = (ri % 2 == 1);
                double claimable = e.getTotal() * new DeductionCalculator(storage).percentFor(cat, year);
                py.append(String.format("ws.cell(row=%d,column=1,value='%s')\n", row, e.getDate()));
                py.append(String.format("ws.cell(row=%d,column=2,value=%s)\n",   row, pyStr(escape(e.getDescription()))));
                py.append(String.format("ws.cell(row=%d,column=3,value=%s)\n",   row, pyStr(cat.getLabel())));
                py.append(String.format("ws.cell(row=%d,column=4,value=%.2f).number_format='$#,##0.00'\n", row, e.getSubtotal()));
                py.append(String.format("ws.cell(row=%d,column=5,value=%.2f).number_format='$#,##0.00'\n", row, e.getGst()));
                py.append(String.format("ws.cell(row=%d,column=6,value=%.2f).number_format='$#,##0.00'\n", row, e.getTotal()));
                py.append(String.format("ws.cell(row=%d,column=7,value=%.2f).number_format='$#,##0.00'\n", row, claimable));
                py.append(String.format("for ci in range(1,8):\n  c=ws.cell(row=%d,column=ci);c.font=data_font;c.border=border%s\n",
                    row, alt ? ";c.fill=alt_fill" : ""));
                row++; ri++;
            }
            int dataEnd = row - 1;
            py.append(String.format("ws.cell(row=%d,column=1,value='Subtotal \u2014 %s')\n", row, cat.getLabel()));
            py.append(String.format("ws.cell(row=%d,column=4,value='=SUM(D%d:D%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("ws.cell(row=%d,column=5,value='=SUM(E%d:E%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("ws.cell(row=%d,column=6,value='=SUM(F%d:F%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("ws.cell(row=%d,column=7,value='=SUM(G%d:G%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("for ci in range(1,8):\n  c=ws.cell(row=%d,column=ci);c.font=sub_font;c.fill=sub_fill;c.border=border\n", row));
            subtotalRows.add(row);
            row += 2;
        }
        if (!subtotalRows.isEmpty()) {
            String dRef = subtotalRows.stream().map(r -> "D" + r).collect(Collectors.joining("+"));
            String eRef = subtotalRows.stream().map(r -> "E" + r).collect(Collectors.joining("+"));
            String fRef = subtotalRows.stream().map(r -> "F" + r).collect(Collectors.joining("+"));
            String gRef = subtotalRows.stream().map(r -> "G" + r).collect(Collectors.joining("+"));
            py.append(String.format("ws.cell(row=%d,column=1,value='GRAND TOTAL')\n", row));
            py.append(String.format("ws.cell(row=%d,column=4,value='=%s').number_format='$#,##0.00'\n", row, dRef));
            py.append(String.format("ws.cell(row=%d,column=5,value='=%s').number_format='$#,##0.00'\n", row, eRef));
            py.append(String.format("ws.cell(row=%d,column=6,value='=%s').number_format='$#,##0.00'\n", row, fRef));
            py.append(String.format("ws.cell(row=%d,column=7,value='=%s').number_format='$#,##0.00'\n", row, gRef));
            py.append(String.format("for ci in range(1,8):\n  c=ws.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", row));
        }
        py.append("for col,w in zip('ABCDEFG',[14,44,20,14,14,12,14]):\n  ws.column_dimensions[col].width=w\n");
        py.append(String.format("ws.row_dimensions[%d].height=20\n", headerRow));
        py.append(String.format("ws.freeze_panes='A%d'\n\n", row));

        // ── Sheet 2: Kilometre Log ────────────────────────────────────────────
        py.append("ws2 = wb.create_sheet('Kilometre Log')\n");
        py.append(String.format("ws2['A1']='Kilometre Log \u2014 %d'\n", year));
        py.append("ws2['A1'].font=Font(name='Arial',bold=True,size=14,color='1E3A5F')\n");
        py.append("ws2.merge_cells('A1:D1')\n\n");

        int kmInfoRow = 2;
        if (info.getFullName() != null && !info.getFullName().isBlank()) {
            py.append(String.format("ws2.cell(row=%d,column=1,value=%s).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", kmInfoRow, pyStr(info.getFullName())));
            py.append(String.format("ws2.merge_cells('A%d:D%d')\n", kmInfoRow, kmInfoRow)); kmInfoRow++;
        }
        if (info.getCompany() != null && !info.getCompany().isBlank()) {
            py.append(String.format("ws2.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", kmInfoRow, pyStr(info.getCompany())));
            py.append(String.format("ws2.merge_cells('A%d:D%d')\n", kmInfoRow, kmInfoRow)); kmInfoRow++;
        }
        if (addr != null) {
            py.append(String.format("ws2.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", kmInfoRow, pyStr(addr)));
            py.append(String.format("ws2.merge_cells('A%d:D%d')\n", kmInfoRow, kmInfoRow)); kmInfoRow++;
        }
        if (!contactLine.isEmpty()) {
            py.append(String.format("ws2.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", kmInfoRow, pyStr(contactLine)));
            py.append(String.format("ws2.merge_cells('A%d:D%d')\n", kmInfoRow, kmInfoRow)); kmInfoRow++;
        }
        int kmSummaryRow = kmInfoRow + 1;

        py.append(String.format("ws2.cell(row=%d,column=1,value='SUMMARY').font=Font(name='Arial',bold=True,size=10,color='64748B')\n", kmSummaryRow));
        py.append(String.format("ws2.cell(row=%d,column=1,value='Start Odometer (km)');ws2.cell(row=%d,column=2,value=%.0f)\n", kmSummaryRow+1, kmSummaryRow+1, odometer.getStartKm()));
        py.append(String.format("ws2.cell(row=%d,column=1,value='End Odometer (km)');  ws2.cell(row=%d,column=2,value=%.0f)\n", kmSummaryRow+2, kmSummaryRow+2, odometer.getEndKm()));
        py.append(String.format("ws2.cell(row=%d,column=1,value='Total KM (year)');    ws2.cell(row=%d,column=2,value=%.1f)\n", kmSummaryRow+3, kmSummaryRow+3, totalKm));
        py.append(String.format("ws2.cell(row=%d,column=1,value='Business KM');        ws2.cell(row=%d,column=2,value=%.1f)\n", kmSummaryRow+4, kmSummaryRow+4, businessKm));
        py.append(String.format("ws2.cell(row=%d,column=1,value='Business Use %%%%');    ws2.cell(row=%d,column=2,value=%.1f)\n", kmSummaryRow+5, kmSummaryRow+5, pct));
        py.append(String.format("ws2.cell(row=%d,column=2).number_format='0.0\"%%\"'\n", kmSummaryRow+5));
        py.append(String.format("for r in range(%d,%d):\n", kmSummaryRow+1, kmSummaryRow+6));
        py.append("  ws2.cell(row=r,column=1).font=Font(name='Arial',size=10,color='64748B')\n");
        py.append("  ws2.cell(row=r,column=2).font=Font(name='Arial',bold=True,size=10)\n\n");

        int kmHeaderRow = kmSummaryRow + 7;
        py.append(String.format("for ci,h in enumerate(['Date','Distance (km)','Note / Purpose','Source'],1):\n"));
        py.append(String.format("  c=ws2.cell(row=%d,column=ci,value=h)\n", kmHeaderRow));
        py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");

        int kmRow = kmHeaderRow + 1; int ki = 0;
        for (KmTrip t : trips) {
            boolean alt = (ki % 2 == 1);
            String src = t.getSourceLogId() != null ? "Auto (Work Log)" : "Manual";
            py.append(String.format("ws2.cell(row=%d,column=1,value='%s')\n",  kmRow, t.getDate()));
            py.append(String.format("ws2.cell(row=%d,column=2,value=%.1f).number_format='#,##0.0'\n", kmRow, t.getKm()));
            py.append(String.format("ws2.cell(row=%d,column=3,value=%s)\n",   kmRow, pyStr(escape(t.getNote()))));
            py.append(String.format("ws2.cell(row=%d,column=4,value=%s)\n",   kmRow, pyStr(src)));
            py.append(String.format("for ci in range(1,5):\n  c=ws2.cell(row=%d,column=ci);c.font=data_font;c.border=border%s\n",
                kmRow, alt ? ";c.fill=alt_fill" : ""));
            kmRow++; ki++;
        }
        if (!trips.isEmpty()) {
            py.append(String.format("ws2.cell(row=%d,column=1,value='TOTAL')\n", kmRow));
            py.append(String.format("ws2.cell(row=%d,column=2,value='=SUM(B%d:B%d)').number_format='#,##0.0'\n", kmRow, kmHeaderRow+1, kmRow-1));
            py.append(String.format("for ci in range(1,5):\n  c=ws2.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", kmRow));
        }
        py.append("for col,w in zip('ABCD',[14,16,44,18]):\n  ws2.column_dimensions[col].width=w\n");
        py.append(String.format("ws2.row_dimensions[%d].height=20\n", kmHeaderRow));
        py.append(String.format("ws2.freeze_panes='A%d'\n\n", kmHeaderRow + 1));

        // ── Sheet 3: Income Summary ───────────────────────────────────────────
        if (!selfEmployedBosses.isEmpty()) {
            py.append("ws3 = wb.create_sheet('Income Summary')\n");
            py.append(String.format("ws3['A1']='Income Summary \u2014 %d'\n", year));
            py.append("ws3['A1'].font=Font(name='Arial',bold=True,size=14,color='1E3A5F')\n");
            py.append("ws3.merge_cells('A1:G1')\n\n");

            int iRow = 2;
            if (info.getFullName() != null && !info.getFullName().isBlank()) {
                py.append(String.format("ws3.cell(row=%d,column=1,value=%s).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", iRow, pyStr(info.getFullName())));
                py.append(String.format("ws3.merge_cells('A%d:G%d')\n", iRow, iRow)); iRow++;
            }
            if (info.getCompany() != null && !info.getCompany().isBlank()) {
                py.append(String.format("ws3.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", iRow, pyStr(info.getCompany())));
                py.append(String.format("ws3.merge_cells('A%d:G%d')\n", iRow, iRow)); iRow++;
            }
            if (addr != null) {
                py.append(String.format("ws3.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", iRow, pyStr(addr)));
                py.append(String.format("ws3.merge_cells('A%d:G%d')\n", iRow, iRow)); iRow++;
            }
            if (!contactLine.isEmpty()) {
                py.append(String.format("ws3.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", iRow, pyStr(contactLine)));
                py.append(String.format("ws3.merge_cells('A%d:G%d')\n", iRow, iRow)); iRow++;
            }
            iRow++; // blank separator

            py.append(String.format("for ci,h in enumerate(['Boss','Company','Total Hours','Total KMs','Total Extras ($)','Gross Income ($)','GST Collected ($)'],1):\n"));
            py.append(String.format("  c=ws3.cell(row=%d,column=ci,value=h)\n", iRow));
            py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");
            iRow++;

            int incDataStart = iRow;
            int bi = 0;
            for (com.github.shanebeee.reconciled.model.Boss boss : selfEmployedBosses) {
                double totalHours = 0, totalKmsBilled = 0, totalExtras = 0;
                for (int m = 1; m <= 12; m++) {
                    String monthKey = String.format("%d-%02d", year, m);
                    List<com.github.shanebeee.reconciled.model.LogEntry> logs = storage.loadLogs(monthKey);
                    for (com.github.shanebeee.reconciled.model.LogEntry entry : logs) {
                        if (!boss.getId().equals(entry.getBossUuid()) && !boss.getName().equals(entry.getBossUuid())) {
                            if (entry.getBossPercentages() != null && entry.getBossPercentages().containsKey(boss.getId())) {
                                double pct2 = entry.getBossPercentages().get(boss.getId()) / 100.0;
                                if (entry.getType() == com.github.shanebeee.reconciled.model.LogEntry.EntryType.TIME
                                    && entry.getStartTime() != null && entry.getEndTime() != null) {
                                    try {
                                        long mins = java.time.Duration.between(
                                            java.time.LocalTime.parse(entry.getStartTime()),
                                            java.time.LocalTime.parse(entry.getEndTime())).toMinutes();
                                        totalHours += (mins / 60.0) * pct2;
                                    } catch (Exception ignored) {}
                                }
                            }
                            continue;
                        }
                        switch (entry.getType()) {
                            case TIME -> {
                                if (entry.getStartTime() != null && entry.getEndTime() != null) {
                                    try {
                                        long mins = java.time.Duration.between(
                                            java.time.LocalTime.parse(entry.getStartTime()),
                                            java.time.LocalTime.parse(entry.getEndTime())).toMinutes();
                                        totalHours += mins / 60.0;
                                    } catch (Exception ignored) {}
                                }
                            }
                            case KILOMETER -> { if (entry.getKilometers() != null) totalKmsBilled += entry.getKilometers(); }
                            case EXTRA -> { if (entry.getUnits() != null && entry.getCostPerUnit() != null) totalExtras += entry.getUnits() * entry.getCostPerUnit(); }
                        }
                    }
                }
                double grossIncome  = (totalHours * boss.getHourlyRate())
                    + (totalKmsBilled * (boss.getKmRate() != null ? boss.getKmRate() : 0)) + totalExtras;
                double gstCollected = grossIncome * (boss.getTaxRate() / 100.0);
                boolean alt = (bi % 2 == 1);
                py.append(String.format("ws3.cell(row=%d,column=1,value=%s)\n", iRow, pyStr(boss.getName())));
                py.append(String.format("ws3.cell(row=%d,column=2,value=%s)\n", iRow, pyStr(boss.getCompany())));
                py.append(String.format("ws3.cell(row=%d,column=3,value=%.2f).number_format='#,##0.00'\n", iRow, totalHours));
                py.append(String.format("ws3.cell(row=%d,column=4,value=%.1f).number_format='#,##0.0'\n",  iRow, totalKmsBilled));
                py.append(String.format("ws3.cell(row=%d,column=5,value=%.2f).number_format='$#,##0.00'\n", iRow, totalExtras));
                py.append(String.format("ws3.cell(row=%d,column=6,value=%.2f).number_format='$#,##0.00'\n", iRow, grossIncome));
                py.append(String.format("ws3.cell(row=%d,column=7,value=%.2f).number_format='$#,##0.00'\n", iRow, gstCollected));
                py.append(String.format("for ci in range(1,8):\n  c=ws3.cell(row=%d,column=ci);c.font=data_font;c.border=border%s\n",
                    iRow, alt ? ";c.fill=alt_fill" : ""));
                iRow++; bi++;
            }
            int incDataEnd = iRow - 1;
            py.append(String.format("ws3.cell(row=%d,column=1,value='TOTAL GROSS INCOME')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=3,value='=SUM(C%d:C%d)').number_format='#,##0.00'\n", iRow, incDataStart, incDataEnd));
            py.append(String.format("ws3.cell(row=%d,column=4,value='=SUM(D%d:D%d)').number_format='#,##0.0'\n",  iRow, incDataStart, incDataEnd));
            py.append(String.format("ws3.cell(row=%d,column=5,value='=SUM(E%d:E%d)').number_format='$#,##0.00'\n", iRow, incDataStart, incDataEnd));
            py.append(String.format("ws3.cell(row=%d,column=6,value='=SUM(F%d:F%d)').number_format='$#,##0.00'\n", iRow, incDataStart, incDataEnd));
            py.append(String.format("ws3.cell(row=%d,column=7,value='=SUM(G%d:G%d)').number_format='$#,##0.00'\n", iRow, incDataStart, incDataEnd));
            py.append(String.format("for ci in range(1,8):\n  c=ws3.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", iRow));
            int totalRow = iRow;
            iRow += 3;

            double totalGstPaid = expenses.stream().mapToDouble(Expenditure::getGst).sum();
            py.append(String.format("ws3.cell(row=%d,column=1,value='GST RECONCILIATION').font=Font(name='Arial',bold=True,size=10,color='64748B')\n", iRow)); iRow++;
            py.append(String.format("ws3.cell(row=%d,column=1,value='GST Collected (from clients)')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2,value='=G%d').number_format='$#,##0.00'\n", iRow, totalRow));
            py.append(String.format("ws3.cell(row=%d,column=1).font=Font(name='Arial',size=10,color='64748B')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2).font=Font(name='Arial',bold=True,size=10)\n", iRow)); iRow++;
            py.append(String.format("ws3.cell(row=%d,column=1,value='GST Paid on Expenses (ITCs)')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2,value=%.2f).number_format='$#,##0.00'\n", iRow, totalGstPaid));
            py.append(String.format("ws3.cell(row=%d,column=1).font=Font(name='Arial',size=10,color='64748B')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2).font=Font(name='Arial',bold=True,size=10)\n", iRow));
            int gstPaidRow = iRow; iRow++;
            py.append(String.format("ws3.cell(row=%d,column=1,value='Net GST Owing to CRA')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2,value='=B%d-B%d').number_format='$#,##0.00'\n", iRow, iRow-2, gstPaidRow));
            py.append(String.format("ws3.cell(row=%d,column=1).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", iRow));
            py.append(String.format("ws3.cell(row=%d,column=2).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", iRow));

            py.append("for col,w in zip('ABCDEFG',[22,22,14,12,16,16,16]):\n  ws3.column_dimensions[col].width=w\n");
            py.append("ws3.freeze_panes='A" + incDataStart + "'\n\n");
        }

        // ── Sheet 4: CCA Schedule ─────────────────────────────────────────────
        // 8 columns: Description | Purchased | Class | Rate | Cost | Opening UCC | Deduction | Closing UCC
        py.append("ws4 = wb.create_sheet('CCA Schedule')\n");
        py.append(String.format("ws4['A1']='CCA Schedule \u2014 %d'\n", year));
        py.append("ws4['A1'].font=Font(name='Arial',bold=True,size=14,color='1E3A5F')\n");
        py.append("ws4.merge_cells('A1:H1')\n\n");

        int ccaInfoRow = 2;
        if (info.getFullName() != null && !info.getFullName().isBlank()) {
            py.append(String.format("ws4.cell(row=%d,column=1,value=%s).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", ccaInfoRow, pyStr(info.getFullName())));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow)); ccaInfoRow++;
        }
        if (info.getCompany() != null && !info.getCompany().isBlank()) {
            py.append(String.format("ws4.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", ccaInfoRow, pyStr(info.getCompany())));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow)); ccaInfoRow++;
        }
        if (addr != null) {
            py.append(String.format("ws4.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", ccaInfoRow, pyStr(addr)));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow)); ccaInfoRow++;
        }
        if (!contactLine.isEmpty()) {
            py.append(String.format("ws4.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", ccaInfoRow, pyStr(contactLine)));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow)); ccaInfoRow++;
        }
        ccaInfoRow++; // blank separator

        py.append(String.format("for ci,h in enumerate(['Description','Purchased','Class','Rate','Cost ($)','Opening UCC ($)','Deduction ($)','Closing UCC ($)'],1):\n"));
        py.append(String.format("  c=ws4.cell(row=%d,column=ci,value=h)\n", ccaInfoRow));
        py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");
        ccaInfoRow++;

        int ccaDataStart = ccaInfoRow;
        if (ccaAssets.isEmpty()) {
            py.append(String.format("ws4.cell(row=%d,column=1,value='No CCA assets recorded.')\n", ccaInfoRow));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow));
            py.append(String.format("ws4.cell(row=%d,column=1).font=Font(name='Arial',italic=True,size=10,color='94A3B8')\n", ccaInfoRow));
        } else {
            int ci2 = 0;
            for (com.github.shanebeee.reconciled.model.CcaAsset a : ccaAssets) {
                double opening   = a.openingUccForYear(year);
                double deduction = a.deductionForYear(year);
                double closing   = a.closingUccForYear(year);
                boolean alt = (ci2 % 2 == 1);
                String rateStr = String.format("%.0f%%", a.getClassRate() * 100);
                py.append(String.format("ws4.cell(row=%d,column=1,value=%s)\n", ccaInfoRow, pyStr(a.getDescription())));
                py.append(String.format("ws4.cell(row=%d,column=2,value=%s)\n", ccaInfoRow, pyStr(a.getPurchaseDate())));
                py.append(String.format("ws4.cell(row=%d,column=3,value=%s)\n", ccaInfoRow, pyStr(a.getAssetClass())));
                py.append(String.format("ws4.cell(row=%d,column=4,value=%s)\n", ccaInfoRow, pyStr(rateStr)));
                py.append(String.format("ws4.cell(row=%d,column=5,value=%.2f).number_format='$#,##0.00'\n", ccaInfoRow, a.getCost()));
                py.append(String.format("ws4.cell(row=%d,column=6,value=%.2f).number_format='$#,##0.00'\n", ccaInfoRow, opening));
                py.append(String.format("ws4.cell(row=%d,column=7,value=%.2f).number_format='$#,##0.00'\n", ccaInfoRow, deduction));
                py.append(String.format("ws4.cell(row=%d,column=8,value=%.2f).number_format='$#,##0.00'\n", ccaInfoRow, closing));
                py.append(String.format("for ci in range(1,9):\n  c=ws4.cell(row=%d,column=ci);c.font=data_font;c.border=border%s\n",
                    ccaInfoRow, alt ? ";c.fill=alt_fill" : ""));
                ccaInfoRow++; ci2++;
            }
            int ccaDataEnd = ccaInfoRow - 1;
            py.append(String.format("ws4.cell(row=%d,column=1,value='TOTAL CCA DEDUCTION \u2014 %d')\n", ccaInfoRow, year));
            py.append(String.format("ws4.cell(row=%d,column=7,value='=SUM(G%d:G%d)').number_format='$#,##0.00'\n", ccaInfoRow, ccaDataStart, ccaDataEnd));
            py.append(String.format("for ci in range(1,9):\n  c=ws4.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", ccaInfoRow));
            ccaInfoRow += 2;

            // GST ITC block - only for assets purchased in this tax year
            double ccaGstThisYear = ccaAssets.stream()
                .filter(a -> a.getPurchaseYear() == year)
                .mapToDouble(a -> a.getCost() * 0.05)
                .sum();
            if (ccaGstThisYear > 0) {
                ccaInfoRow++;
                py.append(String.format("ws4.cell(row=%d,column=1,value='GST/HST ON CAPITAL ACQUISITIONS - %d')\n", ccaInfoRow, year));
                py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow));
                py.append(String.format("ws4.cell(row=%d,column=1).font=Font(name='Arial',bold=True,size=10,color='1E3A5F')\n", ccaInfoRow));
                ccaInfoRow++;
                py.append(String.format("ws4.cell(row=%d,column=1,value='GST paid on capital assets purchased in %d - claim as ITC on GST/HST return')\n", ccaInfoRow, year));
                py.append(String.format("ws4.cell(row=%d,column=8,value=%.2f).number_format='$#,##0.00'\n", ccaInfoRow, ccaGstThisYear));
                py.append("itc_fill=PatternFill('solid',start_color='DBEAFE')\n");
                py.append(String.format("for ci in range(1,9):\n  c=ws4.cell(row=%d,column=ci);c.font=data_font;c.fill=itc_fill;c.border=border\n", ccaInfoRow));
                ccaInfoRow++;
                py.append(String.format("ws4.cell(row=%d,column=1,value='Do not include GST in T2125 expenses. Claim separately on your %d GST/HST return.')\n", ccaInfoRow, year));
                py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow));
                py.append(String.format("ws4.cell(row=%d,column=1).font=Font(name='Arial',italic=True,size=9,color='94A3B8')\n", ccaInfoRow));
            }

            ccaInfoRow += 2;
            py.append(String.format("ws4.cell(row=%d,column=1,value='* Year of purchase uses the half-year rule (50%% of normal first-year rate per CRA).')\n", ccaInfoRow));
            py.append(String.format("ws4.merge_cells('A%d:H%d')\n", ccaInfoRow, ccaInfoRow));
            py.append(String.format("ws4.cell(row=%d,column=1).font=Font(name='Arial',italic=True,size=9,color='94A3B8')\n", ccaInfoRow));
        }
        py.append("for col,w in zip('ABCDEFGH',[36,13,12,8,14,16,14,14]):\n  ws4.column_dimensions[col].width=w\n");
        py.append(String.format("ws4.freeze_panes='A%d'\n\n", ccaDataStart));

        // Save
        py.append(String.format("wb.save(r'%s')\n", outputPath.replace("\\", "\\\\")));
        py.append("print('ok')\n");

        File script = File.createTempFile("reconciled_export_", ".py");
        try (FileWriter fw = new FileWriter(script)) { fw.write(py.toString()); }

        ProcessBuilder pb = new ProcessBuilder(venvPython, script.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        String out = new String(proc.getInputStream().readAllBytes());
        int exit = proc.waitFor();
        script.delete();
        if (exit != 0) throw new IOException("Python export failed:\n" + out);
        return new File(outputPath);
    }

    private void run(String[] cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();
        if (exit != 0) throw new IOException("Command failed: " + String.join(" ", cmd) + "\n" + out);
    }

    private static String pyStr(String s) {
        if (s == null || s.isBlank()) return "''";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String escape(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }
}
