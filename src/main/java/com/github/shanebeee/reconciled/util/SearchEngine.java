package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.Boss;
import com.github.shanebeee.reconciled.model.Expenditure;
import com.github.shanebeee.reconciled.model.ExpenseCategory;
import com.github.shanebeee.reconciled.model.KmTrip;
import com.github.shanebeee.reconciled.model.LogEntry;
import com.github.shanebeee.reconciled.storage.DataStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Searches across all data types and returns a ranked list of results.
 */
public class SearchEngine {

    public record SearchResult(
        String title,
        String subtitle,
        String date,
        String panel,       // nav target e.g. "EXPENSES"
        String emoji,
        Object source       // the original model object
    ) {
    }

    private final DataStorage storage;

    public SearchEngine(DataStorage storage) {
        this.storage = storage;
    }

    public List<SearchResult> search(String query, int maxResults) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.toLowerCase().trim();
        List<SearchResult> results = new ArrayList<>();

        int year = LocalDate.now().getYear();
        List<ExpenseCategory> cats = storage.loadExpenseCategories(String.valueOf(year));
        List<Boss> bosses = storage.loadBosses();

        // ── Expenses ─────────────────────────────────────────────────────────
        List<Expenditure> expenses = storage.loadExpenditures(String.valueOf(year));
        for (Expenditure e : expenses) {
            String desc = e.getDescription() != null ? e.getDescription().toLowerCase() : "";
            ExpenseCategory cat = storage.resolveCategory(e, cats);
            String catLabel = cat != null ? cat.getLabel().toLowerCase() : "";
            String amount = String.format("%.2f", e.getTotal());
            if (desc.contains(q) || catLabel.contains(q) || amount.contains(q) || e.getDate().contains(q)) {
                String catName = cat != null ? cat.getLabel() : "Uncategorized";
                results.add(new SearchResult(
                    e.getDescription() != null && !e.getDescription().isBlank() ? e.getDescription() : catName,
                    catName + "  ·  $" + String.format("%.2f", e.getTotal()),
                    e.getDate(), "EXPENSES", "💸", e));
            }
        }

        // ── KM Trips ─────────────────────────────────────────────────────────
        List<KmTrip> trips = storage.loadKmTrips(String.valueOf(year));
        for (KmTrip t : trips) {
            String note = t.getNote() != null ? t.getNote().toLowerCase() : "";
            String km = String.format("%.1f", t.getKm());
            if (note.contains(q) || km.contains(q) || t.getDate().contains(q)) {
                results.add(new SearchResult(
                    t.getNote() != null && !t.getNote().isBlank() ? t.getNote() : "Trip",
                    km + " km  ·  " + (t.getSourceLogId() != null ? "Auto-logged" : "Manual"),
                    t.getDate(), "KM", "🚗", t));
            }
        }

        // ── Work Logs ────────────────────────────────────────────────────────
        for (int m = 1; m <= 12; m++) {
            String monthKey = String.format("%d-%02d", year, m);
            List<LogEntry> logs = storage.loadLogs(monthKey);
            for (LogEntry entry : logs) {
                // Resolve boss name
                String bossName = bosses.stream()
                    .filter(b -> b.getId().equals(entry.getBossUuid()))
                    .map(Boss::getName).findFirst().orElse("");
                String type = switch (entry.getType()) {
                    case TIME -> "Time entry";
                    case KILOMETER -> "KM entry";
                    case EXTRA -> "Extra entry";
                };
                String searchable = (bossName + " " + type + " " + entry.getDate()).toLowerCase();
                if (searchable.contains(q)) {
                    String sub = type + (bossName.isBlank() ? "" : "  ·  " + bossName);
                    results.add(new SearchResult(
                        type + (bossName.isBlank() ? "" : " — " + bossName),
                        sub, entry.getDate(), "LOGS", "📅", entry));
                }
            }
        }

        // Sort by date desc, limit
        results.sort(Comparator.comparing(SearchResult::date).reversed());
        return results.stream().limit(maxResults).toList();
    }

}
