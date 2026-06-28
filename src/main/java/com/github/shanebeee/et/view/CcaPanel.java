package com.github.shanebeee.et.view;

import com.github.shanebeee.et.model.CcaAsset;
import com.github.shanebeee.et.storage.DataStorage;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CcaPanel extends JPanel {

    private static final Color ACCENT       = new Color(99, 102, 241);
    private static final Color NAVY         = new Color(30,  41,  59);
    private static final Color SLATE        = new Color(100, 116, 139);
    private static final Color MUTED        = new Color(148, 163, 184);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color LIGHT_BG     = new Color(241, 245, 249);

    private static final double[] COL_WEIGHTS = { 2.5, 1.0, 1.0, 1.0, 1.0, 1.0 };
    private static final String[] COL_HEADERS = { "DESCRIPTION", "CLASS", "COST", "OPENING UCC", "DEDUCTION", "CLOSING UCC" };

    private static final String[][] PRESET_CLASSES = {
        { "Class 50",  "0.55", "Computers, laptops, tablets (55%)" },
        { "Class 10",  "0.30", "Vehicles (30%)" },
        { "Class 8",   "0.20", "Equipment, furniture, tools (20%)" },
        { "Class 12",  "1.00", "Small tools under $500 (100%)" },
        { "Class 10.1","0.30", "Passenger vehicles over cost limit (30%)" },
        { "Class 14.1","0.05", "Intangibles, goodwill (5%)" },
    };

    private final DataStorage storage;
    private List<CcaAsset> assets;
    private JPanel tablePanel;
    private JLabel totalDeductionLabel;
    private int previewYear;

    public CcaPanel(DataStorage storage) {
        this.storage = storage;
        this.previewYear = LocalDate.now().getYear();
        this.assets = storage.loadCcaAssets();
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Left: total deduction summary
        JPanel summaryLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        summaryLeft.setOpaque(false);
        totalDeductionLabel = new JLabel();
        totalDeductionLabel.setFont(totalDeductionLabel.getFont().deriveFont(Font.BOLD, 13f));
        totalDeductionLabel.setForeground(NAVY);
        summaryLeft.add(totalDeductionLabel);

        // Right: year spinner + Add button
        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBar.setOpaque(false);

        JLabel yearLbl = new JLabel("Preview year:");
        yearLbl.setFont(yearLbl.getFont().deriveFont(Font.PLAIN, 12f));
        yearLbl.setForeground(SLATE);

        SpinnerNumberModel yearModel = new SpinnerNumberModel(previewYear, 2020, LocalDate.now().getYear() + 5, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);
        yearSpinner.setPreferredSize(new Dimension(80, 28));
        JSpinner.NumberEditor yearEditor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        yearSpinner.setEditor(yearEditor);
        yearSpinner.addChangeListener(e -> { previewYear = (int) yearSpinner.getValue(); refreshList(); });

        JButton btnAdd = new JButton("+ Add Asset");
        btnAdd.putClientProperty("JButton.buttonType", "roundRect");
        btnAdd.setBackground(ACCENT);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD, 12f));
        btnAdd.addActionListener(e -> showAssetDialog(null));

        rightBar.add(yearLbl);
        rightBar.add(yearSpinner);
        rightBar.add(btnAdd);
        topBar.add(summaryLeft, BorderLayout.WEST);
        topBar.add(rightBar,    BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Table (header + rows share one GridBagLayout) ─────────────────────
        tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(tablePanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);

        refreshList();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void refreshList() {
        tablePanel.removeAll();

        // Row 0: column headers
        GridBagConstraints hc = new GridBagConstraints();
        hc.gridy = 0; hc.fill = GridBagConstraints.HORIZONTAL;
        hc.insets = new Insets(0, 6, 8, 6);
        for (int col = 0; col < COL_HEADERS.length; col++) {
            hc.gridx = col; hc.weightx = COL_WEIGHTS[col];
            JLabel lbl = new JLabel(COL_HEADERS[col]);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 9f));
            lbl.setForeground(MUTED);
            if (col > 0) lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            tablePanel.add(lbl, hc);
        }

        if (assets.isEmpty()) {
            GridBagConstraints ec = new GridBagConstraints();
            ec.gridy = 1; ec.gridx = 0; ec.gridwidth = 6;
            ec.fill = GridBagConstraints.HORIZONTAL;
            ec.insets = new Insets(40, 0, 0, 0);
            JLabel empty = new JLabel("No CCA assets yet. Click \"+ Add Asset\" to get started.", JLabel.CENTER);
            empty.setForeground(MUTED);
            empty.setFont(empty.getFont().deriveFont(Font.ITALIC, 13f));
            tablePanel.add(empty, ec);
            totalDeductionLabel.setText("No assets");
        } else {
            double totalDeduction = 0;
            for (int i = 0; i < assets.size(); i++) {
                addAssetCard(assets.get(i), i + 1);
                totalDeduction += assets.get(i).deductionForYear(previewYear);
            }
            // Filler to push rows upward
            GridBagConstraints fc = new GridBagConstraints();
            fc.gridy = assets.size() + 1; fc.gridx = 0; fc.gridwidth = 6;
            fc.weighty = 1.0; fc.fill = GridBagConstraints.BOTH;
            tablePanel.add(new JPanel() {{ setOpaque(false); }}, fc);
            totalDeductionLabel.setText(String.format("Total deduction (%d): $%.2f", previewYear, totalDeduction));
        }

        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private void addAssetCard(CcaAsset asset, int gridy) {
        double opening    = asset.openingUccForYear(previewYear);
        double deduction  = asset.deductionForYear(previewYear);
        double closing    = asset.closingUccForYear(previewYear);

        final boolean[] hovered = {false};
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? new Color(249, 250, 251) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered[0] = true;  card.repaint(); }
            public void mouseExited (MouseEvent e) { hovered[0] = false; card.repaint(); }
            public void mouseClicked(MouseEvent e) { showAssetDialog(asset); }
        });

        GridBagConstraints ic = new GridBagConstraints();
        ic.gridy = 0; ic.fill = GridBagConstraints.HORIZONTAL;

        // Col 0: description + date
        ic.gridx = 0; ic.weightx = COL_WEIGHTS[0];
        ic.insets = new Insets(12, 14, 12, 6);
        JPanel descPanel = new JPanel(new BorderLayout(0, 2));
        descPanel.setOpaque(false);
        JLabel descLbl = new JLabel(asset.getDescription() != null ? asset.getDescription() : "—");
        descLbl.setFont(descLbl.getFont().deriveFont(Font.BOLD, 13f));
        descLbl.setForeground(NAVY);
        JLabel dateLbl = new JLabel(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : "");
        dateLbl.setFont(dateLbl.getFont().deriveFont(Font.PLAIN, 10f));
        dateLbl.setForeground(MUTED);
        descPanel.add(descLbl, BorderLayout.NORTH);
        descPanel.add(dateLbl, BorderLayout.SOUTH);
        card.add(descPanel, ic);

        ic.insets = new Insets(12, 6, 12, 6);

        // Col 1: class + rate
        ic.gridx = 1; ic.weightx = COL_WEIGHTS[1];
        JPanel classPanel = new JPanel(new BorderLayout(0, 2));
        classPanel.setOpaque(false);
        JLabel classLbl = new JLabel(asset.getAssetClass() != null ? asset.getAssetClass() : "—");
        classLbl.setFont(classLbl.getFont().deriveFont(Font.PLAIN, 12f));
        classLbl.setForeground(NAVY);
        classLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel rateLbl = new JLabel(String.format("%.0f%%", asset.getClassRate() * 100));
        rateLbl.setFont(rateLbl.getFont().deriveFont(Font.PLAIN, 10f));
        rateLbl.setForeground(MUTED);
        rateLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        classPanel.add(classLbl, BorderLayout.NORTH);
        classPanel.add(rateLbl,  BorderLayout.SOUTH);
        card.add(classPanel, ic);

        // Col 2: cost
        ic.gridx = 2; ic.weightx = COL_WEIGHTS[2];
        card.add(amountLabel(String.format("$%.2f", asset.getCost())), ic);

        // Col 3: opening UCC
        ic.gridx = 3; ic.weightx = COL_WEIGHTS[3];
        card.add(amountLabel(opening > 0 ? String.format("$%.2f", opening) : "—"), ic);

        // Col 4: deduction
        ic.gridx = 4; ic.weightx = COL_WEIGHTS[4];
        JLabel deductLbl = new JLabel(deduction > 0 ? String.format("-$%.2f", deduction) : "—", SwingConstants.RIGHT);
        deductLbl.setFont(deductLbl.getFont().deriveFont(Font.BOLD, 12f));
        deductLbl.setForeground(deduction > 0 ? ACCENT : MUTED);
        card.add(deductLbl, ic);

        // Col 5: closing UCC
        ic.gridx = 5; ic.weightx = COL_WEIGHTS[5];
        ic.insets = new Insets(12, 6, 12, 14);
        card.add(amountLabel(closing > 0 ? String.format("$%.2f", closing) : "—"), ic);

        // Place card as full-width spanning cell
        GridBagConstraints rc = new GridBagConstraints();
        rc.gridy = gridy; rc.gridx = 0; rc.gridwidth = 6;
        rc.weightx = 1.0; rc.fill = GridBagConstraints.HORIZONTAL;
        rc.insets = new Insets(0, 0, 6, 0);
        tablePanel.add(card, rc);
    }

    private JLabel amountLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.RIGHT);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(NAVY);
        return lbl;
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────

    private void showAssetDialog(CcaAsset existing) {
        boolean isNew = existing == null;
        CcaAsset asset = isNew ? new CcaAsset() : existing;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Add CCA Asset" : "Edit Asset", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(460, 620);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));

        // ── Header ───────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel headerTitle = new JLabel(isNew ? "New CCA Asset"
            : (asset.getDescription() != null ? asset.getDescription() : "Edit Asset"));
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 16f));
        headerTitle.setForeground(NAVY);
        header.add(headerTitle, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);

        JTextField descField = new JTextField(asset.getDescription() != null ? asset.getDescription() : "");
        form.add(formLabel("Description"), gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(descField, gbc); gbc.gridy++;

        JButton dateBtn = new JButton(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : LocalDate.now().toString());
        dateBtn.putClientProperty("JButton.buttonType", "roundRect");
        dateBtn.setBackground(LIGHT_BG); dateBtn.setForeground(NAVY);
        dateBtn.setHorizontalAlignment(SwingConstants.LEFT);
        dateBtn.addActionListener(e -> DatePicker.showPicker(dialog, dateBtn));
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(formLabel("Purchase Date"), gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(dateBtn, gbc); gbc.gridy++;

        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(formLabel("CCA Class"), gbc); gbc.gridy++;

        String[] classNames = new String[PRESET_CLASSES.length + 1];
        classNames[0] = "Custom";
        for (int i = 0; i < PRESET_CLASSES.length; i++) classNames[i + 1] = PRESET_CLASSES[i][0];
        JComboBox<String> classCombo = new JComboBox<>(classNames);
        classCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index <= 0 || value == null) {
                    setText(value != null ? value.toString() : "");
                    setFont(getFont().deriveFont(Font.PLAIN, 13f));
                } else {
                    String[] p = PRESET_CLASSES[index - 1];
                    setText("<html><b>" + p[0] + "</b><br><span style='color:#94a3b8;font-size:10px'>" + p[2] + "</span></html>");
                }
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });

        JTextField classField = new JTextField(asset.getAssetClass() != null ? asset.getAssetClass() : "");
        JTextField rateField  = new JTextField(asset.getClassRate() > 0 ? String.format("%.2f", asset.getClassRate()) : "");
        JTextField costField  = new JTextField(asset.getCost() > 0 ? String.format("%.2f", asset.getCost()) : "");

        JLabel classHint = new JLabel(" ");
        classHint.setFont(classHint.getFont().deriveFont(Font.ITALIC, 11f));
        classHint.setForeground(MUTED);

        JLabel classRateLabel = formLabel("Class Name & Rate");
        JPanel classRateRow = new JPanel(new GridBagLayout());
        classRateRow.setOpaque(false);
        GridBagConstraints cr = new GridBagConstraints();
        cr.fill = GridBagConstraints.HORIZONTAL; cr.gridy = 0;
        cr.weightx = 2.0; cr.gridx = 0; cr.insets = new Insets(0, 0, 0, 8);
        classRateRow.add(classField, cr);
        cr.weightx = 1.0; cr.gridx = 1; cr.insets = new Insets(0, 0, 0, 0);
        JPanel rateWrap = new JPanel(new BorderLayout(4, 0));
        rateWrap.setOpaque(false);
        JLabel ratePct = new JLabel("Rate (0–1):");
        ratePct.setFont(ratePct.getFont().deriveFont(Font.PLAIN, 10f));
        ratePct.setForeground(MUTED);
        rateWrap.add(ratePct, BorderLayout.WEST);
        rateWrap.add(rateField, BorderLayout.CENTER);
        classRateRow.add(rateWrap, cr);

        if (asset.getAssetClass() != null) {
            for (int i = 0; i < PRESET_CLASSES.length; i++) {
                if (PRESET_CLASSES[i][0].equals(asset.getAssetClass())) {
                    classCombo.setSelectedIndex(i + 1);
                    classHint.setText(PRESET_CLASSES[i][2]);
                    break;
                }
            }
        }
        boolean startCustom = (classCombo.getSelectedIndex() == 0);
        classRateLabel.setVisible(startCustom);
        classRateRow.setVisible(startCustom);

        classCombo.addActionListener(e -> {
            int sel = classCombo.getSelectedIndex();
            boolean custom = (sel == 0);
            if (!custom) {
                String[] p = PRESET_CLASSES[sel - 1];
                classField.setText(p[0]); rateField.setText(p[1]); classHint.setText(p[2]);
            } else {
                classField.setText(""); rateField.setText(""); classHint.setText("Enter a class name and rate below");
            }
            classRateLabel.setVisible(custom); classRateRow.setVisible(custom);
            dialog.revalidate(); dialog.repaint();
        });

        gbc.insets = new Insets(0, 0, 2, 0);
        form.add(classCombo, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(classHint, gbc); gbc.gridy++;
        form.add(classRateLabel, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(classRateRow, gbc); gbc.gridy++;

        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(formLabel("Capital Cost ($)"), gbc); gbc.gridy++;
        form.add(costField, gbc); gbc.gridy++;
        JLabel costHint = new JLabel("Enter the pre-tax cost (excluding GST — claimed separately as an ITC).");
        costHint.setFont(costHint.getFont().deriveFont(Font.ITALIC, 10f));
        costHint.setForeground(MUTED);
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(costHint, gbc); gbc.gridy++;

        // ── Receipts ─────────────────────────────────────────────────────────
        List<String> pendingReceipts = new ArrayList<>(asset.getReceiptFiles());
        Runnable[] refreshHolder = {null};

        JPanel receiptsPanel = new JPanel();
        receiptsPanel.setLayout(new BoxLayout(receiptsPanel, BoxLayout.Y_AXIS));
        receiptsPanel.setOpaque(false);

        java.util.function.Consumer<List<java.io.File>> addFiles = files -> {
            String dateStr = dateBtn.getText();
            String yr  = dateStr.length() >= 4 ? dateStr.substring(0, 4) : String.valueOf(LocalDate.now().getYear());
            String mo  = dateStr.length() >= 7 ? dateStr.substring(5, 7) : "01";
            String desc = descField.getText().trim().isEmpty() ? "cca" : descField.getText().trim();
            for (java.io.File f : files) {
                String n = f.getName().toLowerCase();
                if (!n.endsWith(".jpg") && !n.endsWith(".jpeg") && !n.endsWith(".png")
                    && !n.endsWith(".pdf") && !n.endsWith(".heic")) continue;
                String rel = storage.addReceiptFile(f, yr, mo, asset.getId(), desc, pendingReceipts.size() + 1);
                if (rel != null) pendingReceipts.add(rel);
            }
            SwingUtilities.invokeLater(() -> { if (refreshHolder[0] != null) refreshHolder[0].run(); });
        };

        final boolean[] dropHover = {false};
        JPanel dropZone = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dropHover[0] ? new Color(99, 102, 241, 18) : LIGHT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                float[] dash = {6f, 4f};
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
                g2.setColor(dropHover[0] ? ACCENT : BORDER_COLOR);
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        dropZone.setOpaque(false);
        dropZone.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        dropZone.setPreferredSize(new Dimension(0, 52));
        dropZone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        JLabel dropLabel = new JLabel("Drop invoice / receipt here  ·  jpg, png, pdf, heic", JLabel.CENTER);
        dropLabel.setFont(dropLabel.getFont().deriveFont(Font.PLAIN, 11f));
        dropLabel.setForeground(MUTED);
        dropZone.add(dropLabel, BorderLayout.CENTER);
        dropZone.setDropTarget(new java.awt.dnd.DropTarget() {
            public synchronized void dragEnter(java.awt.dnd.DropTargetDragEvent e) { dropHover[0] = true;  dropZone.repaint(); }
            public synchronized void dragExit (java.awt.dnd.DropTargetEvent e)     { dropHover[0] = false; dropZone.repaint(); }
            @SuppressWarnings("unchecked")
            public synchronized void drop(java.awt.dnd.DropTargetDropEvent e) {
                dropHover[0] = false; dropZone.repaint();
                try {
                    e.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                    addFiles.accept((List<java.io.File>) e.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor));
                    e.dropComplete(true);
                } catch (Exception ex) { e.rejectDrop(); }
            }
        });

        Runnable refreshReceipts = () -> {
            receiptsPanel.removeAll();
            for (int i = 0; i < pendingReceipts.size(); i++) {
                final String rel = pendingReceipts.get(i);
                final int idx = i;
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                JButton openBtn = new JButton("📄 " + storage.getReceiptFile(rel).getName());
                openBtn.putClientProperty("JButton.buttonType", "roundRect");
                openBtn.setFont(openBtn.getFont().deriveFont(Font.PLAIN, 11f));
                openBtn.setHorizontalAlignment(SwingConstants.LEFT);
                openBtn.addActionListener(ev -> storage.openReceiptFile(rel));
                JButton removeBtn = new JButton("✕");
                removeBtn.putClientProperty("JButton.buttonType", "roundRect");
                removeBtn.setFont(removeBtn.getFont().deriveFont(Font.PLAIN, 10f));
                removeBtn.setPreferredSize(new Dimension(28, 24));
                removeBtn.addActionListener(ev -> {
                    java.io.File f = storage.getReceiptFile(rel);
                    if (f.exists()) f.delete();
                    pendingReceipts.remove(idx);
                    SwingUtilities.invokeLater(() -> { if (refreshHolder[0] != null) refreshHolder[0].run(); });
                });
                row.add(openBtn, BorderLayout.CENTER);
                row.add(removeBtn, BorderLayout.EAST);
                receiptsPanel.add(row);
                receiptsPanel.add(Box.createVerticalStrut(4));
            }
            receiptsPanel.revalidate(); receiptsPanel.repaint();
        };
        refreshHolder[0] = refreshReceipts;
        refreshReceipts.run();

        JPanel receiptsContainer = new JPanel();
        receiptsContainer.setLayout(new BoxLayout(receiptsContainer, BoxLayout.Y_AXIS));
        receiptsContainer.setOpaque(false);
        receiptsContainer.add(receiptsPanel);
        receiptsContainer.add(Box.createVerticalStrut(6));
        receiptsContainer.add(dropZone);

        JButton btnAddReceipt = new JButton("+ Attach Invoice / Receipt");
        btnAddReceipt.putClientProperty("JButton.buttonType", "roundRect");
        btnAddReceipt.setFont(btnAddReceipt.getFont().deriveFont(Font.PLAIN, 12f));
        btnAddReceipt.addActionListener(e -> {
            java.awt.FileDialog fd = new java.awt.FileDialog((Frame) SwingUtilities.getWindowAncestor(dialog), "Select Receipt", java.awt.FileDialog.LOAD);
            fd.setMultipleMode(true); fd.setVisible(true);
            java.io.File[] chosen = fd.getFiles();
            if (chosen != null && chosen.length > 0) addFiles.accept(java.util.Arrays.asList(chosen));
        });

        gbc.insets = new Insets(14, 0, 4, 0);
        form.add(formLabel("Invoice / Receipt"), gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(receiptsContainer, gbc); gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(btnAddReceipt, gbc);

        dialog.add(new JScrollPane(form) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(new Color(248, 250, 252));
        }}, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        JPanel footerLeft  = new JPanel(new FlowLayout(FlowLayout.LEFT,  10, 12)); footerLeft.setOpaque(false);
        JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12)); footerRight.setOpaque(false);

        JButton btnSave = new JButton(isNew ? "Add Asset" : "Save Changes");
        btnSave.putClientProperty("JButton.buttonType", "roundRect");
        btnSave.setBackground(ACCENT); btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            String desc = descField.getText().trim();
            if (desc.isBlank()) { JOptionPane.showMessageDialog(dialog, "Please enter a description.", "Missing Field", JOptionPane.WARNING_MESSAGE); return; }
            String className = classField.getText().trim();
            if (className.isBlank()) { JOptionPane.showMessageDialog(dialog, "Please select or enter a CCA class.", "Missing Field", JOptionPane.WARNING_MESSAGE); return; }
            double rate;
            try { rate = Double.parseDouble(rateField.getText().trim()); if (rate <= 0 || rate > 1) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Rate must be between 0 and 1 (e.g. 0.55 for 55%).", "Invalid Rate", JOptionPane.WARNING_MESSAGE); return; }
            double cost;
            try { cost = Double.parseDouble(costField.getText().trim()); if (cost <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dialog, "Please enter a valid cost greater than 0.", "Invalid Cost", JOptionPane.WARNING_MESSAGE); return; }
            asset.setDescription(desc);
            asset.setPurchaseDate(dateBtn.getText());
            asset.setAssetClass(className);
            asset.setClassRate(rate);
            asset.setCost(cost);
            asset.setReceiptFiles(new ArrayList<>(pendingReceipts));
            if (isNew) assets.add(asset);
            storage.saveCcaAssets(assets);
            dialog.dispose();
            refreshList();
        });

        if (!isNew) {
            JButton btnDelete = new JButton("Delete");
            btnDelete.putClientProperty("JButton.buttonType", "roundRect");
            btnDelete.setBackground((Color) UIManager.get("App.danger")); btnDelete.setForeground(Color.WHITE);
            btnDelete.addActionListener(e -> {
                int c = JOptionPane.showConfirmDialog(dialog,
                    "Delete \"" + asset.getDescription() + "\"?\nThis cannot be undone.",
                    "Delete Asset", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (c == JOptionPane.OK_OPTION) { assets.remove(asset); storage.saveCcaAssets(assets); dialog.dispose(); refreshList(); }
            });
            footerLeft.add(btnDelete);
        }

        JButton btnCancel = new JButton("Cancel");
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> dialog.dispose());
        footerRight.add(btnCancel);
        footerRight.add(btnSave);
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(btnSave);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancel");
        dialog.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dialog.dispose(); }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JLabel formLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(new Color(71, 85, 105));
        return lbl;
    }
}
