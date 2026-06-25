package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Expenditure;
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
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ExpensesPanel extends JPanel {

    private static final Color ACCENT = new Color(245, 158, 11);

    private static final int ANIM_MS = 200;

    private final DataStorage storage;
    private int currentYear;
    private JLabel yearLabel;
    private JLabel totalLabel;
    private List<Expenditure> currentExpenses;

    // ── View state ───────────────────────────────────────────────────────────
    private JPanel sidebar;          // year summary sidebar (CardLayout wrapper)
    private CardLayout sidebarCards;  // flips between summary and blank
    private JPanel contentArea;      // holds the sliding panels
    private JPanel gridView;         // 3-col month grid
    private JPanel detailView;       // single-month expense list
    private boolean inDetail = false;
    private YearMonth detailMonth;   // which month is showing in detail

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

        JPanel yearNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        yearNav.setOpaque(false);
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        btnPrev.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");
        yearLabel = new JLabel(String.valueOf(currentYear), JLabel.CENTER);
        yearLabel.setFont(yearLabel.getFont().deriveFont(Font.BOLD, 14f));
        yearLabel.setPreferredSize(new Dimension(60, 20));
        btnPrev.addActionListener(e -> { currentYear--; loadYear(); });
        btnNext.addActionListener(e -> { currentYear++; loadYear(); });
        yearNav.add(btnPrev);
        yearNav.add(yearLabel);
        yearNav.add(btnNext);

        JButton btnAdd = new JButton("+ Add Expense");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground(ACCENT);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 13f));
        btnAdd.addActionListener(e -> showExpenseDialog(null, inDetail ? detailMonth : null));

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        headerRight.add(yearNav);
        headerRight.add(btnAdd);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Body: sliding content + sidebar ──────────────────────────────────
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        // Content area uses null layout for slide animation
        contentArea = new JPanel(null);
        contentArea.setOpaque(false);
        body.add(contentArea, BorderLayout.CENTER);

        // Views — built empty, populated by loadYear()
        gridView   = new JPanel(new GridBagLayout());
        gridView.setOpaque(false);
        detailView = new JPanel(new BorderLayout());
        detailView.setOpaque(false);

        contentArea.add(gridView);
        contentArea.add(detailView);

        // Position them side-by-side initially; grid is visible, detail is off-screen right
        contentArea.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutViews(inDetail ? -contentArea.getWidth() : 0, false);
            }
        });

        sidebarCards = new CardLayout();
        sidebar = new JPanel(sidebarCards);
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.add(buildSummaryPanel(), "summary");
        JPanel blank = new JPanel();
        blank.setOpaque(false);
        sidebar.add(blank, "blank");
        sidebarCards.show(sidebar, "summary");
        body.add(sidebar, BorderLayout.EAST);

        add(body, BorderLayout.CENTER);
    }

    /** Positions grid and detail side-by-side with given x offset for grid.
     *  gridX=0 → grid visible; gridX=-width → detail visible. */
    private void layoutViews(int gridX, boolean repaint) {
        int w = contentArea.getWidth();
        int h = contentArea.getHeight();
        if (w == 0) return;
        gridView.setBounds(gridX, 0, w, h);
        detailView.setBounds(gridX + w, 0, w, h);
        if (repaint) contentArea.repaint();
    }

    private void navigateToDetail(YearMonth ym) {
        detailMonth = ym;
        buildDetailView(ym);
        sidebarCards.show(sidebar, "blank");
        animateSlide(false);
    }

    private void navigateToGrid() {
        sidebarCards.show(sidebar, "summary");
        animateSlide(true);
    }

    private void animateSlide(boolean toGrid) {
        int w = contentArea.getWidth();
        int startX = toGrid ? -w : 0;
        int endX   = toGrid ?  0 : -w;
        long startTime = System.currentTimeMillis();
        inDetail = !toGrid;

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1f, elapsed / (float) ANIM_MS);
                // ease-out cubic
                float ease = 1f - (float) Math.pow(1 - t, 3);
                int x = Math.round(startX + (endX - startX) * ease);
                SwingUtilities.invokeLater(() -> layoutViews(x, true));
                if (t >= 1f) timer.cancel();
            }
        }, 0, 16);
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

        JLabel totalYearLabel = new JLabel("total this year");
        totalYearLabel.setFont(totalYearLabel.getFont().deriveFont(Font.PLAIN, 11f));
        totalYearLabel.setForeground(new Color(148, 163, 184));
        totalYearLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(totalYearLabel);
        card.add(Box.createVerticalStrut(12));

        card.setName("summaryCard");
        panel.add(card, BorderLayout.NORTH);
        return panel;
    }

    private void loadYear() {
        yearLabel.setText(String.valueOf(currentYear));
        currentExpenses = storage.loadExpenditures(String.valueOf(currentYear));
        inDetail = false;
        sidebarCards.show(sidebar, "summary");
        buildGridView();
        layoutViews(0, false);
        refreshSummary();
    }

    // ── Grid view (12 month cards) ────────────────────────────────────────────

    private void buildGridView() {
        gridView.removeAll();

        // Group expenses by month key
        Map<String, List<Expenditure>> byMonth = new LinkedHashMap<>();
        for (Expenditure e : currentExpenses) {
            String key = e.getDate().substring(0, 7);
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0 / 3.0;
        gc.weighty = 1.0 / 4.0;
        gc.insets = new Insets(0, 0, 12, 12);

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(currentYear, m);
            String key = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<Expenditure> exps = byMonth.getOrDefault(key, new ArrayList<>());
            double total = exps.stream().mapToDouble(Expenditure::getTotal).sum();
            boolean empty = exps.isEmpty();

            gc.gridx = (m - 1) % 3;
            gc.gridy = (m - 1) / 3;
            if (gc.gridx == 2) gc.insets = new Insets(0, 0, 12, 0);
            else gc.insets = new Insets(0, 0, 12, 12);

            gridView.add(makeMonthCard(ym, total, exps.size(), empty), gc);
        }

        // No filler needed — weighty on every row fills the space naturally

        gridView.revalidate();
        gridView.repaint();
    }

    private JPanel makeMonthCard(YearMonth ym, double total, int count, boolean empty) {
        final boolean[] hovered = {false};
        Color cardBg   = empty ? new Color(248, 250, 252) : Color.WHITE;
        Color cardBgHov = empty ? new Color(243, 244, 246) : new Color(249, 250, 251);

        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? cardBgHov : cardBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(empty ? new Color(226, 232, 240) : new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        if (!empty) card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Month name
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        JLabel nameLbl = new JLabel(monthName.toUpperCase());
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 10f));
        nameLbl.setForeground(empty ? new Color(203, 213, 225) : new Color(100, 116, 139));

        // Total amount
        JLabel totalLbl = new JLabel(empty ? "—" : String.format("$%.2f", total));
        totalLbl.setFont(totalLbl.getFont().deriveFont(Font.BOLD, 18f));
        totalLbl.setForeground(empty ? new Color(203, 213, 225) : new Color(30, 41, 59));

        // Count sub-label
        JLabel countLbl = new JLabel(empty ? "no expenses" : count + " expense" + (count != 1 ? "s" : ""));
        countLbl.setFont(countLbl.getFont().deriveFont(Font.PLAIN, 11f));
        countLbl.setForeground(new Color(148, 163, 184));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(nameLbl, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(totalLbl);
        center.add(Box.createVerticalStrut(2));
        center.add(countLbl);

        // Category color bar across bottom (only if has expenses)
        JPanel colorBar = buildCategoryBar(currentExpenses.stream()
            .filter(e -> e.getDate().startsWith(ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))))
            .toList(), total);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        if (!empty) card.add(colorBar, BorderLayout.SOUTH);

        if (!empty) {
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
                public void mouseExited(MouseEvent e)  { hovered[0] = false; card.repaint(); }
                public void mouseClicked(MouseEvent e) { navigateToDetail(ym); }
            });
        }

        return card;
    }

    /** Builds a thin stacked colour bar showing category proportions. */
    private JPanel buildCategoryBar(List<Expenditure> exps, double total) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (total <= 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = 0;
                Map<Expenditure.Category, Double> byCat = new LinkedHashMap<>();
                for (Expenditure e : exps)
                    if (e.getCategory() != null)
                        byCat.merge(e.getCategory(), e.getTotal(), Double::sum);
                for (Map.Entry<Expenditure.Category, Double> entry : byCat.entrySet()) {
                    int segW = (int) Math.round(entry.getValue() / total * getWidth());
                    g2.setColor(categoryColor(entry.getKey()));
                    g2.fillRect(x, 0, segW, getHeight());
                    x += segW;
                }
                g2.dispose();
            }
            { setOpaque(false); setPreferredSize(new Dimension(0, 4)); }
        };
    }

    // ── Detail view (single month expense list) ───────────────────────────────

    private void buildDetailView(YearMonth ym) {
        detailView.removeAll();

        String key = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Expenditure> exps = currentExpenses.stream()
            .filter(e -> e.getDate().startsWith(key))
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .toList();
        double monthTotal = exps.stream().mapToDouble(Expenditure::getTotal).sum();

        // ── Detail header ────────────────────────────────────────────────────
        JPanel dHeader = new JPanel(new BorderLayout(12, 0));
        dHeader.setOpaque(false);
        dHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JButton backBtn = new JButton("← Back");
        backBtn.putClientProperty("JButton.buttonType", "roundRect");
        backBtn.addActionListener(e -> navigateToGrid());

        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
            + " " + ym.getYear();
        JLabel monthLbl = new JLabel(monthName);
        monthLbl.setFont(monthLbl.getFont().deriveFont(Font.BOLD, 18f));
        monthLbl.setForeground(new Color(30, 41, 59));

        JLabel monthTotalLbl = new JLabel(String.format("$%.2f", monthTotal));
        monthTotalLbl.setFont(monthTotalLbl.getFont().deriveFont(Font.BOLD, 18f));
        monthTotalLbl.setForeground(ACCENT);

        JPanel dHeaderLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dHeaderLeft.setOpaque(false);
        dHeaderLeft.add(backBtn);
        dHeaderLeft.add(monthLbl);

        dHeader.add(dHeaderLeft, BorderLayout.WEST);
        dHeader.add(monthTotalLbl, BorderLayout.EAST);
        detailView.add(dHeader, BorderLayout.NORTH);

        // ── Expense list ─────────────────────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        if (exps.isEmpty()) {
            JLabel empty = new JLabel("No expenses in this month", JLabel.CENTER);
            empty.setForeground(new Color(148, 163, 184));
            empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
            empty.setAlignmentX(CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(40));
            listPanel.add(empty);
        } else {
            for (Expenditure exp : exps) {
                listPanel.add(makeExpenseCard(exp));
                listPanel.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        detailView.add(scroll, BorderLayout.CENTER);

        detailView.revalidate();
        detailView.repaint();
    }

    private void refreshSummary() {
        // Find summaryCard by name
        JPanel summaryCard = findSummaryCard(sidebar);
        if (summaryCard == null) return;

        double grandTotal = currentExpenses.stream().mapToDouble(Expenditure::getTotal).sum();
        totalLabel.setText(String.format("$%.2f", grandTotal));

        // Remove old category rows (after the 4 fixed components)
        while (summaryCard.getComponentCount() > 4) summaryCard.remove(summaryCard.getComponentCount() - 1);

        Map<Expenditure.Category, Double> byCategory = new LinkedHashMap<>();
        for (Expenditure.Category cat : Expenditure.Category.values()) byCategory.put(cat, 0.0);
        for (Expenditure e : currentExpenses)
            if (e.getCategory() != null)
                byCategory.merge(e.getCategory(), e.getTotal(), Double::sum);

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

    private JPanel findSummaryCard(JPanel parent) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JPanel p) {
                if ("summaryCard".equals(p.getName())) return p;
                JPanel found = findSummaryCard(p);
                if (found != null) return found;
            }
        }
        return null;
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
            "  ·  " + exp.getDate();
        if (!exp.getReceiptFiles().isEmpty()) {
            sub += "  ·  📎 " + exp.getReceiptFiles().size() + " receipt" + (exp.getReceiptFiles().size() > 1 ? "s" : "");
        }
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(100, 116, 139));
        text.add(titleLbl, BorderLayout.NORTH);
        text.add(subLbl, BorderLayout.SOUTH);

        // Amount
        JPanel amtPanel = new JPanel(new BorderLayout(0, 2));
        amtPanel.setOpaque(false);
        JLabel amtLbl = new JLabel(String.format("$%.2f", exp.getTotal()), JLabel.RIGHT);
        amtLbl.setFont(amtLbl.getFont().deriveFont(Font.BOLD, 13f));
        amtLbl.setForeground(new Color(30, 41, 59));
        amtPanel.add(amtLbl, BorderLayout.NORTH);
        if (exp.getGst() > 0) {
            JLabel gstLbl = new JLabel(String.format("GST $%.2f", exp.getGst()), JLabel.RIGHT);
            gstLbl.setFont(gstLbl.getFont().deriveFont(Font.PLAIN, 10f));
            gstLbl.setForeground(new Color(148, 163, 184));
            amtPanel.add(gstLbl, BorderLayout.SOUTH);
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
                if (e.getClickCount() == 2) showExpenseDialog(exp, detailMonth);
            }
        });

        return card;
    }

    private void showExpenseDialog(Expenditure existing, YearMonth contextMonth) {
        boolean isNew = existing == null;
        Expenditure exp = isNew ? new Expenditure() : existing;
        // Pre-fill date to first of context month if adding from detail view
        if (isNew && contextMonth != null && (exp.getDate() == null)) {
            exp.setDate(contextMonth.atDay(1).toString());
        }
        // Working copy of receipt files so we can cancel without side effects
        List<String> pendingReceipts = new ArrayList<>(exp.getReceiptFiles());

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add Expense" : "Edit Expense", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 560);
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

        // ── Amount fields ────────────────────────────────────────────────────
        JTextField subtotalField = new JTextField(exp.getSubtotal() > 0 ? String.format("%.2f", exp.getSubtotal()) : "");
        JTextField gstField     = new JTextField(exp.getGst() > 0     ? String.format("%.2f", exp.getGst())      : "");
        JTextField totalField   = new JTextField(exp.getTotal() > 0   ? String.format("%.2f", exp.getTotal())    : "");

        // Auto-calculate GST (5%) when subtotal changes, then update total
        Runnable recalc = () -> {
            try {
                double sub = Double.parseDouble(subtotalField.getText().trim());
                double gst = Math.round(sub * 0.05 * 100.0) / 100.0;
                gstField.setText(String.format("%.2f", gst));
                totalField.setText(String.format("%.2f", sub + gst));
            } catch (NumberFormatException ignored) {}
        };
        // Recalc total when either subtotal or GST field loses focus / is edited
        subtotalField.addActionListener(e -> recalc.run());
        subtotalField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { recalc.run(); }
        });
        gstField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                try {
                    double sub = Double.parseDouble(subtotalField.getText().trim());
                    double gst = Double.parseDouble(gstField.getText().trim());
                    totalField.setText(String.format("%.2f", sub + gst));
                } catch (NumberFormatException ignored) {}
            }
        });

        // ── Receipt attachments ──────────────────────────────────────────────
        JPanel receiptsPanel = new JPanel();
        receiptsPanel.setLayout(new BoxLayout(receiptsPanel, BoxLayout.Y_AXIS));
        receiptsPanel.setOpaque(false);

        // Helper to process one or more dropped/chosen files
        // Defined before refreshReceipts so the drop zone lambda can reference it
        // We'll use a holder so the lambda can reference refreshReceipts too
        Runnable[] refreshHolder = {null};
        java.util.function.Consumer<List<File>> addFiles = files -> {
            String dateStr = dateBtn.getText();
            String year  = dateStr.substring(0, 4);
            String month = dateStr.substring(5, 7);
            String desc  = descField.getText().trim().isEmpty() ? "receipt" : descField.getText().trim();
            String expId = exp.getId();
            for (File f : files) {
                String name = f.getName().toLowerCase();
                if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") &&
                    !name.endsWith(".png") && !name.endsWith(".pdf") &&
                    !name.endsWith(".heic")) continue;
                int nextIdx = pendingReceipts.size() + 1;
                String rel = storage.addReceiptFile(f, year, month, expId, desc, nextIdx);
                if (rel != null) pendingReceipts.add(rel);
            }
            SwingUtilities.invokeLater(() -> { if (refreshHolder[0] != null) refreshHolder[0].run(); });
        };

        // Drag-and-drop zone
        final boolean[] dropHover = {false};
        JPanel dropZone = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = dropHover[0] ? new Color(245, 158, 11, 18) : new Color(241, 245, 249);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                float[] dash = {6f, 4f};
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
                g2.setColor(dropHover[0] ? ACCENT : new Color(203, 213, 225));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        dropZone.setOpaque(false);
        dropZone.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        dropZone.setPreferredSize(new Dimension(0, 52));
        dropZone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel dropLabel = new JLabel("Drop receipts here  ·  jpg, png, pdf, heic", JLabel.CENTER);
        dropLabel.setFont(dropLabel.getFont().deriveFont(Font.PLAIN, 11f));
        dropLabel.setForeground(new Color(148, 163, 184));
        dropZone.add(dropLabel, BorderLayout.CENTER);

        dropZone.setDropTarget(new DropTarget() {
            @Override
            public synchronized void dragEnter(java.awt.dnd.DropTargetDragEvent e) {
                dropHover[0] = true;
                dropZone.repaint();
            }
            @Override
            public synchronized void dragExit(java.awt.dnd.DropTargetEvent e) {
                dropHover[0] = false;
                dropZone.repaint();
            }
            @Override
            @SuppressWarnings("unchecked")
            public synchronized void drop(DropTargetDropEvent e) {
                dropHover[0] = false;
                dropZone.repaint();
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> dropped = (List<File>) e.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    addFiles.accept(dropped);
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.rejectDrop();
                }
            }
        });

        Runnable refreshReceipts = () -> {
            receiptsPanel.removeAll();
            for (int i = 0; i < pendingReceipts.size(); i++) {
                final String rel = pendingReceipts.get(i);
                final int idx = i;
                String filename = storage.getReceiptFile(rel).getName();
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                JButton openBtn = new JButton("📄 " + filename);
                openBtn.putClientProperty("JButton.buttonType", "roundRect");
                openBtn.setFont(openBtn.getFont().deriveFont(Font.PLAIN, 11f));
                openBtn.setHorizontalAlignment(SwingConstants.LEFT);
                openBtn.addActionListener(e -> storage.openReceiptFile(rel));
                JButton removeBtn = new JButton("✕");
                removeBtn.putClientProperty("JButton.buttonType", "roundRect");
                removeBtn.setFont(removeBtn.getFont().deriveFont(Font.PLAIN, 10f));
                removeBtn.setPreferredSize(new Dimension(28, 24));
                removeBtn.addActionListener(e -> {
                    File f = storage.getReceiptFile(rel);
                    if (f.exists()) f.delete();
                    pendingReceipts.remove(idx);
                    SwingUtilities.invokeLater(() -> {
                        if (refreshHolder[0] != null) refreshHolder[0].run();
                    });
                });
                row.add(openBtn, BorderLayout.CENTER);
                row.add(removeBtn, BorderLayout.EAST);
                receiptsPanel.add(row);
                receiptsPanel.add(Box.createVerticalStrut(4));
            }
            receiptsPanel.revalidate();
            receiptsPanel.repaint();
        };
        refreshHolder[0] = refreshReceipts;
        refreshReceipts.run();

        // Wrap the file list + drop zone together in a container
        JPanel receiptsContainer = new JPanel();
        receiptsContainer.setLayout(new BoxLayout(receiptsContainer, BoxLayout.Y_AXIS));
        receiptsContainer.setOpaque(false);
        receiptsContainer.add(receiptsPanel);
        receiptsContainer.add(Box.createVerticalStrut(6));
        receiptsContainer.add(dropZone);

        JButton btnAddReceipt = new JButton("+ Attach Receipt");
        btnAddReceipt.putClientProperty("JButton.buttonType", "roundRect");
        btnAddReceipt.setFont(btnAddReceipt.getFont().deriveFont(Font.PLAIN, 12f));
        btnAddReceipt.addActionListener(e -> {
            // Use native macOS file picker (FileDialog) instead of JFileChooser
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(dialog);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            FileDialog fd = new FileDialog(owner, "Select Receipt", FileDialog.LOAD);
            fd.setMultipleMode(true);
            // macOS FileDialog doesn't support extension filtering natively via setFilenameFilter
            // but we filter after selection
            fd.setVisible(true);
            File[] chosen = fd.getFiles();
            if (chosen != null && chosen.length > 0) {
                addFiles.accept(java.util.Arrays.asList(chosen));
            }
        });

        // Add form rows
        form.add(makeFormLabel("Date"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(dateBtn, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Category"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 2, 0);
        form.add(catCombo, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(hintLabel, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Description"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(descField, gbc);

        // Amount row — three fields side by side
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Amount"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel amtRow = new JPanel(new GridBagLayout());
        amtRow.setOpaque(false);
        GridBagConstraints ag = new GridBagConstraints();
        ag.fill = GridBagConstraints.HORIZONTAL;
        ag.weightx = 1.0;
        ag.insets = new Insets(0, 0, 0, 0);
        ag.gridx = 0; ag.gridy = 0;
        amtRow.add(makeAmtLabel("Subtotal"), ag);
        ag.gridx = 1; ag.insets = new Insets(0, 8, 0, 8);
        amtRow.add(makeAmtLabel("GST (5%)"), ag);
        ag.gridx = 2; ag.insets = new Insets(0, 0, 0, 0);
        amtRow.add(makeAmtLabel("Total"), ag);
        ag.gridx = 0; ag.gridy = 1; ag.insets = new Insets(4, 0, 0, 0);
        amtRow.add(subtotalField, ag);
        ag.gridx = 1; ag.insets = new Insets(4, 8, 0, 8);
        amtRow.add(gstField, ag);
        ag.gridx = 2; ag.insets = new Insets(4, 0, 0, 0);
        amtRow.add(totalField, ag);
        form.add(amtRow, gbc);

        // Receipts section
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(makeFormLabel("Receipts"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(receiptsContainer, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(btnAddReceipt, gbc);

        dialog.add(new JScrollPane(form) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JPanel footerLeft  = new JPanel(new FlowLayout(FlowLayout.LEFT,  10, 12));
        footerLeft.setOpaque(false);
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerRight.setOpaque(false);

        if (!isNew) {
            JButton btnDelete = new JButton("Delete");
            btnDelete.putClientProperty("JButton.buttonType", "roundRect");
            btnDelete.setBackground((Color) UIManager.get("App.danger"));
            btnDelete.setForeground(Color.WHITE);
            btnDelete.addActionListener(e -> {
                storage.deleteReceiptFiles(exp);
                currentExpenses.remove(exp);
                storage.saveExpenditures(String.valueOf(currentYear), currentExpenses);
                refreshSummary();
                buildGridView();
                if (inDetail && detailMonth != null) buildDetailView(detailMonth);
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
                double subtotal = Double.parseDouble(subtotalField.getText().trim());
                double gst      = gstField.getText().trim().isEmpty() ? 0 : Double.parseDouble(gstField.getText().trim());
                double total    = totalField.getText().trim().isEmpty() ? subtotal + gst : Double.parseDouble(totalField.getText().trim());
                exp.setDate(dateBtn.getText());
                exp.setCategory((Expenditure.Category) catCombo.getSelectedItem());
                exp.setDescription(descField.getText().trim());
                exp.setSubtotal(subtotal);
                exp.setGst(gst);
                exp.setTotal(total);
                exp.setReceiptFiles(new ArrayList<>(pendingReceipts));
                if (isNew) currentExpenses.add(exp);
                currentExpenses.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                storage.saveExpenditures(String.valueOf(currentYear), currentExpenses);
                refreshSummary();
                buildGridView();
                if (inDetail && detailMonth != null) buildDetailView(detailMonth);
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
            public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel makeAmtLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        return lbl;
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
