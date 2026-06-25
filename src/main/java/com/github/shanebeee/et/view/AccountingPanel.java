package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Expenditure;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.ExcelExporter;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AccountingPanel extends JPanel {

    private static final Color ACCENT = new Color(99, 102, 241); // indigo

    private final DataStorage storage;
    private JSpinner yearSpinner;

    public AccountingPanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Accounting");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Body ──────────────────────────────────────────────────────────────
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 16, 0);

        // ── Year picker card ─────────────────────────────────────────────────
        JPanel yearCard = makeCard();
        yearCard.setLayout(new BorderLayout(12, 0));

        JLabel yearIcon = new JLabel("📅");
        yearIcon.setFont(yearIcon.getFont().deriveFont(24f));
        JPanel yearIconWrap = new JPanel(new GridBagLayout());
        yearIconWrap.setOpaque(false);
        yearIconWrap.setPreferredSize(new Dimension(48, 48));
        yearIconWrap.add(yearIcon);

        JPanel yearText = new JPanel();
        yearText.setLayout(new BoxLayout(yearText, BoxLayout.Y_AXIS));
        yearText.setOpaque(false);
        JLabel yearTitle = new JLabel("Tax Year");
        yearTitle.setFont(yearTitle.getFont().deriveFont(Font.BOLD, 14f));
        yearTitle.setForeground(new Color(30, 41, 59));
        JLabel yearSub = new JLabel("Select the year to export");
        yearSub.setFont(yearSub.getFont().deriveFont(Font.PLAIN, 11f));
        yearSub.setForeground(new Color(148, 163, 184));
        yearText.add(yearTitle);
        yearText.add(Box.createVerticalStrut(2));
        yearText.add(yearSub);

        int currentYear = LocalDate.now().getYear();
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, 2020, currentYear + 1, 1);
        yearSpinner = new JSpinner(yearModel);
        yearSpinner.setPreferredSize(new Dimension(90, 32));
        JSpinner.NumberEditor yearEditor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        yearSpinner.setEditor(yearEditor);

        yearCard.add(yearIconWrap, BorderLayout.WEST);
        yearCard.add(yearText,    BorderLayout.CENTER);
        yearCard.add(yearSpinner, BorderLayout.EAST);
        body.add(yearCard, gbc);

        // ── Excel export card ────────────────────────────────────────────────
        gbc.gridy++;
        body.add(makeExportCard(
            "📊", "Export to Excel",
            "Expenses, KM log, and KM summary as a formatted .xlsx workbook",
            ACCENT, this::exportExcel
        ), gbc);

        // ── Receipt zip card ─────────────────────────────────────────────────
        gbc.gridy++;
        body.add(makeExportCard(
            "🗂", "Export Receipt Archive",
            "Zip of all receipts for the selected year, organized by month",
            new Color(245, 158, 11), this::exportReceiptZip
        ), gbc);

        // Push cards to top
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        body.add(new JPanel() {{ setOpaque(false); }}, gbc);

        add(body, BorderLayout.CENTER);
    }

    private JPanel makeExportCard(String icon, String title, String subtitle,
                                   Color accentColor, Runnable action) {
        JPanel card = makeCard();
        card.setLayout(new BorderLayout(16, 0));

        // Icon circle
        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconCircle.setOpaque(false);
        iconCircle.setPreferredSize(new Dimension(48, 48));
        iconCircle.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(iconLbl.getFont().deriveFont(20f));
        iconCircle.add(iconLbl);

        // Text
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 14f));
        titleLbl.setForeground(new Color(30, 41, 59));
        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(100, 116, 139));
        text.add(titleLbl);
        text.add(Box.createVerticalStrut(3));
        text.add(subLbl);

        // Button
        JButton btn = new JButton("Export");
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.setBackground(accentColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 12f));
        btn.setPreferredSize(new Dimension(90, 34));
        btn.addActionListener(e -> action.run());

        card.add(iconCircle, BorderLayout.WEST);
        card.add(text,       BorderLayout.CENTER);
        card.add(btn,        BorderLayout.EAST);
        return card;
    }

    // ── Export actions ────────────────────────────────────────────────────────

    private void exportExcel() {
        int year = (int) yearSpinner.getValue();

        java.awt.Frame owner = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        java.awt.FileDialog fd = new java.awt.FileDialog(owner, "Save Excel Export", java.awt.FileDialog.SAVE);
        fd.setFile("EmployeeTimesheet_" + year + ".xlsx");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        String outputPath = fd.getDirectory() + fd.getFile();
        if (!outputPath.endsWith(".xlsx")) outputPath += ".xlsx";
        final String finalPath = outputPath;

        // Run on background thread so UI doesn't freeze
        JDialog progress = makeProgressDialog("Generating Excel file...");
        new Thread(() -> {
            try {
                ExcelExporter exporter = new ExcelExporter(storage);
                File out = exporter.export(year, finalPath);
                SwingUtilities.invokeLater(() -> {
                    progress.dispose();
                    int opt = JOptionPane.showConfirmDialog(this,
                        "Excel file saved to:\n" + out.getAbsolutePath() + "\n\nOpen it now?",
                        "Export Complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        try { Desktop.getDesktop().open(out); } catch (IOException ex) { ex.printStackTrace(); }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progress.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Export failed:\n" + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
        progress.setVisible(true);
    }

    private void exportReceiptZip() {
        int year = (int) yearSpinner.getValue();

        // Check if any receipts exist for this year
        List<Expenditure> expenses = storage.loadExpenditures(String.valueOf(year));
        long totalReceipts = expenses.stream()
            .mapToLong(e -> e.getReceiptFiles().size()).sum();

        if (totalReceipts == 0) {
            JOptionPane.showMessageDialog(this,
                "No receipts found for " + year + ".",
                "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.awt.Frame owner = (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
        java.awt.FileDialog fd = new java.awt.FileDialog(owner, "Save Receipt Archive", java.awt.FileDialog.SAVE);
        fd.setFile("Receipts_" + year + ".zip");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        String zipPath = fd.getDirectory() + fd.getFile();
        if (!zipPath.endsWith(".zip")) zipPath += ".zip";
        final String finalZipPath = zipPath;

        JDialog progress2 = makeProgressDialog("Zipping receipts...");
        new Thread(() -> {
            try {
                int count = buildReceiptZip(expenses, year, finalZipPath);
                File zipFile = new File(finalZipPath);
                SwingUtilities.invokeLater(() -> {
                    progress2.dispose();
                    int opt = JOptionPane.showConfirmDialog(this,
                        count + " receipt" + (count != 1 ? "s" : "") + " archived to:\n"
                            + zipFile.getAbsolutePath() + "\n\nOpen folder?",
                        "Archive Complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        try { Desktop.getDesktop().open(zipFile.getParentFile()); }
                        catch (IOException ex) { ex.printStackTrace(); }
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    progress2.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Archive failed:\n" + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
        progress2.setVisible(true);
    }

    private int buildReceiptZip(List<Expenditure> expenses, int year, String zipPath) throws IOException {
        int count = 0;
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            for (Expenditure exp : expenses) {
                for (String rel : exp.getReceiptFiles()) {
                    File f = storage.getReceiptFile(rel);
                    if (!f.exists()) continue;
                    // rel is like "2026/06/abc_phone-bill_1.pdf"
                    // use it directly as the zip entry path so it's organized by month
                    ZipEntry entry = new ZipEntry(year + "/" + rel.replaceFirst("^\\d{4}/", ""));
                    zos.putNextEntry(entry);
                    zos.write(Files.readAllBytes(f.toPath()));
                    zos.closeEntry();
                    count++;
                }
            }
        }
        return count;
    }

    private JDialog makeProgressDialog(String message) {
        JDialog d = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), false);
        d.setUndecorated(true);
        d.setSize(280, 80);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(message);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 13f));
        lbl.setForeground(new Color(30, 41, 59));
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(lbl, BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        d.add(p);
        return d;
    }

    // ── Card helper ───────────────────────────────────────────────────────────

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
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return card;
    }
}
