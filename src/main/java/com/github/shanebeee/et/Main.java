package com.github.shanebeee.et;

import com.formdev.flatlaf.FlatDarkLaf;
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
        DataStorage storage = new DataStorage();
        String theme = storage.getTheme();
        
        // Global UI tweaks
        UIManager.put("Button.arc", 999);
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("TextComponent.arc", 10);
        
        try {
            if ("light".equals(theme)) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else if ("dark".equals(theme)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                // system default
                UIManager.setLookAndFeel(new FlatDarkLaf()); // Fallback
            }
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }
    }
}
