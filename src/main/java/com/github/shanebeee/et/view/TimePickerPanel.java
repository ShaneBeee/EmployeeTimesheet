package com.github.shanebeee.et.view;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimePickerPanel extends JPanel {

    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private int hour;
    private int minute;
    private boolean isAm;
    private JSpinner spinnerHour;
    private JSpinner spinnerMinute;
    private JToggleButton btnAm;
    private JToggleButton btnPm;
    private JLabel lblPreview;
    private Runnable onSelect;

    public TimePickerPanel(String initialTime, Runnable onSelect) {
        this.onSelect = onSelect;
        try {
            LocalTime time = LocalTime.parse(initialTime, displayFormatter);
            int h = time.getHour();
            this.hour = h % 12;
            if (this.hour == 0) this.hour = 12;
            this.minute = (int) Math.round(time.getMinute() / 5.0) * 5 % 60; // snap to nearest 5
            this.isAm = h < 12;
        } catch (Exception e) {
            this.hour = 11;
            this.minute = 0;
            this.isAm = true;
        }

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setPreferredSize(new Dimension(350, 180));

        // ===== PREVIEW SECTION =====
        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewPanel.setOpaque(false);
        lblPreview = new JLabel(getDisplayTime());
        lblPreview.setFont(lblPreview.getFont().deriveFont(28f).deriveFont(java.awt.Font.BOLD));
        lblPreview.setForeground(new Color(30, 41, 59));
        previewPanel.add(lblPreview);
        add(previewPanel, BorderLayout.NORTH);

        // ===== TIME INPUT SECTION =====
        JPanel timeInputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        timeInputPanel.setOpaque(false);

        // Hour Spinner
        SpinnerNumberModel hourModel = new SpinnerNumberModel(hour, 1, 12, 1);
        spinnerHour = new JSpinner(hourModel);
        spinnerHour.setPreferredSize(new Dimension(70, 36));
        configureSpinner(spinnerHour, false);

        // Separator
        JLabel colonLabel = new JLabel(":");
        colonLabel.setFont(colonLabel.getFont().deriveFont(18f));

        // Minute Spinner
        SpinnerNumberModel minuteModel = new SpinnerNumberModel(minute, 0, 59, 5);
        spinnerMinute = new JSpinner(minuteModel);
        spinnerMinute.setPreferredSize(new Dimension(70, 36));
        configureSpinner(spinnerMinute, true);

        // AM/PM Panel
        JPanel amPmPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        btnAm = new JToggleButton("AM", isAm);
        btnPm = new JToggleButton("PM", !isAm);
        ButtonGroup amPmGroup = new ButtonGroup();
        amPmGroup.add(btnAm);
        amPmGroup.add(btnPm);

        btnAm.setPreferredSize(new Dimension(50, 36));
        btnPm.setPreferredSize(new Dimension(50, 36));
        styleToggleButton(btnAm, true);
        styleToggleButton(btnPm, false);

        btnAm.addActionListener(e -> {
            isAm = true;
            styleToggleButton(btnAm, true);
            styleToggleButton(btnPm, false);
            updatePreview();
        });
        btnPm.addActionListener(e -> {
            isAm = false;
            styleToggleButton(btnAm, false);
            styleToggleButton(btnPm, true);
            updatePreview();
        });

        amPmPanel.add(btnAm);
        amPmPanel.add(btnPm);

        // Assemble time input
        timeInputPanel.add(spinnerHour);
        timeInputPanel.add(colonLabel);
        timeInputPanel.add(spinnerMinute);
        timeInputPanel.add(amPmPanel);

        add(timeInputPanel, BorderLayout.CENTER);

        // ===== ADD CHANGE LISTENERS =====
        spinnerHour.addChangeListener(e -> {
            hour = (Integer) spinnerHour.getValue();
            updatePreview();
            if (onSelect != null) onSelect.run();
        });

        spinnerMinute.addChangeListener(e -> {
            minute = (Integer) spinnerMinute.getValue();
            updatePreview();
            if (onSelect != null) onSelect.run();
        });
    }

    private void configureSpinner(JSpinner spinner, boolean pad) {
        spinner.setFont(spinner.getFont().deriveFont(14f));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
            JFormattedTextField textField = defaultEditor.getTextField();
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setFont(textField.getFont().deriveFont(14f));
            textField.setEditable(true);
            textField.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            textField.setForeground(new Color(30, 41, 59));
        }
        if (pad) {
            // Replace spinner editor with a plain label that we fully control
            JLabel minuteLabel = new JLabel(String.format("%02d", (Integer) spinner.getValue()));
            minuteLabel.setHorizontalAlignment(JLabel.CENTER);
            minuteLabel.setFont(spinner.getFont().deriveFont(14f));
            minuteLabel.setForeground(new Color(30, 41, 59));
            minuteLabel.setOpaque(true);
            minuteLabel.setBackground(UIManager.getColor("TextField.background"));
            minuteLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            spinner.setEditor(minuteLabel);
            spinner.getModel().addChangeListener(e ->
                minuteLabel.setText(String.format("%02d", (Integer) spinner.getValue())));
            spinner.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        }
    }

    private void updatePreview() {
        lblPreview.setText(getDisplayTime());
    }

    private void styleToggleButton(JToggleButton button, boolean isSelected) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setFont(button.getFont().deriveFont(12f));
        if (isSelected) {
            button.setBackground(new Color(59, 130, 246));
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(241, 245, 249));
            button.setForeground(new Color(100, 116, 139));
        }
    }

    public static String formatTime(String time24) {
        try {
            return LocalTime.parse(time24, DateTimeFormatter.ofPattern("HH:mm"))
                .format(DateTimeFormatter.ofPattern("hh:mm a"));
        } catch (Exception e) {
            return time24;
        }
    }

    public static String unformatTime(String time24) {
        try {
            return LocalTime.parse(time24, DateTimeFormatter.ofPattern("hh:mm a"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return time24;
        }
    }

    public static void showPicker(Component parent, JTextField targetField) {
        showPickerInternal(parent, targetField.getText(), targetField::setText);
    }

    public static void showPicker(Component parent, JButton targetButton) {
        showPickerInternal(parent, targetButton.getText(), targetButton::setText);
    }

    private static void showPickerInternal(Component parent, String currentTime, java.util.function.Consumer<String> onConfirm) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Select Time", true);
        TimePickerPanel picker = new TimePickerPanel(currentTime, () -> {
        });

        // ── Footer matching showEntryDialog style ────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnOk = new JButton("Select Time");
        btnOk.putClientProperty("JButton.buttonType", "roundRect");
        btnOk.setBackground(new Color(59, 130, 246));
        btnOk.setForeground(Color.WHITE);
        btnOk.addActionListener(e -> {
            onConfirm.accept(formatTime(picker.getTime()));
            dialog.dispose();
        });

        footer.add(btnCancel);
        footer.add(btnOk);

        dialog.add(picker, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        // Keyboard shortcuts: Enter = confirm, Escape = cancel
        dialog.getRootPane().setDefaultButton(btnOk);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }
        });

        dialog.setVisible(true);
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
