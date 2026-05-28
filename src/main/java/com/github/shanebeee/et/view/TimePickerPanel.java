package com.github.shanebeee.et.view;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimePickerPanel extends JPanel {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private int hour;
    private int minute;
    private boolean isAm;
    private boolean selectingHour = true;
    private Runnable onSelect;

    public TimePickerPanel(String initialTime, Runnable onSelect) {
        this.onSelect = onSelect;
        try {
            LocalTime time = LocalTime.parse(initialTime, formatter);
            int h = time.getHour();
            this.hour = h % 12;
            if (this.hour == 0) this.hour = 12;
            this.minute = time.getMinute();
            this.isAm = h < 12;
        } catch (Exception e) {
            this.hour = 11;
            this.minute = 0;
            this.isAm = true;
        }

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 400));

        JPanel header = new JPanel(new BorderLayout());
        JLabel lblTime = new JLabel(getDisplayTime(), SwingConstants.CENTER);
        lblTime.setFont(lblTime.getFont().deriveFont(24f));
        header.add(lblTime, BorderLayout.CENTER);

        JPanel amPmPanel = new JPanel(new GridLayout(2, 1));
        JToggleButton btnAm = new JToggleButton("AM", isAm);
        JToggleButton btnPm = new JToggleButton("PM", !isAm);
        ButtonGroup amPmGroup = new ButtonGroup();
        amPmGroup.add(btnAm);
        amPmGroup.add(btnPm);

        btnAm.addActionListener(e -> {
            isAm = true;
            repaint();
            lblTime.setText(getDisplayTime());
        });
        btnPm.addActionListener(e -> {
            isAm = false;
            repaint();
            lblTime.setText(getDisplayTime());
        });

        amPmPanel.add(btnAm);
        amPmPanel.add(btnPm);
        header.add(amPmPanel, BorderLayout.EAST);

        JPanel clockFace = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                lblTime.setText(getDisplayTime());

                int size = Math.min(getWidth(), getHeight()) - 40;
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = size / 2;

                // Draw circle
                g2.setColor(getBackground().darker());
                g2.fillOval(centerX - radius, centerY - radius, size, size);

                // Draw numbers
                g2.setColor(getForeground());
                for (int i = 0; i < 12; i++) {
                    int val = selectingHour ? (i == 0 ? 12 : i) : i * 5;
                    double angle = Math.toRadians(i * 30 - 90);
                    int x = (int) (centerX + (radius - 20) * Math.cos(angle));
                    int y = (int) (centerY + (radius - 20) * Math.sin(angle));

                    String text = String.valueOf(val);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(text, x - fm.stringWidth(text) / 2, y + fm.getAscent() / 2 - 2);
                }

                // Draw hand
                int currentVal = selectingHour ? hour % 12 : minute;
                double handAngle = Math.toRadians((selectingHour ? currentVal * 30 : currentVal * 6) - 90);
                int handX = (int) (centerX + (radius - 40) * Math.cos(handAngle));
                int handY = (int) (centerY + (radius - 40) * Math.sin(handAngle));

                g2.setColor(UIManager.getColor("Component.accentColor") != null ? UIManager.getColor("Component.accentColor") : Color.BLUE);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(centerX, centerY, handX, handY);
                g2.fillOval(centerX - 4, centerY - 4, 8, 8);
                g2.fillOval(handX - 10, handY - 10, 20, 20);

                g2.setColor(Color.WHITE);
                String valText = String.valueOf(selectingHour ? (hour == 0 ? 12 : hour) : minute);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(valText, handX - fm.stringWidth(valText) / 2, handY + fm.getAscent() / 2 - 2);
            }
        };

        clockFace.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateTimeFromMouse(e.getX(), e.getY(), clockFace);
            }
        });

        clockFace.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateTimeFromMouse(e.getX(), e.getY(), clockFace);
            }
        });

        JButton btnToggle = new JButton("Minutes");
        btnToggle.addActionListener(e -> {
            selectingHour = !selectingHour;
            btnToggle.setText(selectingHour ? "Minutes" : "Hours");
            clockFace.repaint();
        });

        add(header, BorderLayout.NORTH);
        add(clockFace, BorderLayout.CENTER);
        add(btnToggle, BorderLayout.SOUTH);
    }

    public static String formatTime(String time24) {
        try {
            return LocalTime.parse(time24, DateTimeFormatter.ofPattern("HH:mm")).format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return time24;
        }
    }

    public static void showPicker(Component parent, JTextField targetField) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Select Time", true);
        TimePickerPanel picker = new TimePickerPanel(targetField.getText(), () -> {
        });

        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblPreview = new JLabel("Selected: " + picker.getDisplayTime());
        previewPanel.add(lblPreview);
        picker.onSelect = () -> lblPreview.setText("Selected: " + picker.getDisplayTime());

        JButton btnOk = new JButton("OK");
        btnOk.addActionListener(e -> {
            targetField.setText(picker.getTime());
            dialog.dispose();
        });

        dialog.add(picker, BorderLayout.CENTER);
        dialog.add(previewPanel, BorderLayout.NORTH);
        dialog.add(btnOk, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void updateTimeFromMouse(int x, int y, JPanel clockFace) {
        int centerX = clockFace.getWidth() / 2;
        int centerY = clockFace.getHeight() / 2;
        double angle = Math.toDegrees(Math.atan2(y - centerY, x - centerX)) + 90;
        if (angle < 0) angle += 360;

        if (selectingHour) {
            hour = (int) Math.round(angle / 30) % 12;
            if (hour == 0) hour = 12;
        } else {
            minute = (int) Math.round(angle / 6) % 60;
        }
        repaint();
        if (onSelect != null) onSelect.run();
    }

    public String getDisplayTime() {
        return LocalTime.of(hour % 12 == 0 ? (isAm ? 0 : 12) : (isAm ? hour : hour + 12), minute).format(displayFormatter);
    }

    public String getTime() {
        int h24 = hour % 12;
        if (!isAm) h24 += 12;
        return String.format("%02d:%02d", h24, minute);
    }

}
