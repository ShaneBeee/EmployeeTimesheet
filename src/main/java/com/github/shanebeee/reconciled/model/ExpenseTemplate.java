package com.github.shanebeee.reconciled.model;

import java.util.UUID;

public class ExpenseTemplate {

    private String id;
    private String name;        // display name e.g. "Cell Phone - Koodo"
    private String categoryId;  // ExpenseCategory UUID
    private String description; // pre-fill description
    private double subtotal;
    private double gst;
    private double total;

    public ExpenseTemplate() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String id) {
        this.categoryId = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double v) {
        this.subtotal = v;
    }

    public double getGst() {
        return gst;
    }

    public void setGst(double v) {
        this.gst = v;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double v) {
        this.total = v;
    }

    @Override
    public String toString() {
        return name != null ? name : description;
    }

}
