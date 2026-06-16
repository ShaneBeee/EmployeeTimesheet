package com.github.shanebeee.et.model;

import java.util.UUID;

public class Expenditure {

    private final String id;
    private String date;           // ISO-8601 yyyy-MM-dd
    private Category category;
    private String description;
    private double amount;
    private double businessUsePercent; // 0-100, for mixed-use expenses like phone/home office

    public Expenditure() {
        this.id = UUID.randomUUID().toString();
        this.businessUsePercent = 100.0;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getBusinessUsePercent() {
        return businessUsePercent;
    }

    public void setBusinessUsePercent(double businessUsePercent) {
        this.businessUsePercent = businessUsePercent;
    }

    /**
     * The deductible portion after applying businessUsePercent
     */
    public double getDeductibleAmount() {
        return amount * (businessUsePercent / 100.0);
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
