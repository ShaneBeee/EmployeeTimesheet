package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SettingsPanel extends JPanel {

    private final DataStorage storage;

    public SettingsPanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);

        EmployeeInfo info = storage.loadEmployeeInfo();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        JTextField nameField = new JTextField(info.getFullName());
        JTextField companyField = new JTextField(info.getCompany());
        JTextField addressField = new JTextField(info.getAddress());
        JTextField address2Field = new JTextField(info.getAddress2());
        JTextField phoneField = new JTextField(info.getPhoneNumber());
        JTextField emailField = new JTextField(info.getEmail());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Company:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(companyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Address 1:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(addressField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Address 2:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(address2Field, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(phoneField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(emailField, gbc);

        JPanel appSettingsPanel = new JPanel(new GridBagLayout());
        appSettingsPanel.setOpaque(false);
        appSettingsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JTextField startField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultStartTime()));
        startField.setEditable(false);
        startField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, startField);
            }
        });
        JTextField endField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultEndTime()));
        endField.setEditable(false);
        endField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, endField);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        appSettingsPanel.add(new JLabel("Default Start Time:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        appSettingsPanel.add(startField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        appSettingsPanel.add(new JLabel("Default End Time:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        appSettingsPanel.add(endField, gbc);

        JButton saveBtn = new JButton("Save Settings");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setBackground((Color) UIManager.get("App.accent"));
        saveBtn.setForeground(Color.WHITE);

        saveBtn.addActionListener(e -> {
            info.setFullName(nameField.getText());
            info.setCompany(companyField.getText());
            info.setAddress(addressField.getText());
            info.setAddress2(address2Field.getText());
            info.setPhoneNumber(phoneField.getText());
            info.setEmail(emailField.getText());
            storage.saveEmployeeInfo(info);

            storage.setDefaultStartTime(startField.getText());
            storage.setDefaultEndTime(endField.getText());

            JOptionPane.showMessageDialog(this, "Settings Saved!");

            // Check if we need to set up bosses
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) {
                mainFrame.checkBosses();
            }
        });

        JLabel employeeTitle = new JLabel("Employee Information", SwingConstants.CENTER);
        employeeTitle.setFont(employeeTitle.getFont().deriveFont(Font.BOLD));
        employeeTitle.setForeground(new Color(100, 116, 139));
        employeeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainContent.add(employeeTitle);
        mainContent.add(Box.createVerticalStrut(10));
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainContent.add(panel);
        mainContent.add(Box.createVerticalStrut(30));

        JLabel appTitle = new JLabel("Application Settings", SwingConstants.CENTER);
        appTitle.setFont(appTitle.getFont().deriveFont(Font.BOLD));
        appTitle.setForeground(new Color(100, 116, 139));
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainContent.add(appTitle);
        mainContent.add(Box.createVerticalStrut(10));
        appSettingsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainContent.add(appSettingsPanel);
        mainContent.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(saveBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

}
