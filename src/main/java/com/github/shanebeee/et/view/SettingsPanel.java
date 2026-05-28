package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {
    private final DataStorage storage;

    public SettingsPanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));

        EmployeeInfo info = storage.loadEmployeeInfo();

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Employee Information"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextField nameField = new JTextField(info.getFullName());
        JTextField companyField = new JTextField(info.getCompany());
        JTextField phoneField = new JTextField(info.getPhoneNumber());
        JTextField emailField = new JTextField(info.getEmail());

        panel.add(new JLabel("Full Name:")); panel.add(nameField);
        panel.add(new JLabel("Company:")); panel.add(companyField);
        panel.add(new JLabel("Phone:")); panel.add(phoneField);
        panel.add(new JLabel("Email:")); panel.add(emailField);

        JPanel appSettingsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        appSettingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Application Settings"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JTextField startField = new JTextField(storage.getDefaultStartTime());
        JTextField endField = new JTextField(storage.getDefaultEndTime());
        String[] themes = {"system", "light", "dark"};
        JComboBox<String> themeCombo = new JComboBox<>(themes);
        themeCombo.setSelectedItem(storage.getTheme());

        appSettingsPanel.add(new JLabel("Default Start Time (HH:mm):")); appSettingsPanel.add(startField);
        appSettingsPanel.add(new JLabel("Default End Time (HH:mm):")); appSettingsPanel.add(endField);
        appSettingsPanel.add(new JLabel("Theme:")); appSettingsPanel.add(themeCombo);

        JButton saveBtn = new JButton("Save Settings");

        saveBtn.addActionListener(e -> {
            info.setFullName(nameField.getText());
            info.setCompany(companyField.getText());
            info.setPhoneNumber(phoneField.getText());
            info.setEmail(emailField.getText());
            storage.saveEmployeeInfo(info);

            storage.setDefaultStartTime(startField.getText());
            storage.setDefaultEndTime(endField.getText());
            storage.setTheme((String) themeCombo.getSelectedItem());

            JOptionPane.showMessageDialog(this, "Settings Saved! Restart application if theme was changed.");

            // Check if we need to set up bosses
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) {
                mainFrame.checkBosses();
            }
        });

        mainContent.add(panel);
        mainContent.add(Box.createVerticalStrut(20));
        mainContent.add(appSettingsPanel);
        mainContent.add(Box.createVerticalGlue());

        add(new JScrollPane(mainContent), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(saveBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}
