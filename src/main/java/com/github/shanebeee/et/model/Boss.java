package com.github.shanebeee.et.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Boss {

    private final String id;
    private String name;
    private String address;
    private String address2;
    private String company;
    private String phoneNumber;
    private String email;
    private double hourlyRate;
    private double taxRate;
    private Double kmRate; // Optional
    private List<RateChange> rateHistory = new ArrayList<>();

    public Boss(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public Double getKmRate() {
        return kmRate;
    }

    public void setKmRate(Double kmRate) {
        this.kmRate = kmRate;
    }

    public List<RateChange> getRateHistory() {
        return rateHistory;
    }

    public void setRateHistory(List<RateChange> rateHistory) {
        this.rateHistory = rateHistory;
    }

    /**
     * @param date ISO-8601
     */
    public record RateChange(String date, double rate) {
    }

}
