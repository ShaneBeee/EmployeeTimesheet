package com.github.shanebeee.reconciled.view;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.Invoice;
import com.github.shanebeee.reconciled.model.LogEntry;
import com.github.shanebeee.reconciled.storage.DataStorage;
import com.github.shanebeee.reconciled.util.InvoiceGenerator;
import com.github.shanebeee.reconciled.util.InvoiceMailer;
import com.github.shanebeee.reconciled.util.SummaryGenerator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InvoicePanel extends JPanel {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter SHORT_FMT   = DateTimeFormatter.ofPattern("MMM d");

    private final DataStorage storage;
    private JComboBox<Boss> bossCombo;
    private JButton startBtn;
    private JButton endBtn;

    // History tab
    private DefaultTableModel historyModel;
    private JTable historyTable;
    private List<Invoice> invoiceCache = new ArrayList<>();

    public InvoicePanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // ── Page header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JLabel titleLabel = new JLabel("Invoice Management");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Tabs ─────────────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.addTab("Generate", buildGenerateTab());
        tabs.addTab("History", buildHistoryTab());

        // Refresh history whenever user switches to it
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) refreshHistory();
        });

        add(tabs, BorderLayout.CENTER);
    }

    // ── Generate tab ─────────────────────────────────────────────────────────

    private JPanel buildGenerateTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // ── Invoice Card ─────────────────────────────────────────────────────
        JPanel invoiceCard = makeCard();
        invoiceCard.setLayout(new BoxLayout(invoiceCard, BoxLayout.Y_AXIS));

        invoiceCard.add(makeSectionLabel("GENERATE INVOICE"));
        invoiceCard.add(makeDivider());

        bossCombo = new JComboBox<>();
        List<Boss> bosses = storage.loadBosses();
        for (Boss b : bosses) bossCombo.addItem(b);
        bossCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Boss b) setText(b.getName());
                return this;
            }
        });
        invoiceCard.add(makeFieldRow("Boss", bossCombo));
        invoiceCard.add(makeDivider());

        startBtn = makeDateButton(YearMonth.now().atDay(1).toString());
        endBtn   = makeDateButton(YearMonth.now().atEndOfMonth().toString());
        startBtn.addActionListener(e -> DatePicker.showPicker(InvoicePanel.this, startBtn));
        endBtn.addActionListener(e -> DatePicker.showPicker(InvoicePanel.this, endBtn));

        invoiceCard.add(makeFieldRow("Start Date", startBtn));
        invoiceCard.add(makeDivider());
        invoiceCard.add(makeFieldRow("End Date", endBtn));

        invoiceCard.add(Box.createVerticalStrut(12));
        JPanel invoiceBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        invoiceBtns.setOpaque(false);
        invoiceBtns.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnPreviewInvoice = new JButton("Preview");
        btnPreviewInvoice.putClientProperty("JButton.buttonType", "roundRect");
        btnPreviewInvoice.setBackground(new Color(241, 245, 249));
        btnPreviewInvoice.setForeground(new Color(30, 41, 59));

        JButton btnGenerate = new JButton("Generate Invoice");
        btnGenerate.putClientProperty("JButton.buttonType", "roundRect");
        btnGenerate.setBackground(new Color(59, 130, 246));
        btnGenerate.setForeground(Color.WHITE);

        JButton btnWithBreakdown = new JButton("With Breakdown");
        btnWithBreakdown.putClientProperty("JButton.buttonType", "roundRect");
        btnWithBreakdown.setBackground(new Color(99, 102, 241));
        btnWithBreakdown.setForeground(Color.WHITE);

        invoiceBtns.add(btnPreviewInvoice);
        invoiceBtns.add(btnGenerate);
        invoiceBtns.add(btnWithBreakdown);
        invoiceCard.add(invoiceBtns);
        invoiceCard.add(Box.createVerticalStrut(4));

        btnGenerate.addActionListener(e -> generate(InvoiceGenerator.InvoiceMode.STANDARD));
        btnWithBreakdown.addActionListener(e -> generate(InvoiceGenerator.InvoiceMode.WITH_BREAKDOWN));
        btnPreviewInvoice.addActionListener(e -> showInvoicePreview());

        content.add(invoiceCard);
        content.add(Box.createVerticalStrut(16));

        // ── Monthly Summary Card ──────────────────────────────────────────────
        JPanel summaryCard = makeCard();
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));

        summaryCard.add(makeSectionLabel("MONTHLY SUMMARY"));
        summaryCard.add(makeDivider());

        JPanel summaryDesc = new JPanel(new BorderLayout());
        summaryDesc.setOpaque(false);
        summaryDesc.setBorder(BorderFactory.createEmptyBorder(10, 0, 12, 0));
        summaryDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JLabel descLabel = new JLabel("Export a full monthly breakdown of all work logs and invoices.");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        descLabel.setForeground(new Color(100, 116, 139));
        summaryDesc.add(descLabel, BorderLayout.WEST);
        summaryCard.add(summaryDesc);

        JPanel summaryBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        summaryBtns.setOpaque(false);
        summaryBtns.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnPreview = new JButton("Preview Summary");
        btnPreview.putClientProperty("JButton.buttonType", "roundRect");
        btnPreview.setBackground(new Color(241, 245, 249));
        btnPreview.setForeground(new Color(30, 41, 59));

        JButton btnSummary = new JButton("Export Monthly Summary");
        btnSummary.putClientProperty("JButton.buttonType", "roundRect");
        btnSummary.setBackground((Color) UIManager.get("App.success"));
        btnSummary.setForeground(Color.WHITE);
        summaryBtns.add(btnPreview);
        summaryBtns.add(btnSummary);
        summaryCard.add(summaryBtns);
        summaryCard.add(Box.createVerticalStrut(4));

        btnPreview.addActionListener(e -> showSummaryPreview());
        btnSummary.addActionListener(e -> exportMonthlySummary());

        content.add(summaryCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    // ── History tab ──────────────────────────────────────────────────────────

    private JPanel buildHistoryTab() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Table
        String[] columns = {"#", "Boss", "Period", "Generated", "Status", "Amount"};
        historyModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(36);
        historyTable.setShowGrid(false);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.getTableHeader().setReorderingAllowed(false);
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        historyTable.getColumnModel().getColumn(0).setMaxWidth(50);   // #
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Generated
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Status
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(90);  // Amount

        // Status badge renderer
        historyTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = new JLabel();
                lbl.setOpaque(true);
                lbl.setHorizontalAlignment(CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
                if (value instanceof Invoice.Status status) {
                    lbl.setText(status.getLabel());
                    switch (status) {
                        case DRAFT -> { lbl.setBackground(new Color(241, 245, 249)); lbl.setForeground(new Color(100, 116, 139)); }
                        case SENT  -> { lbl.setBackground(new Color(219, 234, 254)); lbl.setForeground(new Color(37, 99, 235)); }
                        case PAID  -> { lbl.setBackground(new Color(209, 250, 229)); lbl.setForeground(new Color(5, 150, 105)); }
                    }
                }
                if (isSelected) {
                    lbl.setBackground(lbl.getBackground().darker());
                }
                return lbl;
            }
        });

        // Amount right-aligned
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        historyTable.getColumnModel().getColumn(5).setCellRenderer(rightAlign);

        JScrollPane tableScroll = new JScrollPane(historyTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        root.add(tableScroll, BorderLayout.CENTER);

        // ── Action buttons bar ────────────────────────────────────────────────
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnOpenPdf = new JButton("Open PDF");
        btnOpenPdf.putClientProperty("JButton.buttonType", "roundRect");
        btnOpenPdf.setBackground(new Color(241, 245, 249));
        btnOpenPdf.setForeground(new Color(30, 41, 59));

        JButton btnEmail = new JButton("✉ Email Invoice");
        btnEmail.putClientProperty("JButton.buttonType", "roundRect");
        btnEmail.setBackground(new Color(234, 231, 255));
        btnEmail.setForeground(new Color(99, 102, 241));

        JButton btnMarkSent = new JButton("Mark as Sent");
        btnMarkSent.putClientProperty("JButton.buttonType", "roundRect");
        btnMarkSent.setBackground(new Color(219, 234, 254));
        btnMarkSent.setForeground(new Color(37, 99, 235));

        JButton btnMarkPaid = new JButton("Mark as Paid");
        btnMarkPaid.putClientProperty("JButton.buttonType", "roundRect");
        btnMarkPaid.setBackground(new Color(209, 250, 229));
        btnMarkPaid.setForeground(new Color(5, 150, 105));

        JButton btnDelete = new JButton("Delete");
        btnDelete.putClientProperty("JButton.buttonType", "roundRect");
        btnDelete.setBackground(new Color(254, 226, 226));
        btnDelete.setForeground(new Color(220, 38, 38));

        bar.add(btnOpenPdf);
        bar.add(btnEmail);
        bar.add(btnMarkSent);
        bar.add(btnMarkPaid);
        bar.add(btnDelete);
        root.add(bar, BorderLayout.SOUTH);

        // ── Button actions ────────────────────────────────────────────────────
        btnOpenPdf.addActionListener(e -> {
            Invoice inv = selectedInvoice();
            if (inv == null) return;
            File pdf = new File(inv.getPdfPath());
            if (!pdf.exists()) {
                JOptionPane.showMessageDialog(this, "PDF not found at:\n" + inv.getPdfPath());
                return;
            }
            try { Desktop.getDesktop().open(pdf); } catch (IOException ex) { ex.printStackTrace(); }
        });

        btnEmail.addActionListener(e -> {
            Invoice inv = selectedInvoice();
            if (inv == null) return;
            // Find the boss for this invoice
            Boss boss = storage.loadBosses().stream()
                .filter(b -> b.getId().equals(inv.getBossId()))
                .findFirst().orElse(null);
            if (boss == null) {
                JOptionPane.showMessageDialog(this, "Could not find boss for this invoice.");
                return;
            }
            EmployeeInfo employee = storage.loadEmployeeInfo();
            try {
                InvoiceMailer.composeEmail(inv, boss, employee);
                if (inv.getStatus() == Invoice.Status.DRAFT) {
                    inv.setStatus(Invoice.Status.SENT);
                    inv.setSentDate(LocalDate.now().toString());
                    storage.updateInvoice(inv);
                    refreshHistory();
                }
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Email Invoice", JOptionPane.WARNING_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to open Mail:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnMarkSent.addActionListener(e -> {
            Invoice inv = selectedInvoice();
            if (inv == null) return;
            if (inv.getStatus() == Invoice.Status.PAID) {
                JOptionPane.showMessageDialog(this, "This invoice is already marked as Paid.");
                return;
            }
            String date = pickDate("Sent Date", LocalDate.now().toString());
            if (date == null) return;
            inv.setStatus(Invoice.Status.SENT);
            inv.setSentDate(date);
            storage.updateInvoice(inv);
            refreshHistory();
        });

        btnMarkPaid.addActionListener(e -> {
            Invoice inv = selectedInvoice();
            if (inv == null) return;
            String date = pickDate("Paid Date", LocalDate.now().toString());
            if (date == null) return;
            inv.setStatus(Invoice.Status.PAID);
            inv.setPaidDate(date);
            storage.updateInvoice(inv);
            refreshHistory();
            showTaxSetAsideDialog(inv);
        });

        btnDelete.addActionListener(e -> {
            Invoice inv = selectedInvoice();
            if (inv == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete Invoice #" + inv.getInvoiceNumber() + " (" + inv.getBossName() + ")?\n"
                + "The PDF will not be deleted from disk.",
                "Delete Invoice", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) return;
            List<Invoice> invoices = storage.loadInvoices();
            invoices.removeIf(i -> i.getId().equals(inv.getId()));
            storage.saveInvoices(invoices);
            refreshHistory();
        });

        // Double-click row → open PDF
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) btnOpenPdf.doClick();
            }
        });

        return root;
    }

    void showTaxSetAsideDialog(Invoice inv) {
        int year = LocalDate.now().getYear();
        com.github.shanebeee.reconciled.model.TaxBrackets brackets = storage.loadTaxBrackets(year);

        // ── YTD income from paid invoices ─────────────────────────────────────────────
        List<Invoice> allInvoices = storage.loadInvoices();
        double ytdGross = allInvoices.stream()
            .filter(i -> Invoice.Status.PAID.equals(i.getStatus())
                && i.getPaidDate() != null
                && i.getPaidDate().startsWith(String.valueOf(year)))
            .mapToDouble(i -> i.getTotalAmount() / 1.05) // pre-GST
            .sum();

        // Months elapsed so far this year (at least 1)
        int monthsElapsed = Math.max(1, LocalDate.now().getMonthValue());
        double annualizedIncome = (ytdGross / monthsElapsed) * 12;

        // ── This payment ───────────────────────────────────────────────────────────
        double total  = inv.getTotalAmount();
        double preGst = total / 1.05;
        double gst    = total - preGst;

        // ── Tax rates from brackets (based on annualized income) ───────────────
        double fedRate = brackets.federalRateFor(annualizedIncome);
        double bcRate  = brackets.bcRateFor(annualizedIncome);

        // CPP — check YTD already paid, cap at annual max
        double ytdCppPaid = allInvoices.stream()
            .filter(i -> Invoice.Status.PAID.equals(i.getStatus())
                && i.getPaidDate() != null
                && i.getPaidDate().startsWith(String.valueOf(year))
                && !i.getId().equals(inv.getId()))
            .mapToDouble(i -> (i.getTotalAmount() / 1.05) * brackets.getCppRate())
            .sum();
        double ytdCppCapped = Math.min(ytdCppPaid, brackets.getCppMaxContribution());
        double cpp = brackets.cppFor(preGst, ytdCppCapped);

        double fedTax        = preGst * fedRate;
        double bcTax         = preGst * bcRate;
        double totalSetAside = gst + fedTax + bcTax + cpp;
        double keepAmount    = preGst - (fedTax + bcTax + cpp);

        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
            "Invoice #" + inv.getInvoiceNumber() + " — Paid!", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(460, 580);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(24, 28, 12, 28));

        JLabel titleLbl = new JLabel("💰  " + String.format("$%.2f received", total));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 18f));
        titleLbl.setForeground(new Color(30, 41, 59));
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(titleLbl);
        body.add(Box.createVerticalStrut(4));

        // Annualized income context
        JLabel annualizedLbl = new JLabel(String.format(
            "Based on YTD income ÷ %d months × 12 = ~$%.0f/yr estimated annual income",
            monthsElapsed, annualizedIncome));
        annualizedLbl.setFont(annualizedLbl.getFont().deriveFont(Font.ITALIC, 10f));
        annualizedLbl.setForeground(new Color(148, 163, 184));
        annualizedLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(annualizedLbl);
        body.add(Box.createVerticalStrut(4));

        JLabel subLbl = new JLabel("Here's how to split it up:");
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 12f));
        subLbl.setForeground(new Color(100, 116, 139));
        subLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(subLbl);
        body.add(Box.createVerticalStrut(16));

        body.add(makeTaxRow("☁️  Remit to CRA (GST)",
            String.format("$%.2f", gst),
            "5% GST collected",
            new Color(59, 130, 246), new Color(219, 234, 254)));
        body.add(Box.createVerticalStrut(6));

        body.add(makeTaxRow("🏦  Federal Income Tax",
            String.format("$%.2f", fedTax),
            String.format("%.1f%% federal rate (bracket for ~$%.0f/yr)", fedRate * 100, annualizedIncome),
            new Color(245, 158, 11), new Color(254, 243, 199)));
        body.add(Box.createVerticalStrut(4));
        body.add(makeTaxRow("🏦  BC Provincial Tax",
            String.format("$%.2f", bcTax),
            String.format("%.2f%% BC rate (bracket for ~$%.0f/yr)", bcRate * 100, annualizedIncome),
            new Color(245, 158, 11), new Color(254, 243, 199)));
        body.add(Box.createVerticalStrut(4));

        String cppNote = cpp < preGst * brackets.getCppRate()
            ? String.format("%.1f%% CPP (capped — $%.2f remaining to annual max)",
                brackets.getCppRate() * 100, brackets.getCppMaxContribution() - ytdCppCapped)
            : String.format("%.1f%% CPP (employee + employer)", brackets.getCppRate() * 100);
        body.add(makeTaxRow("💼  CPP Contributions",
            String.format("$%.2f", cpp),
            cppNote,
            new Color(245, 158, 11), new Color(254, 243, 199)));
        body.add(Box.createVerticalStrut(6));

        body.add(makeTaxRow("🎯  Total to Set Aside",
            String.format("$%.2f", totalSetAside),
            "GST + Fed + BC + CPP combined",
            new Color(220, 38, 38), new Color(254, 226, 226)));
        body.add(Box.createVerticalStrut(6));

        body.add(makeTaxRow("✅  Yours to Keep",
            String.format("$%.2f", keepAmount),
            "After income tax and CPP",
            new Color(16, 185, 129), new Color(209, 250, 229)));

        body.add(Box.createVerticalStrut(12));
        JLabel disclaimer = new JLabel("* Estimate only. Rates based on your YTD income pattern. Consult a tax professional.");
        disclaimer.setFont(disclaimer.getFont().deriveFont(Font.ITALIC, 9f));
        disclaimer.setForeground(new Color(148, 163, 184));
        disclaimer.setAlignmentX(LEFT_ALIGNMENT);
        body.add(disclaimer);

        dialog.add(body, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JButton btnClose = new JButton("Got it!");
        btnClose.putClientProperty("JButton.buttonType", "roundRect");
        btnClose.setBackground(new Color(59, 130, 246));
        btnClose.setForeground(Color.WHITE);
        btnClose.addActionListener(e -> dialog.dispose());
        footer.add(btnClose);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel makeTaxRow(String label, String amount, String note, Color textColor, Color bgColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(bgColor);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 0),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(textColor.darker());
        JLabel noteLbl = new JLabel(note);
        noteLbl.setFont(noteLbl.getFont().deriveFont(Font.PLAIN, 10f));
        noteLbl.setForeground(textColor.darker());
        left.add(lbl);
        left.add(noteLbl);

        JLabel amtLbl = new JLabel(amount);
        amtLbl.setFont(amtLbl.getFont().deriveFont(Font.BOLD, 15f));
        amtLbl.setForeground(textColor.darker());

        row.add(left,   BorderLayout.CENTER);
        row.add(amtLbl, BorderLayout.EAST);
        return row;
    }

    /** Reloads invoice list from storage and repopulates the table. */
    public void refreshHistory() {
        invoiceCache = storage.loadInvoices();
        // Sort newest first
        invoiceCache.sort((a, b) -> Integer.compare(b.getInvoiceNumber(), a.getInvoiceNumber()));

        historyModel.setRowCount(0);
        for (Invoice inv : invoiceCache) {
            String period = formatPeriod(inv.getStartDate(), inv.getEndDate());
            String generated = inv.getGeneratedDate() != null
                ? LocalDate.parse(inv.getGeneratedDate()).format(DISPLAY_FMT) : "—";
            historyModel.addRow(new Object[]{
                "#" + inv.getInvoiceNumber(),
                inv.getBossName(),
                period,
                generated,
                inv.getStatus(),
                String.format("$%.2f", inv.getTotalAmount())
            });
        }
    }

    private Invoice selectedInvoice() {
        int row = historyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an invoice first.");
            return null;
        }
        return invoiceCache.get(row);
    }

    /** Shows a simple date-picker dialog and returns the chosen ISO date string, or null if cancelled. */
    private String pickDate(String label, String initialDate) {
        JButton btn = makeDateButton(initialDate);
        btn.addActionListener(e -> DatePicker.showPicker(InvoicePanel.this, btn));
        int result = JOptionPane.showConfirmDialog(this, btn, label, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;
        return btn.getText();
    }

    private String formatPeriod(String start, String end) {
        if (start == null || end == null) return "—";
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            if (s.getYear() == e.getYear() && s.getMonth() == e.getMonth()) {
                return s.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            }
            return s.format(SHORT_FMT) + " – " + e.format(SHORT_FMT);
        } catch (Exception ex) {
            return start + " – " + end;
        }
    }

    // ── Generate logic ────────────────────────────────────────────────────────

    private void generate(InvoiceGenerator.InvoiceMode mode) {
        try {
            Boss boss = (Boss) bossCombo.getSelectedItem();
            if (boss == null) return;
            EmployeeInfo employee = storage.loadEmployeeInfo();

            LocalDate start = LocalDate.parse(startBtn.getText());
            LocalDate end   = LocalDate.parse(endBtn.getText());

            List<LogEntry> allLogs = new ArrayList<>();
            LocalDate curr = start.withDayOfMonth(1);
            while (!curr.isAfter(end)) {
                allLogs.addAll(storage.loadLogs(YearMonth.from(curr).toString()));
                curr = curr.plusMonths(1);
            }

            List<LogEntry> filteredLogs = allLogs.stream()
                .filter(l -> {
                    LocalDate d = LocalDate.parse(l.getDate());
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .toList();

            // Calculate total for the invoice record
            InvoiceGenerator.Totals totals = InvoiceGenerator.computeTotals(boss, filteredLogs);

            int invNum = storage.getNextInvoiceNumber();
            String path = storage.getInvoicePath(boss, invNum);
            InvoiceGenerator.generateInvoice(boss, employee, filteredLogs, start.toString(), end.toString(), invNum, path, mode);

            // Record the invoice in the log
            Invoice record = new Invoice();
            record.setInvoiceNumber(invNum);
            record.setBossId(boss.getId());
            record.setBossName(boss.getName());
            record.setStartDate(start.toString());
            record.setEndDate(end.toString());
            record.setGeneratedDate(LocalDate.now().toString());
            record.setStatus(Invoice.Status.DRAFT);
            record.setTotalAmount(totals.total());
            record.setPdfPath(path);
            storage.recordInvoice(record);

            JOptionPane.showMessageDialog(this, "Invoice #" + invNum + " generated at:\n" + path);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating invoice: " + ex.getMessage());
        }
    }

    private double calculateTotal(Boss boss, List<LogEntry> logs, LocalDate start, LocalDate end) {
        return InvoiceGenerator.computeTotals(boss, logs).total();
    }

    // ── Monthly summary (unchanged logic, just extracted) ─────────────────────

    private void exportMonthlySummary() {
        try {
            JComboBox<YearMonth> monthCombo = new JComboBox<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
            monthCombo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof YearMonth ym) setText(ym.format(formatter));
                    return this;
                }
            });
            YearMonth current = YearMonth.now();
            for (int i = 0; i < 24; i++) monthCombo.addItem(current.minusMonths(i));

            int result = JOptionPane.showConfirmDialog(this, monthCombo, "Select Month", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) return;

            YearMonth ym = (YearMonth) monthCombo.getSelectedItem();
            if (ym == null) return;

            List<LogEntry> logs = storage.loadLogs(ym.toString());
            EmployeeInfo employee = storage.loadEmployeeInfo();
            List<Boss> allBosses = storage.loadBosses();
            String path = storage.getSummaryPath(ym.toString());
            SummaryGenerator.generateMonthlySummary(allBosses, employee, logs, ym.toString(), path);
            JOptionPane.showMessageDialog(this, "Monthly Summary generated at: " + path);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating summary: " + ex.getMessage());
        }
    }

    // ── Preview dialogs (unchanged) ───────────────────────────────────────────

    private void showSummaryPreview() {
        JComboBox<YearMonth> monthCombo = new JComboBox<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof YearMonth ym) setText(ym.format(fmt));
                return this;
            }
        });
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 24; i++) monthCombo.addItem(current.minusMonths(i));
        int result = JOptionPane.showConfirmDialog(this, monthCombo, "Select Month to Preview", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;
        YearMonth ym = (YearMonth) monthCombo.getSelectedItem();
        if (ym == null) return;

        List<LogEntry> logs = storage.loadLogs(ym.toString());
        List<Boss> bosses = storage.loadBosses();

        double grandTotal = 0;
        java.util.List<double[]> bossData = new java.util.ArrayList<>();
        for (Boss boss : bosses) {
            double totalHours = 0, totalKm = 0, totalExtras = 0;
            double bossSub = 0, bossTax = 0;
            for (LogEntry log : logs) {
                if (log.getType() == LogEntry.EntryType.TIME) {
                    double perc = log.getBossPercentages() != null ?
                        log.getBossPercentages().getOrDefault(boss.getId(),
                            log.getBossPercentages().getOrDefault(boss.getName(), 0.0)) / 100.0 : 0;
                    if (perc > 0) {
                        java.time.LocalTime start = TimePickerPanel.parseTime(log.getStartTime());
                        java.time.LocalTime end = TimePickerPanel.parseTime(log.getEndTime());
                        double hours = java.time.Duration.between(start, end).toMinutes() / 60.0;
                        totalHours += hours * perc;
                    }
                } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                    if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                        totalKm += log.getKilometers() != null ? log.getKilometers() : 0;
                } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                    if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid())) {
                        double sub = (log.getUnits() != null ? log.getUnits() : 0) * (log.getCostPerUnit() != null ? log.getCostPerUnit() : 0);
                        totalExtras += sub;
                    }
                }
            }
            double hoursSub = totalHours * boss.getHourlyRate();
            double kmSub = totalKm * (boss.getKmRate() != null ? boss.getKmRate() : 0);
            bossSub = hoursSub + kmSub + totalExtras;
            bossTax = bossSub * (boss.getTaxRate() / 100.0);
            double bossTotal = bossSub + bossTax;
            grandTotal += bossTotal;
            bossData.add(new double[]{totalHours, totalKm, totalExtras, bossSub, bossTax, bossTotal});
        }

        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this),
            ym.format(fmt) + " Summary", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(560, 680);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String vibeEmoji = grandTotal > 5000 ? "🤑" : grandTotal > 2000 ? "😊" : grandTotal > 500 ? "😐" : "😬";
        String vibeText  = grandTotal > 5000 ? "YAY I'M RICH" : grandTotal > 2000 ? "Not bad!" : grandTotal > 500 ? "Getting there..." : "AHHH SHIT I'M BROKE";
        Color  vibeColor = grandTotal > 5000 ? new Color(34, 197, 94) : grandTotal > 2000 ? new Color(59, 130, 246) : grandTotal > 500 ? new Color(245, 158, 11) : new Color(239, 68, 68);

        JLabel emojiLabel = new JLabel(vibeEmoji, JLabel.CENTER);
        emojiLabel.setFont(emojiLabel.getFont().deriveFont(Font.PLAIN, 48f));
        emojiLabel.setAlignmentX(CENTER_ALIGNMENT);
        body.add(emojiLabel);
        body.add(Box.createVerticalStrut(4));

        JLabel vibeLabel = new JLabel(vibeText, JLabel.CENTER);
        vibeLabel.setFont(vibeLabel.getFont().deriveFont(Font.BOLD, 16f));
        vibeLabel.setForeground(vibeColor);
        vibeLabel.setAlignmentX(CENTER_ALIGNMENT);
        body.add(vibeLabel);
        body.add(Box.createVerticalStrut(4));

        JLabel totalLabel = new JLabel(String.format("$%.2f", grandTotal), JLabel.CENTER);
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 36f));
        totalLabel.setForeground(new Color(30, 41, 59));
        totalLabel.setAlignmentX(CENTER_ALIGNMENT);
        body.add(totalLabel);
        body.add(Box.createVerticalStrut(20));

        for (int i = 0; i < bosses.size(); i++) {
            Boss boss = bosses.get(i);
            double[] d = bossData.get(i);
            if (d[5] == 0) continue;

            JPanel bossCard = makeCard();
            bossCard.setLayout(new BoxLayout(bossCard, BoxLayout.Y_AXIS));

            JPanel bossHeader = new JPanel(new BorderLayout());
            bossHeader.setOpaque(false);
            bossHeader.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
            JLabel bossName = new JLabel(boss.getName());
            bossName.setFont(bossName.getFont().deriveFont(Font.BOLD, 13f));
            bossName.setForeground(new Color(30, 41, 59));
            JLabel bossTotal = new JLabel(String.format("$%.2f", d[5]));
            bossTotal.setFont(bossTotal.getFont().deriveFont(Font.BOLD, 13f));
            bossTotal.setForeground(new Color(34, 197, 94));
            bossHeader.add(bossName, BorderLayout.WEST);
            bossHeader.add(bossTotal, BorderLayout.EAST);
            bossCard.add(bossHeader);

            if (d[0] > 0) bossCard.add(makeSummaryRow(String.format("%.2f hrs @ $%.2f/hr", d[0], boss.getHourlyRate()), String.format("$%.2f", d[0] * boss.getHourlyRate())));
            if (d[1] > 0) bossCard.add(makeSummaryRow(String.format("%.1f km @ $%.2f/km", d[1], boss.getKmRate() != null ? boss.getKmRate() : 0), String.format("$%.2f", d[1] * (boss.getKmRate() != null ? boss.getKmRate() : 0))));
            if (d[2] > 0) bossCard.add(makeSummaryRow("Extras", String.format("$%.2f", d[2])));
            bossCard.add(makeSummaryRow("Tax (" + (int) boss.getTaxRate() + "%)", String.format("$%.2f", d[4])));

            body.add(bossCard);
            body.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JButton btnClose = new JButton("Close");
        btnClose.putClientProperty("JButton.buttonType", "roundRect");
        btnClose.addActionListener(ev -> dialog.dispose());
        footer.add(btnClose);
        dialog.add(footer, BorderLayout.SOUTH);

        int maxH = (int) java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight() - 100;
        if (dialog.getHeight() > maxH) dialog.setSize(560, maxH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showInvoicePreview() {
        Boss boss = (Boss) bossCombo.getSelectedItem();
        if (boss == null) return;
        EmployeeInfo employee = storage.loadEmployeeInfo();
        LocalDate start = LocalDate.parse(startBtn.getText());
        LocalDate end   = LocalDate.parse(endBtn.getText());

        List<LogEntry> allLogs = new ArrayList<>();
        LocalDate curr = start.withDayOfMonth(1);
        while (!curr.isAfter(end)) {
            allLogs.addAll(storage.loadLogs(YearMonth.from(curr).toString()));
            curr = curr.plusMonths(1);
        }
        List<LogEntry> logs = allLogs.stream()
            .filter(l -> { LocalDate d = LocalDate.parse(l.getDate()); return !d.isBefore(start) && !d.isAfter(end); })
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .toList();

        double totalHours = 0, totalKm = 0, totalExtras = 0;
        for (LogEntry log : logs) {
            if (log.getType() == LogEntry.EntryType.TIME) {
                double perc = log.getBossPercentages() != null ?
                    log.getBossPercentages().getOrDefault(boss.getId(),
                        log.getBossPercentages().getOrDefault(boss.getName(), 0.0)) / 100.0 : 0;
                if (perc > 0) {
                    java.time.LocalTime s = TimePickerPanel.parseTime(log.getStartTime());
                    java.time.LocalTime e2 = TimePickerPanel.parseTime(log.getEndTime());
                    totalHours += java.time.Duration.between(s, e2).toMinutes() / 60.0 * perc;
                }
            } else if (log.getType() == LogEntry.EntryType.KILOMETER) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                    totalKm += log.getKilometers() != null ? log.getKilometers() : 0;
            } else if (log.getType() == LogEntry.EntryType.EXTRA) {
                if (boss.getId().equals(log.getBossUuid()) || boss.getName().equals(log.getBossUuid()))
                    totalExtras += (log.getUnits() != null ? log.getUnits() : 0) * (log.getCostPerUnit() != null ? log.getCostPerUnit() : 0);
            }
        }
        double subtotalHours = totalHours * boss.getHourlyRate();
        double subtotalKm    = totalKm * (boss.getKmRate() != null ? boss.getKmRate() : 0);
        double subtotal      = subtotalHours + subtotalKm + totalExtras;
        double tax           = subtotal * (boss.getTaxRate() / 100.0);
        double grandTotal    = subtotal + tax;

        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "Invoice Preview", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(560, 680);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        JPanel invoiceHeader = new JPanel(new BorderLayout());
        invoiceHeader.setOpaque(false);
        invoiceHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel fromPanel = new JPanel();
        fromPanel.setLayout(new BoxLayout(fromPanel, BoxLayout.Y_AXIS));
        fromPanel.setOpaque(false);
        JLabel fromName = new JLabel(employee != null ? employee.getFullName() : "Your Name");
        fromName.setFont(fromName.getFont().deriveFont(Font.BOLD, 16f));
        fromName.setForeground(new Color(30, 41, 59));
        JLabel fromEmail = new JLabel(employee != null && employee.getEmail() != null ? employee.getEmail() : "");
        fromEmail.setFont(fromEmail.getFont().deriveFont(Font.PLAIN, 11f));
        fromEmail.setForeground(new Color(100, 116, 139));
        fromPanel.add(fromName);
        fromPanel.add(fromEmail);

        JPanel invoiceMeta = new JPanel();
        invoiceMeta.setLayout(new BoxLayout(invoiceMeta, BoxLayout.Y_AXIS));
        invoiceMeta.setOpaque(false);
        JLabel invoiceTitle = new JLabel("INVOICE", JLabel.RIGHT);
        invoiceTitle.setFont(invoiceTitle.getFont().deriveFont(Font.BOLD, 20f));
        invoiceTitle.setForeground(new Color(59, 130, 246));
        invoiceTitle.setAlignmentX(RIGHT_ALIGNMENT);
        JLabel dateRange = new JLabel(start.format(DISPLAY_FMT) + " – " + end.format(DISPLAY_FMT), JLabel.RIGHT);
        dateRange.setFont(dateRange.getFont().deriveFont(Font.PLAIN, 11f));
        dateRange.setForeground(new Color(100, 116, 139));
        dateRange.setAlignmentX(RIGHT_ALIGNMENT);
        invoiceMeta.add(invoiceTitle);
        invoiceMeta.add(dateRange);

        invoiceHeader.add(fromPanel, BorderLayout.WEST);
        invoiceHeader.add(invoiceMeta, BorderLayout.EAST);
        body.add(invoiceHeader);
        body.add(Box.createVerticalStrut(12));
        body.add(makeThinDivider(new Color(59, 130, 246)));
        body.add(Box.createVerticalStrut(12));

        JLabel billToLbl = new JLabel("BILL TO");
        billToLbl.setFont(billToLbl.getFont().deriveFont(Font.BOLD, 9f));
        billToLbl.setForeground(new Color(148, 163, 184));
        billToLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(billToLbl);
        body.add(Box.createVerticalStrut(4));
        JLabel bossNameLbl = new JLabel(boss.getName());
        bossNameLbl.setFont(bossNameLbl.getFont().deriveFont(Font.BOLD, 13f));
        bossNameLbl.setForeground(new Color(30, 41, 59));
        bossNameLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(bossNameLbl);
        body.add(Box.createVerticalStrut(16));

        body.add(makeTableHeader());
        body.add(makeThinDivider(new Color(226, 232, 240)));
        body.add(Box.createVerticalStrut(4));

        if (totalHours > 0) body.add(makeLineItem("Labour", String.format("%.2f hrs @ $%.2f/hr", totalHours, boss.getHourlyRate()), String.format("$%.2f", subtotalHours)));
        if (totalKm > 0)    body.add(makeLineItem("Travel", String.format("%.1f km @ $%.2f/km", totalKm, boss.getKmRate() != null ? boss.getKmRate() : 0), String.format("$%.2f", subtotalKm)));
        if (totalExtras > 0) body.add(makeLineItem("Extras", "Miscellaneous", String.format("$%.2f", totalExtras)));

        body.add(Box.createVerticalStrut(4));
        body.add(makeThinDivider(new Color(226, 232, 240)));
        body.add(Box.createVerticalStrut(8));
        body.add(makeTotalRow("Subtotal", String.format("$%.2f", subtotal), false));
        body.add(Box.createVerticalStrut(4));
        body.add(makeTotalRow(String.format("GST/HST (%.0f%%)", boss.getTaxRate()), String.format("$%.2f", tax), false));
        body.add(Box.createVerticalStrut(8));
        body.add(makeThinDivider(new Color(226, 232, 240)));
        body.add(Box.createVerticalStrut(8));
        body.add(makeTotalRow("TOTAL DUE", String.format("$%.2f", grandTotal), true));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JButton btnClose = new JButton("Close");
        btnClose.putClientProperty("JButton.buttonType", "roundRect");
        btnClose.addActionListener(ev -> dialog.dispose());
        JButton btnGenerate2 = new JButton("Generate PDF");
        btnGenerate2.putClientProperty("JButton.buttonType", "roundRect");
        btnGenerate2.setBackground(new Color(59, 130, 246));
        btnGenerate2.setForeground(Color.WHITE);
        btnGenerate2.addActionListener(ev -> { dialog.dispose(); generate(InvoiceGenerator.InvoiceMode.STANDARD); });
        footer.add(btnClose);
        footer.add(btnGenerate2);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 2, 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(4, 20, 12, 20));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JPanel makeSectionLabel(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        row.add(lbl, BorderLayout.WEST);
        return row;
    }

    private JPanel makeDivider() {
        JPanel div = new JPanel();
        div.setOpaque(false);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(0, 1));
        div.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));
        return div;
    }

    private JPanel makeThinDivider(Color color) {
        JPanel div = new JPanel();
        div.setOpaque(false);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(0, 1));
        div.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, color));
        div.setAlignmentX(LEFT_ALIGNMENT);
        return div;
    }

    private JPanel makeFieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        lbl.setPreferredSize(new Dimension(80, 20));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private JButton makeDateButton(String date) {
        JButton btn = new JButton(date);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(new Color(30, 41, 59));
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        return btn;
    }

    private JPanel makeSummaryRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(new Color(100, 116, 139));
        JLabel val = new JLabel(value);
        val.setFont(val.getFont().deriveFont(Font.PLAIN, 11f));
        val.setForeground(new Color(71, 85, 105));
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel makeTableHeader() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        JLabel desc = new JLabel("DESCRIPTION");
        desc.setFont(desc.getFont().deriveFont(Font.BOLD, 9f));
        desc.setForeground(new Color(148, 163, 184));
        JLabel detail = new JLabel("DETAILS");
        detail.setFont(detail.getFont().deriveFont(Font.BOLD, 9f));
        detail.setForeground(new Color(148, 163, 184));
        JLabel amt = new JLabel("AMOUNT", JLabel.RIGHT);
        amt.setFont(amt.getFont().deriveFont(Font.BOLD, 9f));
        amt.setForeground(new Color(148, 163, 184));
        amt.setPreferredSize(new Dimension(80, 20));
        row.add(desc, BorderLayout.WEST);
        row.add(detail, BorderLayout.CENTER);
        row.add(amt, BorderLayout.EAST);
        return row;
    }

    private JPanel makeLineItem(String description, String detail, String amount) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        JLabel desc = new JLabel(description);
        desc.setFont(desc.getFont().deriveFont(Font.PLAIN, 13f));
        desc.setForeground(new Color(30, 41, 59));
        JLabel det = new JLabel(detail);
        det.setFont(det.getFont().deriveFont(Font.PLAIN, 12f));
        det.setForeground(new Color(100, 116, 139));
        JLabel amt = new JLabel(amount, JLabel.RIGHT);
        amt.setFont(amt.getFont().deriveFont(Font.PLAIN, 13f));
        amt.setForeground(new Color(30, 41, 59));
        amt.setPreferredSize(new Dimension(80, 20));
        row.add(desc, BorderLayout.WEST);
        row.add(det, BorderLayout.CENTER);
        row.add(amt, BorderLayout.EAST);
        return row;
    }

    private JPanel makeTotalRow(String label, String amount, boolean bold) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, bold ? 14f : 12f));
        lbl.setForeground(bold ? new Color(30, 41, 59) : new Color(100, 116, 139));
        JLabel amt = new JLabel(amount, JLabel.RIGHT);
        amt.setFont(amt.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, bold ? 14f : 12f));
        amt.setForeground(bold ? new Color(59, 130, 246) : new Color(30, 41, 59));
        row.add(lbl, BorderLayout.WEST);
        row.add(amt, BorderLayout.EAST);
        return row;
    }
}
