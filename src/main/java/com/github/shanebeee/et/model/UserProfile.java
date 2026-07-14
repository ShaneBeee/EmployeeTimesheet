package com.github.shanebeee.et.model;

import java.util.UUID;

/**
 * Represents a user profile. Each profile has its own independent data directory.
 */
public class UserProfile {

    private String id;
    private String name;
    private String dataPath;   // absolute path to this user's data directory
    private String avatarColor; // hex colour for the avatar circle, e.g. "#3B82F6"

    public UserProfile() {
        this.id = UUID.randomUUID().toString();
    }

    public UserProfile(String name, String dataPath, String avatarColor) {
        this.id          = UUID.randomUUID().toString();
        this.name        = name;
        this.dataPath    = dataPath;
        this.avatarColor = avatarColor;
    }

    public String getId()                      { return id; }
    public void   setId(String v)              { this.id = v; }

    public String getName()                    { return name; }
    public void   setName(String v)            { this.name = v; }

    public String getDataPath()                { return dataPath; }
    public void   setDataPath(String v)        { this.dataPath = v; }

    public String getAvatarColor()             { return avatarColor; }
    public void   setAvatarColor(String v)     { this.avatarColor = v; }

    /** Display-friendly initials (up to 2 chars) derived from name. */
    public String initials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    @Override public String toString() { return name; }
}
