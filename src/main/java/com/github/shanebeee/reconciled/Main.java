package com.github.shanebeee.reconciled;

import com.formdev.flatlaf.FlatLightLaf;
import com.github.shanebeee.reconciled.model.UserProfile;
import com.github.shanebeee.reconciled.storage.DataStorage;
import com.github.shanebeee.reconciled.storage.ProfileManager;
import com.github.shanebeee.reconciled.util.UIUtils;
import com.github.shanebeee.reconciled.view.MainFrame;
import com.github.shanebeee.reconciled.view.OnboardingWizard;
import com.github.shanebeee.reconciled.view.ProfilePickerDialog;

import javax.swing.*;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Taskbar;
import java.io.File;

public class Main {

    static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Reconciled");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Reconciled");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 12);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Button.background", Color.WHITE);
        UIManager.put("Button.foreground", new Color(30, 41, 59));
        UIManager.put("App.accent", new Color(59, 130, 246));
        UIManager.put("App.success", new Color(34, 197, 94));
        UIManager.put("App.warning", new Color(245, 158, 11));
        UIManager.put("App.danger", new Color(239, 68, 68));
        setupLaf();

        SwingUtilities.invokeLater(() -> {
            setupLaf();

            String currentDataDir = DataStorage.getSavedDataDirectory();

            // ── Step 2: Run onboarding if needed ─────────────────────────────
            if (!DataStorage.isOnboardingComplete()) {
                OnboardingWizard wizard = new OnboardingWizard(null);
                wizard.setVisible(true);
                if (!DataStorage.isOnboardingComplete()) {
                    System.exit(0); // user closed without finishing
                }
                // Reload after onboarding set the path
                currentDataDir = DataStorage.getSavedDataDirectory();
            }

            // ── Step 3: Resolve rootDir ─────────────────────────────────
            // Prefer the explicitly saved rootDirectory preference — this is the
            // authoritative source of truth once set. Only fall back to deriving
            // it from the data path (legacy/first-run) when nothing is saved yet,
            // and never let that derivation nest under an existing rootDir.
            String savedRootDir = ProfileManager.getSavedRootDirOrNull();
            String rootDir;
            if (savedRootDir != null && !savedRootDir.isBlank()) {
                rootDir = savedRootDir;
            } else {
                // First run / legacy fallback: derive from the data directory by
                // truncating at the app folder name, whatever it's currently called.
                String norm = currentDataDir.replace("//", "/");
                int idx = norm.indexOf("Reconciled" + File.separator);
                if (idx >= 0) {
                    rootDir = norm.substring(0, idx + "Reconciled".length() + 1);
                } else {
                    // Couldn't find the marker — treat currentDataDir's parent as root
                    // rather than the full (possibly deep) path itself, so we never
                    // start nesting new profile folders inside an existing one.
                    File parent = new File(currentDataDir).getParentFile();
                    rootDir = (parent != null ? parent.getAbsolutePath() : currentDataDir) + File.separator;
                }
            }
            ProfileManager.saveRootDir(rootDir);
            ProfileManager profileManager = new ProfileManager(rootDir);

            // Set app icon early so it shows correctly during profile picker
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
                    taskbar.setIconImage(UIUtils.createAppIcon(128));
            }

            // ── Step 4: Resolve profile ───────────────────────────────────────
            UserProfile activeProfile = null;

            java.util.List<UserProfile> profiles = profileManager.loadProfiles();

            if (profiles.isEmpty()) {
                // No profiles yet — create a named subfolder and point the profile at it
                DataStorage tempStorage = new DataStorage();
                String name = tempStorage.loadEmployeeInfo().getFullName();
                if (name == null || name.isBlank()) name = "Me";
                // Create Reconciled/Shane_Bolenback/ subfolder
                String safeName = name.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim().replace(" ", "_");
                String userDataPath = rootDir + safeName + File.separator;
                new java.io.File(userDataPath).mkdirs();
                // Move any data that onboarding wrote into rootDir down into the subfolder
                for (String sub : new String[]{"settings", "logs", "receipts", "km", "invoices"}) {
                    java.io.File src = new java.io.File(rootDir + sub);
                    java.io.File dst = new java.io.File(userDataPath + sub);
                    if (src.exists()) src.renameTo(dst);
                }
                DataStorage.saveDataDirectory(userDataPath);
                activeProfile = new UserProfile(name, userDataPath, "#3B82F6");
                java.util.List<UserProfile> newList = new java.util.ArrayList<>();
                newList.add(activeProfile);
                profileManager.saveProfiles(newList);
                ProfileManager.saveActiveProfileId(activeProfile.getId());
            } else if (profiles.size() == 1) {
                activeProfile = profiles.get(0);
                ProfileManager.saveActiveProfileId(activeProfile.getId());
            } else {
                ProfilePickerDialog picker = new ProfilePickerDialog(null, profileManager);
                picker.setVisible(true);
                activeProfile = picker.getChosen();
                if (activeProfile == null) System.exit(0);
            }

            // ── Step 5: Point DataStorage at chosen profile ───────────────────
            DataStorage.saveDataDirectory(activeProfile.getDataPath());

            final UserProfile finalProfile = activeProfile;
            final ProfileManager finalPM = profileManager;

            // ── Step 6: Launch ────────────────────────────────────────────────
            MainFrame frame = new MainFrame(finalProfile, finalPM);

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                    desktop.setAboutHandler(e -> JOptionPane.showMessageDialog(frame,
                        "Reconciled\nVersion: " + getAppVersion()
                            + "\nAuthor: ShaneBee\n\nBookkeeping for the self-employed.",
                        "About Reconciled",
                        JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon(UIUtils.createAppIcon(64))));
                }
            }
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE))
                    taskbar.setIconImage(UIUtils.createAppIcon(128));
            }
            frame.setVisible(true);
        });
    }

    private static void setupLaf() {
        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 12);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("TextComponent.arc", 12);
        Color accent = (Color) UIManager.get("App.accent");
        Color selection = new Color(239, 246, 255);
        UIManager.put("Component.accentColor", accent);
        UIManager.put("Component.focusColor", new Color(191, 219, 254));
        UIManager.put("Button.focusedBorderColor", accent);
        UIManager.put("Selection.background", selection);
        UIManager.put("Selection.foreground", new Color(30, 64, 175));
        UIManager.put("List.selectionBackground", selection);
        UIManager.put("List.selectionForeground", new Color(30, 64, 175));
        UIManager.put("Table.selectionBackground", selection);
        UIManager.put("Table.selectionForeground", new Color(30, 64, 175));
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.borderColor", new Color(226, 232, 240));
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", selection);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("MainContent.background", new Color(241, 245, 249));
        UIManager.put("TabbedPane.selectedBackground", Color.WHITE);
        UIManager.put("TableHeader.background", new Color(248, 250, 252));
        UIManager.put("TableHeader.separatorColor", new Color(226, 232, 240));
        UIManager.put("TableHeader.bottomSeparatorColor", new Color(226, 232, 240));
        UIManager.put("TableHeader.font", UIManager.getFont("Label.font").deriveFont(Font.BOLD));
        UIManager.put("TitlePane.background", new Color(64, 64, 64));
        UIManager.put("TitlePane.foreground", Color.WHITE);
        UIManager.put("TitlePane.buttonHoverBackground", new Color(80, 80, 80));
        UIManager.put("TitlePane.buttonPressedBackground", new Color(100, 100, 100));
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
    }

    private static String getAppVersion() {
        try (var stream = Main.class.getResourceAsStream("/version.properties")) {
            if (stream == null) return "unknown";
            var props = new java.util.Properties();
            props.load(stream);
            return props.getProperty("version", "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

}
