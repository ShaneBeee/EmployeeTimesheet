package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;

/**
 * First-run onboarding wizard. Shown once when no data directory preference exists.
 * Guides the user through: storage location → profile → first boss → done.
 */
public class OnboardingWizard extends JDialog {

    private static final Color BLUE  = new Color(59, 130, 246);
    private static final Color NAVY  = new Color(30, 41, 59);
    private static final Color SLATE = new Color(100, 116, 139);
    private static final Color LIGHT = new Color(241, 245, 249);

    private final CardLayout cards = new CardLayout();
    private final JPanel     deck  = new JPanel(cards);

    // Step indices
    private static final String STEP_WELCOME  = "WELCOME";
    private static final String STEP_STORAGE  = "STORAGE";
    private static final String STEP_PROFILE  = "PROFILE";
    private static final String STEP_BOSS     = "BOSS";
    private static final String STEP_DONE     = "DONE";
    private static final String[] STEPS = {STEP_WELCOME, STEP_STORAGE, STEP_PROFILE, STEP_BOSS, STEP_DONE};
    private int currentStep = 0;

    // Storage step state
    private String chosenPath = DataStorage.DEFAULT_LOCAL_BASE;

    // Profile step fields
    private JTextField nameField, companyField, addressField, phoneField, emailField;

    // Boss step fields
    private JTextField bossNameField, hourlyField, kmField, taxField;
    private JComboBox<Boss.IncomeType> incomeTypeCombo;

    // Nav buttons
    private JButton btnBack, btnNext;
    private JLabel  stepLabel;

    // Holds path label for storage step (updated when user picks)
    private JLabel pathLabel;

    public OnboardingWizard(Frame owner) {
        super(owner, "Welcome to Employee Timesheet", true);
        setSize(560, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // must complete onboarding
        initUI();
    }

    private void initUI() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // ── Deck ─────────────────────────────────────────────────────────────
        deck.setOpaque(false);
        deck.add(buildWelcomeStep(),  STEP_WELCOME);
        deck.add(buildStorageStep(),  STEP_STORAGE);
        deck.add(buildProfileStep(),  STEP_PROFILE);
        deck.add(buildBossStep(),     STEP_BOSS);
        deck.add(buildDoneStep(),     STEP_DONE);
        add(deck, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        stepLabel = new JLabel("Step 1 of 4");
        stepLabel.setFont(stepLabel.getFont().deriveFont(Font.PLAIN, 11f));
        stepLabel.setForeground(SLATE);

        JPanel navBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navBtns.setOpaque(false);

        btnBack = new JButton("Back");
        btnBack.putClientProperty("JButton.buttonType", "roundRect");
        btnBack.setEnabled(false);
        btnBack.addActionListener(e -> navigate(-1));

        btnNext = new JButton("Next");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.setBackground(BLUE);
        btnNext.setForeground(Color.WHITE);
        btnNext.addActionListener(e -> navigate(1));

        navBtns.add(btnBack);
        navBtns.add(btnNext);

        footer.add(stepLabel, BorderLayout.WEST);
        footer.add(navBtns,   BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        updateNav();
    }

    private void navigate(int dir) {
        // Validate before moving forward
        if (dir > 0 && !validateCurrentStep()) return;

        currentStep = Math.max(0, Math.min(STEPS.length - 1, currentStep + dir));
        cards.show(deck, STEPS[currentStep]);
        updateNav();
    }

    private void updateNav() {
        boolean isFirst = currentStep == 0;
        boolean isLast  = currentStep == STEPS.length - 1;
        btnBack.setEnabled(!isFirst);
        btnNext.setText(isLast ? "Get Started!" : currentStep == STEPS.length - 2 ? "Finish" : "Next");

        if (isLast) {
            btnNext.addActionListener(e -> finish());
            stepLabel.setText("You're all set!");
        } else {
            // Step label skips the welcome screen in the count
            int displayStep = currentStep; // welcome is step 0 but we show "Step 1 of 4" for storage
            stepLabel.setText(currentStep == 0 ? "" : "Step " + currentStep + " of 4");
        }
    }

    private boolean validateCurrentStep() {
        return switch (STEPS[currentStep]) {
            case STEP_PROFILE -> {
                if (nameField.getText().trim().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Please enter your full name.", "Required", JOptionPane.WARNING_MESSAGE);
                    yield false;
                }
                yield true;
            }
            case STEP_BOSS -> {
                if (bossNameField.getText().trim().isBlank()) {
                    JOptionPane.showMessageDialog(this, "Please enter a name for your boss/client.", "Required", JOptionPane.WARNING_MESSAGE);
                    yield false;
                }
                try { Double.parseDouble(hourlyField.getText().trim()); } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid hourly rate.", "Invalid", JOptionPane.WARNING_MESSAGE);
                    yield false;
                }
                yield true;
            }
            default -> true;
        };
    }

    private void finish() {
        // 1. Save data directory
        DataStorage.saveDataDirectory(chosenPath);

        // 2. Create a fresh DataStorage pointing at the chosen path
        DataStorage storage = new DataStorage();

        // 3. Save employee profile
        EmployeeInfo info = new EmployeeInfo();
        info.setFullName(nameField.getText().trim());
        info.setCompany(companyField.getText().trim());
        info.setAddress(addressField.getText().trim());
        info.setPhoneNumber(phoneField.getText().trim());
        info.setEmail(emailField.getText().trim());
        storage.saveEmployeeInfo(info);

        // 4. Save boss if name provided
        String bossName = bossNameField.getText().trim();
        if (!bossName.isBlank()) {
            Boss boss = new Boss(bossName);
            try { boss.setHourlyRate(Double.parseDouble(hourlyField.getText().trim())); } catch (NumberFormatException ignored) {}
            try { boss.setKmRate(Double.parseDouble(kmField.getText().trim())); } catch (NumberFormatException ignored) {}
            try { boss.setTaxRate(Double.parseDouble(taxField.getText().trim())); } catch (NumberFormatException ignored) {}
            boss.setIncomeType((Boss.IncomeType) incomeTypeCombo.getSelectedItem());
            storage.saveBosses(java.util.List.of(boss));
        }

        dispose();
    }

    // ── Steps ─────────────────────────────────────────────────────────────────

    private JPanel buildWelcomeStep() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(0, 0, 12, 0);

        JLabel emoji = new JLabel("🧾", JLabel.CENTER);
        emoji.setFont(emoji.getFont().deriveFont(64f));
        gc.insets = new Insets(0, 0, 20, 0);
        p.add(emoji, gc);

        gc.gridy++; gc.insets = new Insets(0, 0, 12, 0);
        JLabel title = new JLabel("Welcome to Employee Timesheet", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(NAVY);
        p.add(title, gc);

        gc.gridy++;
        JLabel sub = new JLabel("<html><div style='text-align:center;'>Track your hours, expenses, kilometres, and invoices — all in one place.<br><br>Let's get you set up in just a few steps.</div></html>", JLabel.CENTER);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 13f));
        sub.setForeground(SLATE);
        p.add(sub, gc);

