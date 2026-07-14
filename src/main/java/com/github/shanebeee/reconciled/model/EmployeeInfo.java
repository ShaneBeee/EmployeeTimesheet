package com.github.shanebeee.reconciled.model;

public class EmployeeInfo {

    private String fullName;
    private String company;
    private String address;
    private String address2;
    private String phoneNumber;
    private String email;
    private double homeOfficeSqFt = 0;
    private double homeTotalSqFt = 0;

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public double getHomeOfficeSqFt() {
        return homeOfficeSqFt;
    }

    public void setHomeOfficeSqFt(double v) {
        this.homeOfficeSqFt = v;
    }

    public double getHomeTotalSqFt() {
        return homeTotalSqFt;
    }

    public void setHomeTotalSqFt(double v) {
        this.homeTotalSqFt = v;
    }

    /**
     * Returns the home office deduction % (0.0–1.0), or 0 if not configured.
     */
    public double homeOfficePercent() {
        if (homeTotalSqFt <= 0) return 0;
        return Math.min(1.0, homeOfficeSqFt / homeTotalSqFt);
    }
}
