package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.LogEntry;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.*;
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
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel titleLabel = new JLabel("Work Logs");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel();
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        btnPrev.putClientProperty("JButton.buttonType", "roundRect");
        btnNext.putClientProperty("JButton.buttonType", "roundRect");

        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));
        monthLabel.setPreferredSize(new Dimension(150, 20));

        btnPrev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonth();
        });
        btnNext.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonth();
        });

        navPanel.add(btnPrev);
        navPanel.add(monthLabel);
        navPanel.add(btnNext);
        topPanel.add(navPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        calendarGrid = new JPanel(new GridLayout(0, 7, 5, 5));
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
        for (String day : days) {
            calendarGrid.add(new JLabel(day, JLabel.CENTER));
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
            JButton dayBtn = new JButton(String.valueOf(day));
            dayBtn.putClientProperty("JButton.buttonType", "square");
            
            // Check if there are logs for this day
            long count = currentLogs.stream().filter(l -> l.getDate().equals(date.toString())).count();
            if (count > 0) {
                dayBtn.setBackground(UIManager.getColor("Component.accentColor"));
                dayBtn.setForeground(Color.WHITE);
                String plural = count == 1 ? "log" : "logs";
                dayBtn.setText("<html><center>" + day + "<br><small>(" + count + " " + plural + ")</small></center></html>");
            }

            if (date.equals(LocalDate.now())) {
                dayBtn.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.accentColor"), 2));
            }

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
        dialog.setSize(500, 400);

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
                text = "TIME: " + l.getStartTime() + " - " + l.getEndTime();
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

        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
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

        JPanel btnPanel = new JPanel();
        JButton btnAdd = new JButton("Add Entry");
        JButton btnEdit = new JButton("Edit Entry");
        JButton btnDelete = new JButton("Delete Entry");
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
        JTextField startField = new JTextField(entry.getStartTime() != null ? entry.getStartTime() : storage.getDefaultStartTime());
        JTextField endField = new JTextField(entry.getEndTime() != null ? entry.getEndTime() : storage.getDefaultEndTime());
        startField.setEditable(false);
        endField.setEditable(false);
        startField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { TimePickerPanel.showPicker(LogPanel.this, startField); }
        });
        endField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { TimePickerPanel.showPicker(LogPanel.this, endField); }
        });
        timeInputs.add(new JLabel("Start Time:")); timeInputs.add(startField);
        timeInputs.add(new JLabel("End Time:")); timeInputs.add(endField);
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
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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
        kmCard.add(new JLabel("Select Boss:")); kmCard.add(kmBossCombo);
        kmCard.add(new JLabel("Kilometers:")); kmCard.add(kmField);
        cards.add(kmCard, LogEntry.EntryType.KILOMETER.name());

        // --- EXTRA CARD ---
        JPanel extraCard = new JPanel(new GridLayout(0, 2, 5, 5));
        JComboBox<Boss> extraBossCombo = new JComboBox<>();
        for (Boss b : bosses) extraBossCombo.addItem(b);
        extraBossCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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
        extraCard.add(new JLabel("Select Boss:")); extraCard.add(extraBossCombo);
        extraCard.add(new JLabel("Description:")); extraCard.add(descField);
        extraCard.add(new JLabel("Units:")); extraCard.add(unitsField);
        extraCard.add(new JLabel("Cost Per Unit:")); extraCard.add(costField);
        cards.add(extraCard, LogEntry.EntryType.EXTRA.name());

        mainPanel.add(cards, BorderLayout.CENTER);

        typeCombo.addActionListener(e -> cardLayout.show(cards, ((LogEntry.EntryType) typeCombo.getSelectedItem()).name()));
        cardLayout.show(cards, entry.getType().name());

        int result = JOptionPane.showConfirmDialog(this, mainPanel, "Log Entry", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            LogEntry.EntryType selectedType = (LogEntry.EntryType) typeCombo.getSelectedItem();
            entry.setType(selectedType);

            if (selectedType == LogEntry.EntryType.TIME) {
                entry.setStartTime(startField.getText());
                entry.setEndTime(endField.getText());
                Map<String, Double> percs = new HashMap<>();
                for (Boss b : bosses) {
                    try {
                        percs.put(b.getId(), Double.parseDouble(percFields.get(b.getId()).getText()));
                    } catch (NumberFormatException ex) { percs.put(b.getId(), 0.0); }
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
                } catch (NumberFormatException ex) { entry.setKilometers(0.0); }
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
