package com.github.shanebeee.et.model;

import java.util.UUID;

public class ExpenseCategory {

    private String id;
    private String label;
    private String hint;
    private String t2125Line;
    private String color;   // hex e.g. "#3B82F6"
    private boolean builtIn; // built-in categories can't be deleted

    public enum DeductionType {
        FULL,          // 100% deductible
        FIXED_PERCENT, // fixed % set manually (e.g. phone at 60%)
        KM_PERCENT,    // auto-calculated from KM log (vehicle expenses)
        HOME_OFFICE    // office sqft / total sqft from settings
    }

    private DeductionType deductionType = DeductionType.FULL;
    private double fixedPercent = 1.0; // used when deductionType == FIXED_PERCENT

    public ExpenseCategory() {}

    public ExpenseCategory(String label, String hint, String t2125Line, String color, boolean builtIn) {
        this.id       = UUID.randomUUID().toString();
        this.label    = label;
        this.hint     = hint;
        this.t2125Line = t2125Line;
        this.color    = color;
        this.builtIn  = builtIn;
    }

    public String getId()                    { return id; }
    public void   setId(String id)           { this.id = id; }

    public String getLabel()                 { return label; }
    public void   setLabel(String label)     { this.label = label; }

    public String getHint()                  { return hint; }
    public void   setHint(String hint)       { this.hint = hint; }

    public String getT2125Line()             { return t2125Line; }
    public void   setT2125Line(String t)     { this.t2125Line = t; }

    public String getColor()                 { return color; }
    public void   setColor(String color)     { this.color = color; }

    public boolean isBuiltIn()               { return builtIn; }
    public void    setBuiltIn(boolean b)      { this.builtIn = b; }

    public DeductionType getDeductionType()              { return deductionType != null ? deductionType : DeductionType.FULL; }
    public void          setDeductionType(DeductionType t) { this.deductionType = t; }

    public double getFixedPercent()           { return fixedPercent; }
    public void   setFixedPercent(double v)   { this.fixedPercent = v; }

    @Override
    public String toString()                 { return label; }
}
