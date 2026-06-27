package com.github.shanebeee.et.view;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.SearchEngine;
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
    private InvoicePanel invoicePanel;
    private final Map<String, JButton> navButtons = new HashMap<>();
    private static final Map<String, Color> NAV_COLORS = Map.of(
        "DASHBOARD",  new Color(30,  41,  59),
        "LOGS",       new Color(59,  130, 246),
        "INVOICES",   new Color(139, 92,  246),
        "EXPENSES",   new Color(245, 158, 11),
        "KM",         new Color(16,  185, 129),
        "ACCOUNTING", new Color(99,  102, 241),
        "BOSSES",     new Color(20,  184, 166),
        "SETTINGS",   new Color(100, 116, 139)
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

        // ── Search bar ────────────────────────────────────────────────────────
        SearchEngine searchEngine = new SearchEngine(storage);
        JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "🔍  Search...");
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN, 13f));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel searchWrap = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        searchWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        searchWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setPreferredSize(new Dimension(210, 34));
        searchWrap.add(searchField);
        sidebarContent.add(searchWrap);

        // Search results popup
        JPopupMenu searchPopup = new JPopupMenu();
        searchPopup.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { runSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { runSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { runSearch(); }
            void runSearch() {
                SwingUtilities.invokeLater(() -> {
                    String q = searchField.getText().trim();
                    searchPopup.setVisible(false);
                    searchPopup.removeAll();
                    if (q.isEmpty()) return;

                    List<SearchEngine.SearchResult> results = searchEngine.search(q, 8);
                    if (results.isEmpty()) {
                        JMenuItem none = new JMenuItem("No results for \"" + q + "\"");
                        none.setEnabled(false);
                        none.setFont(none.getFont().deriveFont(Font.ITALIC, 12f));
                        searchPopup.add(none);
                    } else {
                        String lastPanel = null;
                        for (SearchEngine.SearchResult r : results) {
                            // Section separator when panel changes
                            if (!r.panel().equals(lastPanel)) {
                                if (lastPanel != null) searchPopup.addSeparator();
                                JLabel section = new JLabel("  " + panelLabel(r.panel()));
                                section.setFont(section.getFont().deriveFont(Font.BOLD, 10f));
                                section.setForeground(new Color(148, 163, 184));
                                section.setBorder(BorderFactory.createEmptyBorder(4, 4, 2, 4));
                                searchPopup.add(section);
                                lastPanel = r.panel();
                            }
                            JMenuItem item = new JMenuItem();
                            item.setLayout(new BorderLayout(8, 0));
                            JLabel emoji = new JLabel(r.emoji());
                            emoji.setFont(emoji.getFont().deriveFont(14f));
                            JPanel text = new JPanel();
                            text.setOpaque(false);
                            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
                            JLabel title = new JLabel(r.title());
                            title.setFont(title.getFont().deriveFont(Font.PLAIN, 12f));
                            title.setForeground(new Color(30, 41, 59));
                            JLabel sub = new JLabel(r.subtitle() + "  ·  " + r.date());
                            sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 10f));
                            sub.setForeground(new Color(148, 163, 184));
                            text.add(title);
                            text.add(sub);
                            item.add(emoji, BorderLayout.WEST);
                            item.add(text,  BorderLayout.CENTER);
                            item.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                            item.addActionListener(ae -> {
                                showPanel(r.panel());
                                searchField.setText("");
                                searchPopup.setVisible(false);
                            });
                            searchPopup.add(item);
                        }
                    }
                    searchPopup.show(searchField, 0, searchField.getHeight());
                    searchPopup.setPreferredSize(new Dimension(searchField.getWidth(), -1));
                    searchField.requestFocusInWindow();
                });
            }
        });

        // Escape closes popup
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    searchPopup.setVisible(false);
                    searchField.setText("");
                }
            }
        });

        JButton dashBtn = createNavButton("Dashboard", "DASHBOARD", "dashboard.svg");
        sidebarContent.add(dashBtn);
        navButtons.put("DASHBOARD", dashBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton logsBtn = createNavButton("Work Logs", "LOGS", "logs.svg");
        sidebarContent.add(logsBtn);
        navButtons.put("LOGS", logsBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton expensesBtn = createNavButton("Expenses", "EXPENSES", "expenses.svg");
        sidebarContent.add(expensesBtn);
        navButtons.put("EXPENSES", expensesBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton kmBtn = createNavButton("Kilometre Log", "KM", "km.svg");
        sidebarContent.add(kmBtn);
        navButtons.put("KM", kmBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton invoicesBtn = createNavButton("Invoice Management", "INVOICES", "invoices.svg");
        sidebarContent.add(invoicesBtn);
        navButtons.put("INVOICES", invoicesBtn);
        sidebarContent.add(Box.createVerticalStrut(5));

        JButton accountingBtn = createNavButton("Accounting", "ACCOUNTING", "accounting.svg");
        sidebarContent.add(accountingBtn);
        navButtons.put("ACCOUNTING", accountingBtn);
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

        DashboardPanel dashboardPanel = new DashboardPanel(storage);
        dashboardPanel.setMainFrame(this);
        invoicePanel = new InvoicePanel(storage);
        contentPanel.add(wrapInCard(dashboardPanel), "DASHBOARD");
        contentPanel.add(wrapInCard(new LogPanel(storage)),      "LOGS");
        contentPanel.add(wrapInCard(invoicePanel),               "INVOICES");
        contentPanel.add(wrapInCard(new ExpensesPanel(storage)),  "EXPENSES");
        contentPanel.add(wrapInCard(new KmLogPanel(storage)),      "KM");
        contentPanel.add(wrapInCard(new AccountingPanel(storage)),  "ACCOUNTING");
        contentPanel.add(wrapInCard(new BossPanel(storage)),        "BOSSES");
        contentPanel.add(wrapInCard(new SettingsPanel(storage)),  "SETTINGS");

        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Show dashboard by default and refresh it each time it becomes visible
        updateNavButtons(dashBtn);
        cardLayout.show(contentPanel, "DASHBOARD");
        dashboardPanel.refresh();

        // Refresh dashboard whenever it becomes visible
        dashBtn.addActionListener(e -> dashboardPanel.refresh());

        // Show onboarding wizard on first launch
        SwingUtilities.invokeLater(() -> {
            if (!DataStorage.isOnboardingComplete()) {
                OnboardingWizard wizard = new OnboardingWizard(this);
                wizard.setVisible(true);
                // After wizard closes, reinitialize storage and refresh
                dashboardPanel.refresh();
            } else {
                checkBosses();
            }
        });
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
        if ("INVOICES".equals(cardName)) {
            invoicePanel.refreshHistory();
        }
    }

    public void showTaxSetAside(com.github.shanebeee.et.model.Invoice inv) {
        invoicePanel.showTaxSetAsideDialog(inv);
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

    private String panelLabel(String panel) {
        return switch (panel) {
            case "LOGS"       -> "WORK LOGS";
            case "EXPENSES"   -> "EXPENSES";
            case "KM"         -> "KILOMETRE LOG";
            case "INVOICES"   -> "INVOICES";
            case "ACCOUNTING" -> "ACCOUNTING";
            default           -> panel;
        };
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
