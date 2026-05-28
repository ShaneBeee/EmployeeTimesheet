package com.github.shanebeee.et.view;

import javax.swing.*;
import java.awt.*;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.UIUtils;

public class MainFrame extends JFrame {
    private final DataStorage storage;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    public MainFrame() {
        this.storage = new DataStorage();
        setTitle("Employee Timesheet");
        setIconImage(UIUtils.createAppIcon(64));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")));

        JLabel titleLabel = new JLabel("Employee Data");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(titleLabel);

        JButton btnLogs = createNavButton("Work Logs", "LOGS");
        JButton btnInvoices = createNavButton("Invoice Management", "INVOICES");
        JButton btnBosses = createNavButton("Boss Management", "BOSSES");
        JButton btnSettings = createNavButton("Settings", "SETTINGS");

        sidebar.add(btnLogs);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnInvoices);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnBosses);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(btnSettings);
        sidebar.add(Box.createVerticalGlue());

        // Content Area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        contentPanel.add(new LogPanel(storage), "LOGS");
        contentPanel.add(new InvoicePanel(storage), "INVOICES");
        contentPanel.add(new BossPanel(storage), "BOSSES");
        contentPanel.add(new SettingsPanel(storage), "SETTINGS");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        checkFirstTimeSetup();
    }

    private void checkFirstTimeSetup() {
        com.github.shanebeee.et.model.EmployeeInfo info = storage.loadEmployeeInfo();
        if (info.getFullName() == null || info.getFullName().trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                int option = JOptionPane.showConfirmDialog(this,
                        "Welcome to Employee Timesheet!\n\nIt looks like you haven't set up your employee information yet.\nWould you like to do that now?",
                        "First Time Setup",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    showPanel("SETTINGS");
                } else {
                    // Even if they skip employee info, check for bosses
                    checkBosses();
                }
            });
        } else {
            // Employee info is set, now check bosses
            checkBosses();
        }
    }

    public void checkBosses() {
        java.util.List<com.github.shanebeee.et.model.Boss> bosses = storage.loadBosses();
        if (bosses.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                int option = JOptionPane.showConfirmDialog(this,
                        "You don't have any bosses set up yet!\nWould you like to add one now?",
                        "No Bosses Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (option == JOptionPane.YES_OPTION) {
                    showPanel("BOSSES");
                }
            });
        }
    }

    public void showPanel(String cardName) {
        cardLayout.show(contentPanel, cardName);
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        
        btn.addActionListener(e -> showPanel(cardName));
        return btn;
    }
}
