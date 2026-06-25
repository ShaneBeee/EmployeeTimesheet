package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.KmOdometer;
import com.github.shanebeee.et.model.KmTrip;
import com.github.shanebeee.et.model.LogEntry;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KmLogPanel extends JPanel {

    private static final Color ACCENT = new Color(16, 185, 129);

    private final DataStorage storage;
    private int currentYear;

    private JLabel yearLabel;
    private JPanel listPanel;
    private List<KmTrip> currentTrips;
    private KmOdometer odometer;

    private JLabel startOdoLabel;
    private JLabel endOdoLabel;
    private JLabel totalKmLabel;
    private JLabel businessKmLabel;
    private JLabel businessPctLabel;

    public KmLogPanel(DataStorage storage) {
        this.storage = storage;
        this.currentYear = LocalDate.now().getYear();
        setLayout(new BorderLayout());
        initUI();
        loadYear();
    }

    private void initUI() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Kilometre Log");
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

        JButton btnAdd = new JButton("+ Log Trip");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground(ACCENT);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 13f));
        btnAdd.addActionListener(e -> showTripDialog(null));

        JButton btnImport = new JButton("↩ Import from Work Logs");
        btnImport.putClientProperty("JButton.buttonType", "roundRect");
        btnImport.setFont(btnImport.getFont().deriveFont(Font.PLAIN, 12f));
        btnImport.addActionListener(e -> runBackfill());

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        headerRight.add(btnImport);
        headerRight.add(yearNav);
        headerRight.add(btnAdd);
        header.add(headerRight, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        body.add(scroll, BorderLayout.CENTER);

        JPanel sidebar = buildSidebar();
        sidebar.setPreferredSize(new Dimension(220, 0));
        body.add(sidebar, BorderLayout.EAST);

        add(body, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setOpaque(false);

        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel odoTitle = new JLabel("ODOMETER");
        odoTitle.setFont(odoTitle.getFont().deriveFont(Font.BOLD, 10f));
        odoTitle.setForeground(new Color(148, 163, 184));
        odoTitle.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        odoTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(odoTitle);
        card.add(makeOdoRow("Start of year", true));
        card.add(Box.createVerticalStrut(4));
        card.add(makeOdoRow("End of year", false));
        card.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(226, 232, 240));
        sep.setAlignmentX(LEFT_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(12));

        JLabel summaryTitle = new JLabel("YEAR SUMMARY");
        summaryTitle.setFont(summaryTitle.getFont().deriveFont(Font.BOLD, 10f));
        summaryTitle.setForeground(new Color(148, 163, 184));
        summaryTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        summaryTitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(summaryTitle);

        businessKmLabel = new JLabel("0 km");
        businessKmLabel.setFont(businessKmLabel.getFont().deriveFont(Font.BOLD, 22f));
        businessKmLabel.setForeground(new Color(30, 41, 59));
        businessKmLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(businessKmLabel);

        JLabel businessSubLabel = new JLabel("business kilometres");
        businessSubLabel.setFont(businessSubLabel.getFont().deriveFont(Font.PLAIN, 11f));
        businessSubLabel.setForeground(new Color(148, 163, 184));
        businessSubLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(businessSubLabel);
        card.add(Box.createVerticalStrut(12));

        card.add(makeSummaryRow("Total KM (year)", true));
        card.add(Box.createVerticalStrut(4));
        card.add(makeSummaryRow("Business use %", false));

        sidebar.add(card, BorderLayout.NORTH);
        return sidebar;
    }

    private JPanel makeOdoRow(String label, boolean isStart) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel valLabel = new JLabel("—");
        valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 11f));
        valLabel.setForeground(new Color(30, 41, 59));
        valLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        valLabel.setToolTipText("Click to edit");

        if (isStart) startOdoLabel = valLabel;
        else         endOdoLabel   = valLabel;

        valLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                double current = isStart ? odometer.getStartKm() : odometer.getEndKm();
                String input = JOptionPane.showInputDialog(
                    KmLogPanel.this,
                    label + " odometer (km):",
                    current > 0 ? String.valueOf((long) current) : "");
                if (input == null) return;
                try {
                    double val = Double.parseDouble(input.trim());
                    if (isStart) odometer.setStartKm(val);
                    else         odometer.setEndKm(val);
                    storage.saveKmOdometer(String.valueOf(currentYear), odometer);
                    refreshSidebar();
                } catch (NumberFormatException ignored) {
                    JOptionPane.showMessageDialog(KmLogPanel.this,
                        "Please enter a valid number.", "Invalid", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        row.add(lbl,      BorderLayout.WEST);
        row.add(valLabel, BorderLayout.EAST);
        return row;
    }

    private JPanel makeSummaryRow(String label, boolean isTotal) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel val = new JLabel("—");
        val.setFont(val.getFont().deriveFont(Font.PLAIN, 11f));
        val.setForeground(new Color(30, 41, 59));

        if (isTotal) totalKmLabel     = val;
        else         businessPctLabel = val;

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private void loadYear() {
        yearLabel.setText(String.valueOf(currentYear));
        currentTrips = storage.loadKmTrips(String.valueOf(currentYear));
        odometer     = storage.loadKmOdometer(String.valueOf(currentYear));
        refresh();
    }

    private void refresh() {
        listPanel.removeAll();

        if (currentTrips.isEmpty()) {
            JLabel empty = new JLabel("No trips yet — click + Log Trip to get started", JLabel.CENTER);
            empty.setForeground(new Color(148, 163, 184));
            empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
            empty.setAlignmentX(CENTER_ALIGNMENT);
            listPanel.add(Box.createVerticalStrut(40));
            listPanel.add(empty);
        } else {
            Map<String, List<KmTrip>> byMonth = new LinkedHashMap<>();
            List<KmTrip> sorted = new ArrayList<>(currentTrips);
            sorted.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            for (KmTrip t : sorted) {
                String key = t.getDate().substring(0, 7);
                byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }
            byMonth.forEach((monthKey, trips) -> {
                YearMonth ym = YearMonth.parse(monthKey);
                String monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
                double monthKm = trips.stream().mapToDouble(KmTrip::getKm).sum();

                JPanel mHeader = new JPanel(new BorderLayout());
                mHeader.setOpaque(false);
                mHeader.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
                mHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                JLabel mLabel = new JLabel(monthLabel.toUpperCase());
                mLabel.setFont(mLabel.getFont().deriveFont(Font.BOLD, 10f));
                mLabel.setForeground(new Color(148, 163, 184));
                JLabel mTotal = new JLabel(String.format("%.1f km", monthKm));
                mTotal.setFont(mTotal.getFont().deriveFont(Font.BOLD, 10f));
                mTotal.setForeground(ACCENT);
                mHeader.add(mLabel, BorderLayout.WEST);
                mHeader.add(mTotal, BorderLayout.EAST);
                listPanel.add(mHeader);

                for (KmTrip trip : trips) {
                    listPanel.add(makeTripCard(trip));
                    listPanel.add(Box.createVerticalStrut(6));
                }
                listPanel.add(Box.createVerticalStrut(4));
            });
        }

        listPanel.revalidate();
        listPanel.repaint();
        refreshSidebar();
    }

    private void refreshSidebar() {
        startOdoLabel.setText(odometer.getStartKm() > 0
            ? String.format("%,.0f km", odometer.getStartKm()) : "tap to set");
        endOdoLabel.setText(odometer.getEndKm() > 0
            ? String.format("%,.0f km", odometer.getEndKm()) : "tap to set");

        double businessKm = currentTrips.stream().mapToDouble(KmTrip::getKm).sum();
        businessKmLabel.setText(String.format("%.1f km", businessKm));

        double totalKm = odometer.totalKm();
        totalKmLabel.setText(totalKm > 0 ? String.format("%,.0f km", totalKm) : "—");

        if (totalKm > 0 && businessKm > 0) {
            double pct = Math.min(100.0, businessKm / totalKm * 100.0);
            businessPctLabel.setText(String.format("%.1f%%", pct));
            businessPctLabel.setForeground(ACCENT);
        } else {
            businessPctLabel.setText("—");
            businessPctLabel.setForeground(new Color(30, 41, 59));
        }
    }

    private JPanel makeTripCard(KmTrip trip) {
        final boolean[] hovered = {false};
        boolean isAuto = trip.getSourceLogId() != null;

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
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        // Auto-logged trips are not directly editable (edit the work log entry instead)
        if (!isAuto) card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Icon
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 185, 129, 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(ACCENT);
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                var fm = g2.getFontMetrics();
                String ltr = isAuto ? "A" : "K";
                g2.drawString(ltr, (getWidth() - fm.stringWidth(ltr)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(36, 36));

        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        String title = (trip.getNote() != null && !trip.getNote().isBlank())
            ? trip.getNote() : "Business trip";
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 13f));
        titleLbl.setForeground(new Color(30, 41, 59));
        String sub = trip.getDate() + (isAuto ? "  ·  auto-logged" : "");
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(100, 116, 139));
        text.add(titleLbl, BorderLayout.NORTH);
        text.add(subLbl,   BorderLayout.SOUTH);

        JLabel kmLbl = new JLabel(String.format("%.1f km", trip.getKm()), JLabel.RIGHT);
        kmLbl.setFont(kmLbl.getFont().deriveFont(Font.BOLD, 13f));
        kmLbl.setForeground(new Color(30, 41, 59));

        card.add(icon,  BorderLayout.WEST);
        card.add(text,  BorderLayout.CENTER);
        card.add(kmLbl, BorderLayout.EAST);

        if (!isAuto) {
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
                public void mouseExited(MouseEvent e)  { hovered[0] = false; card.repaint(); }
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) showTripDialog(trip);
                }
            });
        } else {
            card.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
                public void mouseExited(MouseEvent e)  { hovered[0] = false; card.repaint(); }
            });
        }

        return card;
    }

    private void showTripDialog(KmTrip existing) {
        boolean isNew = existing == null;
        KmTrip trip = isNew ? new KmTrip() : existing;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Log Trip" : "Edit Trip", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 380);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel headerTitle = new JLabel(isNew ? "New Trip" : "Edit Trip");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 16f));
        headerTitle.setForeground(new Color(30, 41, 59));
        header.add(headerTitle, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);

        JButton dateBtn = new JButton(trip.getDate() != null ? trip.getDate() : LocalDate.now().toString());
        dateBtn.putClientProperty("JButton.buttonType", "roundRect");
        dateBtn.setBackground(new Color(241, 245, 249));
        dateBtn.setForeground(new Color(30, 41, 59));
        dateBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dateBtn.addActionListener(e -> DatePicker.showPicker(dialog, dateBtn));

        JTextField kmField = new JTextField(trip.getKm() > 0 ? String.format("%.1f", trip.getKm()) : "");
        kmField.putClientProperty("JTextField.placeholderText", "e.g. 12.5");

        JTextField noteField = new JTextField(trip.getNote() != null ? trip.getNote() : "");
        noteField.putClientProperty("JTextField.placeholderText", "e.g. showed a house on Main St");

        form.add(makeFormLabel("Date"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(dateBtn, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Distance (km)"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(kmField, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0);
        form.add(makeFormLabel("Note / Purpose"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(noteField, gbc);

        dialog.add(new JScrollPane(form) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, BorderLayout.CENTER);

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
                currentTrips.remove(trip);
                storage.saveKmTrips(String.valueOf(currentYear), currentTrips);
                refresh();
                dialog.dispose();
            });
            footerLeft.add(btnDelete);
        }

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton(isNew ? "Log Trip" : "Save Changes");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(ACCENT);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                double km = Double.parseDouble(kmField.getText().trim());
                if (km <= 0) throw new NumberFormatException();
                trip.setDate(dateBtn.getText());
                trip.setKm(km);
                trip.setNote(noteField.getText().trim());
                if (isNew) currentTrips.add(trip);
                currentTrips.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                storage.saveKmTrips(String.valueOf(currentYear), currentTrips);
                refresh();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Please enter a valid distance (e.g. 12.5).",
                    "Invalid Distance", JOptionPane.WARNING_MESSAGE);
            }
        });

        footerRight.add(btnCancel);
        footerRight.add(btnSave);
        footer.add(footerLeft,  BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(btnSave);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }
        });

        dialog.setLocationRelativeTo(this);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (isNew) kmField.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
    }

    /**
     * Scans all work log months from January of the current year up to today,
     * finds every KILOMETER entry for any boss named "Karen" (case-insensitive),
     * and upserts them as auto-linked KmTrips. Safe to run multiple times.
     */
    private void runBackfill() {
        int year = LocalDate.now().getYear();
        LocalDate today = LocalDate.now();
        int importedCount = 0;
        int skippedCount  = 0;

        for (Month month : Month.values()) {
            YearMonth ym = YearMonth.of(year, month);
            // Only scan January through current month
            if (ym.isAfter(YearMonth.from(today))) break;

            String yearMonth = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            java.util.List<LogEntry> logs = storage.loadLogs(yearMonth);

            for (LogEntry entry : logs) {
                if (entry.getType() != LogEntry.EntryType.KILOMETER) continue;
                if (entry.getKilometers() == null || entry.getKilometers() <= 0) continue;

                // Resolve boss name
                String bossName = "";
                if (entry.getBossUuid() != null) {
                    bossName = storage.loadBosses().stream()
                        .filter(b -> b.getId().equals(entry.getBossUuid())
                                  || b.getName().equals(entry.getBossUuid()))
                        .map(com.github.shanebeee.et.model.Boss::getName)
                        .findFirst().orElse("");
                }

                // Only import Karen's entries
                if (!bossName.toLowerCase().contains("karen")) continue;

                // Check if already imported (by sourceLogId)
                java.util.List<KmTrip> existing = storage.loadKmTrips(String.valueOf(year));
                boolean alreadyExists = existing.stream()
                    .anyMatch(t -> entry.getId().equals(t.getSourceLogId()));

                if (alreadyExists) {
                    skippedCount++;
                    continue;
                }

                KmTrip trip = new KmTrip();
                trip.setSourceLogId(entry.getId());
                trip.setDate(entry.getDate());
                trip.setKm(entry.getKilometers());
                trip.setNote("Work trip for " + bossName);
                storage.upsertAutoKmTrip(String.valueOf(year), trip);
                importedCount++;
            }
        }

        // Reload and show result
        loadYear();
        String msg = importedCount > 0
            ? importedCount + " trip" + (importedCount != 1 ? "s" : "") + " imported from Work Logs."
            + (skippedCount > 0 ? "\n" + skippedCount + " already existed and were skipped." : "")
            : "No new trips found to import."
            + (skippedCount > 0 ? "\n" + skippedCount + " trip" + (skippedCount != 1 ? "s" : "") + " were already imported." : "");
        JOptionPane.showMessageDialog(this, msg,
            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
    }

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
        card.setBorder(BorderFactory.createEmptyBorder(4, 14, 16, 14));
        return card;
    }
}
