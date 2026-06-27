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

### 🏠 Dashboard
Landing screen with at-a-glance stats and recent activity.

- **This Month** — Gross Income, Hours Logged, Expenses, KMs Driven, KMs Billed; each card links to the relevant panel
- **Year to Date** — Gross Income, Total Expenses, Est. Net Income
- **Year over Year** — compares the current calendar month to the same month last year; shows Gross Income, Hours Logged, and Expenses side by side with colour-coded delta indicators (▲/▼); section is hidden until last year's data exists
- **Outstanding Invoices** — any invoices marked Sent but not yet Paid appear here with a one-click **Mark Paid** button; section is hidden when nothing is outstanding
- **Recent Activity** — last 6 events across work logs, expenses, and KM trips

### 📅 Work Logs
Calendar-based monthly view for logging daily work entries. Three entry types per day:

- **Time** — start/end time with boss allocation sliders (split % across multiple bosses, with "Split Evenly" button and live progress bar); optional notes/description field
- **Kilometre** — distance billed to a specific boss (auto-syncs to the Kilometre Log)
- **Extra** — miscellaneous billable items with units and cost per unit

### 🧾 Invoice Management
Two-tab panel for generating and tracking invoices.

**Generate tab**
- Choose boss and date range, then generate a **Standard** invoice or a **With Breakdown** invoice (two-page PDF: page 1 is the standard invoice, page 2 is a supplementary work log detail for the client's reference)
- Preview invoice before generating
- Preview monthly summary with vibe-check emoji (🤑/😊/😐/😬 based on income)
- Export full monthly summary PDF

**History tab**
- Table of all generated invoices showing #, boss, period, generated date, status badge, and amount
- Status badges: **Draft** (grey), **Sent** (blue), **Paid** (green)
- **Mark as Sent** and **Mark as Paid** buttons with date picker
- Double-click any row to open the PDF
- History refreshes automatically every time the panel is opened

**Invoice tracking**
- All generated invoices are recorded in `invoices/invoice_log.json`
- Status changes persist immediately to disk
- Outstanding (Sent) invoices surface on the Dashboard

**Tax set-aside dialog**
Shown automatically when marking an invoice as Paid (from either the History tab or the Dashboard). Breaks down the received amount into:
- GST to remit to CRA (5%)
- Federal income tax to set aside (15%)
- BC provincial tax to set aside (5.06%)
- CPP contributions to set aside (11.9% both sides)
- Total to set aside (all four combined)
- Yours to keep (pre-GST amount minus income tax and CPP)

### 💸 Expenses
Track business expenses for CRA T2125 reporting.

- **Month grid view** — all 12 months at a glance with per-category colour bars and totals; single-click any month to slide into the detail view with a 200ms ease-out animation
- **Amount fields** — subtotal, GST (5%, auto-calculated with override), and total after tax; PST rolls into total
- **Receipt attachments** — attach JPG, PNG, PDF, or HEIC files per expense via drag & drop or native macOS file picker; files organized on disk by `receipts/{year}/{month}/`
- **Expense templates** — save frequently used expenses as templates for one-click re-entry
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

- **Export Accounting Data** — click Export to choose the format:
  - *Excel (.xlsx)* — formatted workbook with two sheets (Expenses by T2125 category, Kilometre Log with odometer summary)
  - *CSV (.csv)* — plain text zip containing two files (`Expenses_{year}.csv` and `KilometreLog_{year}.csv`); useful for accountants who prefer raw data
- **Export Receipt Archive** — zip of all receipt files for the selected year, organized by month; one-click audit-ready package
- **Annual Tax Report** — full multi-page PDF for handing to an accountant:
  - *Cover page* — name, company, year, generated date, and table of contents
  - *Income Summary* — monthly breakdown per boss with hours, labour income, KMs, KM income, and extras; GST collected total
  - *Expense Summary* — all expenses grouped by T2125 category with line numbers; GST reconciliation block (collected, ITCs, net owing/refund)
  - *Kilometre Summary* — full trip log plus per-boss billed KM breakdown
  - *Net Summary* — at-a-glance page with gross income, total expenses, estimated net income, GST reconciliation, and KM totals
- **Export Year Archive** — full backup zip of all data for the selected year (logs, expenses + receipts, KM data, invoices, and that year's category definitions); includes a `manifest.json` with year, export date, and app version
- **Import Year Archive** — restore a previously exported year archive back into the app; refuses to import if data already exists for that year (conflict protection)
- **Clear local data** — after exporting a year archive, optionally remove that year's data from local storage to keep the app folder lean; requires typing the year to confirm; never offered for the current year
- **Year picker** — all exports scoped to the selected year (2020 → current)
- Excel export uses a local Python venv auto-created on first run; no manual Python setup required

### 👥 Boss Management
Add and manage clients/bosses used across Work Logs and Invoices.

- **Income Type** — each boss is marked as Self-Employed (T2125) or T4 Employment; T4 bosses are excluded from self-employed income calculations and the Annual Tax Report

### 🔍 Search
Sidebar search bar with live dropdown results across work logs, expenses, and KM trips. Click any result to navigate directly to the relevant panel.

### ⚙️ Settings
Three tabs:

- **Profile** — employee info (name, company, address, phone, email) used on invoices and exports
- **Preferences** — default start/end times for new log entries; Data Location showing current save path with a **Change...** button to migrate data to a new folder (copies all existing data, then prompts restart)
- **Expense Categories** — per-year category management with `< year >` navigation; add, edit, or delete categories; built-in categories can be edited but not deleted; "Reset to Defaults" restores the full T2125-aligned list for the selected year; new years auto-seed from the previous year's categories

---

## First-Run Onboarding

On first launch the app shows a 5-step setup wizard:

1. **Welcome** — introduction screen
2. **Data Location** — choose iCloud Drive (recommended, auto-detected) or Local Only; custom folder also supported; path shown live
3. **Profile** — enter your name, company, address, phone, and email
4. **First Boss** — add your first client with hourly rate, KM rate, tax rate, and income type
5. **Done** — confirmation with a summary of the chosen save location

The wizard only appears once. Existing users who already have a data directory preference saved skip it entirely.

---

## Data Storage

Data directory is chosen during onboarding and saved to macOS `Preferences` (`~/Library/Preferences/com.github.shanebeee.et.plist`) so the app always knows where to look on startup, independent of the data folder itself.

**Default paths:**
- iCloud Drive: `~/Library/Mobile Documents/com~apple~CloudDocs/EmployeeTimesheet/`
- Local: `~/EmployeeTimesheet/`

The data directory can be changed at any time in **Settings → Preferences → Data Location**. Changing it copies all existing data to the new location and saves the new path to Preferences; the app prompts for a restart.

```
{data_directory}/
├── settings/
│   ├── employee.json                    # Employee info
│   ├── bosses.json                      # Boss list
│   ├── settings.json                    # App settings (invoice counter, default times)
│   └── expense_categories_{year}.json   # Per-year expense categories
├── logs/
│   └── yyyy-MM.json                     # Work log entries per month
├── invoices/
│   ├── invoice_log.json                 # Invoice history and status tracking
│   └── *.pdf                            # Generated invoice and summary PDFs
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

No database, no accounts — everything is plain JSON on disk, synced automatically if stored in iCloud Drive.

### Year Archives
Full-year backups are self-contained zip files with the structure:

```
EmployeeTimesheet_Archive_{year}.zip
├── manifest.json                            # Year, export date, app version
├── logs/yyyy-MM.json                        # Work logs for the year
├── receipts/{year}/expenses.json            # Expense records
├── receipts/{year}/{month}/...              # Receipt files
├── km/{year}/trips.json                     # KM trips
├── km/{year}/odometer.json                  # Odometer readings
├── settings/expense_categories_{year}.json  # Category snapshot for the year
└── invoices/...                             # Invoice PDFs for the year
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
