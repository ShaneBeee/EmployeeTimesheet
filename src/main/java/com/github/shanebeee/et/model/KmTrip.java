package com.github.shanebeee.et.model;

import java.util.UUID;

public class KmTrip {

    private final String id;
    private String date;          // ISO-8601 yyyy-MM-dd
    private double km;            // distance in kilometres
    private String note;          // purpose / description
    private String sourceLogId;   // LogEntry id that auto-created this trip (null if manual)

    public KmTrip() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId()                   { return id; }

    public String getDate()                 { return date; }
    public void   setDate(String d)         { this.date = d; }

    public double getKm()                   { return km; }
    public void   setKm(double km)          { this.km = km; }

    public String getNote()                 { return note; }
    public void   setNote(String n)         { this.note = n; }

    public String getSourceLogId()          { return sourceLogId; }
    public void   setSourceLogId(String id) { this.sourceLogId = id; }
}
