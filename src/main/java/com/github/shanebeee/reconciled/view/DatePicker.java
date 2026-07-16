package com.github.shanebeee.reconciled.view;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DatePicker extends JDialog {

    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color BG = new Color(248, 250, 252);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color WEEKEND_BG = new Color(248, 250, 252);
    private static final Color HOVER_BG = new Color(239, 246, 255);

    private LocalDate selectedDate;
    private YearMonth currentMonth;
    private JPanel calendarGrid;
    private JLabel monthLabel;
    private boolean confirmed = false;

    public DatePicker(Frame parent, LocalDate initialDate) {
        super(parent, "Select Date", true);
        this.selectedDate = initialDate != null ? initialDate : LocalDate.now();
        this.currentMonth = YearMonth.from(selectedDate);
        setLayout(new BorderLayout());
        setSize(380, 420);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        initUI();
        refreshCalendar();
        setLocationRelativeTo(parent);
    }

    // ── Static helpers ───────────────────────────────────────────────────────

    public static void showPicker(Component parent, JButton targetButton) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        LocalDate initialDate;
        try {
            initialDate = LocalDate.parse(targetButton.getText());
        } catch (Exception e) {
            initialDate = LocalDate.now();
        }
        DatePicker picker = new DatePicker((Frame) window, initialDate);
        picker.setVisible(true);
        LocalDate result = picker.getSelectedDate();
        if (result != null) targetButton.setText(result.toString());
    }

    // ── UI Init ──────────────────────────────────────────────────────────────

    private void initUI() {
        // ── Header: prev / month+year / next ────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        JButton btnPrev = makeNavArrow("<");
        JButton btnNext = makeNavArrow(">");
        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 15f));
        monthLabel.setForeground(TEXT_DARK);

        btnPrev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });
        btnNext.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        header.add(btnPrev, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(btnNext, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Calendar grid ────────────────────────────────────────────────────
        calendarGrid = new JPanel(new GridLayout(0, 7, 6, 6));
        calendarGrid.setBackground(Color.WHITE);
        calendarGrid.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        add(calendarGrid, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.setForeground(TEXT_MUTED);
        btnCancel.addActionListener(e -> dispose());

        JButton btnToday = new JButton("Today");
        btnToday.putClientProperty("JButton.buttonType", "roundRect");
        btnToday.setBackground(ACCENT);
        btnToday.setForeground(Color.WHITE);
        btnToday.addActionListener(e -> {
            selectedDate = LocalDate.now();
            currentMonth = YearMonth.from(selectedDate);
            confirmed = true;
            dispose();
        });

        footer.add(btnCancel);
        footer.add(btnToday);
        add(footer, BorderLayout.SOUTH);
    }

    private void refreshCalendar() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calendarGrid.removeAll();

        // Day-of-week headers
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < days.length; i++) {
            JLabel lbl = new JLabel(days[i], JLabel.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
            boolean isWeekend = (i == 5 || i == 6);
            lbl.setForeground(isWeekend ? new Color(148, 163, 184) : TEXT_MUTED);
            calendarGrid.add(lbl);
        }

        // Leading empty cells
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon
        for (int i = 1; i < dayOfWeek; i++) calendarGrid.add(new JLabel(""));

        // Day cells
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            boolean isSelected = date.equals(selectedDate);
            boolean isToday = date.equals(today);
            boolean isWeekend = date.getDayOfWeek().getValue() >= 6;

            calendarGrid.add(makeDayCell(date, day, isSelected, isToday, isWeekend));
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JPanel makeDayCell(LocalDate date, int day, boolean isSelected, boolean isToday, boolean isWeekend) {
        final boolean[] hovered = {false};

        JPanel cell = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (isSelected) {
                    g2.setColor(ACCENT);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                } else if (hovered[0]) {
                    g2.setColor(HOVER_BG);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                } else if (isWeekend) {
                    g2.setColor(WEEKEND_BG);
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                if (isToday && !isSelected) {
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, w - 2, h - 2, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cell.setOpaque(false);
        cell.setPreferredSize(new Dimension(42, 36));
        cell.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        JLabel lbl = new JLabel(String.valueOf(day), JLabel.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(isSelected ? Font.BOLD : Font.PLAIN, 13f));
        lbl.setForeground(isSelected ? Color.WHITE : isWeekend ? TEXT_MUTED : TEXT_DARK);
        cell.add(lbl, BorderLayout.CENTER);

        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
                cell.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered[0] = false;
                cell.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectedDate = date;
                confirmed = true;
                dispose();
            }
        });

        return cell;
    }

    private JButton makeNavArrow(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(TEXT_DARK);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return btn;
    }

    public LocalDate getSelectedDate() {
        return confirmed ? selectedDate : null;
    }

}
