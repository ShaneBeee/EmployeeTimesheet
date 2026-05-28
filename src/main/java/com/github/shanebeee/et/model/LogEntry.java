package com.github.shanebeee.et.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogEntry {

    private final String id;
    private String date; // ISO-8601
    private EntryType type = EntryType.TIME;
    // Time fields
    private String startTime; // HH:mm
    private String endTime; // HH:mm
    private Map<String, Double> bossPercentages; // Boss UUID -> Percentage
    // Kilometer fields
    private String bossUuid; // For KILOMETER and EXTRA types
    private Double kilometers;
    // Extra fields
    private String description;
    private Double units;
    private Double costPerUnit;
    // Old field for backward compatibility (GSON will handle this)
    private List<KmEntry> kmEntries;

    public LogEntry() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Map<String, Double> getBossPercentages() {
        return bossPercentages;
    }

    public void setBossPercentages(Map<String, Double> bossPercentages) {
        this.bossPercentages = bossPercentages;
    }

    public String getBossUuid() {
        return bossUuid;
    }

    public void setBossUuid(String bossUuid) {
        this.bossUuid = bossUuid;
    }

    public Double getKilometers() {
        return kilometers;
    }

    public void setKilometers(Double kilometers) {
        this.kilometers = kilometers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getUnits() {
        return units;
    }

    public void setUnits(Double units) {
        this.units = units;
    }

    public Double getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(Double costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public List<KmEntry> getKmEntries() {
        return kmEntries;
    }

    public void setKmEntries(List<KmEntry> kmEntries) {
        this.kmEntries = kmEntries;
    }

    public enum EntryType {TIME, KILOMETER, EXTRA}

    public static class KmEntry {
        private final String bossUuid;
        private final double kilometers;

        public KmEntry(String bossUuid, double kilometers) {
            this.bossUuid = bossUuid;
            this.kilometers = kilometers;
        }

        public String getBossUuid() {
            return bossUuid;
        }

        public double getKilometers() {
            return kilometers;
        }
    }

}
