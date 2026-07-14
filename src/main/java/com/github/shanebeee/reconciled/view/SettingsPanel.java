package com.github.shanebeee.reconciled.view;

import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.ExpenseCategory;
import com.github.shanebeee.reconciled.model.UserProfile;
import com.github.shanebeee.reconciled.storage.DataStorage;
import com.github.shanebeee.reconciled.storage.ProfileManager;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
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
    private final ProfileManager profileManager;

    // Employee Info tab fields
    private JTextField nameField, companyField, addressField, address2Field, phoneField, emailField;
    private JTextField homeOfficeSqFtField, homeTotalSqFtField;

    // Preferences tab fields
    private JTextField startField, endField;

    // Categories
    private JPanel categoriesListPanel;
    private List<ExpenseCategory> categories;
    private int categoryYear = LocalDate.now().getYear();

    public SettingsPanel(DataStorage storage, ProfileManager profileManager) {
        this.storage = storage;
        this.profileManager = profileManager;
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        EmployeeInfo info = storage.loadEmployeeInfo();
        categories = storage.loadExpenseCategories(String.valueOf(categoryYear));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setOpaque(false);
        tabs.setFont(tabs.getFont().deriveFont(Font.PLAIN, 13f));
        tabs.addTab("Employee Info",      buildEmployeeInfoTab(info));
        tabs.addTab("Preferences",        buildPreferencesTab());
        tabs.addTab("User Profiles",      buildProfilesTab());
        tabs.addTab("Expense Categories", buildCategoriesTab());
        tabs.addTab("Tax Brackets",       buildTaxBracketsTab());
        add(tabs, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        bottomPanel.setOpaque(false);
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setBackground((Color) UIManager.get("App.accent"));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD, 13f));
        bottomPanel.add(saveBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            bottomPanel.setVisible(idx == 0 || idx == 1);
        });

        saveBtn.addActionListener(e -> {
            info.setFullName(nameField.getText());
            info.setCompany(companyField.getText());
            info.setAddress(addressField.getText());
            info.setAddress2(address2Field.getText());
            info.setPhoneNumber(phoneField.getText());
            info.setEmail(emailField.getText());
            try { info.setHomeOfficeSqFt(Double.parseDouble(homeOfficeSqFtField.getText().trim())); }
            catch (NumberFormatException ignored) { info.setHomeOfficeSqFt(0); }
            try { info.setHomeTotalSqFt(Double.parseDouble(homeTotalSqFtField.getText().trim())); }
            catch (NumberFormatException ignored) { info.setHomeTotalSqFt(0); }
            storage.saveEmployeeInfo(info);
            storage.setDefaultStartTime(TimePickerPanel.unformatTime(startField.getText()));
            storage.setDefaultEndTime(TimePickerPanel.unformatTime(endField.getText()));
            JOptionPane.showMessageDialog(this, "Settings saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) mainFrame.checkBosses();
        });
    }

    // ── Tab: Employee Info ────────────────────────────────────────────────────

    private JPanel buildEmployeeInfoTab(EmployeeInfo info) {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        nameField     = new JTextField(info.getFullName());
        companyField  = new JTextField(info.getCompany());
        addressField  = new JTextField(info.getAddress());
        address2Field = new JTextField(info.getAddress2());
        phoneField    = new JTextField(info.getPhoneNumber());
        emailField    = new JTextField(info.getEmail());

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow("Employee Information")); card.add(makeDivider());
        card.add(makeFieldRow("Full Name",  nameField));     card.add(makeDivider());
        card.add(makeFieldRow("Company",    companyField));  card.add(makeDivider());
        card.add(makeFieldRow("Address",    addressField));  card.add(makeDivider());
        card.add(makeFieldRow("Address 2",  address2Field)); card.add(makeDivider());
        card.add(makeFieldRow("Phone",      phoneField));    card.add(makeDivider());
        card.add(makeFieldRow("Email",      emailField));
        tab.add(card);
        tab.add(Box.createVerticalStrut(16));

        homeOfficeSqFtField = new JTextField(info.getHomeOfficeSqFt() > 0 ? String.format("%.0f", info.getHomeOfficeSqFt()) : "");
        homeTotalSqFtField  = new JTextField(info.getHomeTotalSqFt()  > 0 ? String.format("%.0f", info.getHomeTotalSqFt())  : "");
        JLabel homeCalc = new JLabel(homeOfficePctText(info));
        homeCalc.setFont(homeCalc.getFont().deriveFont(Font.ITALIC, 11f));
        homeCalc.setForeground(new Color(100, 116, 139));

        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            void update() {
                try {
                    double off = Double.parseDouble(homeOfficeSqFtField.getText().trim());
                    double tot = Double.parseDouble(homeTotalSqFtField.getText().trim());
                    homeCalc.setText(String.format("Deductible portion: %.1f%%", tot > 0 ? Math.min(100.0, off / tot * 100) : 0));
                } catch (NumberFormatException ex) { homeCalc.setText(""); }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { update(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        };
        homeOfficeSqFtField.getDocument().addDocumentListener(dl);
        homeTotalSqFtField.getDocument().addDocumentListener(dl);

        JPanel homeCard = makeCard();
        homeCard.setLayout(new BoxLayout(homeCard, BoxLayout.Y_AXIS));
        homeCard.add(makeSectionRow("Home Office")); homeCard.add(makeDivider());
        homeCard.add(makeFieldRow("Office Area (sq ft)",      homeOfficeSqFtField)); homeCard.add(makeDivider());
        homeCard.add(makeFieldRow("Total Home Area (sq ft)",  homeTotalSqFtField));  homeCard.add(makeDivider());
        JPanel calcRow = new JPanel(new BorderLayout());
        calcRow.setOpaque(false); calcRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        calcRow.add(homeCalc, BorderLayout.WEST); homeCard.add(calcRow);
        tab.add(homeCard);

        return wrapInScroll(tab);
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
            @Override public void mouseClicked(MouseEvent e) { TimePickerPanel.showPicker(SettingsPanel.this, startField); }
        });
        endField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultEndTime()));
        endField.setEditable(false);
        endField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { TimePickerPanel.showPicker(SettingsPanel.this, endField); }
        });

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow("Work Log Defaults")); card.add(makeDivider());
        card.add(makeFieldRow("Default Start Time", startField)); card.add(makeDivider());
        card.add(makeFieldRow("Default End Time",   endField));
        tab.add(card);

        return wrapInScroll(tab);
    }

    // ── Tab: User Profiles ────────────────────────────────────────────────────

    private JPanel buildProfilesTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow("User Profiles")); card.add(makeDivider());

        if (profileManager == null) {
            JLabel unavail = new JLabel("Profile management unavailable.");
            unavail.setFont(unavail.getFont().deriveFont(Font.ITALIC, 11f));
            unavail.setForeground(new Color(148, 163, 184));
            card.add(unavail);
        } else {
            JPanel listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);

            Runnable[] refreshRef = { null };
            Runnable refresh = () -> {
                listPanel.removeAll();
                List<UserProfile> profiles = profileManager.loadProfiles();
                for (UserProfile p : profiles) {
                    Color ac;
                    try { ac = Color.decode(p.getAvatarColor()); } catch (Exception ex) { ac = new Color(59, 130, 246); }
                    final Color finalAc = ac;

                    JPanel swatch = new JPanel() {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(finalAc);
                            g2.fillOval(0, 0, getWidth(), getHeight());
                            g2.setColor(Color.WHITE);
                            g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                            java.awt.FontMetrics fm = g2.getFontMetrics();
                            String ini = p.initials();
                            g2.drawString(ini, (getWidth() - fm.stringWidth(ini)) / 2,
                                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                            g2.dispose();
                        }
                    };
                    swatch.setOpaque(false);
                    swatch.setPreferredSize(new Dimension(24, 24));
                    swatch.setMinimumSize(new Dimension(24, 24));

                    JLabel nameLbl = new JLabel(p.getName());
                    nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 12f));
                    nameLbl.setForeground(new Color(30, 41, 59));
                    JLabel pathLbl = new JLabel(shortenPath(p.getDataPath()));
                    pathLbl.setFont(pathLbl.getFont().deriveFont(Font.PLAIN, 10f));
                    pathLbl.setForeground(new Color(148, 163, 184));

                    JPanel nameCol = new JPanel();
                    nameCol.setOpaque(false);
                    nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
                    nameCol.add(nameLbl); nameCol.add(Box.createVerticalStrut(2)); nameCol.add(pathLbl);

                    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                    left.setOpaque(false); left.add(swatch); left.add(nameCol);

                    JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
                    btns.setOpaque(false);

                    JButton editBtn = new JButton("Edit");
                    editBtn.putClientProperty("JButton.buttonType", "roundRect");
                    editBtn.setFont(editBtn.getFont().deriveFont(Font.PLAIN, 10f));
                    editBtn.addActionListener(e -> showProfileEditDialog(p, refreshRef));
                    btns.add(editBtn);

                    if (profiles.size() > 1) {
                        JButton deleteBtn = new JButton("Delete");
                        deleteBtn.putClientProperty("JButton.buttonType", "roundRect");
                        deleteBtn.setFont(deleteBtn.getFont().deriveFont(Font.PLAIN, 10f));
                        deleteBtn.setForeground((Color) UIManager.get("App.danger"));
                        deleteBtn.addActionListener(e -> {
                            int confirm = JOptionPane.showConfirmDialog(this,
                                "Delete profile \"" + p.getName() + "\"?\nThis does not delete any data files.",
                                "Delete Profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                            if (confirm == JOptionPane.OK_OPTION) {
                                profileManager.deleteProfile(p.getId());
                                refreshRef[0].run();
                            }
                        });
                        btns.add(deleteBtn);
                    }

                    JPanel row = new JPanel(new BorderLayout(8, 0));
                    row.setOpaque(false);
                    row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
                    row.add(left, BorderLayout.WEST); row.add(btns, BorderLayout.EAST);
                    listPanel.add(row); listPanel.add(makeDivider());
                }
                listPanel.revalidate(); listPanel.repaint();
            };
            refreshRef[0] = refresh;
            refresh.run();
            card.add(listPanel);

            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
            addRow.setOpaque(false);
            JButton addBtn = new JButton("+ Add Profile");
            addBtn.putClientProperty("JButton.buttonType", "roundRect");
            addBtn.setFont(addBtn.getFont().deriveFont(Font.PLAIN, 11f));
            addBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "Enter a name for the new profile:", "New Profile", JOptionPane.PLAIN_MESSAGE);
                if (name != null && !name.isBlank()) { profileManager.createProfile(name.trim()); refresh.run(); }
            });
            addRow.add(addBtn); card.add(addRow);
        }
        tab.add(card);
        return wrapInScroll(tab);
    }

    private void showProfileEditDialog(UserProfile p, Runnable[] refreshRef) {
        DataStorage profileStorage = new DataStorage(p.getDataPath());
        EmployeeInfo pInfo = profileStorage.loadEmployeeInfo();

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Edit Profile — " + p.getName(), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(440, 560);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        Color[] ac = { new Color(59, 130, 246) };
        try { ac[0] = Color.decode(p.getAvatarColor()); } catch (Exception ignored) {}

        JPanel avatarPreview = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ac[0]);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String ini = p.initials();
                g2.drawString(ini, (getWidth() - fm.stringWidth(ini)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatarPreview.setOpaque(false);
        avatarPreview.setPreferredSize(new Dimension(36, 36));

        JLabel hdrTitle = new JLabel(p.getName());
        hdrTitle.setFont(hdrTitle.getFont().deriveFont(Font.BOLD, 15f));
        hdrTitle.setForeground(new Color(30, 41, 59));
        JPanel hdrLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        hdrLeft.setOpaque(false); hdrLeft.add(avatarPreview); hdrLeft.add(hdrTitle);
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        hdr.add(hdrLeft, BorderLayout.WEST);
        dialog.add(hdr, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        gc.gridx = 0; gc.gridy = 0; gc.insets = new Insets(0, 0, 4, 0);

        JTextField colorField = new JTextField(p.getAvatarColor() != null ? p.getAvatarColor() : "#3B82F6");
        JPanel colorSwatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try { g2.setColor(Color.decode(colorField.getText().trim())); }
                catch (Exception ex) { g2.setColor(new Color(148, 163, 184)); }
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.dispose();
            }
        };
        colorSwatch.setOpaque(false);
        colorSwatch.setPreferredSize(new Dimension(28, 28));
        colorSwatch.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        colorSwatch.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Color init;
                try { init = Color.decode(colorField.getText().trim()); } catch (Exception ex) { init = new Color(59, 130, 246); }
                Color chosen = JColorChooser.showDialog(dialog, "Choose Avatar Colour", init);
                if (chosen != null) {
                    String hex = String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
                    colorField.setText(hex); ac[0] = chosen;
                    avatarPreview.repaint(); colorSwatch.repaint();
                }
            }
        });
        colorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { colorSwatch.repaint(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { colorSwatch.repaint(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { colorSwatch.repaint(); }
        });
        JPanel colorRow = new JPanel(new BorderLayout(8, 0));
        colorRow.setOpaque(false);
        colorRow.add(colorField, BorderLayout.CENTER); colorRow.add(colorSwatch, BorderLayout.EAST);

        JTextField pNameField     = new JTextField(pInfo.getFullName()    != null ? pInfo.getFullName()    : "");
        JTextField pCompanyField  = new JTextField(pInfo.getCompany()     != null ? pInfo.getCompany()     : "");
        JTextField pAddressField  = new JTextField(pInfo.getAddress()     != null ? pInfo.getAddress()     : "");
        JTextField pAddress2Field = new JTextField(pInfo.getAddress2()    != null ? pInfo.getAddress2()    : "");
        JTextField pPhoneField    = new JTextField(pInfo.getPhoneNumber() != null ? pInfo.getPhoneNumber() : "");
        JTextField pEmailField    = new JTextField(pInfo.getEmail()       != null ? pInfo.getEmail()       : "");
        JTextField pOfficeSqFt    = new JTextField(pInfo.getHomeOfficeSqFt() > 0 ? String.format("%.0f", pInfo.getHomeOfficeSqFt()) : "");
        JTextField pHomeSqFt      = new JTextField(pInfo.getHomeTotalSqFt()  > 0 ? String.format("%.0f", pInfo.getHomeTotalSqFt())  : "");

        addFormRow(form, gc, "Avatar Colour",            colorRow);       gc.gridy++;
        addFormRow(form, gc, "Display Name",             pNameField);     gc.gridy++;
        addFormRow(form, gc, "Company",                  pCompanyField);  gc.gridy++;
        addFormRow(form, gc, "Address",                  pAddressField);  gc.gridy++;
        addFormRow(form, gc, "Address 2",                pAddress2Field); gc.gridy++;
        addFormRow(form, gc, "Phone",                    pPhoneField);    gc.gridy++;
        addFormRow(form, gc, "Email",                    pEmailField);    gc.gridy++;
        addFormRow(form, gc, "Office Area (sq ft)",      pOfficeSqFt);    gc.gridy++;
        addFormRow(form, gc, "Home Total Area (sq ft)",  pHomeSqFt);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(new Color(248, 250, 252));
        dialog.add(formScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JPanel footerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerBtns.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.putClientProperty("JButton.buttonType", "roundRect");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setBackground((Color) UIManager.get("App.accent"));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> {
            String newName = pNameField.getText().trim();
            p.setName(newName.isBlank() ? p.getName() : newName);
            p.setAvatarColor(colorField.getText().trim());
            profileManager.updateProfile(p);

            pInfo.setFullName(pNameField.getText().trim());
            pInfo.setCompany(pCompanyField.getText().trim());
            pInfo.setAddress(pAddressField.getText().trim());
            pInfo.setAddress2(pAddress2Field.getText().trim());
            pInfo.setPhoneNumber(pPhoneField.getText().trim());
            pInfo.setEmail(pEmailField.getText().trim());
            try { pInfo.setHomeOfficeSqFt(Double.parseDouble(pOfficeSqFt.getText().trim())); } catch (NumberFormatException ignored) {}
            try { pInfo.setHomeTotalSqFt(Double.parseDouble(pHomeSqFt.getText().trim())); }   catch (NumberFormatException ignored) {}
            profileStorage.saveEmployeeInfo(pInfo);

            if (refreshRef != null) refreshRef[0].run();
            dialog.dispose();
        });

        footerBtns.add(cancelBtn); footerBtns.add(saveBtn);
        footer.add(footerBtns, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(saveBtn);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel form, GridBagConstraints gc, String label, JComponent field) {
        gc.insets = new Insets(0, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(new Color(100, 116, 139));
        form.add(lbl, gc); gc.gridy++;
        gc.insets = new Insets(0, 0, 10, 0);
        form.add(field, gc);
    }

    private String shortenPath(String path) {
        if (path == null) return "";
        String p = path.endsWith(java.io.File.separator) ? path.substring(0, path.length() - 1) : path;
        String[] parts = p.split(java.util.regex.Pattern.quote(java.io.File.separator));
        if (parts.length <= 3) return path;
        return "\u2026/" + parts[parts.length - 2] + "/" + parts[parts.length - 1] + "/";
    }

    // ── Tab: Expense Categories ───────────────────────────────────────────────

    private JPanel buildCategoriesTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel catCard = makeCard();
        catCard.setLayout(new BoxLayout(catCard, BoxLayout.Y_AXIS));

        JPanel catHeader = new JPanel(new BorderLayout(8, 0));
        catHeader.setOpaque(false);
        catHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        catHeader.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        JLabel catTitle = new JLabel("EXPENSE CATEGORIES");
        catTitle.setFont(catTitle.getFont().deriveFont(Font.BOLD, 10f));
        catTitle.setForeground(new Color(148, 163, 184));

        JPanel yearNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        yearNav.setOpaque(false);
        JButton btnPrev = new JButton("<"); btnPrev.putClientProperty("JButton.buttonType", "roundRect"); btnPrev.setFont(btnPrev.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel yearLbl = new JLabel(String.valueOf(categoryYear), JLabel.CENTER);
        yearLbl.setFont(yearLbl.getFont().deriveFont(Font.BOLD, 12f)); yearLbl.setForeground(new Color(30, 41, 59)); yearLbl.setPreferredSize(new Dimension(44, 20));
        JButton btnNext = new JButton(">"); btnNext.putClientProperty("JButton.buttonType", "roundRect"); btnNext.setFont(btnNext.getFont().deriveFont(Font.PLAIN, 11f));
        yearNav.add(btnPrev); yearNav.add(yearLbl); yearNav.add(btnNext);

        JPanel catButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        catButtons.setOpaque(false);
        JButton btnReset = new JButton("Reset to Defaults"); btnReset.putClientProperty("JButton.buttonType", "roundRect"); btnReset.setFont(btnReset.getFont().deriveFont(Font.PLAIN, 11f)); btnReset.setForeground(new Color(239, 68, 68));
        JButton btnAddCat = new JButton("+ Add Category"); btnAddCat.putClientProperty("JButton.buttonType", "roundRect"); btnAddCat.setFont(btnAddCat.getFont().deriveFont(Font.PLAIN, 11f));
        btnAddCat.addActionListener(e -> showCategoryDialog(null));
        catButtons.add(btnReset); catButtons.add(btnAddCat);

        catHeader.add(catTitle, BorderLayout.WEST); catHeader.add(yearNav, BorderLayout.CENTER); catHeader.add(catButtons, BorderLayout.EAST);
        catCard.add(catHeader); catCard.add(makeDivider());

        categoriesListPanel = new JPanel();
        categoriesListPanel.setLayout(new BoxLayout(categoriesListPanel, BoxLayout.Y_AXIS));
        categoriesListPanel.setOpaque(false);
        catCard.add(categoriesListPanel);
        categories = storage.loadExpenseCategories(String.valueOf(categoryYear));
        refreshCategoryList();

        btnPrev.addActionListener(e -> { categoryYear--; yearLbl.setText(String.valueOf(categoryYear)); categories = storage.loadExpenseCategories(String.valueOf(categoryYear)); refreshCategoryList(); });
        btnNext.addActionListener(e -> { categoryYear++; yearLbl.setText(String.valueOf(categoryYear)); categories = storage.loadExpenseCategories(String.valueOf(categoryYear)); refreshCategoryList(); });
        btnReset.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this, "Reset " + categoryYear + " categories to defaults?\nCustom categories will be removed.", "Reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c == JOptionPane.YES_OPTION) { categories = storage.getDefaultCategories(String.valueOf(categoryYear)); storage.saveExpenseCategories(String.valueOf(categoryYear), categories); refreshCategoryList(); }
        });

        tab.add(catCard);
        return wrapInScroll(tab);
    }

    private void refreshCategoryList() {
        categoriesListPanel.removeAll();
        for (ExpenseCategory cat : categories) { categoriesListPanel.add(makeCategoryRow(cat)); categoriesListPanel.add(makeDivider()); }
        categoriesListPanel.revalidate(); categoriesListPanel.repaint();
    }

    private JPanel makeCategoryRow(ExpenseCategory cat) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try { g2.setColor(Color.decode(cat.getColor())); } catch (Exception ex) { g2.setColor(new Color(148, 163, 184)); }
                g2.fillOval(0, 0, getWidth(), getHeight()); g2.dispose();
            }
        };
        swatch.setOpaque(false); swatch.setPreferredSize(new Dimension(16, 16));

        JPanel textPanel = new JPanel(); textPanel.setOpaque(false); textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(cat.getLabel()); nameLbl.setFont(nameLbl.getFont().deriveFont(Font.PLAIN, 12f)); nameLbl.setForeground(new Color(30, 41, 59));
        JLabel hintLbl = new JLabel(
            (cat.getT2125Line() != null ? cat.getT2125Line() : "")
            + (cat.getHint() != null && !cat.getHint().isBlank() ? "  \u00b7  " + cat.getHint() : "")
            + switch (cat.getDeductionType()) {
                case FIXED_PERCENT -> String.format("  \u00b7  %.0f%% business use", cat.getFixedPercent() * 100);
                case KM_PERCENT    -> "  \u00b7  KM-based business use";
                case HOME_OFFICE   -> "  \u00b7  Home office %";
                default            -> "";
            });
        hintLbl.setFont(hintLbl.getFont().deriveFont(Font.PLAIN, 10f)); hintLbl.setForeground(new Color(148, 163, 184));
        textPanel.add(nameLbl); textPanel.add(hintLbl);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); left.setOpaque(false);
        left.add(swatch); left.add(Box.createHorizontalStrut(10)); left.add(textPanel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); buttons.setOpaque(false);
        JButton editBtn = new JButton("Edit"); editBtn.putClientProperty("JButton.buttonType", "roundRect"); editBtn.setFont(editBtn.getFont().deriveFont(Font.PLAIN, 11f));
        editBtn.addActionListener(e -> showCategoryDialog(cat));
        if (!cat.isBuiltIn()) {
            JButton delBtn = new JButton("Delete"); delBtn.putClientProperty("JButton.buttonType", "roundRect"); delBtn.setFont(delBtn.getFont().deriveFont(Font.PLAIN, 11f));
            delBtn.setForeground((Color) UIManager.get("App.danger"));
            delBtn.addActionListener(e -> {
                int c = JOptionPane.showConfirmDialog(this, "Delete \"" + cat.getLabel() + "\"?\nExpenses will show as uncategorized.", "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (c == JOptionPane.YES_OPTION) { categories.remove(cat); storage.saveExpenseCategories(String.valueOf(categoryYear), categories); refreshCategoryList(); }
            });
            buttons.add(delBtn);
        }
        buttons.add(editBtn); row.add(left, BorderLayout.WEST); row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void showCategoryDialog(ExpenseCategory existing) {
        boolean isNew = existing == null;
        ExpenseCategory cat = isNew ? new ExpenseCategory() : existing;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isNew ? "Add Category" : "Edit Category", true);
        dialog.setLayout(new BorderLayout()); dialog.setSize(420, 480);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)), BorderFactory.createEmptyBorder(14, 20, 14, 20)));
        JLabel hdrTitle = new JLabel(isNew ? "New Category" : "Edit Category"); hdrTitle.setFont(hdrTitle.getFont().deriveFont(Font.BOLD, 15f)); hdrTitle.setForeground(new Color(30, 41, 59));
        hdr.add(hdrTitle, BorderLayout.WEST); dialog.add(hdr, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(new Color(248, 250, 252)); form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);

        JTextField labelField = new JTextField(cat.getLabel()     != null ? cat.getLabel()     : "");
        JTextField hintField  = new JTextField(cat.getHint()      != null ? cat.getHint()      : "");
        JTextField t2125Field = new JTextField(cat.getT2125Line() != null ? cat.getT2125Line() : "");
        JTextField colorField = new JTextField(cat.getColor()     != null ? cat.getColor()     : "#94A3B8");

        JPanel colorPreview = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try { g2.setColor(Color.decode(colorField.getText().trim())); } catch (Exception ex) { g2.setColor(new Color(148, 163, 184)); }
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4); g2.dispose();
            }
        };
        colorPreview.setOpaque(false); colorPreview.setPreferredSize(new Dimension(28, 28));
        colorPreview.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        colorPreview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Color init; try { init = Color.decode(colorField.getText().trim()); } catch (Exception ex) { init = new Color(148, 163, 184); }
                Color chosen = JColorChooser.showDialog(dialog, "Choose Colour", init);
                if (chosen != null) colorField.setText(String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue()));
            }
        });
        colorField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { colorPreview.repaint(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { colorPreview.repaint(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { colorPreview.repaint(); }
        });
        JPanel colorRow = new JPanel(new BorderLayout(8, 0)); colorRow.setOpaque(false);
        colorRow.add(colorField, BorderLayout.CENTER); colorRow.add(colorPreview, BorderLayout.EAST);

        form.add(makeDialogLabel("Name"), gbc);             gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0); form.add(labelField, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0); form.add(makeDialogLabel("Hint"), gbc); gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0); form.add(hintField, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0); form.add(makeDialogLabel("T2125 Line"), gbc); gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0); form.add(t2125Field, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0); form.add(makeDialogLabel("Colour"), gbc); gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0); form.add(colorRow, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0); form.add(makeDialogLabel("Business Use"), gbc); gbc.gridy++;

        JComboBox<String> deductionCombo = new JComboBox<>(new String[]{"100% — Fully deductible", "Fixed % — e.g. phone/internet", "KM-based % — vehicle expenses", "Home office % — rent/utilities"});
        ExpenseCategory.DeductionType currentType = cat.getDeductionType();
        deductionCombo.setSelectedIndex(switch (currentType) { case FULL -> 0; case FIXED_PERCENT -> 1; case KM_PERCENT -> 2; case HOME_OFFICE -> 3; });
        gbc.insets = new Insets(0, 0, 4, 0); form.add(deductionCombo, gbc); gbc.gridy++;

        JTextField percentField = new JTextField(currentType == ExpenseCategory.DeductionType.FIXED_PERCENT ? String.format("%.0f", cat.getFixedPercent() * 100) : "");
        JLabel percentLabel = makeDialogLabel("Business use % (e.g. 60 for 60%)");
        percentLabel.setVisible(currentType == ExpenseCategory.DeductionType.FIXED_PERCENT);
        percentField.setVisible(currentType == ExpenseCategory.DeductionType.FIXED_PERCENT);
        deductionCombo.addActionListener(e -> { boolean show = deductionCombo.getSelectedIndex() == 1; percentLabel.setVisible(show); percentField.setVisible(show); dialog.revalidate(); dialog.repaint(); });
        gbc.insets = new Insets(4, 0, 4, 0); form.add(percentLabel, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0); form.add(percentField, gbc);
        dialog.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout()); footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12)); footerRight.setOpaque(false);
        JButton btnCancel = new JButton("Cancel"); btnCancel.putClientProperty("JButton.buttonType", "roundRect"); btnCancel.addActionListener(e -> dialog.dispose());
        JButton btnSave = new JButton(isNew ? "Add Category" : "Save"); btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground((Color) UIManager.get("App.accent")); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            String name = labelField.getText().trim();
            if (name.isBlank()) { JOptionPane.showMessageDialog(dialog, "Name is required.", "Required", JOptionPane.WARNING_MESSAGE); return; }
            cat.setLabel(name); cat.setHint(hintField.getText().trim()); cat.setT2125Line(t2125Field.getText().trim()); cat.setColor(colorField.getText().trim());
            ExpenseCategory.DeductionType dtype = switch (deductionCombo.getSelectedIndex()) { case 1 -> ExpenseCategory.DeductionType.FIXED_PERCENT; case 2 -> ExpenseCategory.DeductionType.KM_PERCENT; case 3 -> ExpenseCategory.DeductionType.HOME_OFFICE; default -> ExpenseCategory.DeductionType.FULL; };
            cat.setDeductionType(dtype);
            if (dtype == ExpenseCategory.DeductionType.FIXED_PERCENT) {
                try { double pct = Double.parseDouble(percentField.getText().trim()); if (pct <= 0 || pct > 100) throw new NumberFormatException(); cat.setFixedPercent(pct / 100.0); }
                catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Enter a valid % (1-100).", "Invalid", JOptionPane.WARNING_MESSAGE); return; }
            }
            if (isNew) categories.add(cat);
            storage.saveExpenseCategories(String.valueOf(categoryYear), categories); refreshCategoryList(); dialog.dispose();
        });
        footerRight.add(btnCancel); footerRight.add(btnSave); footer.add(footerRight, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH); dialog.getRootPane().setDefaultButton(btnSave);
        dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    // ── Tab: Tax Brackets ─────────────────────────────────────────────────────

    private JPanel buildTaxBracketsTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        int year = LocalDate.now().getYear();
        com.github.shanebeee.reconciled.model.TaxBrackets[] bh = { storage.loadTaxBrackets(year) };

        JPanel yearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); yearRow.setOpaque(false); yearRow.setAlignmentX(LEFT_ALIGNMENT); yearRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel yearLbl = new JLabel("Tax Year:"); yearLbl.setFont(yearLbl.getFont().deriveFont(Font.PLAIN, 12f)); yearLbl.setForeground(new Color(71, 85, 105));
        SpinnerNumberModel ym = new SpinnerNumberModel(year, 2020, year + 2, 1);
        JSpinner yearSpinner = new JSpinner(ym); yearSpinner.setPreferredSize(new Dimension(80, 28));
        JSpinner.NumberEditor ye = new JSpinner.NumberEditor(yearSpinner, "#"); ye.getTextField().setHorizontalAlignment(JTextField.CENTER); yearSpinner.setEditor(ye);
        yearRow.add(yearLbl); yearRow.add(yearSpinner); tab.add(yearRow); tab.add(Box.createVerticalStrut(12));

        JPanel[] cnt = { new JPanel() }; cnt[0].setLayout(new BoxLayout(cnt[0], BoxLayout.Y_AXIS)); cnt[0].setOpaque(false); cnt[0].setAlignmentX(LEFT_ALIGNMENT);
        Runnable build = () -> {
            cnt[0].removeAll();
            com.github.shanebeee.reconciled.model.TaxBrackets brackets = bh[0];
            JPanel cppCard = makeCard(); cppCard.setLayout(new BoxLayout(cppCard, BoxLayout.Y_AXIS));
            cppCard.add(makeSectionRow("CPP (Canada Pension Plan)")); cppCard.add(makeDivider());
            JTextField cppRate = new JTextField(String.format("%.1f", brackets.getCppRate() * 100));
            JTextField cppMax  = new JTextField(String.format("%.2f", brackets.getCppMaxContribution()));
            JLabel cppHint = new JLabel("Self-employed pay both employee + employer sides (combined rate)");
            cppHint.setFont(cppHint.getFont().deriveFont(Font.ITALIC, 10f)); cppHint.setForeground(new Color(148, 163, 184));
            cppCard.add(makeFieldRow("Combined Rate (%)", cppRate)); cppCard.add(makeDivider());
            cppCard.add(makeFieldRow("Annual Maximum ($)", cppMax)); cppCard.add(makeDivider());
            JPanel hr = new JPanel(new BorderLayout()); hr.setOpaque(false); hr.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0)); hr.add(cppHint, BorderLayout.WEST); cppCard.add(hr);
            JButton saveCpp = new JButton("Save CPP"); saveCpp.putClientProperty("JButton.buttonType", "roundRect"); saveCpp.setBackground(new Color(16, 185, 129)); saveCpp.setForeground(Color.WHITE);
            saveCpp.addActionListener(e -> { try { brackets.setCppRate(Double.parseDouble(cppRate.getText().trim()) / 100.0); brackets.setCppMaxContribution(Double.parseDouble(cppMax.getText().trim())); storage.saveTaxBrackets(brackets); JOptionPane.showMessageDialog(this, "CPP saved.", "Saved", JOptionPane.INFORMATION_MESSAGE); } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid numbers.", "Error", JOptionPane.WARNING_MESSAGE); } });
            JPanel cppBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6)); cppBtn.setOpaque(false); cppBtn.add(saveCpp); cppCard.add(cppBtn);
            cnt[0].add(cppCard); cnt[0].add(Box.createVerticalStrut(12));
            cnt[0].add(makeBracketCard("Federal Income Tax Brackets", brackets.getFederal(), brackets, true)); cnt[0].add(Box.createVerticalStrut(12));
            cnt[0].add(makeBracketCard("BC Provincial Tax Brackets",  brackets.getBc(),     brackets, false));
            cnt[0].revalidate(); cnt[0].repaint();
        };
        build.run();
        yearSpinner.addChangeListener(e -> { bh[0] = storage.loadTaxBrackets((int) yearSpinner.getValue()); build.run(); });
        tab.add(cnt[0]);
        return wrapInScroll(tab);
    }

    private JPanel makeBracketCard(String title, java.util.List<com.github.shanebeee.reconciled.model.TaxBrackets.Bracket> brackets, com.github.shanebeee.reconciled.model.TaxBrackets tb, boolean isFederal) {
        JPanel card = makeCard(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(makeSectionRow(title)); card.add(makeDivider());
        JPanel colHdr = new JPanel(new java.awt.GridLayout(1, 3, 8, 0)); colHdr.setOpaque(false); colHdr.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0)); colHdr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        for (String h : new String[]{"Up To ($)", "Rate (%)", ""}) { JLabel l = new JLabel(h); l.setFont(l.getFont().deriveFont(Font.BOLD, 9f)); l.setForeground(new Color(148, 163, 184)); colHdr.add(l); }
        card.add(colHdr); card.add(makeDivider());
        JPanel rowsPanel = new JPanel(); rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS)); rowsPanel.setOpaque(false);
        Runnable refreshRows = () -> {
            rowsPanel.removeAll();
            for (int i = 0; i < brackets.size(); i++) {
                final int idx = i; com.github.shanebeee.reconciled.model.TaxBrackets.Bracket b = brackets.get(i); boolean isLast = i == brackets.size() - 1;
                JPanel row = new JPanel(new java.awt.GridLayout(1, 3, 8, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
                JTextField upTo = new JTextField(isLast ? "No limit" : String.format("%.0f", b.getUpTo())); upTo.setEnabled(!isLast);
                JTextField rate = new JTextField(String.format("%.2f", b.getRate() * 100));
                JButton del = new JButton("✕"); del.putClientProperty("JButton.buttonType", "roundRect"); del.setFont(del.getFont().deriveFont(Font.PLAIN, 10f)); del.setEnabled(brackets.size() > 1);
                del.addActionListener(e -> { brackets.remove(idx); rowsPanel.removeAll(); rowsPanel.revalidate(); });
                row.add(upTo); row.add(rate); row.add(del); rowsPanel.add(row);
                upTo.addFocusListener(new java.awt.event.FocusAdapter() { public void focusLost(java.awt.event.FocusEvent e) { if (!isLast) try { b.setUpTo(Double.parseDouble(upTo.getText().trim())); } catch (NumberFormatException ignored) {} } });
                rate.addFocusListener(new java.awt.event.FocusAdapter() { public void focusLost(java.awt.event.FocusEvent e) { try { b.setRate(Double.parseDouble(rate.getText().trim()) / 100.0); } catch (NumberFormatException ignored) {} } });
            }
            rowsPanel.revalidate(); rowsPanel.repaint();
        };
        refreshRows.run(); card.add(rowsPanel); card.add(makeDivider());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6)); btnRow.setOpaque(false);
        JButton btnAdd = new JButton("+ Add Bracket"); btnAdd.putClientProperty("JButton.buttonType", "roundRect"); btnAdd.setFont(btnAdd.getFont().deriveFont(Font.PLAIN, 11f));
        btnAdd.addActionListener(e -> { double prev = brackets.size() > 1 ? brackets.get(brackets.size() - 2).getUpTo() + 10000 : 50000; brackets.add(brackets.size() - 1, new com.github.shanebeee.reconciled.model.TaxBrackets.Bracket(prev, 0.20)); refreshRows.run(); });
        JButton btnSave = new JButton("Save Brackets"); btnSave.putClientProperty("JButton.buttonType", "roundRect"); btnSave.setBackground(new Color(16, 185, 129)); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> { if (!brackets.isEmpty()) brackets.get(brackets.size() - 1).setUpTo(1e12); if (isFederal) tb.setFederal(brackets); else tb.setBc(brackets); storage.saveTaxBrackets(tb); JOptionPane.showMessageDialog(this, title + " saved.", "Saved", JOptionPane.INFORMATION_MESSAGE); });
        btnRow.add(btnAdd); btnRow.add(btnSave); card.add(btnRow);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JPanel wrapInScroll(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false); scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setOpaque(false); wrapper.add(scroll);
        return wrapper;
    }

    private JLabel makeDialogLabel(String text) {
        JLabel lbl = new JLabel(text); lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f)); lbl.setForeground(new Color(71, 85, 105)); return lbl;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 8)); g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 2, 14, 14);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);
                g2.setColor(new Color(226, 232, 240)); g2.setStroke(new BasicStroke(1f)); g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.dispose(); super.paintComponent(g);
            }
        };
        card.setOpaque(false); card.setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 20)); card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel makeSectionRow(String title) {
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(title.toUpperCase()); lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f)); lbl.setForeground(new Color(148, 163, 184));
        row.add(lbl, BorderLayout.WEST); return row;
    }

    private JPanel makeFieldRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(16, 0)); row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel lbl = new JLabel(label); lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f)); lbl.setForeground(new Color(71, 85, 105)); lbl.setPreferredSize(new Dimension(160, 20));
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f)); row.add(lbl, BorderLayout.WEST); row.add(field, BorderLayout.CENTER); return row;
    }

    private JPanel makeDivider() {
        JPanel div = new JPanel(); div.setOpaque(false); div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); div.setPreferredSize(new Dimension(0, 1));
        div.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249))); return div;
    }

    private String homeOfficePctText(EmployeeInfo info) {
        if (info.getHomeTotalSqFt() <= 0) return "";
        return String.format("Deductible portion: %.1f%%", Math.min(100.0, info.getHomeOfficeSqFt() / info.getHomeTotalSqFt() * 100));
    }
}
