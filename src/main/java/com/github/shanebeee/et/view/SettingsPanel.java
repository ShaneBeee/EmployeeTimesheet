package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.ExpenseCategory;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

public class SettingsPanel extends JPanel {

    private final DataStorage storage;
    private JPanel categoriesListPanel;
    private List<ExpenseCategory> categories;
    private int categoryYear = LocalDate.now().getYear();

    // Profile tab fields
    private JTextField nameField, companyField, addressField, address2Field, phoneField, emailField;
    private JTextField homeOfficeSqFtField, homeTotalSqFtField;
    private JLabel profileName, profileSub;

    // Preferences tab fields
    private JTextField startField, endField;

    public SettingsPanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        EmployeeInfo info = storage.loadEmployeeInfo();
        categories = storage.loadExpenseCategories(String.valueOf(categoryYear));

        // ── Page header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Tabs ──────────────────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setOpaque(false);
        tabs.setFont(tabs.getFont().deriveFont(Font.PLAIN, 13f));
        tabs.addTab("Profile",            buildProfileTab(info));
        tabs.addTab("Preferences",        buildPreferencesTab());
        tabs.addTab("Expense Categories", buildCategoriesTab());
        tabs.addTab("Tax Brackets",       buildTaxBracketsTab());
        add(tabs, BorderLayout.CENTER);

