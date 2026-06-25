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

- **Month grid view** — all 12 months at a glance with per-category colour bars and totals; click any month to slide into the detail view
- **Amount fields** — subtotal, GST (5%, auto-calculated with override), and total after tax
- **Receipt attachments** — attach JPG, PNG, PDF, or HEIC files per expense via drag & drop or native macOS file picker; files organized on disk by `receipts/{year}/{month}/`
- **Year summary sidebar** — total spent and per-category breakdown
- **Categories** mapped to CRA T2125 line numbers: Vehicle (9281), Phone & Internet (9270), Home Office (9945), Meals & Entertainment (8523), Office Supplies (8810), Professional Fees (8860), Advertising (8520), Other (9270)

### 🚗 Kilometre Log
Track business KMs for vehicle expense deduction at tax time.

- **Chronological trip list** with month section headers
- **Log a trip** — date, distance (km), and a note/purpose
- **Odometer readings** — set year start and year end to calculate total KMs driven
- **Business use %** — auto-calculated from business KMs ÷ total KMs; the number your accountant needs
- **Auto-logging** — KM entries in Work Logs automatically create linked trips here (synced on edit/delete)
- **Backfill import** — "Import from Work Logs" button scans January → today and imports any previously logged KM entries (safe to run multiple times, skips duplicates)

### 👥 Boss Management
Add and manage clients/bosses used across Work Logs and Invoices.

### ⚙️ Settings
Configure employee info (name, address, etc.) used on invoices, and set default start/end times for new log entries.

---

## Data Storage

All data is stored locally under `~/ShaneApps/EmployeeTimesheet/`:

```
~/ShaneApps/EmployeeTimesheet/
├── settings/
│   ├── employee.json       # Employee info
│   ├── bosses.json         # Boss list
│   └── settings.json       # App settings (invoice counter, default times)
├── logs/
│   └── yyyy-MM.json        # Work log entries per month
├── invoices/               # Generated PDF invoices and summaries
├── receipts/
│   └── {year}/
│       ├── expenses.json   # Expense records for the year
│       └── {month}/        # Receipt files (jpg, png, pdf, heic)
└── km/
    └── {year}/
        ├── trips.json      # KM trip log for the year
        └── odometer.json   # Year start/end odometer readings
```

No database, no cloud, no accounts — everything is plain JSON on disk.

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
