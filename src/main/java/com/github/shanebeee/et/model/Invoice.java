package com.github.shanebeee.et.model;

import java.util.UUID;

public class Invoice {

    private final String id;
    private int invoiceNumber;
    private String bossId;
    private String bossName;      // denormalized for display even if boss is deleted
    private String startDate;     // ISO-8601
    private String endDate;       // ISO-8601
    private String generatedDate; // ISO-8601
    private Status status;
    private String sentDate;      // ISO-8601, nullable
    private String paidDate;      // ISO-8601, nullable
    private double totalAmount;
    private String pdfPath;       // absolute path to the generated PDF

    public Invoice() {
        this.id = UUID.randomUUID().toString();
        this.status = Status.DRAFT;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()            { return id; }
    public int getInvoiceNumber()    { return invoiceNumber; }
    public String getBossId()        { return bossId; }
    public String getBossName()      { return bossName; }
    public String getStartDate()     { return startDate; }
    public String getEndDate()       { return endDate; }
    public String getGeneratedDate() { return generatedDate; }
    public Status getStatus()        { return status; }
    public String getSentDate()      { return sentDate; }
    public String getPaidDate()      { return paidDate; }
    public double getTotalAmount()   { return totalAmount; }
    public String getPdfPath()       { return pdfPath; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setInvoiceNumber(int invoiceNumber)    { this.invoiceNumber = invoiceNumber; }
    public void setBossId(String bossId)               { this.bossId = bossId; }
    public void setBossName(String bossName)           { this.bossName = bossName; }
    public void setStartDate(String startDate)         { this.startDate = startDate; }
    public void setEndDate(String endDate)             { this.endDate = endDate; }
    public void setGeneratedDate(String generatedDate) { this.generatedDate = generatedDate; }
    public void setStatus(Status status)               { this.status = status; }
    public void setSentDate(String sentDate)           { this.sentDate = sentDate; }
    public void setPaidDate(String paidDate)           { this.paidDate = paidDate; }
    public void setTotalAmount(double totalAmount)     { this.totalAmount = totalAmount; }
    public void setPdfPath(String pdfPath)             { this.pdfPath = pdfPath; }

    // ── Status enum ───────────────────────────────────────────────────────────

    public enum Status {
        DRAFT("Draft"),
        SENT("Sent"),
        PAID("Paid");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
}
