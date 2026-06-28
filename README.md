# Employee Timesheet

A personal desktop application for self-employed professionals to track work hours, log kilometrage, manage expenses, generate invoices, and prepare CRA-ready tax documentation — all in one place.

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
- **✉ Email Invoice** — opens Mail.app with the invoice PDF attached, To/Subject/Body pre-filled; requires the boss to have an email address set in Boss Management
- **Delete** — removes the invoice record from the log (PDF stays on disk); requires confirmation
- Double-click any row to open the PDF
- History refreshes automatically every time the panel is opened

**Invoice tracking**
- All generated invoices are recorded in `invoices/invoice_log.json`
- Status changes persist immediately to disk
- Outstanding (Sent) invoices surface on the Dashboard

**Tax set-aside dialog**
Shown automatically when marking an invoice as Paid. Breaks down the received amount into:
- GST to remit to CRA (5%)
- Federal income tax to set aside (15%)
- BC provincial tax to set aside (5.06%)
- CPP contributions to set aside (11.9% both sides)
- Total to set aside (all four combined)
- Yours to keep (pre-GST amount minus income tax and CPP)

### 💸 Expenses
Two-tab panel for tracking business expenses and capital assets.

#### Expenses Tab
- **Month grid view** — all 12 months at a glance with per-category colour bars and totals; single-click any month to slide into the detail view with a 200ms ease-out animation
- **Amount fields** — subtotal, GST (5%, auto-calculated with override), and total after tax
- **Claimable amount** — each expense card shows the claimable portion in purple below the total for partial-use categories (e.g. "60%" for phone, "KM-based" for vehicle)
- **Receipt attachments** — attach JPG, PNG, PDF, or HEIC files per expense via drag & drop or native macOS file picker; files organized on disk by `receipts/{year}/{month}/`
- **Expense templates** — save frequently used expenses as templates for one-click re-entry
- **Year summary sidebar** — total spent and per-category breakdown with claimable sub-amounts for partial-use categories
- **Per-year categories** — expense categories are stored independently per tax year

**Partial-use deduction types** (set per category in Settings):
- `FULL` — 100% deductible (default)
- `FIXED_PERCENT` — fixed business-use % you set (e.g. 60% for cell phone)
- `KM_PERCENT` — auto-calculated from KM log (business KM ÷ total KM); used for vehicle expenses
- `HOME_OFFICE` — calculated from office sq ft ÷ total home sq ft set in Settings → Profile

**Default categories sourced from CRA T2125 E (25) Part 4:**
Advertising (8521), Meals & Entertainment (8523), Insurance (8690), Interest & Bank Charges (8710), Licences & Memberships (8760), Office Expenses (8810), Office Supplies (8811), Professional Fees (8860), Management & Admin Fees (8871), Rent (8910), Repairs & Maintenance (8960), Travel (9200), Utilities (9220), Fuel non-vehicle (9224), Delivery & Freight (9275), Motor Vehicle (9281), Home Office (9945), Other (9270)

#### CCA Assets Tab
Track depreciable capital assets (laptops, vehicles, equipment) for CRA Capital Cost Allowance deductions.

- **Pure computed model** — Opening UCC, Deduction, and Closing UCC are calculated mathematically from the original cost and purchase date using the CRA half-year rule; no mutable state
- **Asset list** — shows Description, Class, Cost, Opening UCC, Deduction, and Closing UCC for any preview year (adjustable via year spinner)
- **Add/Edit dialog** — description, purchase date, CCA class (preset dropdown with Class 50/10/8/12/10.1/14.1 or Custom), pre-tax capital cost, and invoice/receipt attachment
- **Preset classes** — dropdown shows class name and description (e.g. "Class 50 — Computers, laptops, tablets (55%)")
- **Receipt attachment** — drag & drop or file picker, same as expense receipts
- **Summary** — total deduction for the preview year shown in the top bar

**CRA half-year rule:** Year of purchase deduction = `cost × rate × 50%`. Subsequent years = `openingUCC × rate`.

**Common CCA classes pre-loaded:**
| Class | Rate | Description |
|---|---|---|
| Class 50 | 55% | Computers, laptops, tablets |
| Class 10 | 30% | Vehicles |
| Class 8 | 20% | Equipment, furniture, tools |
| Class 12 | 100% | Small tools under $500 |
| Class 10.1 | 30% | Passenger vehicles over cost limit |
| Class 14.1 | 5% | Intangibles, goodwill |

### 🚗 Kilometre Log
Track business KMs for vehicle expense deduction at tax time.

- **Chronological trip list** with month section headers and monthly KM totals
- **Log a trip** — date, distance (km), and a note/purpose
- **Odometer readings** — set year start and year end to calculate total KMs driven
- **Business use %** — auto-calculated from business KMs ÷ total KMs; feeds directly into vehicle expense deductions
- **Auto-logging** — KM entries in Work Logs automatically create linked trips here (synced on edit/delete); shows "auto-logged" badge on card
- **Backfill import** — "Import from Work Logs" button scans January → today and imports any previously logged KM entries (safe to run multiple times, skips duplicates)