        // ── Save button (hidden on categories tab) ───────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        bottomPanel.setOpaque(false);
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setBackground((Color) UIManager.get("App.accent"));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD, 13f));
        bottomPanel.add(saveBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        tabs.addChangeListener(e -> bottomPanel.setVisible(tabs.getSelectedIndex() != 2 && tabs.getSelectedIndex() != 3));

        saveBtn.addActionListener(e -> {
            info.setFullName(nameField.getText());
            info.setCompany(companyField.getText());
            info.setAddress(addressField.getText());
            info.setAddress2(address2Field.getText());
            info.setPhoneNumber(phoneField.getText());
            info.setEmail(emailField.getText());
            storage.saveEmployeeInfo(info);
            storage.setDefaultStartTime(TimePickerPanel.unformatTime(startField.getText()));
            storage.setDefaultEndTime(TimePickerPanel.unformatTime(endField.getText()));
            // Save home office sq ft
            try { info.setHomeOfficeSqFt(Double.parseDouble(homeOfficeSqFtField.getText().trim())); } catch (NumberFormatException ignored) { info.setHomeOfficeSqFt(0); }
            try { info.setHomeTotalSqFt(Double.parseDouble(homeTotalSqFtField.getText().trim()));   } catch (NumberFormatException ignored) { info.setHomeTotalSqFt(0); }
            storage.saveEmployeeInfo(info);
            profileName.setText(nameField.getText().isBlank() ? "Your Name" : nameField.getText());
            profileSub.setText(
                (companyField.getText().isBlank() ? "" : companyField.getText()) +
                    (emailField.getText().isBlank() ? "" : "  ·  " + emailField.getText()));
            JOptionPane.showMessageDialog(this, "Settings saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) mainFrame.checkBosses();
        });
    }

    // ── Tab: Profile ──────────────────────────────────────────────────────────

    private JPanel buildProfileTab(EmployeeInfo info) {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        String initials = info.getFullName() == null || info.getFullName().isBlank() ? "?"
            : info.getFullName().contains(" ")
              ? String.valueOf(info.getFullName().charAt(0))
                + info.getFullName().charAt(info.getFullName().indexOf(' ') + 1)
              : String.valueOf(info.getFullName().charAt(0));
        initials = initials.toUpperCase();
        final String fin = initials;
        Color avatarColor = new Color(59, 130, 246);

        JPanel profileStrip = new JPanel(new BorderLayout(14, 0));
        profileStrip.setOpaque(false);
        profileStrip.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        profileStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        profileStrip.setAlignmentX(LEFT_ALIGNMENT);

        JPanel avatarCircle = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, avatarColor.brighter(), 0, getHeight(), avatarColor);
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fin, (getWidth() - fm.stringWidth(fin)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatarCircle.setOpaque(false);
        avatarCircle.setPreferredSize(new Dimension(44, 44));
        avatarCircle.setMinimumSize(new Dimension(44, 44));

        JPanel profileText = new JPanel();
        profileText.setOpaque(false);
        profileText.setLayout(new BoxLayout(profileText, BoxLayout.Y_AXIS));
        profileName = new JLabel(info.getFullName() != null && !info.getFullName().isBlank()
            ? info.getFullName() : "Your Name");
        profileName.setFont(profileName.getFont().deriveFont(Font.BOLD, 14f));
        profileName.setForeground(new Color(30, 41, 59));
        profileSub = new JLabel(
            (info.getCompany() != null && !info.getCompany().isBlank() ? info.getCompany() : "") +
                (info.getEmail() != null && !info.getEmail().isBlank() ? "  ·  " + info.getEmail() : ""));
        profileSub.setFont(profileSub.getFont().deriveFont(Font.PLAIN, 11f));
        profileSub.setForeground(new Color(100, 116, 139));
        profileText.add(profileName);
        profileText.add(Box.createVerticalStrut(2));
        profileText.add(profileSub);

        profileStrip.add(avatarCircle, BorderLayout.WEST);
        profileStrip.add(profileText,  BorderLayout.CENTER);
        tab.add(profileStrip);

        nameField     = new JTextField(info.getFullName());
        companyField  = new JTextField(info.getCompany());
        addressField  = new JTextField(info.getAddress());
        address2Field = new JTextField(info.getAddress2());
        phoneField    = new JTextField(info.getPhoneNumber());
        emailField    = new JTextField(info.getEmail());

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow("Employee Information"));
        card.add(makeDivider());
        card.add(makeFieldRow("Full Name",  nameField));     card.add(makeDivider());
        card.add(makeFieldRow("Company",    companyField));  card.add(makeDivider());
        card.add(makeFieldRow("Address",    addressField));  card.add(makeDivider());
        card.add(makeFieldRow("Address 2",  address2Field)); card.add(makeDivider());
        card.add(makeFieldRow("Phone",      phoneField));    card.add(makeDivider());
        card.add(makeFieldRow("Email",      emailField));
        tab.add(card);

        tab.add(Box.createVerticalStrut(16));

        // Home office card
        homeOfficeSqFtField = new JTextField(
            info.getHomeOfficeSqFt() > 0 ? String.format("%.0f", info.getHomeOfficeSqFt()) : "");
        homeTotalSqFtField = new JTextField(
            info.getHomeTotalSqFt() > 0 ? String.format("%.0f", info.getHomeTotalSqFt()) : "");

        JLabel homeOfficeCalc = new JLabel(homeOfficePctText(info));
        homeOfficeCalc.setFont(homeOfficeCalc.getFont().deriveFont(Font.ITALIC, 11f));
        homeOfficeCalc.setForeground(new Color(100, 116, 139));

        javax.swing.event.DocumentListener homeCalcListener = new javax.swing.event.DocumentListener() {
            void update() {
                try {
                    double off  = Double.parseDouble(homeOfficeSqFtField.getText().trim());
                    double home = Double.parseDouble(homeTotalSqFtField.getText().trim());
                    double pct  = home > 0 ? Math.min(100.0, off / home * 100) : 0;
                    homeOfficeCalc.setText(String.format("Deductible portion: %.1f%%", pct));
                } catch (NumberFormatException e) {
                    homeOfficeCalc.setText("");
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        };
        homeOfficeSqFtField.getDocument().addDocumentListener(homeCalcListener);
        homeTotalSqFtField.getDocument().addDocumentListener(homeCalcListener);

        JPanel homeCard = makeCard();
        homeCard.setLayout(new BoxLayout(homeCard, BoxLayout.Y_AXIS));
        homeCard.add(makeSectionRow("Home Office"));
        homeCard.add(makeDivider());
        homeCard.add(makeFieldRow("Office Area (sq ft)",     homeOfficeSqFtField));
        homeCard.add(makeDivider());
        homeCard.add(makeFieldRow("Total Home Area (sq ft)", homeTotalSqFtField));
        homeCard.add(makeDivider());
        JPanel calcRow = new JPanel(new BorderLayout());
        calcRow.setOpaque(false);
        calcRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        calcRow.add(homeOfficeCalc, BorderLayout.WEST);
        homeCard.add(calcRow);
        tab.add(homeCard);

        JScrollPane scroll = new JScrollPane(tab);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        return wrapper;
    }

    // ── Tab: Preferences ─────────────────────────────────────────────────────

    private JPanel buildPreferencesTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        startField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultStartTime()));
        startField.setEditable(false);
        startField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, startField);
            }
        });
        endField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultEndTime()));
        endField.setEditable(false);
        endField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, endField);
            }
        });

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow("Work Log Defaults"));
        card.add(makeDivider());
        card.add(makeFieldRow("Default Start Time", startField)); card.add(makeDivider());
        card.add(makeFieldRow("Default End Time",   endField));
        tab.add(card);

        tab.add(Box.createVerticalStrut(16));

        // ── Data Location card ─────────────────────────────────────────
        JPanel locationCard = makeCard();
        locationCard.setLayout(new BoxLayout(locationCard, BoxLayout.Y_AXIS));
        locationCard.add(makeSectionRow("Data Location"));
        locationCard.add(makeDivider());

        JTextField pathField = new JTextField(DataStorage.getSavedDataDirectory());
        pathField.setEditable(false);
        pathField.setFont(pathField.getFont().deriveFont(Font.PLAIN, 11f));
        pathField.setForeground(new Color(71, 85, 105));

        JPanel pathRow = new JPanel(new java.awt.BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        pathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JButton changeBtn = new JButton("Change...");
        changeBtn.putClientProperty("JButton.buttonType", "roundRect");
        changeBtn.setFont(changeBtn.getFont().deriveFont(Font.PLAIN, 11f));
        changeBtn.addActionListener(e -> {
            java.awt.FileDialog fd = new java.awt.FileDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Data Folder", java.awt.FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getDirectory() == null) return;
            String newPath = fd.getDirectory() + "EmployeeTimesheet";

            int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Move your data to:<br><b>" + newPath + "</b><br><br>"
                + "Your existing data will be copied there and the app will need to restart.</html>",
                "Change Data Location", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) return;

            try {
                storage.migrateDataTo(newPath);
                pathField.setText(newPath);
                JOptionPane.showMessageDialog(this,
                    "Data moved successfully. Please restart the app.",
                    "Restart Required", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Failed to move data: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        pathRow.add(pathField,  java.awt.BorderLayout.CENTER);
        pathRow.add(changeBtn,  java.awt.BorderLayout.EAST);
        locationCard.add(pathRow);
        tab.add(locationCard);

        JScrollPane scroll = new JScrollPane(tab);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        return wrapper;
    }

    // ── Tab: Expense Categories ───────────────────────────────────────────────

    private JPanel buildCategoriesTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel catCard = makeCard();
        catCard.setLayout(new BoxLayout(catCard, BoxLayout.Y_AXIS));

        // ── Header: title | year nav | action buttons ─────────────────────────
        JPanel catHeader = new JPanel(new BorderLayout(8, 0));
        catHeader.setOpaque(false);
        catHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        catHeader.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));

        JLabel catTitle = new JLabel("EXPENSE CATEGORIES");
        catTitle.setFont(catTitle.getFont().deriveFont(Font.BOLD, 10f));
        catTitle.setForeground(new Color(148, 163, 184));

        // Year nav — centred
        JPanel yearNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        yearNav.setOpaque(false);
        JButton btnPrev = new JButton("<");
        btnPrev.putClientProperty("JButton.buttonType", "roundRect");
        btnPrev.setFont(btnPrev.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel yearLbl = new JLabel(String.valueOf(categoryYear), JLabel.CENTER);
        yearLbl.setFont(yearLbl.getFont().deriveFont(Font.BOLD, 12f));
        yearLbl.setForeground(new Color(30, 41, 59));
        yearLbl.setPreferredSize(new Dimension(44, 20));
        JButton btnNext = new JButton(">");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.setFont(btnNext.getFont().deriveFont(Font.PLAIN, 11f));
        yearNav.add(btnPrev);
        yearNav.add(yearLbl);
        yearNav.add(btnNext);

        // Action buttons — right-aligned
        JPanel catButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        catButtons.setOpaque(false);
        JButton btnReset = new JButton("Reset to Defaults");
        btnReset.putClientProperty("JButton.buttonType", "roundRect");
        btnReset.setFont(btnReset.getFont().deriveFont(Font.PLAIN, 11f));
        btnReset.setForeground(new Color(239, 68, 68));
        JButton btnAddCat = new JButton("+ Add Category");
        btnAddCat.putClientProperty("JButton.buttonType", "roundRect");
        btnAddCat.setFont(btnAddCat.getFont().deriveFont(Font.PLAIN, 11f));
        btnAddCat.addActionListener(e -> showCategoryDialog(null));
        catButtons.add(btnReset);
        catButtons.add(btnAddCat);

        catHeader.add(catTitle,   BorderLayout.WEST);
        catHeader.add(yearNav,    BorderLayout.CENTER);
        catHeader.add(catButtons, BorderLayout.EAST);
        catCard.add(catHeader);
        catCard.add(makeDivider());

        categoriesListPanel = new JPanel();
        categoriesListPanel.setLayout(new BoxLayout(categoriesListPanel, BoxLayout.Y_AXIS));
        categoriesListPanel.setOpaque(false);
        catCard.add(categoriesListPanel);

        // Load and display
        categories = storage.loadExpenseCategories(String.valueOf(categoryYear));
        refreshCategoryList();

        // Wire year nav
        btnPrev.addActionListener(e -> {
            categoryYear--;
            yearLbl.setText(String.valueOf(categoryYear));
            categories = storage.loadExpenseCategories(String.valueOf(categoryYear));
            refreshCategoryList();
        });
        btnNext.addActionListener(e -> {
            categoryYear++;
            yearLbl.setText(String.valueOf(categoryYear));
            categories = storage.loadExpenseCategories(String.valueOf(categoryYear));
            refreshCategoryList();
        });
        btnReset.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Reset " + categoryYear + " categories to the T2125 defaults?\n"
                + "Any custom categories for " + categoryYear + " will be removed.",
                "Reset Categories", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                categories = storage.getDefaultCategories(String.valueOf(categoryYear));
                storage.saveExpenseCategories(String.valueOf(categoryYear), categories);
                refreshCategoryList();
            }
        });

        tab.add(catCard);

        JScrollPane scroll = new JScrollPane(tab);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        return wrapper;
    }

    // ── Category list ─────────────────────────────────────────────────────────

    private void refreshCategoryList() {
        categoriesListPanel.removeAll();
        for (ExpenseCategory cat : categories) {
            categoriesListPanel.add(makeCategoryRow(cat));
            categoriesListPanel.add(makeDivider());
        }
        categoriesListPanel.revalidate();
        categoriesListPanel.repaint();
    }

    private JPanel makeCategoryRow(ExpenseCategory cat) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try { g2.setColor(Color.decode(cat.getColor())); }
                catch (Exception ex) { g2.setColor(new Color(148, 163, 184)); }
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        swatch.setOpaque(false);
        swatch.setPreferredSize(new Dimension(16, 16));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(cat.getLabel());
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.PLAIN, 12f));
        nameLbl.setForeground(new Color(30, 41, 59));
        JLabel hintLbl = new JLabel(
            (cat.getT2125Line() != null ? cat.getT2125Line() : "")
            + (cat.getHint() != null && !cat.getHint().isBlank() ? "  \u00b7  " + cat.getHint() : "")
            + switch (cat.getDeductionType()) {
                case FIXED_PERCENT -> String.format("  \u00b7  %.0f%% business use", cat.getFixedPercent() * 100);
                case KM_PERCENT    -> "  \u00b7  KM-based business use";
                case HOME_OFFICE   -> "  \u00b7  Home office %";
                default            -> "";
            });
        hintLbl.setFont(hintLbl.getFont().deriveFont(Font.PLAIN, 10f));
        hintLbl.setForeground(new Color(148, 163, 184));
        textPanel.add(nameLbl);
        textPanel.add(hintLbl);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(swatch);
        left.add(Box.createHorizontalStrut(10));
        left.add(textPanel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        JButton editBtn = new JButton("Edit");
        editBtn.putClientProperty("JButton.buttonType", "roundRect");
        editBtn.setFont(editBtn.getFont().deriveFont(Font.PLAIN, 11f));
        editBtn.addActionListener(e -> showCategoryDialog(cat));

        if (!cat.isBuiltIn()) {
            JButton delBtn = new JButton("Delete");
            delBtn.putClientProperty("JButton.buttonType", "roundRect");
            delBtn.setFont(delBtn.getFont().deriveFont(Font.PLAIN, 11f));
            delBtn.setForeground((Color) UIManager.get("App.danger"));
            delBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete category \"" + cat.getLabel() + "\"?\nExpenses in this category will show as uncategorized.",
                    "Delete Category", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    categories.remove(cat);
                    storage.saveExpenseCategories(String.valueOf(categoryYear), categories);
                    refreshCategoryList();
                }
            });
            buttons.add(delBtn);
        }
        buttons.add(editBtn);

        row.add(left,    BorderLayout.WEST);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void showCategoryDialog(ExpenseCategory existing) {
        boolean isNew = existing == null;
        ExpenseCategory cat = isNew ? new ExpenseCategory() : existing;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Category" : "Edit Category", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(420, 480);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        JLabel hdrTitle = new JLabel(isNew ? "New Category" : "Edit Category");
        hdrTitle.setFont(hdrTitle.getFont().deriveFont(Font.BOLD, 15f));
        hdrTitle.setForeground(new Color(30, 41, 59));
        hdr.add(hdrTitle, BorderLayout.WEST);
        dialog.add(hdr, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);

        JTextField labelField  = new JTextField(cat.getLabel()     != null ? cat.getLabel()     : "");
        JTextField hintField   = new JTextField(cat.getHint()      != null ? cat.getHint()      : "");
        JTextField t2125Field  = new JTextField(cat.getT2125Line() != null ? cat.getT2125Line() : "");
        JTextField colorField  = new JTextField(cat.getColor()     != null ? cat.getColor()     : "#94A3B8");

        JPanel colorPreview = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try { g2.setColor(Color.decode(colorField.getText().trim())); }
                catch (Exception ex) { g2.setColor(new Color(148, 163, 184)); }
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.dispose();
            }
        };
        colorPreview.setOpaque(false);
        colorPreview.setPreferredSize(new Dimension(28, 28));
        colorPreview.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        colorPreview.setToolTipText("Click to pick a colour");
        colorPreview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Color initial;
                try { initial = Color.decode(colorField.getText().trim()); }
                catch (Exception ex) { initial = new Color(148, 163, 184); }
                Color chosen = JColorChooser.showDialog(dialog, "Choose Category Colour", initial);
                if (chosen != null) {
                    colorField.setText(String.format("#%02X%02X%02X",
                        chosen.getRed(), chosen.getGreen(), chosen.getBlue()));
                }
            }
        });
        colorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { colorPreview.repaint(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { colorPreview.repaint(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { colorPreview.repaint(); }
        });

        JPanel colorRow = new JPanel(new BorderLayout(8, 0));
        colorRow.setOpaque(false);
        colorRow.add(colorField,   BorderLayout.CENTER);
        colorRow.add(colorPreview, BorderLayout.EAST);

        form.add(makeDialogLabel("Name"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(labelField, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeDialogLabel("Hint (shown in expense dialog)"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(hintField, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeDialogLabel("T2125 Line Number"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(t2125Field, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeDialogLabel("Colour (hex, e.g. #3B82F6)"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(colorRow, gbc);

        // ── Deduction type ────────────────────────────────────────────────────
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeDialogLabel("Business Use"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);

        JComboBox<String> deductionCombo = new JComboBox<>(new String[]{
            "100% — Fully deductible",
            "Fixed % — e.g. phone/internet",
            "KM-based % — vehicle expenses",
            "Home office % — rent/utilities"
        });
        ExpenseCategory.DeductionType currentType = cat.getDeductionType();
        deductionCombo.setSelectedIndex(switch (currentType) {
            case FULL          -> 0;
            case FIXED_PERCENT -> 1;
            case KM_PERCENT    -> 2;
            case HOME_OFFICE   -> 3;
        });
        form.add(deductionCombo, gbc);

        JTextField percentField = new JTextField(
            cat.getDeductionType() == ExpenseCategory.DeductionType.FIXED_PERCENT
                ? String.format("%.0f", cat.getFixedPercent() * 100) : "");
        JLabel percentLabel = makeDialogLabel("Business use % (e.g. 60 for 60%)");
        percentLabel.setVisible(currentType == ExpenseCategory.DeductionType.FIXED_PERCENT);
        percentField.setVisible(currentType == ExpenseCategory.DeductionType.FIXED_PERCENT);

        deductionCombo.addActionListener(e -> {
            boolean showPct = deductionCombo.getSelectedIndex() == 1;
            percentLabel.setVisible(showPct);
            percentField.setVisible(showPct);
            dialog.revalidate(); dialog.repaint();
        });

        gbc.gridy++; gbc.insets = new Insets(4, 0, 4, 0);
        form.add(percentLabel, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(percentField, gbc);
        dialog.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerRight.setOpaque(false);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton(isNew ? "Add Category" : "Save");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground((Color) UIManager.get("App.accent"));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            String name = labelField.getText().trim();
            if (name.isBlank()) {
                JOptionPane.showMessageDialog(dialog, "Category name is required.", "Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            cat.setLabel(name);
            cat.setHint(hintField.getText().trim());
            cat.setT2125Line(t2125Field.getText().trim());
            cat.setColor(colorField.getText().trim());
            ExpenseCategory.DeductionType dtype = switch (deductionCombo.getSelectedIndex()) {
                case 1  -> ExpenseCategory.DeductionType.FIXED_PERCENT;
                case 2  -> ExpenseCategory.DeductionType.KM_PERCENT;
                case 3  -> ExpenseCategory.DeductionType.HOME_OFFICE;
                default -> ExpenseCategory.DeductionType.FULL;
            };
            cat.setDeductionType(dtype);
            if (dtype == ExpenseCategory.DeductionType.FIXED_PERCENT) {
                try {
                    double pct = Double.parseDouble(percentField.getText().trim());
                    if (pct <= 0 || pct > 100) throw new NumberFormatException();
                    cat.setFixedPercent(pct / 100.0);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Please enter a valid percentage (1–100).", "Invalid %", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            if (isNew) categories.add(cat);
            storage.saveExpenseCategories(String.valueOf(categoryYear), categories);
            refreshCategoryList();
            dialog.dispose();
        });

        footerRight.add(btnCancel);
        footerRight.add(btnSave);
        footer.add(footerRight, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(btnSave);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Tab: Tax Brackets ──────────────────────────────────────────────────

    private JPanel buildTaxBracketsTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        int year = LocalDate.now().getYear();
        com.github.shanebeee.et.model.TaxBrackets[] bracketsHolder =
            { storage.loadTaxBrackets(year) };

        // Year picker
        JPanel yearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        yearRow.setOpaque(false);
        yearRow.setAlignmentX(LEFT_ALIGNMENT);
        yearRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel yearLbl = new JLabel("Tax Year:");
        yearLbl.setFont(yearLbl.getFont().deriveFont(Font.PLAIN, 12f));
        yearLbl.setForeground(new Color(71, 85, 105));
        SpinnerNumberModel yearModel = new SpinnerNumberModel(year, 2020, year + 2, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setPreferredSize(new Dimension(80, 28));
        JSpinner.NumberEditor yearEditor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        yearSpinner.setEditor(yearEditor);
        yearRow.add(yearLbl);
        yearRow.add(yearSpinner);
        tab.add(yearRow);
        tab.add(Box.createVerticalStrut(12));

        // We'll rebuild the bracket panels when year changes
        JPanel[] bracketsContainer = { new JPanel() };
        bracketsContainer[0].setLayout(new BoxLayout(bracketsContainer[0], BoxLayout.Y_AXIS));
        bracketsContainer[0].setOpaque(false);
        bracketsContainer[0].setAlignmentX(LEFT_ALIGNMENT);

        Runnable[] refreshHolder = { null };

        Runnable buildBracketPanels = () -> {
            JPanel container = bracketsContainer[0];
            container.removeAll();
            com.github.shanebeee.et.model.TaxBrackets brackets = bracketsHolder[0];

            // ── CPP card ─────────────────────────────────────────────────────
            JPanel cppCard = makeCard();
            cppCard.setLayout(new BoxLayout(cppCard, BoxLayout.Y_AXIS));
            cppCard.add(makeSectionRow("CPP (Canada Pension Plan)"));
            cppCard.add(makeDivider());

            JTextField cppRateField = new JTextField(String.format("%.1f", brackets.getCppRate() * 100));
            JTextField cppMaxField  = new JTextField(String.format("%.2f", brackets.getCppMaxContribution()));
            JLabel cppHint = new JLabel("Self-employed pay both employee + employer sides (combined rate)");
            cppHint.setFont(cppHint.getFont().deriveFont(Font.ITALIC, 10f));
            cppHint.setForeground(new Color(148, 163, 184));

            cppCard.add(makeFieldRow("Combined Rate (%)",    cppRateField));
            cppCard.add(makeDivider());
            cppCard.add(makeFieldRow("Annual Maximum ($)",   cppMaxField));
            cppCard.add(makeDivider());
            JPanel hintRow = new JPanel(new BorderLayout());
            hintRow.setOpaque(false);
            hintRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            hintRow.add(cppHint, BorderLayout.WEST);
            cppCard.add(hintRow);

            JButton saveCpp = new JButton("Save CPP");
            saveCpp.putClientProperty("JButton.buttonType", "roundRect");
            saveCpp.setBackground(new Color(16, 185, 129));
            saveCpp.setForeground(Color.WHITE);
            saveCpp.setAlignmentX(LEFT_ALIGNMENT);
            saveCpp.addActionListener(e -> {
                try {
                    brackets.setCppRate(Double.parseDouble(cppRateField.getText().trim()) / 100.0);
                    brackets.setCppMaxContribution(Double.parseDouble(cppMaxField.getText().trim()));
                    storage.saveTaxBrackets(brackets);
                    JOptionPane.showMessageDialog(this, "CPP settings saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Error", JOptionPane.WARNING_MESSAGE);
                }
            });
            JPanel cppBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
            cppBtnRow.setOpaque(false);
            cppBtnRow.add(saveCpp);
            cppCard.add(cppBtnRow);
            container.add(cppCard);
            container.add(Box.createVerticalStrut(12));

            // ── Federal brackets card ─────────────────────────────────────────
            container.add(makeBracketCard("Federal Income Tax Brackets",
                brackets.getFederal(), brackets, true));
            container.add(Box.createVerticalStrut(12));

            // ── BC brackets card ──────────────────────────────────────────────
            container.add(makeBracketCard("BC Provincial Tax Brackets",
                brackets.getBc(), brackets, false));

            container.revalidate();
            container.repaint();
        };
        refreshHolder[0] = buildBracketPanels;
        buildBracketPanels.run();

        yearSpinner.addChangeListener(e -> {
            int y = (int) yearSpinner.getValue();
            bracketsHolder[0] = storage.loadTaxBrackets(y);
            buildBracketPanels.run();
        });

        tab.add(bracketsContainer[0]);

        JScrollPane scroll = new JScrollPane(tab);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scroll);
        return wrapper;
    }

    private JPanel makeBracketCard(String title,
            java.util.List<com.github.shanebeee.et.model.TaxBrackets.Bracket> brackets,
            com.github.shanebeee.et.model.TaxBrackets taxBrackets, boolean isFederal) {

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow(title));
        card.add(makeDivider());

        // Column headers
        JPanel colHdr = new JPanel(new java.awt.GridLayout(1, 3, 8, 0));
        colHdr.setOpaque(false);
        colHdr.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        colHdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        for (String h : new String[]{"Up To ($)", "Rate (%)", ""}) {
            JLabel l = new JLabel(h);
            l.setFont(l.getFont().deriveFont(Font.BOLD, 9f));
            l.setForeground(new Color(148, 163, 184));
            colHdr.add(l);
        }
        card.add(colHdr);
        card.add(makeDivider());

        // Bracket rows — each row has upTo field, rate field, delete button
        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setOpaque(false);

        Runnable refreshRows = () -> {
            rowsPanel.removeAll();
            for (int i = 0; i < brackets.size(); i++) {
                final int idx = i;
                com.github.shanebeee.et.model.TaxBrackets.Bracket b = brackets.get(i);
                JPanel row = new JPanel(new java.awt.GridLayout(1, 3, 8, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

                boolean isLast = (i == brackets.size() - 1);
                JTextField upToField = new JTextField(
                    isLast ? "No limit" : String.format("%.0f", b.getUpTo()));
                upToField.setEnabled(!isLast);
                JTextField rateField = new JTextField(
                    String.format("%.2f", b.getRate() * 100));

                JButton delBtn = new JButton("✕");
                delBtn.putClientProperty("JButton.buttonType", "roundRect");
                delBtn.setFont(delBtn.getFont().deriveFont(Font.PLAIN, 10f));
                delBtn.setEnabled(brackets.size() > 1);
                delBtn.addActionListener(e -> {
                    brackets.remove(idx);
                    rowsPanel.removeAll();
                    rowsPanel.revalidate();
                });

                row.add(upToField);
                row.add(rateField);
                row.add(delBtn);
                rowsPanel.add(row);

                // Wire field changes into bracket object on focus-lost
                upToField.addFocusListener(new java.awt.event.FocusAdapter() {
                    public void focusLost(java.awt.event.FocusEvent e) {
                        if (!isLast) try { b.setUpTo(Double.parseDouble(upToField.getText().trim())); } catch (NumberFormatException ignored) {}
                    }
                });
                rateField.addFocusListener(new java.awt.event.FocusAdapter() {
                    public void focusLost(java.awt.event.FocusEvent e) {
                        try { b.setRate(Double.parseDouble(rateField.getText().trim()) / 100.0); } catch (NumberFormatException ignored) {}
                    }
                });
            }
            rowsPanel.revalidate();
            rowsPanel.repaint();
        };
        refreshRows.run();
        card.add(rowsPanel);
        card.add(makeDivider());

        // Add bracket + Save buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnRow.setOpaque(false);

        JButton btnAdd = new JButton("+ Add Bracket");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.PLAIN, 11f));
        btnAdd.addActionListener(e -> {
            // Insert before the last ("no limit") bracket
            double prevUpTo = brackets.size() > 1
                ? brackets.get(brackets.size() - 2).getUpTo() + 10000 : 50000;
            com.github.shanebeee.et.model.TaxBrackets.Bracket newB =
                new com.github.shanebeee.et.model.TaxBrackets.Bracket(prevUpTo, 0.20);
            brackets.add(brackets.size() - 1, newB); // before the last
            refreshRows.run();
        });

        JButton btnSave = new JButton("Save Brackets");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            // Ensure last bracket has 1e12 as upTo
            if (!brackets.isEmpty()) brackets.get(brackets.size() - 1).setUpTo(1e12);
            if (isFederal) taxBrackets.setFederal(brackets);
            else           taxBrackets.setBc(brackets);
            storage.saveTaxBrackets(taxBrackets);
            JOptionPane.showMessageDialog(this, title + " saved.", "Saved", JOptionPane.INFORMATION_MESSAGE);
        });

        btnRow.add(btnAdd);
        btnRow.add(btnSave);
        card.add(btnRow);
        return card;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private JLabel makeDialogLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        return lbl;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
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
        card.setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel makeSectionRow(String title) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        row.add(lbl, BorderLayout.WEST);
        return row;
    }

    private JPanel makeFieldRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        lbl.setPreferredSize(new Dimension(160, 20));
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));
        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
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

    private String homeOfficePctText(com.github.shanebeee.et.model.EmployeeInfo info) {
        if (info.getHomeTotalSqFt() <= 0) return "";
        double pct = Math.min(100.0, info.getHomeOfficeSqFt() / info.getHomeTotalSqFt() * 100);
        return String.format("Deductible portion: %.1f%%", pct);
    }
}
