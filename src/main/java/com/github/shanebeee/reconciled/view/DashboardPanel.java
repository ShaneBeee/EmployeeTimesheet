package com.github.shanebeee.reconciled.view;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.EmployeeInfo;
import com.github.shanebeee.reconciled.model.Expenditure;
import com.github.shanebeee.reconciled.model.Invoice;
import com.github.shanebeee.reconciled.model.KmTrip;
import com.github.shanebeee.reconciled.model.LogEntry;
import com.github.shanebeee.reconciled.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DashboardPanel extends JPanel {

    private static final Color BLUE   = new Color(59,  130, 246);
    private static final Color GREEN  = new Color(16,  185, 129);
    private static final Color AMBER  = new Color(245, 158, 11);
    private static final Color PURPLE = new Color(139, 92,  246);
    private static final Color NAVY   = new Color(30,  41,  59);
    private static final Color RED    = new Color(239, 68,  68);
    private static final Color SLATE  = new Color(100, 116, 139);

    private final DataStorage storage;
    private MainFrame mainFrame;

    public DashboardPanel(DataStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
    }

    public void setMainFrame(MainFrame frame) {
        this.mainFrame = frame;
    }

    public void refresh() {
        removeAll();

        LocalDate today     = LocalDate.now();
        int       year      = today.getYear();
        YearMonth thisMonth = YearMonth.from(today);
        String    monthKey  = thisMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        EmployeeInfo info        = storage.loadEmployeeInfo();
        List<Boss>   bosses      = storage.loadBosses();
        List<Boss>   selfEmployed = bosses.stream().filter(Boss::isSelfEmployed).toList();

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));
        String name = info.getFullName() != null && !info.getFullName().isBlank()
            ? info.getFullName().split(" ")[0] : "there";
        JLabel titleLbl = new JLabel(greeting() + ", " + name + " \uD83D\uDC4B");
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 22f));
        titleLbl.setForeground(NAVY);
        JLabel dateLbl = new JLabel(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateLbl.setFont(dateLbl.getFont().deriveFont(Font.PLAIN, 13f));
        dateLbl.setForeground(SLATE);
        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(titleLbl);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(dateLbl);
        header.add(headerText, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 16, 0);

        // This Month
        List<LogEntry> monthLogs = storage.loadLogs(monthKey);
        double monthHours = 0, monthKmsBilled = 0, monthIncome = 0;
        for (Boss boss : selfEmployed) {
            for (LogEntry entry : monthLogs) {
                if (entry.getType() == LogEntry.EntryType.TIME
                    && entry.getStartTime() != null && entry.getEndTime() != null) {
                    try {
                        long mins = java.time.Duration.between(
                            LocalTime.parse(entry.getStartTime()),
                            LocalTime.parse(entry.getEndTime())).toMinutes();
                        double hrs = mins / 60.0;
                        if (entry.getBossPercentages() != null
                            && entry.getBossPercentages().containsKey(boss.getId())) {
                            double perc = entry.getBossPercentages().get(boss.getId()) / 100.0;
                            monthIncome += hrs * boss.getHourlyRate() * perc;
                            monthHours  += hrs * perc;
                        } else if (boss.getId().equals(entry.getBossUuid())) {
                            monthIncome += hrs * boss.getHourlyRate();
                            monthHours  += hrs;
                        }
                    } catch (Exception ignored) {}
                } else if (entry.getType() == LogEntry.EntryType.KILOMETER
                    && boss.getId().equals(entry.getBossUuid())
                    && entry.getKilometers() != null) {
                    monthKmsBilled += entry.getKilometers();
                    if (boss.getKmRate() != null) monthIncome += entry.getKilometers() * boss.getKmRate();
                } else if (entry.getType() == LogEntry.EntryType.EXTRA
                    && boss.getId().equals(entry.getBossUuid())
                    && entry.getUnits() != null && entry.getCostPerUnit() != null) {
                    monthIncome += entry.getUnits() * entry.getCostPerUnit();
                }
            }
        }
        List<Expenditure> monthExpenses = storage.loadExpenditures(String.valueOf(year))
            .stream().filter(e -> e.getDate().startsWith(monthKey)).toList();
        double monthExpTotal = monthExpenses.stream().mapToDouble(Expenditure::getTotal).sum();
        List<KmTrip> monthTrips = storage.loadKmTrips(String.valueOf(year))
            .stream().filter(t -> t.getDate().startsWith(monthKey)).toList();
        double monthKmTrips = monthTrips.stream().mapToDouble(KmTrip::getKm).sum();

        body.add(makeSectionLabel("THIS MONTH — " +
            thisMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).toUpperCase()), gc);
        gc.gridy++;

        JPanel statsRow = new JPanel(new GridBagLayout());
        statsRow.setOpaque(false);
        GridBagConstraints sg = new GridBagConstraints();
        sg.fill = GridBagConstraints.BOTH; sg.weighty = 1.0;
        sg.insets = new Insets(0, 0, 0, 12);
        sg.weightx = 1.0; sg.gridx = 0;
        statsRow.add(makeStatCard("Gross Income",  String.format("$%.2f", monthIncome),  "this month",       GREEN,  "Work Logs",  "LOGS"),       sg);
        sg.gridx = 1;
        statsRow.add(makeStatCard("Hours Logged",  String.format("%.1f hrs", monthHours), "this month",      BLUE,   "Work Logs",  "LOGS"),       sg);
        sg.gridx = 2;
        statsRow.add(makeStatCard("Expenses",      String.format("$%.2f", monthExpTotal), "this month",      AMBER,  "Expenses",   "EXPENSES"),   sg);
        sg.gridx = 3;
        statsRow.add(makeStatCard("KMs Driven",    String.format("%.1f km", monthKmTrips),"this month",      GREEN,  "KM Log",     "KM"),         sg);
        sg.gridx = 4; sg.insets = new Insets(0, 0, 0, 0);
        statsRow.add(makeStatCard("KMs Billed",    String.format("%.1f km", monthKmsBilled),"billed to clients", PURPLE, "Work Logs", "LOGS"),    sg);
        body.add(statsRow, gc);
        gc.gridy++;
        gc.insets = new Insets(0, 0, 16, 0);

        // Year to Date
        double ytdIncome = 0;
        for (Boss boss : selfEmployed) {
            for (int m = 1; m <= today.getMonthValue(); m++) {
                String mk = String.format("%d-%02d", year, m);
                for (LogEntry entry : storage.loadLogs(mk)) {
                    if (entry.getType() == LogEntry.EntryType.TIME
                        && entry.getStartTime() != null && entry.getEndTime() != null) {
                        try {
                            long mins = java.time.Duration.between(
                                LocalTime.parse(entry.getStartTime()),
                                LocalTime.parse(entry.getEndTime())).toMinutes();
                            double hrs = mins / 60.0;
                            if (entry.getBossPercentages() != null
                                && entry.getBossPercentages().containsKey(boss.getId())) {
                                ytdIncome += hrs * boss.getHourlyRate()
                                    * entry.getBossPercentages().get(boss.getId()) / 100.0;
                            } else if (boss.getId().equals(entry.getBossUuid())) {
                                ytdIncome += hrs * boss.getHourlyRate();
                            }
                        } catch (Exception ignored) {}
                    } else if (entry.getType() == LogEntry.EntryType.KILOMETER
                        && boss.getId().equals(entry.getBossUuid())
                        && entry.getKilometers() != null && boss.getKmRate() != null) {
                        ytdIncome += entry.getKilometers() * boss.getKmRate();
                    } else if (entry.getType() == LogEntry.EntryType.EXTRA
                        && boss.getId().equals(entry.getBossUuid())
                        && entry.getUnits() != null && entry.getCostPerUnit() != null) {
                        ytdIncome += entry.getUnits() * entry.getCostPerUnit();
                    }
                }
            }
        }
        double ytdExpTotal = storage.loadExpenditures(String.valueOf(year))
            .stream().mapToDouble(Expenditure::getTotal).sum();
        double ytdNet = ytdIncome - ytdExpTotal;

        body.add(makeSectionLabel("YEAR TO DATE — " + year), gc);
        gc.gridy++;

        JPanel ytdRow = new JPanel(new GridBagLayout());
        ytdRow.setOpaque(false);
        GridBagConstraints yg = new GridBagConstraints();
        yg.fill = GridBagConstraints.BOTH; yg.weighty = 1.0;
        yg.insets = new Insets(0, 0, 0, 12);
        yg.weightx = 1.0; yg.gridx = 0;
        ytdRow.add(makeStatCard("Gross Income",   String.format("$%.2f", ytdIncome),  "self-employed only",                   BLUE,  "Accounting", "ACCOUNTING"), yg);
        yg.gridx = 1;
        ytdRow.add(makeStatCard("Total Expenses", String.format("$%.2f", ytdExpTotal),"all categories",                       AMBER, "Expenses",   "EXPENSES"),   yg);
        yg.gridx = 2; yg.insets = new Insets(0, 0, 0, 0);
        ytdRow.add(makeNetCard(ytdNet), yg);
        body.add(ytdRow, gc);
        gc.gridy++;
        gc.insets = new Insets(0, 0, 16, 0);

        // Year over Year
        String lastMonthKey = String.format("%d-%02d", year - 1, today.getMonthValue());
        List<LogEntry>    lastLogs     = storage.loadLogs(lastMonthKey);
        List<Expenditure> lastExpenses = storage.loadExpenditures(String.valueOf(year - 1))
            .stream().filter(e -> e.getDate().startsWith(lastMonthKey)).toList();

        if (!lastLogs.isEmpty() || !lastExpenses.isEmpty()) {
            double lastIncome = 0, lastHours = 0;
            for (Boss boss : selfEmployed) {
                for (LogEntry entry : lastLogs) {
                    if (entry.getType() == LogEntry.EntryType.TIME
                        && entry.getStartTime() != null && entry.getEndTime() != null) {
                        try {
                            long mins = java.time.Duration.between(
                                LocalTime.parse(entry.getStartTime()),
                                LocalTime.parse(entry.getEndTime())).toMinutes();
                            double hrs = mins / 60.0;
                            if (entry.getBossPercentages() != null
                                && entry.getBossPercentages().containsKey(boss.getId())) {
                                double perc = entry.getBossPercentages().get(boss.getId()) / 100.0;
                                lastIncome += hrs * boss.getHourlyRate() * perc;
                                lastHours  += hrs * perc;
                            } else if (boss.getId().equals(entry.getBossUuid())) {
                                lastIncome += hrs * boss.getHourlyRate();
                                lastHours  += hrs;
                            }
                        } catch (Exception ignored) {}
                    } else if (entry.getType() == LogEntry.EntryType.KILOMETER
                        && boss.getId().equals(entry.getBossUuid())
                        && entry.getKilometers() != null && boss.getKmRate() != null) {
                        lastIncome += entry.getKilometers() * boss.getKmRate();
                    } else if (entry.getType() == LogEntry.EntryType.EXTRA
                        && boss.getId().equals(entry.getBossUuid())
                        && entry.getUnits() != null && entry.getCostPerUnit() != null) {
                        lastIncome += entry.getUnits() * entry.getCostPerUnit();
                    }
                }
            }
            double lastExpTotal = lastExpenses.stream().mapToDouble(Expenditure::getTotal).sum();

            String monthName = today.format(DateTimeFormatter.ofPattern("MMMM")).toUpperCase();
            body.add(makeSectionLabel("YEAR OVER YEAR \u2014 " + monthName), gc);
            gc.gridy++;
            body.add(makeYoyCard(year - 1, year, lastIncome, monthIncome,
                lastHours, monthHours, lastExpTotal, monthExpTotal), gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 16, 0);
        }

        // Outstanding invoices
        List<Invoice> outstanding = storage.loadInvoices().stream()
            .filter(inv -> inv.getStatus() == Invoice.Status.SENT)
            .sorted((a, b) -> a.getGeneratedDate().compareTo(b.getGeneratedDate()))
            .toList();
        if (!outstanding.isEmpty()) {
            body.add(makeSectionLabel("OUTSTANDING INVOICES"), gc);
            gc.gridy++;
            JPanel outCard = makeCard();
            outCard.setLayout(new BoxLayout(outCard, BoxLayout.Y_AXIS));
            for (int i = 0; i < outstanding.size(); i++) {
                outCard.add(makeOutstandingRow(outstanding.get(i)));
                if (i < outstanding.size() - 1) outCard.add(makeDivider());
            }
            body.add(outCard, gc);
            gc.gridy++;
            gc.insets = new Insets(0, 0, 16, 0);
        }

        // Recent activity
        body.add(makeSectionLabel("RECENT ACTIVITY"), gc);
        gc.gridy++;
        JPanel activityCard = makeCard();
        activityCard.setLayout(new BoxLayout(activityCard, BoxLayout.Y_AXIS));
        List<ActivityItem> activity = buildRecentActivity(year, today);
        if (activity.isEmpty()) {
            JLabel empty = new JLabel("No recent activity \u2014 start logging to see it here.");
            empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 12f));
            empty.setForeground(new Color(148, 163, 184));
            activityCard.add(Box.createVerticalStrut(8));
            activityCard.add(empty);
        } else {
            for (int i = 0; i < activity.size(); i++) {
                activityCard.add(makeActivityRow(activity.get(i)));
                if (i < activity.size() - 1) activityCard.add(makeDivider());
            }
        }
        body.add(activityCard, gc);

        gc.gridy++;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        body.add(new JPanel() {{ setOpaque(false); }}, gc);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // Year over year card
    private JPanel makeYoyCard(int lastYear, int thisYear,
                                double lastIncome, double thisIncome,
                                double lastHours,  double thisHours,
                                double lastExp,    double thisExp) {
        JPanel card = makeCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 4, 0, 4);

        // Header row
        gc.gridy = 0;
        gc.gridx = 0; gc.weightx = 2.0;
        card.add(makeYoyCell("", Font.BOLD, SLATE, false), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        card.add(makeYoyCell(String.valueOf(lastYear), Font.BOLD, SLATE, true), gc);
        gc.gridx = 2;
        card.add(makeYoyCell(String.valueOf(thisYear), Font.BOLD, SLATE, true), gc);
        gc.gridx = 3;
        card.add(makeYoyCell("Change", Font.BOLD, SLATE, true), gc);

        // Divider
        gc.gridy = 1; gc.gridx = 0; gc.gridwidth = 4; gc.insets = new Insets(4, 0, 4, 0);
        card.add(makeDivider(), gc);
        gc.gridwidth = 1; gc.insets = new Insets(2, 4, 2, 4);

        // Income row
        gc.gridy = 2;
        gc.gridx = 0; gc.weightx = 2.0;
        card.add(makeYoyCell("Gross Income", Font.PLAIN, NAVY, false), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        card.add(makeYoyCell(String.format("$%.2f", lastIncome), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 2;
        card.add(makeYoyCell(String.format("$%.2f", thisIncome), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 3;
        card.add(makeDeltaLabel(lastIncome, thisIncome, true), gc);

        // Hours row
        gc.gridy = 3;
        gc.gridx = 0; gc.weightx = 2.0;
        card.add(makeYoyCell("Hours Logged", Font.PLAIN, NAVY, false), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        card.add(makeYoyCell(String.format("%.1f hrs", lastHours), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 2;
        card.add(makeYoyCell(String.format("%.1f hrs", thisHours), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 3;
        card.add(makeDeltaLabel(lastHours, thisHours, true), gc);

        // Expenses row
        gc.gridy = 4;
        gc.gridx = 0; gc.weightx = 2.0;
        card.add(makeYoyCell("Expenses", Font.PLAIN, NAVY, false), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        card.add(makeYoyCell(String.format("$%.2f", lastExp), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 2;
        card.add(makeYoyCell(String.format("$%.2f", thisExp), Font.PLAIN, NAVY, true), gc);
        gc.gridx = 3;
        // For expenses, up is bad (red), down is good (green) — so invert
        card.add(makeDeltaLabel(lastExp, thisExp, false), gc);

        return card;
    }

    private JLabel makeYoyCell(String text, int style, Color color, boolean rightAlign) {
        JLabel lbl = new JLabel(text, rightAlign ? JLabel.RIGHT : JLabel.LEFT);
        lbl.setFont(lbl.getFont().deriveFont(style, 12f));
        lbl.setForeground(color);
        return lbl;
    }

    private JLabel makeDeltaLabel(double oldVal, double newVal, boolean upIsGood) {
        if (oldVal == 0 && newVal == 0) {
            JLabel lbl = new JLabel("—", JLabel.RIGHT);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
            lbl.setForeground(SLATE);
            return lbl;
        }
        double pct = oldVal == 0 ? 100.0 : ((newVal - oldVal) / oldVal) * 100.0;
        boolean increased = pct > 0;
        boolean isGood    = upIsGood ? increased : !increased;
        String arrow = increased ? "\u25B2 " : "\u25BC ";
        Color  color = pct == 0 ? SLATE : (isGood ? GREEN : RED);
        JLabel lbl = new JLabel(String.format("%s%.1f%%", arrow, Math.abs(pct)), JLabel.RIGHT);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(color);
        return lbl;
    }

    // Stat card
    private JPanel makeStatCard(String title, String value, String sub,
                                 Color accent, String linkLabel, String navTarget) {
        final boolean[] hovered = {false};
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? new Color(249, 250, 251) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(0, 110));
        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 10f));
        titleLbl.setForeground(new Color(148, 163, 184));
        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(valueLbl.getFont().deriveFont(Font.BOLD, 22f));
        valueLbl.setForeground(NAVY);
        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(148, 163, 184));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLbl);
        text.add(Box.createVerticalStrut(4));
        text.add(valueLbl);
        text.add(Box.createVerticalStrut(2));
        text.add(subLbl);
        card.add(text, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
            public void mouseExited(MouseEvent e)  { hovered[0] = false; card.repaint(); }
            public void mousePressed(MouseEvent e) { if (mainFrame != null) mainFrame.showPanel(navTarget); }
        });
        return card;
    }

    private JPanel makeNetCard(double net) {
        Color accent = net >= 0 ? GREEN : RED;
        String value = String.format("%s$%.2f", net < 0 ? "-" : "", Math.abs(net));
        return makeStatCard("Est. Net Income", value,
            "before vehicle & home office deductions", accent, "Accounting", "ACCOUNTING");
    }

    // Recent activity
    private record ActivityItem(String date, String title, String sub, Color accent, String emoji) {}

    private List<ActivityItem> buildRecentActivity(int year, LocalDate today) {
        List<ActivityItem> items = new ArrayList<>();
        for (int m = today.getMonthValue(); m >= Math.max(1, today.getMonthValue() - 2); m--) {
            for (LogEntry entry : storage.loadLogs(String.format("%d-%02d", year, m))) {
                String title = switch (entry.getType()) {
                    case TIME      -> "Work log entry";
                    case KILOMETER -> String.format("%.0f km billed", entry.getKilometers() != null ? entry.getKilometers() : 0);
                    case EXTRA     -> "Extra billed";
                };
                items.add(new ActivityItem(entry.getDate(), title, "Work Log", BLUE, "\uD83D\uDCC5"));
            }
        }
        storage.loadExpenditures(String.valueOf(year)).forEach(e ->
            items.add(new ActivityItem(e.getDate(),
                e.getDescription() != null && !e.getDescription().isBlank() ? e.getDescription() : "Expense",
                String.format("$%.2f", e.getTotal()), AMBER, "\uD83D\uDCB8")));
        storage.loadKmTrips(String.valueOf(year)).forEach(t ->
            items.add(new ActivityItem(t.getDate(),
                t.getNote() != null && !t.getNote().isBlank() ? t.getNote() : "Trip",
                String.format("%.1f km", t.getKm()), GREEN, "\uD83D\uDE97")));
        items.sort(Comparator.comparing(ActivityItem::date).reversed());
        return items.stream().limit(6).toList();
    }

    private JPanel makeActivityRow(ActivityItem item) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JLabel emoji = new JLabel(item.emoji());
        emoji.setFont(emoji.getFont().deriveFont(16f));
        emoji.setPreferredSize(new Dimension(28, 28));
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        JLabel titleLbl = new JLabel(item.title());
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.PLAIN, 12f));
        titleLbl.setForeground(NAVY);
        JLabel subLbl = new JLabel(item.sub());
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(148, 163, 184));
        text.add(titleLbl, BorderLayout.NORTH);
        text.add(subLbl,   BorderLayout.SOUTH);
        JLabel dateLbl = new JLabel(item.date(), JLabel.RIGHT);
        dateLbl.setFont(dateLbl.getFont().deriveFont(Font.PLAIN, 11f));
        dateLbl.setForeground(new Color(148, 163, 184));
        row.add(emoji,   BorderLayout.WEST);
        row.add(text,    BorderLayout.CENTER);
        row.add(dateLbl, BorderLayout.EAST);
        return row;
    }

    // Outstanding invoice row
    private JPanel makeOutstandingRow(Invoice inv) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel invLbl = new JLabel("Invoice #" + inv.getInvoiceNumber() + "  \u2022  " + inv.getBossName());
        invLbl.setFont(invLbl.getFont().deriveFont(Font.BOLD, 12f));
        invLbl.setForeground(NAVY);
        String sentStr = inv.getSentDate() != null
            ? "Sent " + LocalDate.parse(inv.getSentDate()).format(DateTimeFormatter.ofPattern("MMM d")) : "Sent";
        JLabel subLbl = new JLabel(formatInvPeriod(inv.getStartDate(), inv.getEndDate()) + "  \u00B7  " + sentStr);
        subLbl.setFont(subLbl.getFont().deriveFont(Font.PLAIN, 11f));
        subLbl.setForeground(new Color(148, 163, 184));
        left.add(invLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(subLbl);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel amtLbl = new JLabel(String.format("$%.2f", inv.getTotalAmount()));
        amtLbl.setFont(amtLbl.getFont().deriveFont(Font.BOLD, 13f));
        amtLbl.setForeground(new Color(37, 99, 235));
        JButton markPaid = new JButton("Mark Paid");
        markPaid.putClientProperty("JButton.buttonType", "roundRect");
        markPaid.setBackground(new Color(209, 250, 229));
        markPaid.setForeground(new Color(5, 150, 105));
        markPaid.setFont(markPaid.getFont().deriveFont(Font.BOLD, 11f));
        markPaid.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        markPaid.addActionListener(e -> {
            inv.setStatus(Invoice.Status.PAID);
            inv.setPaidDate(LocalDate.now().toString());
            storage.updateInvoice(inv);
            refresh();
            if (mainFrame != null) mainFrame.showTaxSetAside(inv);
        });
        right.add(amtLbl);
        right.add(markPaid);
        row.add(left,  BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private String formatInvPeriod(String start, String end) {
        if (start == null || end == null) return "";
        try {
            LocalDate s = LocalDate.parse(start);
            LocalDate e = LocalDate.parse(end);
            if (s.getMonth() == e.getMonth() && s.getYear() == e.getYear())
                return s.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            return s.format(DateTimeFormatter.ofPattern("MMM d"))
                + " \u2013 " + e.format(DateTimeFormatter.ofPattern("MMM d"));
        } catch (Exception ex) { return start; }
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private JLabel makeSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setForeground(new Color(148, 163, 184));
        return lbl;
    }

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 3, getWidth()-4, getHeight()-2, 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth()-3, getHeight()-3, 14, 14);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-4, getHeight()-4, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);
        return card;
    }

    private JPanel makeDivider() {
        JPanel div = new JPanel();
        div.setOpaque(false);
        div.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        div.setPreferredSize(new Dimension(0, 1));
        div.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));
        return div;
    }
}
