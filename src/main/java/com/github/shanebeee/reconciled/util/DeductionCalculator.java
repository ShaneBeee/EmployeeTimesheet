package com.github.shanebeee.reconciled.util;

import com.github.shanebeee.reconciled.model.ExpenseCategory;
import com.github.shanebeee.reconciled.model.KmOdometer;
import com.github.shanebeee.reconciled.model.KmTrip;
import com.github.shanebeee.reconciled.storage.DataStorage;

import java.util.List;

/**
 * Resolves the business-use deduction percentage for an expense category in a given year.
 * All results are in the range 0.0–1.0.
 */
public class DeductionCalculator {

    private final DataStorage storage;

    public DeductionCalculator(DataStorage storage) {
        this.storage = storage;
    }

    /**
     * Returns the deductible fraction (0.0–1.0) for the given category and year.
     */
    public double percentFor(ExpenseCategory cat, int year) {
        if (cat == null) return 1.0;
        return switch (cat.getDeductionType()) {
            case FULL -> 1.0;
            case FIXED_PERCENT -> cat.getFixedPercent();
            case KM_PERCENT -> kmPercent(year);
            case HOME_OFFICE -> storage.loadEmployeeInfo().homeOfficePercent();
        };
    }

    /**
     * Returns the deductible amount for a given expense total, category, and year.
     */
    public double deductibleAmount(double total, ExpenseCategory cat, int year) {
        return total * percentFor(cat, year);
    }

    /**
     * Returns a display string for the percentage, e.g. "60%", "11.8%", "KM-based (30.2%)"
     */
    public String percentLabel(ExpenseCategory cat, int year) {
        if (cat == null) return "100%";
        return switch (cat.getDeductionType()) {
            case FULL -> "100%";
            case FIXED_PERCENT -> String.format("%.0f%%", cat.getFixedPercent() * 100);
            case KM_PERCENT -> String.format("KM-based (%.1f%%)", kmPercent(year) * 100);
            case HOME_OFFICE -> {
                double pct = storage.loadEmployeeInfo().homeOfficePercent();
                yield String.format("Home office (%.1f%%)", pct * 100);
            }
        };
    }

    /**
     * Whether this category has any partial-use restriction (i.e. not 100% deductible).
     */
    public boolean isPartial(ExpenseCategory cat, int year) {
        if (cat == null) return false;
        return percentFor(cat, year) < 1.0;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private double kmPercent(int year) {
        List<KmTrip> trips = storage.loadKmTrips(String.valueOf(year));
        KmOdometer odometer = storage.loadKmOdometer(String.valueOf(year));
        double businessKm = trips.stream().mapToDouble(KmTrip::getKm).sum();
        double totalKm = odometer.totalKm();
        if (totalKm <= 0 || businessKm <= 0) return 0;
        return Math.min(1.0, businessKm / totalKm);
    }

}
