# Employee Timesheet

A personal desktop application for self-employed professionals to track work hours, log kilometrage, manage expenses, and generate invoices — all in one place.

Built specifically for use at Century 21 Amos Realty in the South Okanagan, BC.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| UI Framework | Swing + [FlatLaf 3.5.4](https://www.formdev.com/flatlaf/) |
| Build System | Gradle (Kotlin DSL) |
| Data Serialization | [Gson 2.11.0](https://github.com/google/gson) |
| PDF Generation | [iText 8](https://itextpdf.com/) |
| Excel Export | Python 3 + openpyxl (auto-installed into a local venv on first export) |
| Packaging | jpackage (macOS DMG) |

---

## Features

### 📅 Work Logs
Calendar-based monthly view for logging daily work entries. Three entry types per day:

- **Time** — start/end time with boss allocation sliders (split % across multiple bosses, with "Split Evenly" button and live progress bar)
- **Kilometre** — distance billed to a specific boss (auto-syncs to the Kilometre Log)
- **Extra** — miscellaneous billable items with units and cost per unit

### 🧾 Invoice Management
Generate professional PDF invoices and monthly summaries per boss, with auto-incrementing invoice numbers.

### 💸 Expenses
Track business expenses for CRA T2125 reporting.

- **Month grid view** — all 12 months at a glance with per-category colour bars and totals; single-click any month to slide into the detail view with a 200ms ease-out animation
- **Amount fields** — subtotal, GST (5%, auto-calculated with override), and total after tax; PST rolls into total
- **Receipt attachments** — attach JPG, PNG, PDF, or HEIC files per expense via drag & drop or native macOS file picker; files organized on disk by `receipts/{year}/{month}/`
- **Year summary sidebar** — total spent and per-category breakdown
- **Per-year categories** — expense categories are stored independently per tax year, so CRA line number changes in future years never affect past data

**Default categories sourced from CRA T2125 E (25) Part 4:**
Advertising (8521), Meals & Entertainment (8523), Insurance (8690), Interest & Bank Charges (8710), Licences & Memberships (8760), Office Expenses (8810), Office Supplies (8811), Professional Fees (8860), Management & Admin Fees (8871), Rent (8910), Repairs & Maintenance (8960), Travel (9200), Utilities (9220), Fuel non-vehicle (9224), Delivery & Freight (9275), Motor Vehicle (9281), Home Office (9945), Other (9270)

### 🚗 Kilometre Log
Track business KMs for vehicle expense deduction at tax time.

- **Chronological trip list** with month section headers and monthly KM totals
- **Log a trip** — date, distance (km), and a note/purpose
- **Odometer readings** — set year start and year end to calculate total KMs driven
- **Business use %** — auto-calculated from business KMs ÷ total KMs; the number your accountant needs
- **Auto-logging** — KM entries in Work Logs automatically create linked trips here (synced on edit/delete); shows "auto-logged" badge on card
- **Backfill import** — "Import from Work Logs" button scans January → today and imports any previously logged KM entries (safe to run multiple times, skips duplicates)

### 📊 Accounting
Year-based export and archival tools for handing off to an accountant or storing for CRA's 7-year retention requirement.

- **Export to Excel** — formatted `.xlsx` workbook with two sheets, both including employee info header:
  - *Expenses* — expenses grouped by category with coloured T2125 header rows, alternating row shading, per-category subtotals, and a grand total; sorted oldest-first
  - *Kilometre Log* — odometer summary block (start/end, total KM, business KM, business use %) followed by full trip list with auto vs manual source tracking
- **Export Receipt Archive** — zip of all receipt files for the selected year, organized by month; one-click audit-ready package
- **Export Year Archive** — full backup zip of all data for the selected year (logs, expenses + receipts, KM data, invoices, and that year's category definitions); includes a `manifest.json` with year, export date, and app version
- **Import Year Archive** — restore a previously exported year archive back into the app; refuses to import if data already exists for that year (conflict protection)
- **Clear local data** — after exporting a year archive, optionally remove that year's data from local storage to keep the app folder lean; requires typing the year to confirm; never offered for the current year
- **Year picker** — all exports scoped to the selected year (2020 → current)
- Excel export uses a local Python venv (`~/ShaneApps/EmployeeTimesheet/python_venv/`) auto-created on first run; no manual Python setup required

### 👥 Boss Management
Add and manage clients/bosses used across Work Logs and Invoices.

### ⚙️ Settings
Three tabs:

- **Profile** — employee info (name, company, address, phone, email) used on invoices and Excel exports
- **Preferences** — default start/end times for new log entries
- **Expense Categories** — per-year category management with `< year >` navigation; add, edit, or delete categories; built-in categories can be edited but not deleted; "Reset to Defaults" restores the full T2125-aligned list for the selected year; new years auto-seed from the previous year's categories

---

## Data Storage

All data is stored locally under `~/ShaneApps/EmployeeTimesheet/`:

```
~/ShaneApps/EmployeeTimesheet/
├── settings/
│   ├── employee.json                    # Employee info
│   ├── bosses.json                      # Boss list
│   ├── settings.json                    # App settings (invoice counter, default times)
│   └── expense_categories_{year}.json   # Per-year expense categories (e.g. expense_categories_2026.json)
├── logs/
│   └── yyyy-MM.json                     # Work log entries per month
├── invoices/                            # Generated PDF invoices and summaries
├── receipts/
│   └── {year}/
│       ├── expenses.json                # Expense records for the year
│       └── {month}/                     # Receipt files (jpg, png, pdf, heic)
├── km/
│   └── {year}/
│       ├── trips.json                   # KM trip log for the year
│       └── odometer.json                # Year start/end odometer readings
└── python_venv/                         # Auto-created Python venv for Excel export
```

No database, no cloud, no accounts — everything is plain JSON on disk.

### Year Archives
Full-year backups are self-contained zip files with the structure:

```
EmployeeTimesheet_Archive_{year}.zip
├── manifest.json                        # Year, export date, app version
├── logs/yyyy-MM.json                    # Work logs for the year
├── receipts/{year}/expenses.json        # Expense records
├── receipts/{year}/{month}/...          # Receipt files
├── km/{year}/trips.json                 # KM trips
├── km/{year}/odometer.json              # Odometer readings
├── settings/expense_categories_{year}.json  # Category snapshot for the year
└── invoices/...                         # Invoice PDFs for the year
```

---

## Building

```bash
# Run in development
./gradlew run

# Build fat JAR
./gradlew jar

# Package as macOS DMG (requires jpackage)
./gradlew jpackageMac
```

Requires Java 25+.

---

## License

See [LICENSE](LICENSE).
