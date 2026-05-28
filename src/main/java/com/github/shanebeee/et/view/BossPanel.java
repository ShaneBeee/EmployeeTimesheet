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
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Boss Management");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String[] columns = {"Name", "Company", "Phone", "Hourly Rate"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        bossTable = new JTable(tableModel);
        bossTable.setRowHeight(35);
        bossTable.setIntercellSpacing(new Dimension(0, 0));
        bossTable.setShowGrid(false);
        bossTable.getTableHeader().setReorderingAllowed(false);
        bossTable.getTableHeader().setBackground(new Color(248, 250, 252));
        bossTable.getTableHeader().setForeground(new Color(100, 116, 139));
        bossTable.getTableHeader().setFont(bossTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        refreshTable();

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(bossTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        mainContent.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnAdd = new JButton("Add Boss");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground((Color) UIManager.get("App.success"));
        btnAdd.setForeground(Color.WHITE);

        JButton btnEdit = new JButton("Edit Boss");
        btnEdit.putClientProperty("JButton.buttonType", "roundRect");
        btnEdit.setBackground((Color) UIManager.get("App.warning"));
        btnEdit.setForeground(Color.WHITE);

        JButton btnDelete = new JButton("Delete Boss");
        btnDelete.putClientProperty("JButton.buttonType", "roundRect");
        btnDelete.setBackground((Color) UIManager.get("App.danger"));
        btnDelete.setForeground(Color.WHITE);
        
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
        JTextField address2Field = new JTextField(b.getAddress2());
        JTextField phoneField = new JTextField(b.getPhoneNumber());
        JTextField emailField = new JTextField(b.getEmail());
        JTextField rateField = new JTextField(String.valueOf(b.getHourlyRate()));
        JTextField taxField = new JTextField(String.valueOf(b.getTaxRate()));
        JTextField kmField = new JTextField(b.getKmRate() != null ? String.valueOf(b.getKmRate()) : "");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panel.add(new JLabel("Company:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(companyField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panel.add(new JLabel("Address 1:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(addressField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        panel.add(new JLabel("Address 2:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(address2Field, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(phoneField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0;
        panel.add(new JLabel("Hourly Rate:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(rateField, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.0;
        panel.add(new JLabel("Tax Rate (%):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(taxField, gbc);

        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0;
        panel.add(new JLabel("KM Rate:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(kmField, gbc);

        panel.setPreferredSize(new java.awt.Dimension(500, panel.getPreferredSize().height));

        int result = JOptionPane.showConfirmDialog(this, panel, isNew ? "Add Boss" : "Edit Boss", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            double oldRate = b.getHourlyRate();
            b.setName(nameField.getText());
            b.setCompany(companyField.getText());
            b.setAddress(addressField.getText());
            b.setAddress2(address2Field.getText());
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
