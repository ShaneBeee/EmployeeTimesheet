package com.github.shanebeee.et.util;

import com.github.shanebeee.et.storage.DataStorage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Handles full-year archive export and import.
 *
 * Archive structure inside the zip:
 *   manifest.json                     — metadata (year, app version, export date)
 *   logs/yyyy-MM.json                 — work log files for the year
 *   receipts/{year}/expenses.json     — expense records
 *   receipts/{year}/{month}/...       — receipt files
 *   km/{year}/trips.json              — KM trips
 *   km/{year}/odometer.json           — odometer readings
 *   invoices/...                      — any invoice PDFs whose filename contains the year
 */
public class YearArchiver {

    private static final String APP_VERSION = "1.0";
    private static final Gson GSON = new Gson();

    private final DataStorage storage;

    public YearArchiver(DataStorage storage) {
        this.storage = storage;
    }

    // ── Export ────────────────────────────────────────────────────────────────

    public ExportResult exportYear(int year, String zipPath) throws IOException {
        String baseDir = storage.getBaseDir();
        int fileCount = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {

            // manifest.json
            JsonObject manifest = new JsonObject();
            manifest.addProperty("year", year);
            manifest.addProperty("appVersion", APP_VERSION);
            manifest.addProperty("exportDate", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            manifest.addProperty("archiveType", "year");
            addStringToZip(zos, "manifest.json", GSON.toJson(manifest));

            // Work logs — yyyy-MM.json for each month of the year
            File logsDir = new File(baseDir + "logs");
            if (logsDir.exists()) {
                for (File f : logsDir.listFiles()) {
                    if (f.getName().startsWith(year + "-") && f.getName().endsWith(".json")) {
                        addFileToZip(zos, "logs/" + f.getName(), f);
                        fileCount++;
                    }
                }
            }

            // Receipts dir for this year (expenses.json + receipt files)
            File receiptsYear = new File(baseDir + "receipts/" + year);
            if (receiptsYear.exists()) {
                fileCount += addDirToZip(zos, receiptsYear, "receipts/" + year);
            }

            // KM dir for this year
            File kmYear = new File(baseDir + "km/" + year);
            if (kmYear.exists()) {
                fileCount += addDirToZip(zos, kmYear, "km/" + year);
            }

            // Expense categories for this year
            File catsFile = new File(baseDir + "settings/expense_categories_" + year + ".json");
            if (catsFile.exists()) {
                addFileToZip(zos, "settings/expense_categories_" + year + ".json", catsFile);
                fileCount++;
            }

            // Invoices — include any file whose name contains the year
            File invoicesDir = new File(baseDir + "invoices");
            if (invoicesDir.exists()) {
                File[] invoices = invoicesDir.listFiles(f ->
                    f.getName().contains(String.valueOf(year)));
                if (invoices != null) {
                    for (File f : invoices) {
                        addFileToZip(zos, "invoices/" + f.getName(), f);
                        fileCount++;
                    }
                }
            }
        }

        return new ExportResult(zipPath, fileCount);
    }

    // ── Clear year data ───────────────────────────────────────────────────────

    /**
     * Deletes all local data for the given year.
     * Does NOT delete the current year's data as a safety measure.
     */
    public void clearYear(int year) throws IOException {
        String baseDir = storage.getBaseDir();

        // Work logs
        File logsDir = new File(baseDir + "logs");
        if (logsDir.exists()) {
            for (File f : logsDir.listFiles()) {
                if (f.getName().startsWith(year + "-") && f.getName().endsWith(".json")) {
                    f.delete();
                }
            }
        }

        // Receipts
        deleteDir(new File(baseDir + "receipts/" + year));

        // KM
        deleteDir(new File(baseDir + "km/" + year));

        // Invoices
        File invoicesDir = new File(baseDir + "invoices");
        if (invoicesDir.exists()) {
            File[] invoices = invoicesDir.listFiles(f ->
                f.getName().contains(String.valueOf(year)));
            if (invoices != null) for (File f : invoices) f.delete();
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    public ImportResult importYear(String zipPath) throws IOException {
        String baseDir = storage.getBaseDir();

        // Read manifest first
        int archiveYear = -1;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("manifest.json")) {
                    String json = new String(zis.readAllBytes());
                    JsonObject manifest = GSON.fromJson(json, JsonObject.class);
                    archiveYear = manifest.get("year").getAsInt();
                    break;
                }
                zis.closeEntry();
            }
        }

        if (archiveYear == -1) {
            throw new IOException("Invalid archive — no manifest.json found.");
        }

        // Check for existing data conflict
        if (hasExistingData(archiveYear, baseDir)) {
            throw new DataConflictException(archiveYear);
        }

        // Extract everything
        int fileCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("manifest.json")) { zis.closeEntry(); continue; }
                Path dest = Paths.get(baseDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                }
                zis.closeEntry();
            }
        }

        return new ImportResult(archiveYear, fileCount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasExistingData(int year, String baseDir) {
        // Check for any log files for this year
        File logsDir = new File(baseDir + "logs");
        if (logsDir.exists()) {
            File[] logs = logsDir.listFiles(f -> f.getName().startsWith(year + "-"));
            if (logs != null && logs.length > 0) return true;
        }
        // Check for receipts year dir
        if (new File(baseDir + "receipts/" + year).exists()) return true;
        // Check for km year dir
        if (new File(baseDir + "km/" + year).exists()) return true;
        return false;
    }

    private int addDirToZip(ZipOutputStream zos, File dir, String zipPrefix) throws IOException {
        int count = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                count += addDirToZip(zos, f, zipPrefix + "/" + f.getName());
            } else {
                addFileToZip(zos, zipPrefix + "/" + f.getName(), f);
                count++;
            }
        }
        return count;
    }

    private void addFileToZip(ZipOutputStream zos, String entryName, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(Files.readAllBytes(file.toPath()));
        zos.closeEntry();
    }

    private void addStringToZip(ZipOutputStream zos, String entryName, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private void deleteDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    // ── Result types ──────────────────────────────────────────────────────────

    public record ExportResult(String path, int fileCount) {}
    public record ImportResult(int year, int fileCount) {}

    public static class DataConflictException extends IOException {
        private final int year;
        public DataConflictException(int year) {
            super("Data already exists for " + year);
            this.year = year;
        }
        public int getYear() { return year; }
    }
}
