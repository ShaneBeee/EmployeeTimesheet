package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        EmployeeInfo info = storage.loadEmployeeInfo();

        // ── Page header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Main content (no scroll) ─────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);

        // ── Profile strip ────────────────────────────────────────────────────
        String initials = info.getFullName() == null || info.getFullName().isBlank() ? "?"
            : info.getFullName().contains(" ")
              ? String.valueOf(info.getFullName().charAt(0)) + info.getFullName().charAt(info.getFullName().indexOf(' ') + 1)
              : String.valueOf(info.getFullName().charAt(0));
        initials = initials.toUpperCase();
        final String fin = initials;
        Color avatarColor = new Color(59, 130, 246);

        JPanel profileStrip = new JPanel(new BorderLayout(14, 0));
        profileStrip.setOpaque(false);
        profileStrip.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

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
        JLabel profileName = new JLabel(info.getFullName() != null && !info.getFullName().isBlank()
            ? info.getFullName() : "Your Name");
        profileName.setFont(profileName.getFont().deriveFont(Font.BOLD, 14f));
        profileName.setForeground(new Color(30, 41, 59));
        JLabel profileSub = new JLabel(
            (info.getCompany() != null && !info.getCompany().isBlank() ? info.getCompany() : "") +
                (info.getEmail() != null && !info.getEmail().isBlank() ? "  ·  " + info.getEmail() : ""));
        profileSub.setFont(profileSub.getFont().deriveFont(Font.PLAIN, 11f));
        profileSub.setForeground(new Color(100, 116, 139));
        profileText.add(profileName);
        profileText.add(Box.createVerticalStrut(2));
        profileText.add(profileSub);

        profileStrip.add(avatarCircle, BorderLayout.WEST);
        profileStrip.add(profileText, BorderLayout.CENTER);

        // ── Fields panel ─────────────────────────────────────────────────────
        JTextField nameField     = new JTextField(info.getFullName());
        JTextField companyField  = new JTextField(info.getCompany());
        JTextField addressField  = new JTextField(info.getAddress());
        JTextField address2Field = new JTextField(info.getAddress2());
        JTextField phoneField    = new JTextField(info.getPhoneNumber());
        JTextField emailField    = new JTextField(info.getEmail());

        JTextField startField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultStartTime()));
        startField.setEditable(false);
        startField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, startField);
            }
        });
        JTextField endField = new JTextField(TimePickerPanel.formatTime(storage.getDefaultEndTime()));
        endField.setEditable(false);
        endField.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(SettingsPanel.this, endField);
            }
        });

        // Single card with all fields, two sections separated by a gap row
        JPanel allFields = makeCard();
        allFields.setLayout(new BoxLayout(allFields, BoxLayout.Y_AXIS));

        allFields.add(makeSectionRow("Employee Information"));
        allFields.add(makeDivider());
        allFields.add(makeFieldRow("Full Name",    nameField));     allFields.add(makeDivider());
        allFields.add(makeFieldRow("Company",      companyField));  allFields.add(makeDivider());
        allFields.add(makeFieldRow("Address",      addressField));  allFields.add(makeDivider());
        allFields.add(makeFieldRow("Address 2",    address2Field)); allFields.add(makeDivider());
        allFields.add(makeFieldRow("Phone",        phoneField));    allFields.add(makeDivider());
        allFields.add(makeFieldRow("Email",        emailField));

        // Gap between sections
        allFields.add(makeGapRow());

        allFields.add(makeSectionRow("Application Settings"));
        allFields.add(makeDivider());
        allFields.add(makeFieldRow("Default Start Time", startField)); allFields.add(makeDivider());
        allFields.add(makeFieldRow("Default End Time",   endField));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(profileStrip, BorderLayout.NORTH);
        center.add(allFields, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // ── Save button ──────────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        bottomPanel.setOpaque(false);
        JButton saveBtn = new JButton("Save Settings");
        saveBtn.putClientProperty("JButton.buttonType", "roundRect");
        saveBtn.setBackground((Color) UIManager.get("App.accent"));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD, 13f));
        bottomPanel.add(saveBtn);
        add(bottomPanel, BorderLayout.SOUTH);

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

            profileName.setText(nameField.getText().isBlank() ? "Your Name" : nameField.getText());
            profileSub.setText(
                (companyField.getText().isBlank() ? "" : companyField.getText()) +
                    (emailField.getText().isBlank() ? "" : "  ·  " + emailField.getText()));

            JOptionPane.showMessageDialog(this, "Settings saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof MainFrame mainFrame) mainFrame.checkBosses();
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
        lbl.setPreferredSize(new Dimension(150, 20));

        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));

        row.add(lbl, BorderLayout.WEST);
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

    private JPanel makeGapRow() {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        gap.setPreferredSize(new Dimension(0, 10));
        gap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));
        return gap;
    }
}
