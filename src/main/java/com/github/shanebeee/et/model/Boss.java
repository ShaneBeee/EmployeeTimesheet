package com.github.shanebeee.et.model;

import java.util.ArrayList;
import java.util.List;

public class Boss {
    private String id;
    private String name;
    private String address;
    private String company;
    private String phoneNumber;
    private String email;
    private double hourlyRate;
    private double taxRate;
    private Double kmRate; // Optional
    private List<RateChange> rateHistory = new ArrayList<>();

    public Boss(String name) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }
    public Double getKmRate() { return kmRate; }
    public void setKmRate(Double kmRate) { this.kmRate = kmRate; }
    public List<RateChange> getRateHistory() { return rateHistory; }
    public void setRateHistory(List<RateChange> rateHistory) { this.rateHistory = rateHistory; }

    public static class RateChange {
        private String date; // ISO-8601
        private double rate;

        public RateChange(String date, double rate) {
            this.date = date;
            this.rate = rate;
        }
        public String getDate() { return date; }
        public double getRate() { return rate; }
    }
}
