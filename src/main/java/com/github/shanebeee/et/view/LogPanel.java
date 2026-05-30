package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.LogEntry;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class LogPanel extends JPanel {

    private final DataStorage storage;
    private YearMonth currentMonth;
    private JPanel calendarGrid;
    private JLabel monthLabel;
    private List<LogEntry> currentLogs;

    public LogPanel(DataStorage storage) {
        this.storage = storage;
        this.currentMonth = YearMonth.now();
        setLayout(new BorderLayout());
        initUI();
        loadMonth();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Work Logs");
        titleLabel.setForeground(new Color(30, 41, 59));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel();
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        btnPrev.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");

        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));
        monthLabel.setPreferredSize(new Dimension(150, 20));

        JButton btnToday = new JButton("Today");
        btnToday.putClientProperty("JButton.buttonType", "roundRect");

        btnPrev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonth();
            btnToday.setVisible(!currentMonth.equals(YearMonth.now()));
        });
        btnNext.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonth();
            btnToday.setVisible(!currentMonth.equals(YearMonth.now()));
        });
        btnToday.addActionListener(e -> {
            currentMonth = YearMonth.now();
            loadMonth();
            btnToday.setVisible(false);
        });
        btnToday.setVisible(false); // hidden when already on current month

        navPanel.setOpaque(false);
        navPanel.add(btnToday);
        navPanel.add(btnPrev);
        navPanel.add(monthLabel);
        navPanel.add(btnNext);
        topPanel.add(navPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        calendarGrid = new JPanel(new GridLayout(0, 7, 8, 8));
        calendarGrid.setOpaque(false);
        add(calendarGrid, BorderLayout.CENTER);
    }

    private void loadMonth() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        currentLogs = storage.loadLogs(currentMonth.toString());
        refreshCalendar();
    }

    private void refreshCalendar() {
        calendarGrid.removeAll();

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < days.length; i++) {
            JLabel label = new JLabel(days[i], JLabel.CENTER);
            label.setForeground(i >= 5
                ? new Color(148, 163, 184) // muted for Sat/Sun
                : new Color(100, 116, 139));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            calendarGrid.add(label);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)

        for (int i = 1; i < dayOfWeek; i++) {
            calendarGrid.add(new JLabel(""));
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            LocalDate date = currentMonth.atDay(day);
            final long logCount = currentLogs.stream().filter(l -> l.getDate().equals(date.toString())).count();
            final boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
            final boolean isToday = date.equals(LocalDate.now());
            final boolean[] hovered = {false};

            JButton dayBtn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    if (logCount > 0) {
                        // Gradient blue for logged days
                        Color top = hovered[0] ? new Color(120, 172, 255) : new Color(99, 158, 255);
                        Color bot = hovered[0] ? new Color(82, 148, 255) : new Color(59, 130, 246);
                        g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    } else if (isToday) {
                        // Soft accent tint for today
                        g2.setColor(new Color(235, 245, 255));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.setColor(new Color(226, 232, 240));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    } else if (isWeekend) {
                        g2.setColor(hovered[0] ? new Color(226, 232, 240) : new Color(241, 245, 249));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.setColor(new Color(203, 213, 225));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    } else {
                        g2.setColor(hovered[0] ? new Color(239, 246, 255) : Color.WHITE);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        g2.setColor(new Color(226, 232, 240));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    }

                    // Today: bright green dot in top-right corner
                    if (isToday) {
                        int dotSize = 10;
                        int dotX = getWidth() - dotSize - 6;
                        int dotY = 6;
                        // Shadow
                        g2.setColor(new Color(0, 0, 0, 50));
                        g2.fillOval(dotX + 1, dotY + 2, dotSize, dotSize);
                        // Bright green fill
                        g2.setColor(new Color(74, 222, 128));
                        g2.fillOval(dotX, dotY, dotSize, dotSize);
                    }

                    // Draw day number
                    String dayStr = String.valueOf(d);
                    g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (getWidth() - fm.stringWidth(dayStr)) / 2;

                    if (logCount > 0) {
                        // Day number near top when badge is shown
                        g2.setColor(Color.WHITE);
                        g2.drawString(dayStr, textX, fm.getAscent() + 8);

                        // Badge pill at bottom
                        String badgeText = logCount + (logCount == 1 ? " log" : " logs");
                        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
                        FontMetrics bfm = g2.getFontMetrics();
                        int bw = bfm.stringWidth(badgeText) + 10;
                        int bh = bfm.getHeight() + 2;
                        int bx = (getWidth() - bw) / 2;
                        int by = getHeight() - bh - 6;
                        g2.setColor(new Color(255, 255, 255, 60));
                        g2.fillRoundRect(bx, by, bw, bh, 8, 8);
                        g2.setColor(Color.WHITE);
                        g2.drawString(badgeText, bx + 5, by + bfm.getAscent() + 1);
                    } else {
                        // Top-aligned day number (consistent with logged cells)
                        if (isToday) {
                            g2.setColor(new Color(59, 130, 246));
                        } else {
                            g2.setColor(isWeekend ? new Color(148, 163, 184) : new Color(30, 41, 59));
                        }
                        g2.drawString(dayStr, textX, fm.getAscent() + 8);
                    }

                    g2.dispose();
                }
            };
            dayBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered[0] = true;
                    dayBtn.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered[0] = false;
                    dayBtn.repaint();
                }
            });
            dayBtn.setOpaque(false);
            dayBtn.setContentAreaFilled(false);
            dayBtn.setBorderPainted(false);
            dayBtn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            dayBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            dayBtn.addActionListener(e -> showDayLogs(date));
            calendarGrid.add(dayBtn);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private void showDayLogs(LocalDate date) {
        List<Boss> bosses = storage.loadBosses();
        List<LogEntry> dayLogs = new ArrayList<>();
        for (LogEntry l : currentLogs) {
            if (l.getDate().equals(date.toString())) dayLogs.add(l);
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(680, 480);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel headerTitle = new JLabel(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 18f));
        headerTitle.setForeground(new Color(30, 41, 59));
        JLabel headerSub = new JLabel(dayLogs.size() + (dayLogs.size() == 1 ? " entry" : " entries"));
        headerSub.setFont(headerSub.getFont().deriveFont(Font.PLAIN, 12f));
        headerSub.setForeground(new Color(100, 116, 139));
        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(headerTitle);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(headerSub);
        header.add(headerText, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Entry cards ─────────────────────────────────────────────────────
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(new Color(248, 250, 252));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        final int[] selectedIdx = {dayLogs.isEmpty() ? -1 : 0};
        List<JPanel> cardPanels = new ArrayList<>();

        Supplier<Void> rebuildCards = null;
        final Supplier<Void>[] rebuildRef = new Supplier[1];

        rebuildRef[0] = () -> {
            cardsPanel.removeAll();
            cardPanels.clear();
            for (int ci = 0; ci < dayLogs.size(); ci++) {
                final int idx = ci;
                LogEntry l = dayLogs.get(ci);

                // Resolve boss label
                String bossLabel = "";
                if (l.getBossUuid() != null) {
                    String uuid = l.getBossUuid();
                    bossLabel = bosses.stream()
                        .filter(b -> b.getId().equals(uuid) || b.getName().equals(uuid))
                        .map(Boss::getName).findFirst().orElse("Unknown");
                }

                // Accent color + icon letter per type
                Color accent;
                String iconLetter;
                String titleText;
                String subtitleText;
                if (l.getType() == LogEntry.EntryType.TIME) {
                    accent = new Color(59, 130, 246);
                    iconLetter = "T";
                    titleText = TimePickerPanel.formatTime(l.getStartTime()) + "  →  " + TimePickerPanel.formatTime(l.getEndTime());
                    List<String> parts = new ArrayList<>();
                    if (l.getBossPercentages() != null) {
                        l.getBossPercentages().forEach((uid, pct) -> {
                            if (pct > 0) {
                                String bn = bosses.stream()
                                    .filter(b -> b.getId().equals(uid) || b.getName().equals(uid))
                                    .map(Boss::getName).findFirst().orElse("Unknown");
                                parts.add(bn + " " + pct.intValue() + "%");
                            }
                        });
                    }
                    subtitleText = parts.isEmpty() ? "No boss assigned" : String.join("  ·  ", parts);
                } else if (l.getType() == LogEntry.EntryType.KILOMETER) {
                    accent = new Color(34, 197, 94);
                    iconLetter = "K";
                    titleText = l.getKilometers() + " km";
                    subtitleText = bossLabel;
                } else {
                    accent = new Color(245, 158, 11);
                    iconLetter = "E";
                    titleText = l.getDescription() != null ? l.getDescription() : "Extra";
                    subtitleText = bossLabel + (l.getUnits() != null ? "  ·  " + l.getUnits() + " × $" + l.getCostPerUnit() : "");
                }

                final Color cardAccent = accent;
                final boolean[] cardHovered = {false};
                final boolean isSelected = (idx == selectedIdx[0]);

                JPanel card = new JPanel(new BorderLayout(12, 0)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(isSelected ? new Color(239, 246, 255) : (cardHovered[0] ? new Color(249, 250, 251) : Color.WHITE));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                        if (isSelected) {
                            g2.setColor(new Color(59, 130, 246, 80));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                        } else {
                            g2.setColor(new Color(226, 232, 240));
                            g2.setStroke(new BasicStroke(1f));
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                        }
                        // Left accent bar
                        g2.setColor(cardAccent);
                        g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                card.setOpaque(false);
                card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

                // Icon circle
                JPanel iconCircle = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(cardAccent.getRed(), cardAccent.getGreen(), cardAccent.getBlue(), 20));
                        g2.fillOval(0, 0, getWidth(), getHeight());
                        g2.setColor(cardAccent);
                        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(iconLetter, (getWidth() - fm.stringWidth(iconLetter)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                        g2.dispose();
                    }
                };
                iconCircle.setOpaque(false);
                iconCircle.setPreferredSize(new Dimension(40, 40));

                // Text
                JPanel textPanel = new JPanel();
                textPanel.setOpaque(false);
                textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
                JLabel titleLbl = new JLabel(titleText);
                titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 13f));
                titleLbl.setForeground(new Color(30, 41, 59));
                JLabel subLbl = new JLabel(subtitleText);
                subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
                subLbl.setForeground(new Color(100, 116, 139));
                textPanel.add(titleLbl);
                textPanel.add(Box.createVerticalStrut(3));
                textPanel.add(subLbl);

                card.add(iconCircle, BorderLayout.WEST);
                card.add(textPanel, BorderLayout.CENTER);

                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedIdx[0] = idx;
                        rebuildRef[0].get();
                        if (e.getClickCount() == 2) {
                            if (showEntryDialog(dayLogs.get(idx))) {
                                storage.saveLogs(currentMonth.toString(), currentLogs);
                                dialog.dispose();
                                refreshCalendar();
                            }
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cardHovered[0] = true;
                        card.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        cardHovered[0] = false;
                        card.repaint();
                    }
                });

                cardPanels.add(card);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(8));
            }

            if (dayLogs.isEmpty()) {
                JLabel empty = new JLabel("No entries yet — click Add Entry to get started", JLabel.CENTER);
                empty.setForeground(new Color(148, 163, 184));
                empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
                cardsPanel.add(empty);
            }

            cardsPanel.revalidate();
            cardsPanel.repaint();
            return null;
        };
        rebuildRef[0].get();

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(248, 250, 252));
        dialog.add(scroll, BorderLayout.CENTER);

        // ── Button bar ───────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnAdd = new JButton("Add Entry");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground((Color) UIManager.get("App.success"));
        btnAdd.setForeground(Color.WHITE);

        JButton btnEdit = new JButton("Edit Entry");
        btnEdit.putClientProperty("JButton.buttonType", "roundRect");
        btnEdit.setBackground((Color) UIManager.get("App.warning"));
        btnEdit.setForeground(Color.WHITE);

        JButton btnDelete = new JButton("Delete Entry");
        btnDelete.putClientProperty("JButton.buttonType", "roundRect");
        btnDelete.setBackground((Color) UIManager.get("App.danger"));
        btnDelete.setForeground(Color.WHITE);

        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            LogEntry entry = new LogEntry();
            entry.setDate(date.toString());
            if (showEntryDialog(entry)) {
                currentLogs.add(entry);
                storage.saveLogs(currentMonth.toString(), currentLogs);
                dialog.dispose();
                refreshCalendar();
            }
        });

        btnEdit.addActionListener(e -> {
            if (selectedIdx[0] >= 0) {
                if (showEntryDialog(dayLogs.get(selectedIdx[0]))) {
                    storage.saveLogs(currentMonth.toString(), currentLogs);
                    dialog.dispose();
                    refreshCalendar();
                }
            }
        });

        btnDelete.addActionListener(e -> {
            if (selectedIdx[0] >= 0) {
                currentLogs.remove(dayLogs.get(selectedIdx[0]));
                storage.saveLogs(currentMonth.toString(), currentLogs);
                dialog.dispose();
                refreshCalendar();
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private boolean showEntryDialog(LogEntry entry) {
        List<Boss> bosses = storage.loadBosses();
        if (bosses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add a boss first!");
            return false;
        }

        final boolean[] confirmed = {false};

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Log Entry", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(520, 460);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel headerTitle = new JLabel(entry.getDate() != null ? entry.getDate() : "New Entry");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 16f));
        headerTitle.setForeground(new Color(30, 41, 59));
        header.add(headerTitle, BorderLayout.WEST);

        // Type toggle buttons
        JPanel typeToggle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        typeToggle.setOpaque(false);
        JButton btnTime = makeTypeToggle("Time", entry.getType() == LogEntry.EntryType.TIME);
        JButton btnKm = makeTypeToggle("KM", entry.getType() == LogEntry.EntryType.KILOMETER);
        JButton btnExtra = makeTypeToggle("Extra", entry.getType() == LogEntry.EntryType.EXTRA);
        typeToggle.add(btnTime);
        typeToggle.add(btnKm);
        typeToggle.add(btnExtra);
        header.add(typeToggle, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Form area ────────────────────────────────────────────────────────
        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.setBackground(new Color(248, 250, 252));

        // Helper to create a styled form row
        // TIME CARD ──────────────────────────────────────────────────────────
        JPanel timeCard = new JPanel();
        timeCard.setLayout(new BoxLayout(timeCard, BoxLayout.Y_AXIS));
        timeCard.setBackground(new Color(248, 250, 252));
        timeCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JTextField startField = new JTextField(entry.getStartTime() != null
            ? TimePickerPanel.formatTime(entry.getStartTime())
            : TimePickerPanel.formatTime(storage.getDefaultStartTime()));
        JTextField endField = new JTextField(entry.getEndTime() != null
            ? TimePickerPanel.formatTime(entry.getEndTime())
            : TimePickerPanel.formatTime(storage.getDefaultEndTime()));
        startField.setEditable(false);
        endField.setEditable(false);
        startField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(LogPanel.this, startField);
            }
        });
        endField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TimePickerPanel.showPicker(LogPanel.this, endField);
            }
        });

        timeCard.add(makeFormSection("Time Range"));
        timeCard.add(Box.createVerticalStrut(8));
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        timeRow.setOpaque(false);
        timeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        timeRow.add(makeFieldBlock("Start Time", startField));
        timeRow.add(makeFieldBlock("End Time", endField));
        timeCard.add(timeRow);
        timeCard.add(Box.createVerticalStrut(16));
        timeCard.add(makeFormSection("Boss Allocation"));
        timeCard.add(Box.createVerticalStrut(8));

        Map<String, JTextField> percFields = new HashMap<>();
        for (Boss b : bosses) {
            Double currentPerc = 0.0;
            if (entry.getBossPercentages() != null) {
                currentPerc = entry.getBossPercentages().get(b.getId());
                if (currentPerc == null) currentPerc = entry.getBossPercentages().getOrDefault(b.getName(), 0.0);
            }
            JTextField pField = new JTextField(String.valueOf(currentPerc));
            percFields.put(b.getId(), pField);
            JPanel row = makeFieldBlock(b.getName() + " (%)", pField);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            timeCard.add(row);
            timeCard.add(Box.createVerticalStrut(6));
        }
        cards.add(new JScrollPane(timeCard) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, LogEntry.EntryType.TIME.name());

        // KM CARD ────────────────────────────────────────────────────────────
        JPanel kmCard = new JPanel();
        kmCard.setLayout(new BoxLayout(kmCard, BoxLayout.Y_AXIS));
        kmCard.setBackground(new Color(248, 250, 252));
        kmCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JComboBox<Boss> kmBossCombo = makeBossCombo(bosses, entry.getBossUuid());
        JTextField kmField = new JTextField(entry.getKilometers() != null ? String.valueOf(entry.getKilometers()) : "0.0");
        kmCard.add(makeFormSection("Kilometer Details"));
        kmCard.add(Box.createVerticalStrut(8));
        JPanel kmRow = makeFieldBlock("Boss", kmBossCombo);
        kmRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        kmCard.add(kmRow);
        kmCard.add(Box.createVerticalStrut(6));
        JPanel kmAmtRow = makeFieldBlock("Kilometers", kmField);
        kmAmtRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        kmCard.add(kmAmtRow);
        cards.add(kmCard, LogEntry.EntryType.KILOMETER.name());

        // EXTRA CARD ─────────────────────────────────────────────────────────
        JPanel extraCard = new JPanel();
        extraCard.setLayout(new BoxLayout(extraCard, BoxLayout.Y_AXIS));
        extraCard.setBackground(new Color(248, 250, 252));
        extraCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JComboBox<Boss> extraBossCombo = makeBossCombo(bosses, entry.getBossUuid());
        JTextField descField = new JTextField(entry.getDescription() != null ? entry.getDescription() : "");
        JTextField unitsField = new JTextField(entry.getUnits() != null ? String.valueOf(entry.getUnits()) : "1.0");
        JTextField costField = new JTextField(entry.getCostPerUnit() != null ? String.valueOf(entry.getCostPerUnit()) : "0.0");
        extraCard.add(makeFormSection("Extra Entry Details"));
        extraCard.add(Box.createVerticalStrut(8));
        for (Object[] pair : new Object[][]{{"Boss", extraBossCombo}, {"Description", descField}, {"Units", unitsField}, {"Cost Per Unit ($)", costField}}) {
            JPanel row = makeFieldBlock((String) pair[0], (JComponent) pair[1]);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            extraCard.add(row);
            extraCard.add(Box.createVerticalStrut(6));
        }
        cards.add(extraCard, LogEntry.EntryType.EXTRA.name());

        dialog.add(cards, BorderLayout.CENTER);
        cardLayout.show(cards, entry.getType() != null ? entry.getType().name() : LogEntry.EntryType.TIME.name());

        // Type toggle actions
        final JButton[] toggleBtns = {btnTime, btnKm, btnExtra};
        final LogEntry.EntryType[] toggleTypes = {LogEntry.EntryType.TIME, LogEntry.EntryType.KILOMETER, LogEntry.EntryType.EXTRA};
        for (int ti = 0; ti < toggleBtns.length; ti++) {
            final int fi = ti;
            toggleBtns[ti].addActionListener(e -> {
                for (JButton tb : toggleBtns) setTypeToggleActive(tb, false);
                setTypeToggleActive(toggleBtns[fi], true);
                cardLayout.show(cards, toggleTypes[fi].name());
            });
        }

        // ── Footer buttons ───────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");

        JButton btnSave = new JButton("Save Entry");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(new Color(59, 130, 246));
        btnSave.setForeground(Color.WHITE);

        footer.add(btnCancel);
        footer.add(btnSave);
        dialog.add(footer, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            // Figure out active type
            LogEntry.EntryType selectedType = LogEntry.EntryType.TIME;
            for (int ti = 0; ti < toggleBtns.length; ti++) {
                if (toggleBtns[ti].getClientProperty("active") == Boolean.TRUE) {
                    selectedType = toggleTypes[ti];
                    break;
                }
            }
            entry.setType(selectedType);
            if (selectedType == LogEntry.EntryType.TIME) {
                entry.setStartTime(TimePickerPanel.unformatTime(startField.getText()));
                entry.setEndTime(TimePickerPanel.unformatTime(endField.getText()));
                Map<String, Double> percs = new HashMap<>();
                for (Boss b : bosses) {
                    try {
                        percs.put(b.getId(), Double.parseDouble(percFields.get(b.getId()).getText()));
                    } catch (NumberFormatException ex) {
                        percs.put(b.getId(), 0.0);
                    }
                }
                entry.setBossPercentages(percs);
                entry.setBossUuid(null);
                entry.setKilometers(null);
                entry.setDescription(null);
                entry.setUnits(null);
                entry.setCostPerUnit(null);
            } else if (selectedType == LogEntry.EntryType.KILOMETER) {
                Boss b = (Boss) kmBossCombo.getSelectedItem();
                entry.setBossUuid(b != null ? b.getId() : null);
                try {
                    entry.setKilometers(Double.parseDouble(kmField.getText()));
                } catch (NumberFormatException ex) {
                    entry.setKilometers(0.0);
                }
                entry.setStartTime(null);
                entry.setEndTime(null);
                entry.setBossPercentages(null);
                entry.setDescription(null);
                entry.setUnits(null);
                entry.setCostPerUnit(null);
            } else {
                Boss b = (Boss) extraBossCombo.getSelectedItem();
                entry.setBossUuid(b != null ? b.getId() : null);
                entry.setDescription(descField.getText());
                try {
                    entry.setUnits(Double.parseDouble(unitsField.getText()));
                    entry.setCostPerUnit(Double.parseDouble(costField.getText()));
                } catch (NumberFormatException ex) {
                    entry.setUnits(1.0);
                    entry.setCostPerUnit(0.0);
                }
                entry.setStartTime(null);
                entry.setEndTime(null);
                entry.setBossPercentages(null);
                entry.setKilometers(null);
            }
            confirmed[0] = true;
            dialog.dispose();
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return confirmed[0];
    }

    // ── UI helper methods ────────────────────────────────────────────────────

    private JButton makeTypeToggle(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("active", active);
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));
        if (active) {
            btn.setBackground(new Color(59, 130, 246));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(241, 245, 249));
            btn.setForeground(new Color(100, 116, 139));
        }
        return btn;
    }

    private void setTypeToggleActive(JButton btn, boolean active) {
        btn.putClientProperty("active", active);
        btn.setBackground(active ? new Color(59, 130, 246) : new Color(241, 245, 249));
        btn.setForeground(active ? Color.WHITE : new Color(100, 116, 139));
    }

    private JPanel makeFormSection(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel makeFieldBlock(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JComboBox<Boss> makeBossCombo(List<Boss> bosses, String selectedUuid) {
        JComboBox<Boss> combo = new JComboBox<>();
        for (Boss b : bosses) combo.addItem(b);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Boss b) setText(b.getName());
                return this;
            }
        });
        if (selectedUuid != null) {
            for (Boss b : bosses) {
                if (b.getId().equals(selectedUuid) || b.getName().equals(selectedUuid)) {
                    combo.setSelectedItem(b);
                    break;
                }
            }
        }
        return combo;
    }

}
