package com.github.shanebeee.et;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.github.shanebeee.et.storage.DataStorage;
import com.github.shanebeee.et.util.UIUtils;
import com.github.shanebeee.et.view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Set application name for macOS - MUST BE SET BEFORE AWT/SWING LOADS
        System.setProperty("apple.awt.application.name", "Employee Timesheet");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Employee Timesheet");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Setup Look and Feel
        setupLaf();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();

            // macOS specific UI integration
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                // Set Custom About Dialog
                if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                    desktop.setAboutHandler(e -> {
                        JOptionPane.showMessageDialog(frame,
                                "Employee Timesheet\nVersion 1.0.0\n\nProfessional time logging and invoicing.",
                                "About Employee Timesheet",
                                JOptionPane.INFORMATION_MESSAGE,
                                new ImageIcon(UIUtils.createAppIcon(64)));
                    });
                }
            }

            // Set Taskbar icon for macOS
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(UIUtils.createAppIcon(128));
                }
            }

            frame.setVisible(true);
        });
    }

    private static void setupLaf() {
        // Global UI tweaks
        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 12);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("TextComponent.arc", 12);

        // Color palette (Professional, Vibrant)
        Color accentColor = new Color(59, 130, 246); // Vibrant Blue (Tailwind Blue 500)
        Color selectionColor = new Color(239, 246, 255); // Very light blue
        
        UIManager.put("Component.accentColor", accentColor);
        UIManager.put("Component.focusColor", new Color(191, 219, 254)); // Light blue focus
        UIManager.put("Button.focusedBorderColor", accentColor);
        UIManager.put("Selection.background", selectionColor);
        UIManager.put("Selection.foreground", new Color(30, 64, 175)); // Darker blue for selected text
        UIManager.put("List.selectionBackground", selectionColor);
        UIManager.put("List.selectionForeground", new Color(30, 64, 175));
        UIManager.put("Table.selectionBackground", selectionColor);
        UIManager.put("Table.selectionForeground", new Color(30, 64, 175));
        
        // Input fields styling
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.borderColor", new Color(226, 232, 240));
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", selectionColor);
        
        // Panel backgrounds
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("MainContent.background", new Color(248, 250, 252));
        
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
    }
}
