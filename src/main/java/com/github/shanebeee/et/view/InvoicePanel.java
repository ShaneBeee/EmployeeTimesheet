package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.LogEntry;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.InvoiceGenerator;
import com.github.shanebeee.et.util.SummaryGenerator;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InvoicePanel extends JPanel {
    private final DataStorage storage;

    public InvoicePanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Invoice Management");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        JComboBox<Boss> bossCombo = new JComboBox<>();
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

        JTextField startField = new JTextField(YearMonth.now().atDay(1).toString());
        JTextField endField = new JTextField(YearMonth.now().atEndOfMonth().toString());

        startField.setEditable(false);
        endField.setEditable(false);
        startField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                DatePicker.showPicker(InvoicePanel.this, startField);
            }
        });
        endField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                DatePicker.showPicker(InvoicePanel.this, endField);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        form.add(new JLabel("Select Boss:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(bossCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        form.add(new JLabel("Start Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(startField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        form.add(new JLabel("End Date:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        form.add(endField, gbc);

        JButton btnGenerate = new JButton("Generate Invoice (PDF)");
        JButton btnSummary = new JButton("Export Monthly Summary (PDF)");

        btnGenerate.addActionListener(e -> {
            try {
                Boss boss = (Boss) bossCombo.getSelectedItem();
                if (boss == null) return;
                EmployeeInfo employee = storage.loadEmployeeInfo();
                
                LocalDate start = LocalDate.parse(startField.getText());
                LocalDate end = LocalDate.parse(endField.getText());

                // Collect logs for range
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

                int invNum = storage.getNextInvoiceNumber();
                String path = storage.getInvoicePath(boss, invNum);
                
                InvoiceGenerator.generateInvoice(boss, employee, filteredLogs, start.toString(), end.toString(), invNum, path);
                
                JOptionPane.showMessageDialog(this, "Invoice generated at: " + path);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error generating invoice: " + ex.getMessage());
            }
        });

        btnSummary.addActionListener(e -> {
            try {
                // Month Selection Dialog
                JComboBox<YearMonth> monthCombo = new JComboBox<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                monthCombo.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        if (value instanceof YearMonth ym) {
                            setText(ym.format(formatter));
                        }
                        return this;
                    }
                });
                YearMonth current = YearMonth.now();
                // Add last 24 months for selection
                for (int i = 0; i < 24; i++) {
                    monthCombo.addItem(current.minusMonths(i));
                }

                int result = JOptionPane.showConfirmDialog(this, monthCombo, "Select Month for Summary", JOptionPane.OK_CANCEL_OPTION);
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
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnGenerate);
        buttonPanel.add(btnSummary);

        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setOpaque(false);
        formContainer.add(form, BorderLayout.NORTH);

        JLabel paramsTitle = new JLabel("Parameters");
        paramsTitle.setFont(paramsTitle.getFont().deriveFont(Font.BOLD));
        paramsTitle.setForeground(new Color(100, 116, 139));
        mainContent.add(paramsTitle, BorderLayout.NORTH);
        mainContent.add(formContainer, BorderLayout.CENTER);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);

        add(mainContent, BorderLayout.CENTER);
    }
}
