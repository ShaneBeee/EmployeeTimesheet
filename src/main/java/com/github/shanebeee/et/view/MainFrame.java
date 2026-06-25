package com.github.shanebeee.et.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final DataStorage storage;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private static final Map<String, Color> NAV_COLORS = Map.of(
        "LOGS",     new Color(59,  130, 246),
        "INVOICES", new Color(139, 92,  246),
        "EXPENSES", new Color(245, 158, 11),
        "BOSSES",   new Color(20,  184, 166),
        "SETTINGS", new Color(100, 116, 139)
    );

    public MainFrame() {
        this.storage = new DataStorage();
        setTitle("Employee Timesheet");
        setIconImage(UIUtils.createAppIcon(64));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);

        initUI();
    }

    private void initUI() {
        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(new Color(248, 250, 252));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel sidebarContent = new JPanel();
        sidebarContent.setLayout(new BoxLayout(sidebarContent, BoxLayout.Y_AXIS));
        sidebarContent.setOpaque(false);

        JLabel titleLabel = new JLabel("<html>Employee<br>Timesheet</html>");
        Image iconImage = UIUtils.createAppIcon(48);
        if (iconImage != null) {
            titleLabel.setIcon(new ImageIcon(iconImage));
            titleLabel.setIconTextGap(10);
        }
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarContent.add(titleLabel);

        JButton logsBtn = createNavButton("Work Logs", "LOGS", "logs.svg");
        sidebarContent.add(logsBtn);
        navButtons.put("LOGS", logsBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton invoicesBtn = createNavButton("Invoice Management", "INVOICES", "invoices.svg");
        sidebarContent.add(invoicesBtn);
        navButtons.put("INVOICES", invoicesBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton expensesBtn = createNavButton("Expenses", "EXPENSES", "expenses.svg");
        sidebarContent.add(expensesBtn);
        navButtons.put("EXPENSES", expensesBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton bossesBtn = createNavButton("Boss Management", "BOSSES", "bosses.svg");
        sidebarContent.add(bossesBtn);
        navButtons.put("BOSSES", bossesBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton settingsBtn = createNavButton("Settings", "SETTINGS", "settings.svg");
        sidebarContent.add(settingsBtn);
        navButtons.put("SETTINGS", settingsBtn);

        sidebar.add(sidebarContent, BorderLayout.NORTH);

        // Content Area
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIManager.getColor("MainContent.background"));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        contentPanel.add(wrapInCard(new LogPanel(storage)),      "LOGS");
        contentPanel.add(wrapInCard(new InvoicePanel(storage)),   "INVOICES");
        contentPanel.add(wrapInCard(new ExpensesPanel(storage)),  "EXPENSES");
        contentPanel.add(wrapInCard(new BossPanel(storage)),      "BOSSES");
        contentPanel.add(wrapInCard(new SettingsPanel(storage)),  "SETTINGS");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Highlight first panel
        updateNavButtons(logsBtn);

        checkFirstTimeSetup();
    }

    private JPanel wrapInCard(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        wrapper.add(panel, BorderLayout.CENTER);

        // Enhanced 3D Card Look
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw a subtle shadow
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 16, 16);

                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 16, 16);

                // Draw background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);

                // Draw border
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 4));
        card.add(wrapper);

        return card;
    }

    private void checkFirstTimeSetup() {
        EmployeeInfo info = storage.loadEmployeeInfo();
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
        List<Boss> bosses = storage.loadBosses();
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
        JButton btn = navButtons.get(cardName);
        if (btn != null) {
            updateNavButtons(btn);
        }
    }

    private JButton createNavButton(String text, String cardName, String iconName) {
        Color navColor = NAV_COLORS.getOrDefault(cardName, new Color(100, 116, 139));
        final boolean[] hovered = {false};

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = getBackground() != null && isOpaque();
                if (isActive) {
                    // Active: filled pill background
                    g2.setColor(new Color(navColor.getRed(), navColor.getGreen(), navColor.getBlue(), 18));
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 10, 10);
                } else if (hovered[0]) {
                    // Hover: subtle highlight
                    g2.setColor(new Color(navColor.getRed(), navColor.getGreen(), navColor.getBlue(), 10));
                    g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        if (iconName != null) {
            // Tint the SVG icon with the nav color
            FlatSVGIcon icon = new FlatSVGIcon("icons/" + iconName, 18, 18);
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> navColor));
            btn.setIcon(icon);
            btn.setIconTextGap(12);
        }
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 10));
        btn.setForeground(new Color(100, 116, 139));
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered[0] = false;
                btn.repaint();
            }
        });
        btn.addActionListener(e -> {
            showPanel(cardName);
            updateNavButtons(btn);
        });
        return btn;
    }

    private void updateNavButtons(JButton activeBtn) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton b = entry.getValue();
            Color navColor = NAV_COLORS.getOrDefault(entry.getKey(), new Color(100, 116, 139));
            if (b == activeBtn) {
                b.setForeground(navColor);
                b.setFont(b.getFont().deriveFont(Font.BOLD));
                b.setOpaque(true);
                b.setBackground(new Color(navColor.getRed(), navColor.getGreen(), navColor.getBlue(), 18));
            } else {
                b.setForeground(new Color(100, 116, 139));
                b.setFont(b.getFont().deriveFont(Font.PLAIN));
                b.setOpaque(false);
            }
            b.repaint();
        }
    }

}