### 📊 Accounting
Year-based export and archival tools for handing off to an accountant or storing for CRA's 7-year retention requirement.

**Export formats:**

- **Excel (.xlsx)** — formatted 4-sheet workbook:
  - *Expenses* — Date, Description, Category, Subtotal, GST, Total, Claimable, Business Use % per row; grouped by category with subtotals; category headers show business-use % (e.g. "MOTOR VEHICLE — Line 9281 (KM-based 29.3% claimable)"); grand total row
  - *Kilometre Log* — full trip list with odometer summary and business-use % calculation
  - *Income Summary* — per-boss breakdown with hours, KMs, extras, gross income, and GST collected; GST reconciliation block (collected, expense ITCs, net owing/refund)
  - *CCA Schedule* — all assets with Purchased date, Class, Rate, Cost, Opening UCC, Deduction, and Closing UCC; total deduction row; GST/ITC block for assets purchased in the export year
  - All sheets include full employee info header (name, company, address, phone/email)

- **CSV (.csv)** — plain text zip containing two files:
  - `Expenses_{year}.csv` — Date, Category, T2125 Line, Description, Subtotal, GST, Total, Claimable, Business Use %
  - `KilometreLog_{year}.csv` — odometer summary then full trip list

- **Annual Tax Report (PDF)** — multi-page report for your accountant:
  - *Cover page* — name, company, year, generated date, and table of contents
  - *Income Summary* — monthly breakdown per boss with hours, labour income, KMs, KM income, and extras; GST collected total
  - *Expense Summary* — all expenses grouped by T2125 category with line numbers; **Total Paid** and **Claimable** columns showing partial-use deductions; GST reconciliation block (collected, expense ITCs, capital asset ITCs, net owing/refund)
  - *CCA Schedule* — all assets grouped by class with per-class subtotals; Opening UCC, Deduction, Closing UCC; GST/ITC block for assets purchased in the report year
  - *Kilometre Summary* — full trip log plus per-boss billed KM breakdown
  - *Annual Net Summary* — gross income, total expenses paid, total claimable expenses (after partial-use rules), CCA deduction, estimated net income; full GST reconciliation including capital asset ITCs

- **Receipt Archive** — zip of all receipt files for the selected year, organized by month
- **Year Archive** — full backup zip of all data for the selected year; includes a `manifest.json`
- **Import Year Archive** — restore a previously exported archive; refuses to import if data already exists (conflict protection)
- **Clear local data** — remove a year's data after archiving; requires typing the year to confirm; never offered for the current year

### 👥 Boss Management
Add and manage clients/bosses used across Work Logs and Invoices.

- **Income Type** — each boss is marked as Self-Employed (T2125) or T4 Employment; T4 bosses are excluded from self-employed income calculations and the Annual Tax Report

### 🔍 Search
Sidebar search bar with live dropdown results across work logs, expenses, and KM trips. Click any result to navigate directly to the relevant panel.

### ↩ Undo
Deleting a work log entry, expense, or KM trip shows a snackbar with an **Undo** button. Clicking Undo within 8 seconds restores the item; otherwise the delete is committed to disk.

### ⚙️ Settings
Three tabs:

- **Profile** — employee info (name, company, address, phone, email) used on invoices and exports; **Home Office** section with office sq ft and total home sq ft fields and a live "Deductible portion: X%" calculation
- **Preferences** — default start/end times for new log entries; Data Location with a **Change...** button to migrate data to a new folder
- **Expense Categories** — per-year category management; add, edit, or delete categories; each category has a **Business Use** type (100% / Fixed % / KM-based / Home office) with optional fixed-% field; colour picker for category swatch; "Reset to Defaults" restores the full T2125-aligned list

---

## First-Run Onboarding

On first launch the app shows a 7-step setup wizard:

1. **Welcome** — introduction screen
2. **Data Location** — choose iCloud Drive (recommended, auto-detected) or Local Only; custom folder also supported
3. **Profile** — enter your name, company, address, phone, and email
4. **Home Office** — optional; enter office sq ft and total home sq ft to configure the home-office deduction percentage; shows live calculation
5. **CCA Assets** — optional; add any capital assets purchased to date (laptop, vehicle, etc.) with class and cost; can be skipped and added later
6. **First Boss** — add your first client with hourly rate, KM rate, tax rate, and income type
7. **Done** — confirmation with a summary of the chosen save location

The wizard only appears once. Existing users who already have a data directory preference skip it entirely.

---

## Data Storage

Data directory is chosen during onboarding and saved to macOS `Preferences` (`~/Library/Preferences/com.github.shanebeee.et.plist`).

**Default paths:**
- iCloud Drive: `~/Library/Mobile Documents/com~apple~CloudDocs/EmployeeTimesheet/`
- Local: `~/EmployeeTimesheet/`

```
{data_directory}/
├── settings/
│   ├── employee.json                    # Employee info (incl. home office sq ft)
│   ├── bosses.json                      # Boss list
│   ├── settings.json                    # App settings (invoice counter, default times)
│   ├── expense_categories_{year}.json   # Per-year expense categories with deduction types
│   └── cca_assets.json                  # CCA assets (not year-scoped; span multiple years)
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
