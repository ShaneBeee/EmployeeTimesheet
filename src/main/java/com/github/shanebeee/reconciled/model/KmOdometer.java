package com.github.shanebeee.reconciled.model;

public class KmOdometer {
    private double startKm = 0;
    private double endKm   = 0;

    public double getStartKm()          { return startKm; }
    public void   setStartKm(double v)  { this.startKm = v; }

    public double getEndKm()            { return endKm; }
    public void   setEndKm(double v)    { this.endKm = v; }

    /** Total KMs on the vehicle for the year (0 if not yet set). */
    public double totalKm() {
        if (endKm <= 0 || startKm <= 0) return 0;
        return Math.max(0, endKm - startKm);
    }
}
