package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.UserProfile;
import com.github.shanebeee.et.storage.ProfileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Profile picker shown at launch when 2+ profiles exist.
 * The user clicks their name to load their data.
 */
public class ProfilePickerDialog extends JDialog {

    private static final Color NAVY  = new Color(30, 41, 59);
    private static final Color SLATE = new Color(100, 116, 139);
    private static final Color MUTED = new Color(148, 163, 184);
    private static final Color BG    = new Color(241, 245, 249);

    private final ProfileManager profileManager;
    private UserProfile chosen = null;

    public ProfilePickerDialog(Frame owner, ProfileManager profileManager) {
        super(owner, "Who's using Employee Timesheet?", true);
        this.profileManager = profileManager;
        setSize(480, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // must pick someone
        initUI();
    }

    private void initUI() {
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(32, 0, 20, 0));

        JLabel emoji = new JLabel("👤", JLabel.CENTER);
        emoji.setFont(emoji.getFont().deriveFont(40f));
        emoji.setAlignmentX(CENTER_ALIGNMENT);
        header.add(emoji);
        header.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("Select your profile", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(NAVY);
        title.setAlignmentX(CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Each profile has its own separate books", JLabel.CENTER);
        sub.setFont(sub.getFont().deriveFont(Font.PLAIN, 12f));
        sub.setForeground(SLATE);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        header.add(sub);
        add(header, BorderLayout.NORTH);

        // ── Profile grid ──────────────────────────────────────────────────────
        JPanel grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 32));

        refreshProfiles(grid);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // ── Footer: Add Profile ───────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton btnAdd = new JButton("+ Add Profile");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.PLAIN, 13f));
        btnAdd.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this,
                "Enter a name for the new profile:", "New Profile", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.isBlank()) return;
            profileManager.createProfile(name.trim());
            refreshProfiles(grid);
            grid.revalidate();
            grid.repaint();
        });
        footer.add(btnAdd);
        add(footer, BorderLayout.SOUTH);
    }

    private void refreshProfiles(JPanel grid) {
        grid.removeAll();
        List<UserProfile> profiles = profileManager.loadProfiles();

        for (UserProfile p : profiles) {
            grid.add(Box.createVerticalStrut(8));
            grid.add(makeProfileCard(p, grid));
        }
        grid.add(Box.createVerticalStrut(8));
    }

    private JPanel makeProfileCard(UserProfile profile, JPanel grid) {
        final boolean[] hovered = {false};

        JPanel card = new JPanel(new BorderLayout(16, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? new Color(239, 246, 255) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(hovered[0] ? new Color(59, 130, 246) : new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(hovered[0] ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Avatar
        Color avatarColor;
        try { avatarColor = Color.decode(profile.getAvatarColor()); }
        catch (Exception ex) { avatarColor = new Color(59, 130, 246); }
        final Color ac = avatarColor;

        String initials = profile.initials();
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ac);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials,
                    (getWidth() - fm.stringWidth(initials)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(44, 44));
        avatar.setMinimumSize(new Dimension(44, 44));

        // Name + path
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(profile.getName());
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 14f));
        nameLbl.setForeground(NAVY);
        JLabel pathLbl = new JLabel(shortenPath(profile.getDataPath()));
        pathLbl.setFont(pathLbl.getFont().deriveFont(Font.PLAIN, 10f));
        pathLbl.setForeground(MUTED);
        text.add(nameLbl);
        text.add(Box.createVerticalStrut(2));
        text.add(pathLbl);

        // Arrow
        JLabel arrow = new JLabel("→");
        arrow.setFont(arrow.getFont().deriveFont(Font.PLAIN, 18f));
        arrow.setForeground(MUTED);

        card.add(avatar, BorderLayout.WEST);
        card.add(text,   BorderLayout.CENTER);
        card.add(arrow,  BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); arrow.setForeground(new Color(59, 130, 246)); }
            public void mouseExited (MouseEvent e) { hovered[0] = false; card.repaint(); arrow.setForeground(MUTED); }
            public void mousePressed(MouseEvent e) {
                chosen = profile;
                ProfileManager.saveActiveProfileId(profile.getId());
                dispose();
            }
        });

        return card;
    }

    /** Returns the profile the user chose, or null if they closed the dialog. */
    public UserProfile getChosen() { return chosen; }

    /** Shortens a path to show just .../{last two segments} */
    private String shortenPath(String path) {
        if (path == null) return "";
        String p = path.endsWith(java.io.File.separator)
            ? path.substring(0, path.length() - 1) : path;
        String[] parts = p.split(java.util.regex.Pattern.quote(java.io.File.separator));
        if (parts.length <= 3) return path;
        return "\u2026/" + parts[parts.length - 2] + "/" + parts[parts.length - 1] + "/";
    }
}
