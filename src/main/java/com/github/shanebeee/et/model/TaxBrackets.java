package com.github.shanebeee.et.model;

import java.util.ArrayList;
import java.util.List;

/**
 * CRA tax brackets and rates for a given year.
 * Stored in settings/tax_brackets_{year}.json.
 */
public class TaxBrackets {

    private int year;

    // Federal and BC brackets: list of thresholds + marginal rate
    // The last bracket has upTo = Double.MAX_VALUE (no ceiling)
    private List<Bracket> federal = defaultFederal();
    private List<Bracket> bc      = defaultBc();

    // CPP
    private double cppRate           = 0.119;  // combined employee + employer (self-employed pays both)
    private double cppMaxContribution = 3867.50; // 2025 approximate; update annually

    // ── Inner class ───────────────────────────────────────────────────────────

    public static class Bracket {
        private double upTo; // income ceiling for this bracket (use 1e12 for "no limit")
        private double rate; // marginal rate e.g. 0.15 for 15%

        public Bracket() {}
        public Bracket(double upTo, double rate) { this.upTo = upTo; this.rate = rate; }

        public double getUpTo() { return upTo; }
        public void   setUpTo(double v) { this.upTo = v; }
        public double getRate() { return rate; }
        public void   setRate(double v) { this.rate = v; }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getYear()                       { return year; }
    public void setYear(int v)                 { this.year = v; }

    public List<Bracket> getFederal()          { return federal; }
    public void          setFederal(List<Bracket> v) { this.federal = v; }

    public List<Bracket> getBc()               { return bc; }
    public void          setBc(List<Bracket> v){ this.bc = v; }

    public double getCppRate()                         { return cppRate; }
    public void   setCppRate(double v)                 { this.cppRate = v; }

    public double getCppMaxContribution()              { return cppMaxContribution; }
    public void   setCppMaxContribution(double v)      { this.cppMaxContribution = v; }

    // ── Tax calculation ───────────────────────────────────────────────────────

    /**
     * Computes the marginal federal rate for the given annual income.
     * Returns the rate of the highest bracket the income falls into.
     */
    public double federalRateFor(double annualIncome) {
        return marginalRate(federal, annualIncome);
    }

    /** Computes the marginal BC provincial rate for the given annual income. */
    public double bcRateFor(double annualIncome) {
        return marginalRate(bc, annualIncome);
    }

    /**
     * Computes the CPP contribution for a single payment, given how much CPP
     * has already been contributed this year. Returns 0 once the annual max is hit.
     * @param preGstPayment  pre-GST income from this payment
     * @param ytdCppPaid     CPP already contributed so far this year
     */
    public double cppFor(double preGstPayment, double ytdCppPaid) {
        if (ytdCppPaid >= cppMaxContribution) return 0;
        double owing = preGstPayment * cppRate;
        return Math.min(owing, cppMaxContribution - ytdCppPaid);
    }

    private double marginalRate(List<Bracket> brackets, double income) {
        if (brackets == null || brackets.isEmpty()) return 0;
        for (Bracket b : brackets) {
            if (income <= b.getUpTo()) return b.getRate();
        }
        return brackets.get(brackets.size() - 1).getRate();
    }

    // ── Defaults (2025 / 2026 CRA brackets) ──────────────────────────────────

    public static List<Bracket> defaultFederal() {
        List<Bracket> list = new ArrayList<>();
        list.add(new Bracket(57375,  0.15));
        list.add(new Bracket(114750, 0.205));
        list.add(new Bracket(177882, 0.26));
        list.add(new Bracket(253414, 0.29));
        list.add(new Bracket(1e12,   0.33));
        return list;
    }

    public static List<Bracket> defaultBc() {
        List<Bracket> list = new ArrayList<>();
        list.add(new Bracket(47937,  0.0506));
        list.add(new Bracket(95875,  0.077));
        list.add(new Bracket(110076, 0.105));
        list.add(new Bracket(133664, 0.1229));
        list.add(new Bracket(181232, 0.147));
        list.add(new Bracket(252752, 0.168));
        list.add(new Bracket(1e12,   0.205));
        return list;
    }
}
