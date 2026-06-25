package com.github.shanebeee.et.model;

import java.util.UUID;

public class ExpenseCategory {

    private String id;
    private String label;
    private String hint;
    private String t2125Line;
    private String color;   // hex e.g. "#3B82F6"
    private boolean builtIn; // built-in categories can't be deleted

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
    public void    setBuiltIn(boolean b)     { this.builtIn = b; }

    @Override
    public String toString()                 { return label; }
}
