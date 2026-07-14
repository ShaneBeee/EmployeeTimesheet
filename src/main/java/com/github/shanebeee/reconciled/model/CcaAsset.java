package com.github.shanebeee.reconciled.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a depreciable capital asset tracked for CRA Capital Cost Allowance (CCA).
 *
 * UCC and deduction amounts are always derived mathematically from cost + purchaseDate —
 * there is no mutable running balance. Assets stay on the books until manually removed
 * (e.g. when sold or disposed of).
 */
public class CcaAsset {

    private String id;
    private String description;    // e.g. "Apple MacBook Air"
    private String purchaseDate;   // ISO-8601 yyyy-MM-dd
    private double cost;           // pre-tax capital cost (excluding GST)
    private String assetClass;     // e.g. "Class 50"
    private double classRate;      // e.g. 0.55 for Class 50
    private List<String> receiptFiles = new ArrayList<>();

    public CcaAsset() {
        this.id = UUID.randomUUID().toString();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                         { return id; }
    public void   setId(String v)                 { this.id = v; }

    public String getDescription()                { return description; }
    public void   setDescription(String v)        { this.description = v; }

    public String getPurchaseDate()               { return purchaseDate; }
    public void   setPurchaseDate(String v)       { this.purchaseDate = v; }

    public double getCost()                       { return cost; }
    public void   setCost(double v)               { this.cost = v; }

    public String getAssetClass()                 { return assetClass; }
    public void   setAssetClass(String v)         { this.assetClass = v; }

    public double getClassRate()                  { return classRate; }
    public void   setClassRate(double v)          { this.classRate = v; }

    public List<String> getReceiptFiles()         { return receiptFiles != null ? receiptFiles : new ArrayList<>(); }
    public void         setReceiptFiles(List<String> v) { this.receiptFiles = v; }

    // ── Pure computed properties ──────────────────────────────────────────────

    /** Year the asset was purchased, derived from purchaseDate. */
    public int getPurchaseYear() {
        if (purchaseDate == null || purchaseDate.length() < 4) return 0;
        try { return Integer.parseInt(purchaseDate.substring(0, 4)); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Opening UCC at the start of the given tax year (before this year's deduction).
     * - Before purchase year: 0
     * - Purchase year: full cost (deduction taken during the year, not before)
     * - Subsequent years: cost × (1 - rate×0.5) × (1 - rate)^(yearsElapsed-1)
     */
    public double openingUccForYear(int taxYear) {
        int purchaseYear = getPurchaseYear();
        if (purchaseYear == 0 || taxYear < purchaseYear) return 0;
        if (taxYear == purchaseYear) return cost;
        // After purchase year: apply half-year rule for year 1, full rate thereafter
        double ucc = cost * (1 - classRate * 0.5);          // end of purchase year
        for (int y = purchaseYear + 1; y < taxYear; y++) {
            ucc = ucc * (1 - classRate);
        }
        return ucc;
    }

    /**
     * CCA deduction for the given tax year.
     * - Purchase year: cost × rate × 0.5  (half-year rule)
     * - Subsequent years: openingUcc × rate
     * - Before purchase year: 0
     */
    public double deductionForYear(int taxYear) {
        int purchaseYear = getPurchaseYear();
        if (purchaseYear == 0 || taxYear < purchaseYear) return 0;
        double opening = openingUccForYear(taxYear);
        if (taxYear == purchaseYear) return opening * classRate * 0.5;
        return opening * classRate;
    }

    /**
     * Closing UCC at the end of the given tax year (after deduction).
     */
    public double closingUccForYear(int taxYear) {
        return Math.max(0, openingUccForYear(taxYear) - deductionForYear(taxYear));
    }
}
