package com.github.shanebeee.et.util;

import com.github.shanebeee.et.model.ExpenseCategory;
import com.github.shanebeee.et.model.Expenditure;
import com.github.shanebeee.et.model.KmOdometer;
import com.github.shanebeee.et.model.KmTrip;
import com.github.shanebeee.et.storage.DataStorage;

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

        double businessKm = trips.stream().mapToDouble(KmTrip::getKm).sum();
        double totalKm    = odometer.totalKm();
        double pct        = (totalKm > 0 && businessKm > 0)
            ? Math.min(100.0, businessKm / totalKm * 100.0) : 0.0;

        // Ensure venv with openpyxl
        String venvDir    = System.getProperty("user.home") + File.separator
            + "ShaneApps" + File.separator + "EmployeeTimesheet" + File.separator + "python_venv";
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

        // ── Sheet 1: Expenses ─────────────────────────────────────────────────
        // 5 columns: Date | Description | Subtotal ($) | GST ($) | Total ($)
        py.append("ws = wb.active\n");
        py.append("ws.title = 'Expenses'\n");
        py.append(String.format("ws['A1'] = 'Expenses \u2014 %d'\n", year));
        py.append("ws['A1'].font = Font(name='Arial', bold=True, size=14, color='1E3A5F')\n");
        py.append("ws.merge_cells('A1:E1')\n");
        py.append("ws['A1'].alignment = Alignment(horizontal='left')\n\n");

        // Employee info rows below title
        com.github.shanebeee.et.model.EmployeeInfo info = storage.loadEmployeeInfo();
        int infoRow = 2;
        if (info.getFullName() != null && !info.getFullName().isBlank()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',bold=True,size=11,color='1E3A5F')\n", infoRow, pyStr(info.getFullName())));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow));
            infoRow++;
        }
        if (info.getCompany() != null && !info.getCompany().isBlank()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(info.getCompany())));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow));
            infoRow++;
        }
        if (info.getAddress() != null && !info.getAddress().isBlank()) {
            String addr = info.getAddress()
                + (info.getAddress2() != null && !info.getAddress2().isBlank() ? ", " + info.getAddress2() : "");
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(addr)));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow));
            infoRow++;
        }
        // Phone + email on one row
        String contactLine = "";
        if (info.getPhoneNumber() != null && !info.getPhoneNumber().isBlank()) contactLine += info.getPhoneNumber();
        if (info.getEmail()       != null && !info.getEmail().isBlank()) {
            if (!contactLine.isEmpty()) contactLine += "   |   ";
            contactLine += info.getEmail();
        }
        if (!contactLine.isEmpty()) {
            py.append(String.format("ws.cell(row=%d,column=1,value=%s).font=Font(name='Arial',size=10,color='64748B')\n", infoRow, pyStr(contactLine)));
            py.append(String.format("ws.merge_cells('A%d:E%d')\n", infoRow, infoRow));
            infoRow++;
        }

        // Blank separator row, then column headers
        int headerRow = infoRow + 1;
        py.append(String.format("\n# Column headers at row %d\n", headerRow));
        py.append(String.format("for ci,h in enumerate(['Date','Description','Subtotal ($)','GST ($)','Total ($)'],1):\n"));
        py.append(String.format("  c=ws.cell(row=%d,column=ci,value=h)\n", headerRow));
        py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");
        int row = headerRow + 1;
        List<Integer> subtotalRows = new ArrayList<>();

        // Group by category
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

            // Category header row — merged A:E, coloured, "LABEL — Line XXXX"
            String hex = cat.getColor() != null ? cat.getColor().replace("#", "") : "94A3B8";
            String catLabel = cat.getLabel().toUpperCase()
                + (cat.getT2125Line() != null && !cat.getT2125Line().isBlank()
                   ? " \u2014 Line " + cat.getT2125Line() : "");
            py.append(String.format("cf=PatternFill('solid',start_color='%s')\n", hex));
            py.append(String.format("c=ws.cell(row=%d,column=1,value=%s)\n", row, pyStr(catLabel)));
            py.append(String.format("ws.merge_cells(start_row=%d,start_column=1,end_row=%d,end_column=5)\n", row, row));
            py.append(String.format("c.font=Font(name='Arial',bold=True,size=10,color='FFFFFF');c.fill=cf;c.border=border\n"));
            py.append(String.format("for ci in range(2,6):\n  ws.cell(row=%d,column=ci).fill=cf\n  ws.cell(row=%d,column=ci).border=border\n", row, row));
            row++;

            // Data rows — 5 columns
            int dataStart = row;
            int ri = 0;
            for (Expenditure e : catExps) {
                boolean alt = (ri % 2 == 1);
                py.append(String.format("ws.cell(row=%d,column=1,value='%s')\n", row, e.getDate()));
                py.append(String.format("ws.cell(row=%d,column=2,value=%s)\n",   row, pyStr(escape(e.getDescription()))));
                py.append(String.format("ws.cell(row=%d,column=3,value=%.2f).number_format='$#,##0.00'\n", row, e.getSubtotal()));
                py.append(String.format("ws.cell(row=%d,column=4,value=%.2f).number_format='$#,##0.00'\n", row, e.getGst()));
                py.append(String.format("ws.cell(row=%d,column=5,value=%.2f).number_format='$#,##0.00'\n", row, e.getTotal()));
                py.append(String.format("for ci in range(1,6):\n  c=ws.cell(row=%d,column=ci);c.font=data_font;c.border=border%s\n",
                    row, alt ? ";c.fill=alt_fill" : ""));
                row++; ri++;
            }
            int dataEnd = row - 1;

            // Subtotal row
            py.append(String.format("ws.cell(row=%d,column=1,value='Subtotal \u2014 %s')\n", row, cat.getLabel()));
            py.append(String.format("ws.cell(row=%d,column=3,value='=SUM(C%d:C%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("ws.cell(row=%d,column=4,value='=SUM(D%d:D%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("ws.cell(row=%d,column=5,value='=SUM(E%d:E%d)').number_format='$#,##0.00'\n", row, dataStart, dataEnd));
            py.append(String.format("for ci in range(1,6):\n  c=ws.cell(row=%d,column=ci);c.font=sub_font;c.fill=sub_fill;c.border=border\n", row));
            subtotalRows.add(row);
            row += 2; // subtotal + blank spacer
        }

        // Grand total row
        if (!subtotalRows.isEmpty()) {
            String cRef = subtotalRows.stream().map(r -> "C" + r).collect(Collectors.joining("+"));
            String dRef = subtotalRows.stream().map(r -> "D" + r).collect(Collectors.joining("+"));
            String eRef = subtotalRows.stream().map(r -> "E" + r).collect(Collectors.joining("+"));
            py.append(String.format("ws.cell(row=%d,column=1,value='GRAND TOTAL')\n", row));
            py.append(String.format("ws.cell(row=%d,column=3,value='=%s').number_format='$#,##0.00'\n", row, cRef));
            py.append(String.format("ws.cell(row=%d,column=4,value='=%s').number_format='$#,##0.00'\n", row, dRef));
            py.append(String.format("ws.cell(row=%d,column=5,value='=%s').number_format='$#,##0.00'\n", row, eRef));
            py.append(String.format("for ci in range(1,6):\n  c=ws.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", row));
        }

        // Column widths (A=Date, B=Description, C=Subtotal, D=GST, E=Total)
        py.append("for col,w in zip('ABCDE',[14,44,14,12,14]):\n  ws.column_dimensions[col].width=w\n");
        py.append(String.format("ws.row_dimensions[%d].height=20\n", headerRow));
        py.append(String.format("ws.freeze_panes='A%d'\n\n", row));

        // ── Sheet 2: Kilometre Log ────────────────────────────────────────────
        py.append("ws2 = wb.create_sheet('Kilometre Log')\n");
        py.append(String.format("ws2['A1']='Kilometre Log \u2014 %d'\n", year));
        py.append("ws2['A1'].font=Font(name='Arial',bold=True,size=14,color='1E3A5F')\n");
        py.append("ws2.merge_cells('A1:D1')\n\n");

        py.append("ws2['A3']='SUMMARY'\n");
        py.append("ws2['A3'].font=Font(name='Arial',bold=True,size=10,color='64748B')\n");
        py.append(String.format("ws2['A4']='Start Odometer (km)';ws2['B4']=%.0f\n", odometer.getStartKm()));
        py.append(String.format("ws2['A5']='End Odometer (km)';  ws2['B5']=%.0f\n", odometer.getEndKm()));
        py.append(String.format("ws2['A6']='Total KM (year)';    ws2['B6']=%.1f\n", totalKm));
        py.append(String.format("ws2['A7']='Business KM';        ws2['B7']=%.1f\n", businessKm));
        py.append(String.format("ws2['A8']='Business Use %%';    ws2['B8']=%.1f\n", pct));
        py.append("ws2['B8'].number_format='0.0\"%\"'\n");
        py.append("for r in range(4,9):\n");
        py.append("  ws2.cell(row=r,column=1).font=Font(name='Arial',size=10,color='64748B')\n");
        py.append("  ws2.cell(row=r,column=2).font=Font(name='Arial',bold=True,size=10)\n\n");

        py.append("for ci,h in enumerate(['Date','Distance (km)','Note / Purpose','Source'],1):\n");
        py.append("  c=ws2.cell(row=10,column=ci,value=h)\n");
        py.append("  c.font=hdr_font;c.fill=hdr_fill;c.alignment=hdr_align;c.border=border\n\n");

        int kmRow = 11; int ki = 0;
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
            py.append(String.format("ws2.cell(row=%d,column=2,value='=SUM(B11:B%d)').number_format='#,##0.0'\n", kmRow, kmRow-1));
            py.append(String.format("for ci in range(1,5):\n  c=ws2.cell(row=%d,column=ci);c.font=tot_font;c.fill=tot_fill;c.border=border\n", kmRow));
        }
        py.append("for col,w in zip('ABCD',[14,16,44,18]):\n  ws2.column_dimensions[col].width=w\n");
        py.append("ws2.row_dimensions[10].height=20\n");
        py.append("ws2.freeze_panes='A11'\n\n");

        // Save
        py.append(String.format("wb.save(r'%s')\n", outputPath.replace("\\", "\\\\")));
        py.append("print('ok')\n");

        File script = File.createTempFile("et_export_", ".py");
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
