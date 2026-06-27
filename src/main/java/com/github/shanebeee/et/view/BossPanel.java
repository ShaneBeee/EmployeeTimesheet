package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

public class BossPanel extends JPanel {

    private final DataStorage storage;
    private final List<Boss> bosses;
    private JPanel cardsPanel;

    private static final Color[] AVATAR_COLORS = {
        new Color(59, 130, 246),   // blue
        new Color(99, 102, 241),   // indigo
        new Color(139, 92, 246),   // violet
        new Color(20, 184, 166),   // teal
        new Color(6, 182, 212),    // cyan
        new Color(16, 185, 129),   // emerald
    };

    public BossPanel(DataStorage storage) {
        this.storage = storage;
        this.bosses = storage.loadBosses();
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Boss Management");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);

        JButton btnAdd = new JButton("+ Add Boss");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground((Color) UIManager.get("App.success"));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 13f));
        header.add(btnAdd, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        btnAdd.addActionListener(e -> showBossDialog(null));

        // ── Cards grid ──────────────────────────────────────────────────────
        cardsPanel = new JPanel(new java.awt.GridLayout(0, 2, 16, 16));
        cardsPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(248, 250, 252));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        refreshCards();
    }

    private void refreshCards() {
        cardsPanel.removeAll();
        for (Boss boss : bosses) {
            cardsPanel.add(makeBossCard(boss));
        }
        // Add empty placeholder if odd number
        if (bosses.size() % 2 != 0) {
            cardsPanel.add(new JPanel() {{ setOpaque(false); }});
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private JPanel makeBossCard(Boss boss) {
        Color accent = AVATAR_COLORS[Math.abs(boss.getName().hashCode()) % AVATAR_COLORS.length];
        final boolean[] hovered = {false};

        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new Color(0, 0, 0, hovered[0] ? 18 : 10));
                g2.fillRoundRect(3, 4, getWidth() - 6, getHeight() - 4, 16, 16);
                // Card background
                g2.setColor(hovered[0] ? new Color(250, 252, 255) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 16, 16);
                // Border
                if (hovered[0]) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
                    g2.setStroke(new BasicStroke(1.5f));
                } else {
                    g2.setColor(new Color(226, 232, 240));
                    g2.setStroke(new BasicStroke(1f));
                }
                g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 16, 16);
                // Top accent gradient bar — rounded top corners only, flush with card
                GradientPaint gp = new GradientPaint(0, 0,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220),
                    getWidth(), 0,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                g2.setPaint(gp);
                // Fill a taller rounded rect and clip the bottom half so only top corners are round
                java.awt.geom.RoundRectangle2D roundTop = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth() - 5, 16, 16, 16);
                java.awt.Rectangle clipRect = new java.awt.Rectangle(0, 0, getWidth(), 6);
                g2.setClip(clipRect);
                g2.fill(roundTop);
                g2.setClip(null);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(0, 160));

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered[0] = false; card.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showBossDialog(boss);
            }
        });

        // ── Avatar + name row ────────────────────────────────────────────────
        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setOpaque(false);

        // Avatar
        String initials = boss.getName().isBlank() ? "?" :
            boss.getName().contains(" ")
            ? String.valueOf(boss.getName().charAt(0)) + boss.getName().charAt(boss.getName().indexOf(' ') + 1)
            : String.valueOf(boss.getName().charAt(0));
        initials = initials.toUpperCase();
        final String fin = initials;

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient fill
                GradientPaint gp = new GradientPaint(0, 0,
                    accent.brighter(), 0, getHeight(), accent);
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Initials
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 17f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fin, (getWidth() - fm.stringWidth(fin)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(48, 48));
        avatar.setMinimumSize(new Dimension(48, 48));
        avatar.setMaximumSize(new Dimension(48, 48));

        // Name + company
        JPanel nameBlock = new JPanel();
        nameBlock.setOpaque(false);
        nameBlock.setLayout(new BoxLayout(nameBlock, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(boss.getName());
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 15f));
        nameLbl.setForeground(new Color(30, 41, 59));
        JLabel companyLbl = new JLabel(boss.getCompany() != null && !boss.getCompany().isBlank()
            ? boss.getCompany() : "No company");
        companyLbl.setFont(companyLbl.getFont().deriveFont(Font.PLAIN, 12f));
        companyLbl.setForeground(new Color(100, 116, 139));
        nameBlock.add(nameLbl);
        nameBlock.add(Box.createVerticalStrut(3));
        nameBlock.add(companyLbl);

        // Edit + Delete buttons (top right)
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBtns.setOpaque(false);
        JButton btnEdit = new JButton("Edit");
        btnEdit.putClientProperty("JButton.buttonType", "roundRect");
        btnEdit.setBackground((Color) UIManager.get("App.warning"));
        btnEdit.setForeground(Color.WHITE);
        btnEdit.setFont(btnEdit.getFont().deriveFont(Font.PLAIN, 11f));
        JButton btnDelete = new JButton("Delete");
        btnDelete.putClientProperty("JButton.buttonType", "roundRect");
        btnDelete.setBackground((Color) UIManager.get("App.danger"));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(btnDelete.getFont().deriveFont(Font.PLAIN, 11f));
        actionBtns.add(btnEdit);
        actionBtns.add(btnDelete);

        btnEdit.addActionListener(e -> showBossDialog(boss));
        btnDelete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + boss.getName() + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                bosses.remove(boss);
                storage.saveBosses(bosses);
                refreshCards();
            }
        });

        topRow.add(avatar, BorderLayout.WEST);
        // Income type badge
        JLabel incomeBadge = new JLabel(boss.isSelfEmployed() ? "Self-Employed" : "T4");
        incomeBadge.setFont(incomeBadge.getFont().deriveFont(Font.BOLD, 10f));
        incomeBadge.setForeground(boss.isSelfEmployed() ? new Color(16, 185, 129) : new Color(245, 158, 11));
        incomeBadge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        topRow.add(nameBlock, BorderLayout.CENTER);
        topRow.add(incomeBadge, BorderLayout.EAST);

        // ── Info chips row ───────────────────────────────────────────────────
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chipsRow.setOpaque(false);
        chipsRow.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        chipsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ((FlowLayout)chipsRow.getLayout()).setVgap(6);

        chipsRow.add(makeChip("$" + String.format("%.2f", boss.getHourlyRate()) + "/hr", new Color(59, 130, 246)));
        chipsRow.add(makeChip(String.format("%.1f%%", boss.getTaxRate()) + " tax", new Color(139, 92, 246)));
        if (boss.getKmRate() != null) {
            chipsRow.add(makeChip("$" + String.format("%.2f", boss.getKmRate()) + "/km", new Color(34, 197, 94)));
        }
        if (boss.getPhoneNumber() != null && !boss.getPhoneNumber().isBlank()) {
            chipsRow.add(makeChip(boss.getPhoneNumber(), new Color(100, 116, 139)));
        }

        // ── Action buttons row ──────────────────────────────────────────────
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel actionBtns2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBtns2.setOpaque(false);
        actionBtns2.add(btnEdit);
        actionBtns2.add(btnDelete);
        bottomRow.add(actionBtns2, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(chipsRow, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    private JPanel makeChip(String text, Color color) {
        JPanel chip = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chip.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setForeground(color.darker());
        chip.add(lbl);
        return chip;
    }

    private void showBossDialog(Boss boss) {
        boolean isNew = boss == null;
        Boss b = isNew ? new Boss("") : boss;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Boss" : "Edit Boss", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(540, 580);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        Color accent = isNew ? AVATAR_COLORS[0] : AVATAR_COLORS[Math.abs(b.getName().hashCode()) % AVATAR_COLORS.length];

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        String initials = b.getName().isBlank() ? "?" :
            (b.getName().contains(" ")
             ? String.valueOf(b.getName().charAt(0)) + b.getName().charAt(b.getName().indexOf(' ') + 1)
             : String.valueOf(b.getName().charAt(0))).toUpperCase();
        final String fin = initials;

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, accent.brighter(), 0, getHeight(), accent);
                g2.setPaint(gp);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(fin, (getWidth() - fm.stringWidth(fin)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(52, 52));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        JLabel titleLbl = new JLabel(isNew ? "New Boss" : b.getName());
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 17f));
        titleLbl.setForeground(new Color(30, 41, 59));
        JLabel subLbl = new JLabel(isNew ? "Fill in the details below" :
            (b.getCompany() != null && !b.getCompany().isBlank() ? b.getCompany() : "No company set"));
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 12f));
        subLbl.setForeground(new Color(100, 116, 139));
        headerText.add(titleLbl);
        headerText.add(Box.createVerticalStrut(3));
        headerText.add(subLbl);

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(avatar);
        headerLeft.add(headerText);
        header.add(headerLeft, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Form ────────────────────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JTextField nameField     = new JTextField(b.getName());
        JTextField companyField  = new JTextField(b.getCompany());
        JTextField addressField  = new JTextField(b.getAddress());
        JTextField address2Field = new JTextField(b.getAddress2());
        JTextField phoneField    = new JTextField(b.getPhoneNumber());
        JTextField emailField    = new JTextField(b.getEmail());
        JTextField rateField     = new JTextField(String.valueOf(b.getHourlyRate()));
        JTextField taxField      = new JTextField(String.valueOf(b.getTaxRate()));
        JTextField kmField       = new JTextField(b.getKmRate() != null ? String.valueOf(b.getKmRate()) : "");

        form.add(makeSectionLabel("Contact Info"));
        form.add(Box.createVerticalStrut(8));
        for (Object[] pair : new Object[][]{
            {"Name", nameField}, {"Company", companyField}, {"Address", addressField},
            {"Address 2", address2Field}, {"Phone", phoneField}, {"Email", emailField}}) {
            form.add(makeRow((String) pair[0], (JTextField) pair[1]));
            form.add(Box.createVerticalStrut(6));
        }
        form.add(Box.createVerticalStrut(8));
        form.add(makeSectionLabel("Rates"));
        form.add(Box.createVerticalStrut(8));
        JPanel rateRow = new JPanel(new java.awt.GridLayout(1, 3, 10, 0));
        rateRow.setOpaque(false);
        rateRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        rateRow.add(makeFieldBlock("Hourly Rate ($)", rateField));
        rateRow.add(makeFieldBlock("Tax Rate (%)", taxField));
        rateRow.add(makeFieldBlock("KM Rate ($)", kmField));
        form.add(rateRow);
        form.add(Box.createVerticalStrut(12));
        form.add(makeSectionLabel("Income Type"));
        form.add(Box.createVerticalStrut(8));
        JComboBox<Boss.IncomeType> incomeCombo = new JComboBox<>(Boss.IncomeType.values());
        incomeCombo.setSelectedItem(b.getIncomeType());
        incomeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel incomeHint = new JLabel(b.isSelfEmployed()
            ? "Included in T2125 income summary export"
            : "T4 slip provided by employer — excluded from T2125 export");
        incomeHint.setFont(incomeHint.getFont().deriveFont(Font.ITALIC, 11f));
        incomeHint.setForeground(new Color(148, 163, 184));
        incomeCombo.addActionListener(e -> {
            boolean se = incomeCombo.getSelectedItem() == Boss.IncomeType.SELF_EMPLOYED;
            incomeHint.setText(se
                ? "Included in T2125 income summary export"
                : "T4 slip provided by employer — excluded from T2125 export");
        });
        form.add(incomeCombo);
        form.add(Box.createVerticalStrut(4));
        form.add(incomeHint);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(new Color(248, 250, 252));
        dialog.add(formScroll, BorderLayout.CENTER);

        // ── Footer ──────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        JButton btnSave = new JButton(isNew ? "Add Boss" : "Save Changes");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(new Color(59, 130, 246));
        btnSave.setForeground(Color.WHITE);
        footer.add(btnCancel);
        footer.add(btnSave);
        dialog.add(footer, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
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
                    b.getRateHistory().add(new Boss.RateChange(LocalDate.now().toString(), oldRate));
                    b.setHourlyRate(newRate);
                }
                b.setTaxRate(Double.parseDouble(taxField.getText()));
                String km = kmField.getText();
                b.setKmRate(km.isEmpty() ? null : Double.parseDouble(km));
                b.setIncomeType((Boss.IncomeType) incomeCombo.getSelectedItem());
                if (isNew) bosses.add(b);
                storage.saveBosses(bosses);
                refreshCards();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid number format — check Rate fields.");
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JPanel makeSectionLabel(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel makeRow(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel makeFieldBlock(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }
}
