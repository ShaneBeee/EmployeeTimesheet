package com.github.shanebeee.et.storage;

import com.github.shanebeee.et.model.Boss;
import com.github.shanebeee.et.model.EmployeeInfo;
import com.github.shanebeee.et.model.Expenditure;
import com.github.shanebeee.et.model.LogEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DataStorage {

    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + "EmployeeTimesheet" + File.separator;
    private static final String SETTINGS_DIR = BASE_DIR + "settings/";
    private static final String BOSSES_FILE = SETTINGS_DIR + "bosses.json";
    private static final String EMPLOYEE_FILE = SETTINGS_DIR + "employee.json";
    private static final String SETTINGS_FILE = SETTINGS_DIR + "settings.json";
    private static final String LOGS_DIR = BASE_DIR + "logs/";
    private static final String INVOICES_DIR = BASE_DIR + "invoices/";
    private static final String EXPENSES_DIR = BASE_DIR + "expenses/";

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
            Files.createDirectories(Paths.get(EXPENSES_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String file = EXPENSES_DIR + year + ".json";
        return loadList(file, new TypeToken<List<Expenditure>>() {}.getType());
    }

    public void saveExpenditures(String year, List<Expenditure> expenditures) {
        String file = EXPENSES_DIR + year + ".json";
        saveToFile(file, expenditures);
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
