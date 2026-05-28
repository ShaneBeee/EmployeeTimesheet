package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BossPanel extends JPanel {
    private final DataStorage storage;
    private final List<Boss> bosses;
    private JTable bossTable;
    private DefaultTableModel tableModel;

    public BossPanel(DataStorage storage) {
        this.storage = storage;
        this.bosses = storage.loadBosses();
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Boss Management");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String[] columns = {"Name", "Company", "Phone", "Hourly Rate"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        bossTable = new JTable(tableModel);
        refreshTable();

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        bossTable.setRowHeight(25);
        bossTable.setShowVerticalLines(false);
        bossTable.setIntercellSpacing(new Dimension(0, 1));
        
        mainContent.add(new JScrollPane(bossTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton("Add Boss");
        JButton btnEdit = new JButton("Edit Boss");
        JButton btnDelete = new JButton("Delete Boss");
        
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        mainContent.add(btnPanel, BorderLayout.SOUTH);
        
        add(mainContent, BorderLayout.CENTER);
        
        bossTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = bossTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        bossTable.setRowSelectionInterval(row, row);
                        showBossDialog(bosses.get(row));
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> showBossDialog(null));
        btnEdit.addActionListener(e -> {
            int row = bossTable.getSelectedRow();
            if (row >= 0) {
                showBossDialog(bosses.get(row));
            }
        });
        btnDelete.addActionListener(e -> {
            int row = bossTable.getSelectedRow();
            if (row >= 0) {
                bosses.remove(row);
                storage.saveBosses(bosses);
                refreshTable();
            }
        });
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Boss boss : bosses) {
            tableModel.addRow(new Object[]{boss.getName(), boss.getCompany(), boss.getPhoneNumber(), boss.getHourlyRate()});
        }
    }

    private void showBossDialog(Boss boss) {
        boolean isNew = boss == null;
        Boss b = isNew ? new Boss("") : boss;

        JTextField nameField = new JTextField(b.getName());
        JTextField companyField = new JTextField(b.getCompany());
        JTextField addressField = new JTextField(b.getAddress());
        JTextField phoneField = new JTextField(b.getPhoneNumber());
        JTextField emailField = new JTextField(b.getEmail());
        JTextField rateField = new JTextField(String.valueOf(b.getHourlyRate()));
        JTextField taxField = new JTextField(String.valueOf(b.getTaxRate()));
        JTextField kmField = new JTextField(b.getKmRate() != null ? String.valueOf(b.getKmRate()) : "");

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Name:")); panel.add(nameField);
        panel.add(new JLabel("Company:")); panel.add(companyField);
        panel.add(new JLabel("Address:")); panel.add(addressField);
        panel.add(new JLabel("Phone:")); panel.add(phoneField);
        panel.add(new JLabel("Email:")); panel.add(emailField);
        panel.add(new JLabel("Hourly Rate:")); panel.add(rateField);
        panel.add(new JLabel("Tax Rate (%):")); panel.add(taxField);
        panel.add(new JLabel("KM Rate:")); panel.add(kmField);

        int result = JOptionPane.showConfirmDialog(this, panel, isNew ? "Add Boss" : "Edit Boss", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double oldRate = b.getHourlyRate();
            b.setName(nameField.getText());
            b.setCompany(companyField.getText());
            b.setAddress(addressField.getText());
            b.setPhoneNumber(phoneField.getText());
            b.setEmail(emailField.getText());
            try {
                double newRate = Double.parseDouble(rateField.getText());
                if (newRate != oldRate) {
                    b.getRateHistory().add(new Boss.RateChange(java.time.LocalDate.now().toString(), oldRate));
                    b.setHourlyRate(newRate);
                }
                b.setTaxRate(Double.parseDouble(taxField.getText()));
                String km = kmField.getText();
                b.setKmRate(km.isEmpty() ? null : Double.parseDouble(km));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format");
            }

            if (isNew) bosses.add(b);
            storage.saveBosses(bosses);
            refreshTable();
        }
    }
}
