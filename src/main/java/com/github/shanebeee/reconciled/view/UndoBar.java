package com.github.shanebeee.reconciled.view;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * A transient snackbar-style undo bar.
 * <p>
 * Usage:
 * UndoBar bar = new UndoBar(parentPanel, "Entry deleted.", onCommit);
 * bar.show(() -> { /* restore logic *\/ });
 * <p>
 * When shown, the bar appears at the bottom of the parent panel.
 * If the user clicks Undo within 8 seconds, the restore runnable is called.
 * If they don't, the commit runnable is called and the bar disappears.
 */
public class UndoBar {

    private static final int TIMEOUT_MS = 8_000;
    private static final Color BG = new Color(30, 41, 59);
    private static final Color TEXT_COLOR = new Color(226, 232, 240);
    private static final Color UNDO_COLOR = new Color(99, 179, 237);

    private final JPanel parent;
    private final String message;
    private final Runnable onCommit;

    private JPanel barPanel;
    private Timer timer;
    private boolean committed = false;
    private boolean restored = false;

    /**
     * @param parent   The panel to attach the bar to (must use BorderLayout).
     * @param message  Short description e.g. "Work log entry deleted."
     * @param onCommit Called when the timeout expires without Undo being clicked.
     *                 This is where you actually write the delete to disk.
     */
    public UndoBar(JPanel parent, String message, Runnable onCommit) {
        this.parent = parent;
        this.message = message;
        this.onCommit = onCommit;
    }

    /**
     * Shows the undo bar. Call this immediately after removing the item from the UI.
     *
     * @param onRestore Called if the user clicks Undo — restore the item to the UI and storage here.
     */
    public void show(Runnable onRestore) {
        // Dismiss any existing bar first
        dismiss(false);

        barPanel = buildBar(onRestore);
        parent.add(barPanel, BorderLayout.SOUTH);
        parent.revalidate();
        parent.repaint();

        timer = new Timer(TIMEOUT_MS, e -> commit());
        timer.setRepeats(false);
        timer.start();
    }

    private JPanel buildBar(Runnable onRestore) {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(BG);
        bar.setPreferredSize(new Dimension(0, 48));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);
        JLabel msgLbl = new JLabel(message);
        msgLbl.setFont(msgLbl.getFont().deriveFont(Font.PLAIN, 13f));
        msgLbl.setForeground(TEXT_COLOR);
        left.add(msgLbl);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JButton undoBtn = new JButton("Undo");
        undoBtn.putClientProperty("JButton.buttonType", "roundRect");
        undoBtn.setBackground(new Color(49, 61, 79));
        undoBtn.setForeground(UNDO_COLOR);
        undoBtn.setFont(undoBtn.getFont().deriveFont(Font.BOLD, 12f));
        undoBtn.addActionListener(e -> {
            if (!committed) {
                restored = true;
                dismiss(false);
                onRestore.run();
            }
        });

        JButton dismissBtn = new JButton("✕");
        dismissBtn.putClientProperty("JButton.buttonType", "roundRect");
        dismissBtn.setBackground(new Color(49, 61, 79));
        dismissBtn.setForeground(new Color(148, 163, 184));
        dismissBtn.setFont(dismissBtn.getFont().deriveFont(Font.PLAIN, 11f));
        dismissBtn.addActionListener(e -> commit());

        right.add(undoBtn);
        right.add(dismissBtn);

        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void commit() {
        if (!restored && !committed) {
            committed = true;
            onCommit.run();
        }
        dismiss(false);
    }

    private void dismiss(boolean runCommit) {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        if (barPanel != null) {
            parent.remove(barPanel);
            parent.revalidate();
            parent.repaint();
            barPanel = null;
        }
        if (runCommit) commit();
    }

}
