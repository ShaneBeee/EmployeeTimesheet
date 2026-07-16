package com.github.shanebeee.reconciled.storage;

import com.github.shanebeee.reconciled.model.UserProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages user profiles. Profiles are stored in a profiles.json file
 * at the root storage location (parent of all user data directories).
 * <p>
 * Root layout:
 * {rootDir}/
 * profiles.json
 * {profileName}/      ← each user's data directory
 * settings/
 * logs/
 * ...
 * <p>
 * Migration: if the app has existing single-user data (Preferences key set,
 * no profiles.json), we auto-create a "Default" profile pointing at that path.
 */
public class ProfileManager {

    private static final String PREFS_KEY = "dataDirectory";
    private static final String PREFS_ROOT_KEY = "rootDirectory";
    private static final String PROFILES_FILE = "profiles.json";

    // Palette of avatar colours to cycle through for new profiles
    private static final String[] AVATAR_COLORS = {
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#06B6D4", "#84CC16"
    };

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String rootDir;

    public ProfileManager(String rootDir) {
        if (!rootDir.endsWith(File.separator)) rootDir = rootDir + File.separator;
        this.rootDir = rootDir;
        try {
            Files.createDirectories(Paths.get(rootDir));
        } catch (IOException ignored) {
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    /**
     * Returns the root directory where profiles.json lives.
     * Defaults to ~/Reconciled/ or the iCloud equivalent.
     */
    public static String getRootDir() {
        Preferences prefs = Preferences.userNodeForPackage(ProfileManager.class);
        String saved = prefs.get(PREFS_ROOT_KEY, null);
        if (saved != null) return saved;
        String legacy = DataStorage.getSavedDataDirectory();
        if (legacy != null && !legacy.isBlank()) return legacy;
        return System.getProperty("user.home") + File.separator + "Reconciled" + File.separator;
    }

    /**
     * Returns the explicitly saved rootDirectory preference, or null if none has
     * ever been saved. Unlike {@link #getRootDir()}, this does NOT fall back to
     * guessing — callers use the null case to know they need to derive a root
     * (e.g. on first run) rather than silently trusting a guessed value.
     */
    public static String getSavedRootDirOrNull() {
        Preferences prefs = Preferences.userNodeForPackage(ProfileManager.class);
        return prefs.get(PREFS_ROOT_KEY, null);
    }

    public static void saveRootDir(String path) {
        if (!path.endsWith(File.separator)) path = path + File.separator;
        try {
            Preferences prefs = Preferences.userNodeForPackage(ProfileManager.class);
            prefs.put(PREFS_ROOT_KEY, path);
            prefs.flush();
        } catch (java.util.prefs.BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public static String getActiveProfileId() {
        return Preferences.userNodeForPackage(ProfileManager.class).get("activeProfileId", null);
    }

    public static void saveActiveProfileId(String id) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ProfileManager.class);
            prefs.put("activeProfileId", id);
            prefs.flush();
        } catch (java.util.prefs.BackingStoreException e) {
            e.printStackTrace();
        }
    }

    // ── Profile CRUD ──────────────────────────────────────────────────────────

    public List<UserProfile> loadProfiles() {
        File f = new File(rootDir + PROFILES_FILE);
        if (!f.exists()) return new ArrayList<>();
        try (FileReader r = new FileReader(f)) {
            List<UserProfile> list = gson.fromJson(r, new TypeToken<List<UserProfile>>() {
            }.getType());
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveProfiles(List<UserProfile> profiles) {
        try (FileWriter w = new FileWriter(rootDir + PROFILES_FILE)) {
            gson.toJson(profiles, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UserProfile findById(String id) {
        return loadProfiles().stream()
            .filter(p -> p.getId().equals(id))
            .findFirst().orElse(null);
    }

    /**
     * Creates a new profile, creates its data directory, saves it, and returns it.
     */
    public UserProfile createProfile(String name) {
        List<UserProfile> profiles = loadProfiles();
        String color = AVATAR_COLORS[profiles.size() % AVATAR_COLORS.length];
        String safeName = name.trim().replaceAll("[^a-zA-Z0-9_\\- ]", "").trim().replace(" ", "_");
        String dataPath = rootDir + safeName + File.separator;
        // Deduplicate path if needed
        int suffix = 2;
        while (new File(dataPath).exists() && !isDirFreeForNewProfile(dataPath, profiles)) {
            dataPath = rootDir + safeName + "_" + suffix + File.separator;
            suffix++;
        }
        UserProfile profile = new UserProfile(name.trim(), dataPath, color);
        try {
            Files.createDirectories(Paths.get(dataPath));
        } catch (IOException ignored) {
        }
        profiles.add(profile);
        saveProfiles(profiles);
        return profile;
    }

    public void updateProfile(UserProfile updated) {
        List<UserProfile> profiles = loadProfiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getId().equals(updated.getId())) {
                profiles.set(i, updated);
                break;
            }
        }
        saveProfiles(profiles);
    }

    public void deleteProfile(String id) {
        List<UserProfile> profiles = loadProfiles();
        profiles.removeIf(p -> p.getId().equals(id));
        saveProfiles(profiles);
    }

    // ── Migration ─────────────────────────────────────────────────────────────

    /**
     * If the app has existing single-user data (legacy Preferences path set)
     * and no profiles.json yet, migrates all existing data into a named
     * subfolder and creates a profile pointing at it.
     * Safe to call on every launch — does nothing if profiles already exist.
     */
    public UserProfile migrateIfNeeded(String userName) {
        List<UserProfile> profiles = loadProfiles();
        if (!profiles.isEmpty()) return null; // already migrated

        String legacyPath = DataStorage.getSavedDataDirectory();
        if (legacyPath == null || legacyPath.isBlank()) return null;
        if (!legacyPath.endsWith(File.separator)) legacyPath = legacyPath + File.separator;

        String name = (userName != null && !userName.isBlank()) ? userName : "Me";
        String safeName = name.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim().replace(" ", "_");
        String newDataPath = rootDir + safeName + File.separator;

        // Move existing data into the named subfolder (unless it's already there)
        if (!legacyPath.equals(newDataPath)) {
            try {
                java.nio.file.Path src = java.nio.file.Paths.get(legacyPath);
                java.nio.file.Path dest = java.nio.file.Paths.get(newDataPath);
                java.nio.file.Files.createDirectories(dest);

                // Move each top-level item that isn't profiles.json
                try (java.util.stream.Stream<java.nio.file.Path> stream =
                         java.nio.file.Files.list(src)) {
                    stream
                        .filter(p -> !p.getFileName().toString().equals(PROFILES_FILE))
                        .forEach(p -> {
                            try {
                                java.nio.file.Files.move(p,
                                    dest.resolve(p.getFileName()),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                            }
                        });
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
                // Migration failed — fall back to keeping data in-place
                newDataPath = legacyPath;
            }
        }

        // Update DataStorage Preferences to point at the new subfolder
        DataStorage.saveDataDirectory(newDataPath);

        UserProfile profile = new UserProfile(name, newDataPath, AVATAR_COLORS[0]);
        profiles.add(profile);
        saveProfiles(profiles);
        saveRootDir(rootDir);
        return profile;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isDirFreeForNewProfile(String path, List<UserProfile> existing) {
        return existing.stream().noneMatch(p -> p.getDataPath().equals(path));
    }

    public String getRootDirPath() {
        return rootDir;
    }

}
