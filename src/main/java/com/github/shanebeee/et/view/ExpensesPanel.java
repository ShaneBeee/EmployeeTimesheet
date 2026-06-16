package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Expenditure;
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
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpensesPanel extends JPanel {

    private static final Color ACCENT = new Color(245, 158, 11);

    private final DataStorage storage;
    private int currentYear;
    private JLabel yearLabel;
    private JPanel listPanel;
    private JLabel totalLabel;
    private List<Expenditure> currentExpenses;

    public ExpensesPanel(DataStorage storage) {
        this.storage = storage;
        this.currentYear = LocalDate.now().getYear();
        setLayout(new BorderLayout());
        initUI();
        loadYear();
    }

    private void initUI() {
        // ── Page header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Expenses");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        header.add(titleLabel, BorderLayout.WEST);

        // Year nav
        JPanel yearNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        yearNav.setOpaque(false);
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        btnPrev.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");
        yearLabel = new JLabel(String.valueOf(currentYear), JLabel.CENTER);
        yearLabel.setFont(yearLabel.getFont().deriveFont(Font.BOLD, 14f));
        yearLabel.setPreferredSize(new Dimension(60, 20));

        btnPrev.addActionListener(e -> {
            currentYear--;
            loadYear();
        });
        btnNext.addActionListener(e -> {
            currentYear++;
            loadYear();
        });

        yearNav.add(btnPrev);
        yearNav.add(yearLabel);
        yearNav.add(btnNext);

        JButton btnAdd = new JButton("+ Add Expense");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground(ACCENT);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 13f));
        btnAdd.addActionListener(e -> showExpenseDialog(null));

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        headerRight.add(yearNav);
        headerRight.add(btnAdd);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Split: list on left, summary on right ────────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        // Entry list
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        body.add(scroll, BorderLayout.CENTER);

        // Summary sidebar
        JPanel sidebar = buildSummaryPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        body.add(sidebar, BorderLayout.EAST);

        add(body, BorderLayout.CENTER);
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel summaryTitle = new JLabel("YEAR SUMMARY");
        summaryTitle.setFont(summaryTitle.getFont().deriveFont(Font.BOLD, 10f));
        summaryTitle.setForeground(new Color(148, 163, 184));
        summaryTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        summaryTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(summaryTitle);

        totalLabel = new JLabel("$0.00");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 22f));
        totalLabel.setForeground(new Color(30, 41, 59));
        totalLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(totalLabel);

        JLabel deductLabel = new JLabel("total deductible");
        deductLabel.setFont(deductLabel.getFont().deriveFont(Font.PLAIN, 11f));
        deductLabel.setForeground(new Color(148, 163, 184));
        deductLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(deductLabel);
        card.add(Box.createVerticalStrut(12));

        // Category breakdown rows injected on refresh
        card.setName("summaryCard");
        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private void loadYear() {
        yearLabel.setText(String.valueOf(currentYear));
        currentExpenses = storage.loadExpenditures(String.valueOf(currentYear));
        refresh();
    }

    private void refresh() {
        listPanel.removeAll();

        if (currentExpenses.isEmpty()) {
            JLabel empty = new JLabel("No expenses yet — click + Add Expense to get started", JLabel.CENTER);
            empty.setForeground(new Color(148, 163, 184));
            empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
            empty.setAlignmentX(CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(40));
            listPanel.add(empty);
        } else {
            // Group by month
            Map<String, List<Expenditure>> byMonth = new LinkedHashMap<>();
            for (Expenditure e : currentExpenses) {
                String month = e.getDate().substring(0, 7); // yyyy-MM
                byMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(e);
            }
            // Sort months descending
            byMonth.entrySet().stream()
                .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                .forEach(entry -> {
                    String monthKey = entry.getKey();
                    List<Expenditure> monthExpenses = entry.getValue();
                    YearMonth ym = YearMonth.parse(monthKey);
                    String monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
                    double monthTotal = monthExpenses.stream().mapToDouble(Expenditure::getDeductibleAmount).sum();

                    // Month header
                    JPanel mHeader = new JPanel(new BorderLayout());
                    mHeader.setOpaque(false);
                    mHeader.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
                    mHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    JLabel mLabel = new JLabel(monthLabel.toUpperCase());
                    mLabel.setFont(mLabel.getFont().deriveFont(Font.BOLD, 10f));
                    mLabel.setForeground(new Color(148, 163, 184));
                    JLabel mTotal = new JLabel(String.format("$%.2f", monthTotal));
                    mTotal.setFont(mTotal.getFont().deriveFont(Font.BOLD, 10f));
                    mTotal.setForeground(ACCENT);
                    mHeader.add(mLabel, BorderLayout.WEST);
                    mHeader.add(mTotal, BorderLayout.EAST);
                    listPanel.add(mHeader);

                    for (Expenditure exp : monthExpenses) {
                        listPanel.add(makeExpenseCard(exp));
                        listPanel.add(Box.createVerticalStrut(6));
                    }
                    listPanel.add(Box.createVerticalStrut(4));
                });
        }

        listPanel.revalidate();
        listPanel.repaint();
        refreshSummary();
    }

    private void refreshSummary() {
        // Find the summary card
        Component[] components = ((JPanel) getComponent(1)).getComponents();
        JPanel sidebar = null;
        for (Component c : components) {
            if (c instanceof JPanel p && BorderLayout.EAST.equals(((BorderLayout) ((JPanel) getComponent(1)).getLayout()).getConstraints(c))) {
                sidebar = p;
                break;
            }
        }
        if (sidebar == null) return;

        JPanel card = null;
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JPanel p && "summaryCard".equals(p.getName())) {
                card = p;
                break;
            }
        }
        if (card == null) return;
        final JPanel summaryCard = card;  // effectively final for lambda capture

        // Recalculate totals
        double grandTotal = currentExpenses.stream().mapToDouble(Expenditure::getDeductibleAmount).sum();
        totalLabel.setText(String.format("$%.2f", grandTotal));

        // Remove old category rows (everything after index 3)
        while (summaryCard.getComponentCount() > 4) summaryCard.remove(summaryCard.getComponentCount() - 1);

        // Add per-category rows
        Map<Expenditure.Category, Double> byCategory = new LinkedHashMap<>();
        for (Expenditure.Category cat : Expenditure.Category.values()) byCategory.put(cat, 0.0);
        for (Expenditure e : currentExpenses) {
            if (e.getCategory() != null)
                byCategory.merge(e.getCategory(), e.getDeductibleAmount(), Double::sum);
        }
        byCategory.forEach((cat, total) -> {
            if (total > 0) {
                JPanel row = new JPanel(new BorderLayout(4, 0));
                row.setOpaque(false);
                row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                row.setAlignmentX(LEFT_ALIGNMENT);
                JLabel lbl = new JLabel(cat.getLabel());
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
                lbl.setForeground(new Color(100, 116, 139));
                JLabel val = new JLabel(String.format("$%.2f", total));
                val.setFont(val.getFont().deriveFont(Font.PLAIN, 11f));
                val.setForeground(new Color(30, 41, 59));
                row.add(lbl, BorderLayout.WEST);
                row.add(val, BorderLayout.EAST);
                summaryCard.add(row);
            }
        });

        summaryCard.revalidate();
        summaryCard.repaint();
    }

    private JPanel makeExpenseCard(Expenditure exp) {
        Color accent = categoryColor(exp.getCategory());
        final boolean[] hovered = {false};

        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? new Color(249, 250, 251) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Icon circle
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(accent);
                String letter = exp.getCategory() != null ? exp.getCategory().getLabel().substring(0, 1) : "?";
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter, (getWidth() - fm.stringWidth(letter)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(36, 36));

        // Text
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        String title = exp.getDescription() != null && !exp.getDescription().isBlank()
            ? exp.getDescription()
            : (exp.getCategory() != null ? exp.getCategory().getLabel() : "Expense");
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 13f));
        titleLbl.setForeground(new Color(30, 41, 59));
        String sub = (exp.getCategory() != null ? exp.getCategory().getLabel() : "") +
            (exp.getBusinessUsePercent() < 100 ? "  ·  " + (int) exp.getBusinessUsePercent() + "% business use" : "") +
            "  ·  " + exp.getDate();
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(100, 116, 139));
        text.add(titleLbl, BorderLayout.NORTH);
        text.add(subLbl, BorderLayout.SOUTH);

        // Amount
        JPanel amtPanel = new JPanel(new BorderLayout(0, 2));
        amtPanel.setOpaque(false);
        JLabel amtLbl = new JLabel(String.format("$%.2f", exp.getAmount()), JLabel.RIGHT);
        amtLbl.setFont(amtLbl.getFont().deriveFont(Font.BOLD, 13f));
        amtLbl.setForeground(new Color(30, 41, 59));
        amtPanel.add(amtLbl, BorderLayout.NORTH);
        if (exp.getBusinessUsePercent() < 100) {
            JLabel deductLbl = new JLabel(String.format("$%.2f deductible", exp.getDeductibleAmount()), JLabel.RIGHT);
            deductLbl.setFont(deductLbl.getFont().deriveFont(Font.PLAIN, 10f));
            deductLbl.setForeground(new Color(34, 197, 94));
            amtPanel.add(deductLbl, BorderLayout.SOUTH);
        }

        card.add(icon, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        card.add(amtPanel, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered[0] = false;
                card.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showExpenseDialog(exp);
            }
        });

        return card;
    }

    private void showExpenseDialog(Expenditure existing) {
        boolean isNew = existing == null;
        Expenditure exp = isNew ? new Expenditure() : existing;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Expense" : "Edit Expense", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(460, 420);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel headerTitle = new JLabel(isNew ? "New Expense" : exp.getDescription() != null ? exp.getDescription() : "Edit Expense");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 16f));
        headerTitle.setForeground(new Color(30, 41, 59));
        header.add(headerTitle, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);

        // Date
        JButton dateBtn = new JButton(exp.getDate() != null ? exp.getDate() : LocalDate.now().toString());
        dateBtn.putClientProperty("JButton.buttonType", "roundRect");
        dateBtn.setBackground(new Color(241, 245, 249));
        dateBtn.setForeground(new Color(30, 41, 59));
        dateBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dateBtn.addActionListener(e -> DatePicker.showPicker(dialog, dateBtn));

        // Category
        JComboBox<Expenditure.Category> catCombo = new JComboBox<>(Expenditure.Category.values());
        if (exp.getCategory() != null) catCombo.setSelectedItem(exp.getCategory());

        // Category hint
        JLabel hintLabel = new JLabel(((Expenditure.Category) catCombo.getSelectedItem()).getHint());
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        hintLabel.setForeground(new Color(148, 163, 184));
        catCombo.addActionListener(e -> hintLabel.setText(((Expenditure.Category) catCombo.getSelectedItem()).getHint()));

        // Description
        JTextField descField = new JTextField(exp.getDescription() != null ? exp.getDescription() : "");

        // Amount
        JTextField amountField = new JTextField(exp.getAmount() > 0 ? String.format("%.2f", exp.getAmount()) : "");

        // Business use slider
        JSlider useSlider = new JSlider(0, 100, (int) exp.getBusinessUsePercent());
        useSlider.setMajorTickSpacing(25);
        useSlider.setOpaque(false);
        JLabel useLabel = new JLabel((int) exp.getBusinessUsePercent() + "% business use", JLabel.RIGHT);
        useLabel.setFont(useLabel.getFont().deriveFont(Font.BOLD, 11f));
        useLabel.setForeground(new Color(59, 130, 246));
        useSlider.addChangeListener(e -> useLabel.setText(useSlider.getValue() + "% business use"));

        // Add rows
        form.add(makeFormLabel("Date"), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(dateBtn, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Category"), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);
        form.add(catCombo, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(hintLabel, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Description"), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(descField, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Amount ($)"), gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(amountField, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);

        JPanel useRow = new JPanel(new BorderLayout(8, 0));
        useRow.setOpaque(false);
        useRow.add(makeFormLabel("Business Use"), BorderLayout.WEST);
        useRow.add(useLabel, BorderLayout.EAST);
        form.add(useRow, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(useSlider, gbc);

        dialog.add(new JScrollPane(form) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        footerLeft.setOpaque(false);
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerRight.setOpaque(false);

        if (!isNew) {
            JButton btnDelete = new JButton("Delete");
            btnDelete.putClientProperty("JButton.buttonType", "roundRect");
            btnDelete.setBackground((Color) UIManager.get("App.danger"));
            btnDelete.setForeground(Color.WHITE);
            btnDelete.addActionListener(e -> {
                currentExpenses.remove(exp);
                storage.saveExpenditures(String.valueOf(currentYear), currentExpenses);
                refresh();
                dialog.dispose();
            });
            footerLeft.add(btnDelete);
        }

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton(isNew ? "Add Expense" : "Save Changes");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(ACCENT);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                exp.setDate(dateBtn.getText());
                exp.setCategory((Expenditure.Category) catCombo.getSelectedItem());
                exp.setDescription(descField.getText().trim());
                exp.setAmount(Double.parseDouble(amountField.getText().trim()));
                exp.setBusinessUsePercent(useSlider.getValue());
                if (isNew) currentExpenses.add(exp);
                // Sort by date descending
                currentExpenses.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                storage.saveExpenditures(String.valueOf(currentYear), currentExpenses);
                refresh();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid amount.", "Invalid Amount", JOptionPane.WARNING_MESSAGE);
            }
        });

        footerRight.add(btnCancel);
        footerRight.add(btnSave);
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(btnSave);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private JLabel makeFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        return lbl;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
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
        card.setBorder(BorderFactory.createEmptyBorder(4, 14, 12, 14));
        return card;
    }

    private Color categoryColor(Expenditure.Category cat) {
        if (cat == null) return new Color(148, 163, 184);
        return switch (cat) {
            case VEHICLE -> new Color(239, 68, 68);
            case PHONE_INTERNET -> new Color(59, 130, 246);
            case HOME_OFFICE -> new Color(139, 92, 246);
            case MEALS -> new Color(245, 158, 11);
            case SUPPLIES -> new Color(20, 184, 166);
            case PROFESSIONAL -> new Color(99, 102, 241);
            case ADVERTISING -> new Color(236, 72, 153);
            case OTHER -> new Color(148, 163, 184);
        };
    }

}