        return p;
    }

    private JPanel buildStorageStep() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        JLabel title = new JLabel("Where should your data be saved?");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(NAVY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(8));

        JLabel sub = new JLabel("<html>Choose iCloud Drive to keep your data backed up and available across<br>your Apple devices, or save locally on this Mac only.</html>");
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(SLATE);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(24));

        // iCloud button
        boolean iCloudAvailable = DataStorage.isICloudAvailable();
        String iCloudPath = DataStorage.ICLOUD_BASE + "EmployeeTimesheet";

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        optionsRow.setOpaque(false);
        optionsRow.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnICloud = makeStorageOptionButton("☁️", "iCloud Drive", "Recommended — syncs automatically", iCloudAvailable);
        JButton btnLocal  = makeStorageOptionButton("💻", "Local Only", "Saved on this Mac only", true);

        optionsRow.add(btnICloud);
        optionsRow.add(btnLocal);
        p.add(optionsRow);
        p.add(Box.createVerticalStrut(20));

        // Path display
        JLabel pathTitle = new JLabel("Save location:");
        pathTitle.setFont(pathTitle.getFont().deriveFont(Font.PLAIN, 11f));
        pathTitle.setForeground(SLATE);
        pathTitle.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pathTitle);
        p.add(Box.createVerticalStrut(4));

        pathLabel = new JLabel(chosenPath);
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        pathLabel.setForeground(new Color(71, 85, 105));
        pathLabel.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pathLabel);
        p.add(Box.createVerticalStrut(8));

        JButton btnCustom = new JButton("Choose Custom Folder...");
        btnCustom.putClientProperty("JButton.buttonType", "roundRect");
        btnCustom.setFont(btnCustom.getFont().deriveFont(Font.PLAIN, 11f));
        btnCustom.setAlignmentX(LEFT_ALIGNMENT);
        btnCustom.addActionListener(e -> {
            FileDialog fd = new FileDialog((Frame) getOwner(), "Choose Data Folder", FileDialog.LOAD);
            fd.setFile("EmployeeTimesheet"); // hint
            fd.setVisible(true);
            if (fd.getDirectory() != null) {
                chosenPath = fd.getDirectory() + "EmployeeTimesheet";
                pathLabel.setText(chosenPath);
            }
        });
        p.add(btnCustom);

        // Button actions
        btnICloud.addActionListener(e -> {
            if (!iCloudAvailable) return;
            chosenPath = iCloudPath;
            pathLabel.setText(chosenPath);
        });
        btnLocal.addActionListener(e -> {
            chosenPath = DataStorage.DEFAULT_LOCAL_BASE;
            pathLabel.setText(chosenPath);
        });

        // Default to iCloud if available
        if (iCloudAvailable) {
            chosenPath = iCloudPath;
            pathLabel.setText(chosenPath);
        }

        return p;
    }

    private JButton makeStorageOptionButton(String emoji, String label, String sub, boolean enabled) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? Color.WHITE : new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setLayout(new BoxLayout(btn, BoxLayout.Y_AXIS));
        btn.setEnabled(enabled);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(185, 90));
        btn.setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btn.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(emojiLbl.getFont().deriveFont(24f));
        emojiLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(labelLbl.getFont().deriveFont(Font.BOLD, 13f));
        labelLbl.setForeground(enabled ? NAVY : SLATE);
        labelLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(enabled ? sub : "iCloud Drive not detected");
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 10f));
        subLbl.setForeground(SLATE);
        subLbl.setAlignmentX(LEFT_ALIGNMENT);

        btn.add(emojiLbl);
        btn.add(Box.createVerticalStrut(4));
        btn.add(labelLbl);
        btn.add(subLbl);
        return btn;
    }

    private JPanel buildProfileStep() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        JLabel title = new JLabel("Tell us about yourself");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(NAVY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("This information appears on your invoices.");
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(SLATE);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        nameField    = new JTextField();
        companyField = new JTextField();
        addressField = new JTextField();
        phoneField   = new JTextField();
        emailField   = new JTextField();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0; gc.gridy = 0;
        gc.insets = new Insets(0, 0, 4, 0);

        addFormRow(form, gc, "Full Name *", nameField);    gc.gridy++;
        addFormRow(form, gc, "Company",     companyField); gc.gridy++;
        addFormRow(form, gc, "Address",     addressField); gc.gridy++;
        addFormRow(form, gc, "Phone",       phoneField);   gc.gridy++;
        addFormRow(form, gc, "Email",       emailField);

        p.add(form);
        return p;
    }

    private JPanel buildBossStep() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 20, 40));

        JLabel title = new JLabel("Add your first boss / client");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(NAVY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("You can add more from Boss Management at any time.");
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(SLATE);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        bossNameField = new JTextField();
        hourlyField   = new JTextField("0.00");
        kmField       = new JTextField("0.00");
        taxField      = new JTextField("5.0");
        incomeTypeCombo = new JComboBox<>(Boss.IncomeType.values());

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setAlignmentX(LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0; gc.gridy = 0;
        gc.insets = new Insets(0, 0, 4, 0);

        addFormRow(form, gc, "Name *",          bossNameField);   gc.gridy++;
        addFormRow(form, gc, "Hourly Rate ($)*", hourlyField);    gc.gridy++;
        addFormRow(form, gc, "KM Rate ($/km)",   kmField);        gc.gridy++;
        addFormRow(form, gc, "Tax Rate (%)",      taxField);       gc.gridy++;
        addFormRow(form, gc, "Income Type",       incomeTypeCombo);

        p.add(form);
        return p;
    }

    private JPanel buildDoneStep() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;

        gc.gridy = 0; gc.insets = new Insets(0, 0, 20, 0);
        JLabel emoji = new JLabel("🎉", JLabel.CENTER);
        emoji.setFont(emoji.getFont().deriveFont(64f));
        p.add(emoji, gc);

        gc.gridy++; gc.insets = new Insets(0, 0, 12, 0);
        JLabel title = new JLabel("You're all set!", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(NAVY);
        p.add(title, gc);

        gc.gridy++;
        JLabel sub = new JLabel("<html><div style='text-align:center;'>Your data will be saved to:<br><br><b>" + chosenPath + "</b><br><br>You can change this at any time in Settings → Preferences.</div></html>", JLabel.CENTER);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(SLATE);
        p.add(sub, gc);

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addFormRow(JPanel form, GridBagConstraints gc, String label, JComponent field) {
        gc.insets = new Insets(0, 0, 2, 0);
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(SLATE);
        form.add(lbl, gc);
        gc.gridy++;
        gc.insets = new Insets(0, 0, 10, 0);
        form.add(field, gc);
    }
}
