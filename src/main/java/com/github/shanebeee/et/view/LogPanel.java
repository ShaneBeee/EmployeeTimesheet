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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GradientPaint;
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
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int textX = (getWidth() - fm.stringWidth(dayStr)) / 2;

                    if (logCount > 0) {
                        // Day number near top when badge is shown
                        g2.setColor(Color.WHITE);
                        g2.drawString(dayStr, textX, fm.getAscent() + 8);

                        // Badge pill at bottom
                        String badgeText = logCount + (logCount == 1 ? " log" : " logs");
                        g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
                        java.awt.FontMetrics bfm = g2.getFontMetrics();
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
                @Override public void mouseEntered(MouseEvent e) { hovered[0] = true;  dayBtn.repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered[0] = false; dayBtn.repaint(); }
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
            if (l.getDate().equals(date.toString())) {
                dayLogs.add(l);
            }
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Logs for " + date, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(650, 400);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (LogEntry l : dayLogs) {
            String text = "";
            String bossLabel = "";
            if (l.getBossUuid() != null) {
                String finalBossUuid = l.getBossUuid();
                bossLabel = bosses.stream()
                    .filter(b -> b.getId().equals(finalBossUuid) || b.getName().equals(finalBossUuid))
                    .map(Boss::getName)
                    .findFirst()
                    .orElse("Unknown");
            }

            if (l.getType() == LogEntry.EntryType.TIME) {
                List<String> bossPercentages = new ArrayList<>();
                l.getBossPercentages().forEach((uuidString, percent) -> {
                    if (percent > 0) {
                        String bossName = bosses.stream()
                            .filter(boss -> boss.getId().equals(uuidString) || boss.getName().equals(uuidString))
                            .map(Boss::getName)
                            .findFirst()
                            .orElse("Unknown");
                        bossPercentages.add(bossName + ": " + percent + "%");
                    }
                });
                text = "TIME: " + TimePickerPanel.formatTime(l.getStartTime()) + " - " + TimePickerPanel.formatTime(l.getEndTime()) + " (" + String.join(", ", bossPercentages) + ")";
            } else if (l.getType() == LogEntry.EntryType.KILOMETER) {
                text = "KM: " + l.getKilometers() + " (" + bossLabel + ")";
            } else if (l.getType() == LogEntry.EntryType.EXTRA) {
                text = "EXTRA: " + l.getDescription() + " (" + bossLabel + ")";
            }
            listModel.addElement(text);
        }
        JList<String> list = new JList<>(listModel);
        if (!listModel.isEmpty()) {
            list.setSelectedIndex(0);
        }

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = list.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        list.setSelectedIndex(idx);
                        LogEntry entry = dayLogs.get(idx);
                        if (showEntryDialog(entry)) {
                            storage.saveLogs(currentMonth.toString(), currentLogs);
                            dialog.dispose();
                            refreshCalendar();
                        }
                    }
                }
            }
        });

        dialog.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                LogEntry entry = dayLogs.get(idx);
                if (showEntryDialog(entry)) {
                    storage.saveLogs(currentMonth.toString(), currentLogs);
                    dialog.dispose();
                    refreshCalendar();
                }
            }
        });

        btnDelete.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0) {
                currentLogs.remove(dayLogs.get(idx));
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

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Type Selection
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.add(new JLabel("Entry Type:"));
        JComboBox<LogEntry.EntryType> typeCombo = new JComboBox<>(LogEntry.EntryType.values());
        typeCombo.setSelectedItem(entry.getType());
        typePanel.add(typeCombo);
        mainPanel.add(typePanel, BorderLayout.NORTH);

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        // --- TIME CARD ---
        JPanel timeCard = new JPanel(new BorderLayout());
        JPanel timeInputs = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField startField = new JTextField(entry.getStartTime() != null ? TimePickerPanel.formatTime(entry.getStartTime()) : TimePickerPanel.formatTime(storage.getDefaultStartTime()));
        JTextField endField = new JTextField(entry.getEndTime() != null ? TimePickerPanel.formatTime(entry.getEndTime()) : TimePickerPanel.formatTime(storage.getDefaultEndTime()));
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
        timeInputs.add(new JLabel("Start Time:"));
        timeInputs.add(startField);
        timeInputs.add(new JLabel("End Time:"));
        timeInputs.add(endField);
        timeCard.add(timeInputs, BorderLayout.NORTH);

        JPanel bossPercPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        Map<String, JTextField> percFields = new HashMap<>();
        for (Boss b : bosses) {
            bossPercPanel.add(new JLabel(b.getName() + " %:"));
            Double currentPerc = 0.0;
            if (entry.getBossPercentages() != null) {
                currentPerc = entry.getBossPercentages().get(b.getId());
                if (currentPerc == null) {
                    currentPerc = entry.getBossPercentages().getOrDefault(b.getName(), 0.0);
                }
            }
            JTextField pField = new JTextField(String.valueOf(currentPerc));
            percFields.put(b.getId(), pField);
            bossPercPanel.add(pField);
        }
        timeCard.add(new JScrollPane(bossPercPanel), BorderLayout.CENTER);
        cards.add(timeCard, LogEntry.EntryType.TIME.name());

        // --- KILOMETER CARD ---
        JPanel kmCard = new JPanel(new GridLayout(0, 2, 5, 5));
        JComboBox<Boss> kmBossCombo = new JComboBox<>();
        for (Boss b : bosses) kmBossCombo.addItem(b);
        kmBossCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Boss b) setText(b.getName());
                return this;
            }
        });
        if (entry.getBossUuid() != null) {
            for (Boss b : bosses) {
                if (b.getId().equals(entry.getBossUuid()) || b.getName().equals(entry.getBossUuid())) {
                    kmBossCombo.setSelectedItem(b);
                    break;
                }
            }
        }
        JTextField kmField = new JTextField(entry.getKilometers() != null ? String.valueOf(entry.getKilometers()) : "0.0");
        kmCard.add(new JLabel("Select Boss:"));
        kmCard.add(kmBossCombo);
        kmCard.add(new JLabel("Kilometers:"));
        kmCard.add(kmField);
        cards.add(kmCard, LogEntry.EntryType.KILOMETER.name());

        // --- EXTRA CARD ---
        JPanel extraCard = new JPanel(new GridLayout(0, 2, 5, 5));
        JComboBox<Boss> extraBossCombo = new JComboBox<>();
        for (Boss b : bosses) extraBossCombo.addItem(b);
        extraBossCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Boss b) setText(b.getName());
                return this;
            }
        });
        if (entry.getBossUuid() != null) {
            for (Boss b : bosses) {
                if (b.getId().equals(entry.getBossUuid()) || b.getName().equals(entry.getBossUuid())) {
                    extraBossCombo.setSelectedItem(b);
                    break;
                }
            }
        }
        JTextField descField = new JTextField(entry.getDescription() != null ? entry.getDescription() : "");
        JTextField unitsField = new JTextField(entry.getUnits() != null ? String.valueOf(entry.getUnits()) : "1.0");
        JTextField costField = new JTextField(entry.getCostPerUnit() != null ? String.valueOf(entry.getCostPerUnit()) : "0.0");
        extraCard.add(new JLabel("Select Boss:"));
        extraCard.add(extraBossCombo);
        extraCard.add(new JLabel("Description:"));
        extraCard.add(descField);
        extraCard.add(new JLabel("Units:"));
        extraCard.add(unitsField);
        extraCard.add(new JLabel("Cost Per Unit:"));
        extraCard.add(costField);
        cards.add(extraCard, LogEntry.EntryType.EXTRA.name());

        mainPanel.add(cards, BorderLayout.CENTER);

        typeCombo.addActionListener(e -> cardLayout.show(cards, ((LogEntry.EntryType) typeCombo.getSelectedItem()).name()));
        cardLayout.show(cards, entry.getType().name());

        int result = JOptionPane.showConfirmDialog(this, mainPanel, "Log Entry", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            LogEntry.EntryType selectedType = (LogEntry.EntryType) typeCombo.getSelectedItem();
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
            } else if (selectedType == LogEntry.EntryType.EXTRA) {
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
            return true;
        }
        return false;
    }

}
