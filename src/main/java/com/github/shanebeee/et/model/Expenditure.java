package com.github.shanebeee.et.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Expenditure {

    private final String id;
    private String date;           // ISO-8601 yyyy-MM-dd
    private Category category;     // legacy enum — kept for Gson backwards compat
    private String categoryId;     // UUID pointing to ExpenseCategory
    private String description;
    private double subtotal;       // pre-tax amount
    private double gst;            // GST paid
    private double total;          // total after all taxes
    private List<String> receiptFiles; // relative paths under receipts/{year}/{month}/

    public Expenditure() {
        this.id = UUID.randomUUID().toString();
        this.receiptFiles = new ArrayList<>();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getCategoryId()              { return categoryId; }
    public void   setCategoryId(String id)     { this.categoryId = id; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getGst() {
        return gst;
    }

    public void setGst(double gst) {
        this.gst = gst;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<String> getReceiptFiles() {
        if (receiptFiles == null) receiptFiles = new ArrayList<>();
        return receiptFiles;
    }

    public void setReceiptFiles(List<String> receiptFiles) {
        this.receiptFiles = receiptFiles;
    }

    // ── Category enum — mapped to T2125 line numbers ─────────────────────────

    public enum Category {
        VEHICLE("Vehicle", "Gas, maintenance, insurance, repairs", "9281"),
        PHONE_INTERNET("Phone & Internet", "Cell phone, home internet", "9270"),
        HOME_OFFICE("Home Office", "Rent, utilities (% of home)", "9945"),
        MEALS("Meals & Entertain.", "50% deductible by CRA", "8523"),
        SUPPLIES("Office Supplies", "Paper, ink, tools, software", "8810"),
        PROFESSIONAL("Professional Fees", "Accountant, lawyer, subscriptions", "8860"),
        ADVERTISING("Advertising", "Business cards, online ads, marketing", "8520"),
        OTHER("Other", "Miscellaneous business expenses", "9270");

        private final String label;
        private final String hint;
        private final String t2125Line;

        Category(String label, String hint, String t2125Line) {
            this.label = label;
            this.hint = hint;
            this.t2125Line = t2125Line;
        }

        public String getLabel() {
            return label;
        }

        public String getHint() {
            return hint;
        }

        public String getT2125Line() {
            return t2125Line;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}
