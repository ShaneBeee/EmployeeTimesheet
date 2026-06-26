package com.github.shanebeee.et.storage;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.ExpenseCategory;
import com.github.shanebeee.et.model.Expenditure;
import com.github.shanebeee.et.model.KmOdometer;
import com.github.shanebeee.et.model.KmTrip;
import com.github.shanebeee.et.model.LogEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.awt.Desktop;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {

    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + "ShaneApps" + File.separator + "EmployeeTimesheet" + File.separator;
    private static final String SETTINGS_DIR = BASE_DIR + "settings/";
    private static final String BOSSES_FILE      = SETTINGS_DIR + "bosses.json";
    private static final String EMPLOYEE_FILE    = SETTINGS_DIR + "employee.json";
    private static final String SETTINGS_FILE    = SETTINGS_DIR + "settings.json";
    private static final String CATEGORIES_FILE  = SETTINGS_DIR + "expense_categories.json";
    private static final String LOGS_DIR     = BASE_DIR + "logs/";
    private static final String INVOICES_DIR = BASE_DIR + "invoices/";
    private static final String RECEIPTS_DIR = BASE_DIR + "receipts/";
    private static final String KM_DIR       = BASE_DIR + "km/";

    private final Gson gson;

    public DataStorage() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(LOGS_DIR));
            Files.createDirectories(Paths.get(SETTINGS_DIR));
            Files.createDirectories(Paths.get(INVOICES_DIR));
            Files.createDirectories(Paths.get(RECEIPTS_DIR));
            Files.createDirectories(Paths.get(KM_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Expense Categories
    public List<ExpenseCategory> loadExpenseCategories() {
        List<ExpenseCategory> cats = loadList(CATEGORIES_FILE,
            new TypeToken<List<ExpenseCategory>>() {}.getType());
        if (cats.isEmpty()) {
            cats = defaultCategories();
            saveExpenseCategories(cats);
        }
        return cats;
    }

    public void saveExpenseCategories(List<ExpenseCategory> categories) {
        saveToFile(CATEGORIES_FILE, categories);
    }

    /** Resolves an Expenditure to its ExpenseCategory.
     *  Tries categoryId first, falls back to matching the legacy enum label. */
    public ExpenseCategory resolveCategory(Expenditure exp, List<ExpenseCategory> allCats) {
        if (exp.getCategoryId() != null) {
            return allCats.stream()
                .filter(c -> c.getId().equals(exp.getCategoryId()))
                .findFirst().orElse(null);
        }
        if (exp.getCategory() != null) {
            String label = exp.getCategory().getLabel();
            return allCats.stream()
                .filter(c -> c.getLabel().equalsIgnoreCase(label))
                .findFirst().orElse(null);
        }
        return null;
    }

    private List<ExpenseCategory> defaultCategories() {
        List<ExpenseCategory> cats = new ArrayList<>();
        cats.add(new ExpenseCategory("Vehicle",           "Gas, maintenance, insurance, repairs",   "9281", "#EF4444", true));
        cats.add(new ExpenseCategory("Phone & Internet",  "Cell phone, home internet",              "9270", "#3B82F6", true));
        cats.add(new ExpenseCategory("Home Office",       "Rent, utilities (% of home)",            "9945", "#8B5CF6", true));
        cats.add(new ExpenseCategory("Meals & Entertain.","50% deductible by CRA",                 "8523", "#F59E0B", true));
        cats.add(new ExpenseCategory("Office Supplies",   "Paper, ink, tools, software",           "8810", "#14B8A6", true));
        cats.add(new ExpenseCategory("Professional Fees", "Accountant, lawyer, subscriptions",     "8860", "#6366F1", true));
        cats.add(new ExpenseCategory("Advertising",       "Business cards, online ads, marketing", "8520", "#EC4899", true));
        cats.add(new ExpenseCategory("Other",             "Miscellaneous business expenses",        "9270", "#94A3B8", true));
        return cats;
    }

    // Bosses
    public List<Boss> loadBosses() {
        return loadList(BOSSES_FILE, new TypeToken<List<Boss>>() {
        }.getType());
    }

    public void saveBosses(List<Boss> bosses) {
        saveToFile(BOSSES_FILE, bosses);
    }

    // Expenditures
    public List<Expenditure> loadExpenditures(String year) {
        ensureYearDir(year);
        String file = RECEIPTS_DIR + year + "/expenses.json";
        return loadList(file, new TypeToken<List<Expenditure>>() {
        }.getType());
    }

    public void saveExpenditures(String year, List<Expenditure> expenditures) {
        ensureYearDir(year);
        String file = RECEIPTS_DIR + year + "/expenses.json";
        saveToFile(file, expenditures);
    }

    // Receipt file helpers

    /**
     * Copies a receipt file into the organized folder structure and returns the
     * relative path (relative to RECEIPTS_DIR) to store on the Expenditure.
     * e.g. "2025/06/abc123_phone-bill_1.pdf"
     */
    public String addReceiptFile(File sourceFile, String year, String month, String expenseId, String description, int index) {
        ensureMonthDir(year, month);
        String ext = "";
        int dot = sourceFile.getName().lastIndexOf('.');
        if (dot >= 0) ext = sourceFile.getName().substring(dot);
        String safeName = description.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        if (safeName.length() > 30) safeName = safeName.substring(0, 30);
        String filename = expenseId.substring(0, 8) + "_" + safeName + "_" + index + ext;
        Path dest = Paths.get(RECEIPTS_DIR, year, month, filename);
        try {
            Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return year + "/" + month + "/" + filename;
    }

    /**
     * Returns the absolute File for a stored relative receipt path.
     */
    public File getReceiptFile(String relativePath) {
        return new File(RECEIPTS_DIR + relativePath);
    }

    /**
     * Opens a receipt in the system default viewer (Preview on macOS).
     */
    public void openReceiptFile(String relativePath) {
        File f = getReceiptFile(relativePath);
        if (!f.exists()) return;
        try {
            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes all receipt files for a given expenditure.
     */
    public void deleteReceiptFiles(Expenditure exp) {
        for (String rel : exp.getReceiptFiles()) {
            File f = getReceiptFile(rel);
            if (f.exists()) f.delete();
        }
    }

    private void ensureYearDir(String year) {
        try {
            Files.createDirectories(Paths.get(RECEIPTS_DIR, year));
        } catch (IOException ignored) {
        }
    }

    private void ensureMonthDir(String year, String month) {
        try {
            Files.createDirectories(Paths.get(RECEIPTS_DIR, year, month));
        } catch (IOException ignored) {
        }
    }

    public String getBaseDir() {
        return BASE_DIR;
    }

    public String getReceiptsDir() {
        return RECEIPTS_DIR;
    }

    // KM Trips
    public List<KmTrip> loadKmTrips(String year) {
        ensureKmYearDir(year);
        return loadList(KM_DIR + year + "/trips.json",
            new TypeToken<List<KmTrip>>() {}.getType());
    }

    public void saveKmTrips(String year, List<KmTrip> trips) {
        ensureKmYearDir(year);
        saveToFile(KM_DIR + year + "/trips.json", trips);
    }

    // KM Odometer
    public KmOdometer loadKmOdometer(String year) {
        ensureKmYearDir(year);
        KmOdometer o = loadObject(KM_DIR + year + "/odometer.json", KmOdometer.class);
        return o != null ? o : new KmOdometer();
    }

    public void saveKmOdometer(String year, KmOdometer odometer) {
        ensureKmYearDir(year);
        saveToFile(KM_DIR + year + "/odometer.json", odometer);
    }

    private void ensureKmYearDir(String year) {
        try { Files.createDirectories(Paths.get(KM_DIR, year)); } catch (IOException ignored) {}
    }

    /**
     * Upserts a KmTrip linked to a LogEntry. Replaces any existing trip with the
     * same sourceLogId, or adds a new one. Call this whenever a KILOMETER LogEntry
     * is saved (add or edit).
     */
    public void upsertAutoKmTrip(String year, KmTrip trip) {
        List<KmTrip> trips = loadKmTrips(year);
        trips.removeIf(t -> trip.getSourceLogId() != null
            && trip.getSourceLogId().equals(t.getSourceLogId()));
        trips.add(trip);
        trips.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        saveKmTrips(year, trips);
    }

    /**
     * Removes any KmTrip linked to the given LogEntry id.
     * Call this when a KILOMETER LogEntry is deleted.
     */
    public void removeAutoKmTrip(String year, String sourceLogId) {
        List<KmTrip> trips = loadKmTrips(year);
        trips.removeIf(t -> sourceLogId.equals(t.getSourceLogId()));
        saveKmTrips(year, trips);
    }

    // Employee Info
    public EmployeeInfo loadEmployeeInfo() {
        EmployeeInfo info = loadObject(EMPLOYEE_FILE, EmployeeInfo.class);
        return info != null ? info : new EmployeeInfo();
    }

    public void saveEmployeeInfo(EmployeeInfo info) {
        saveToFile(EMPLOYEE_FILE, info);
    }

    // Logs
    public List<LogEntry> loadLogs(String yearMonth) { // yearMonth as "yyyy-MM"
        String file = LOGS_DIR + yearMonth + ".json";
        return loadList(file, new TypeToken<List<LogEntry>>() {
        }.getType());
    }

    public void saveLogs(String yearMonth, List<LogEntry> logs) {
        String file = LOGS_DIR + yearMonth + ".json";
        saveToFile(file, logs);
    }

    // Generic helpers
    private <T> List<T> loadList(String filePath, Type type) {
        File file = new File(filePath);
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            List<T> list = gson.fromJson(reader, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private <T> T loadObject(String filePath, Class<T> clazz) {
        File file = new File(filePath);
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(String filePath, Object data) {
        try (Writer writer = new FileWriter(filePath)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getNextInvoiceNumber() {
        // Simple implementation: count invoices or store a counter
        // For now, let's store a simple settings object
        Settings settings = loadSettings();
        int num = settings.lastInvoiceNumber + 1;
        settings.lastInvoiceNumber = num;
        saveSettings(settings);
        return num;
    }

    private Settings loadSettings() {
        Settings s = loadObject(SETTINGS_FILE, Settings.class);
        return s != null ? s : new Settings();
    }

    private void saveSettings(Settings s) {
        saveToFile(SETTINGS_FILE, s);
    }

    public String getTheme() {
        return "light";
    }

    public void setTheme(String theme) {
    }

    public String getDefaultStartTime() {
        return loadSettings().defaultStartTime;
    }

    public void setDefaultStartTime(String startTime) {
        Settings s = loadSettings();
        s.defaultStartTime = startTime;
        saveSettings(s);
    }

    public String getDefaultEndTime() {
        return loadSettings().defaultEndTime;
    }

    public void setDefaultEndTime(String endTime) {
        Settings s = loadSettings();
        s.defaultEndTime = endTime;
        saveSettings(s);
    }

    public String getInvoicePath(Boss boss, int invNum) {
        return INVOICES_DIR + "Invoice_" + boss.getName().replace(" ", "_") + "_" + invNum + ".pdf";
    }

    public String getSummaryPath(String yearMonth) {
        return INVOICES_DIR + "Summary_" + yearMonth + ".pdf";
    }

    private static class Settings {
        int lastInvoiceNumber = 0;
        String defaultStartTime = "11:00";
        String defaultEndTime = "15:00";
    }

}
