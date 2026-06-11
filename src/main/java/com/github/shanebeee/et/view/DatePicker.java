package com.github.shanebeee.et.view;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DatePicker extends JDialog {

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
        setSize(350, 400);
        setResizable(false);

        initUI();
        refreshCalendar();
        setLocationRelativeTo(parent);
    }

    public static void showPicker(Component parent, JTextField targetField) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        LocalDate initialDate;
        try {
            initialDate = LocalDate.parse(targetField.getText());
        } catch (Exception e) {
            initialDate = LocalDate.now();
        }
        DatePicker picker = new DatePicker((Frame) window, initialDate);
        picker.setVisible(true);
        LocalDate result = picker.getSelectedDate();
        if (result != null) targetField.setText(result.toString());
    }

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

    private void initUI() {
        JPanel navPanel = new JPanel(new BorderLayout());
        JButton btnPrev = new JButton("<");
        JButton btnNext = new JButton(">");
        monthLabel = new JLabel("", JLabel.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 14f));

        btnPrev.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });
        btnNext.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        navPanel.add(btnPrev, BorderLayout.WEST);
        navPanel.add(monthLabel, BorderLayout.CENTER);
        navPanel.add(btnNext, BorderLayout.EAST);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(navPanel, BorderLayout.NORTH);

        calendarGrid = new JPanel(new GridLayout(0, 7, 2, 2));
        calendarGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(calendarGrid, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        bottomPanel.add(btnCancel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void refreshCalendar() {
        monthLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        calendarGrid.removeAll();

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            JLabel lbl = new JLabel(day, JLabel.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
            calendarGrid.add(lbl);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();

        for (int i = 1; i < dayOfWeek; i++) {
            calendarGrid.add(new JLabel(""));
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            JButton dayBtn = new JButton(String.valueOf(day));
            dayBtn.setMargin(new Insets(2, 2, 2, 2));

            if (date.equals(selectedDate)) {
                dayBtn.setBackground(UIManager.getColor("Component.accentColor"));
                dayBtn.setForeground(Color.WHITE);
            }

            if (date.equals(LocalDate.now())) {
                dayBtn.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.accentColor"), 1));
            }

            dayBtn.addActionListener(e -> {
                selectedDate = date;
                confirmed = true;
                dispose();
            });
            calendarGrid.add(dayBtn);
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    public LocalDate getSelectedDate() {
        return confirmed ? selectedDate : null;
    }

}
