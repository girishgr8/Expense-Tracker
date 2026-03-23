# 💰 ExpenseTracker — Android App

A **premium, feature-rich personal finance app** built with modern Android development best practices.

---

## ✨ Features

| # | Feature | Details |
|---|---------|---------|
| 1 | **Add Transactions** | Expense / Income / Transfer with category, account, note, date-time, up to 5 tags and up to 5 attachments (PDF, JPEG, XLSX) |
| 2 | **Payment Accounts** | Bank accounts, credit cards, wallets, cash — with colour labels and opening balances |
| 3 | **Categories** | Default + custom categories per transaction type, colour-coded |
| 4 | **Dashboard** | Gradient summary card (This Month / This Year / All Time), recent 5 transactions, budget progress ring, quick-action shortcuts |
| 5 | **Budget** | Monthly or yearly budget, optional per-category limits, real-time progress bar |
| 6 | **Filter Transactions** | Full-text search + filter by year, month, type, category, account, tag; swipe-to-delete; CSV export via share sheet |
| 7 | **Authentication** | Google Sign-In only (Firebase Auth); monthly Google Drive backup via WorkManager |
| 8 | **Settings** | Theme (System/Light/Dark), Material You dynamic colour toggle, currency symbol, backup on/off, manual backup trigger |

---

## 🏗️ Architecture & Tech Stack

```
Architecture : MVVM + Clean Architecture (domain / data / presentation layers)
UI           : Jetpack Compose + Material Design 3
DI           : Hilt
DB           : Room (SQLite) with Flow-based reactive queries
Auth         : Firebase Authentication (Google Sign-In)
Backup       : Google Drive REST API via WorkManager (periodic, 30 days)
Async        : Kotlin Coroutines + Flow
Navigation   : Jetpack Navigation Compose (animated transitions)
Images       : Coil
Prefs        : Jetpack DataStore
CSV Export   : OpenCSV
```

---

## 📁 Folder Structure

```
app/src/main/java/com/expensetracker/
├── ExpenseTrackerApp.kt              ← Hilt Application + WorkManager init
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt            ← Room database definition
│   │   ├── converter/
│   │   │   └── Converters.kt         ← Room TypeConverters
│   │   ├── dao/
│   │   │   ├── TransactionDao.kt
│   │   │   └── OtherDaos.kt          ← Category / Account / Attachment / Budget / Tag DAOs
│   │   └── entity/
│   │       ├── Entities.kt           ← Room @Entity classes
│   │       └── Mappers.kt            ← Entity ↔ Domain mappers
│   └── repository/
│       ├── Repositories.kt           ← Interfaces + Implementations
│       ├── DefaultCategories.kt      ← Seed data
│       ├── AuthManager.kt            ← Firebase + Google Sign-In wrapper
│       └── UserPreferencesRepository.kt ← DataStore wrapper
│
├── di/
│   ├── AppModule.kt                  ← Database + Repository bindings
│   ├── UtilModule.kt                 ← Auth / CSV / Backup / Prefs
│   └── UseCaseModule.kt              ← Use-case bindings
│
├── domain/
│   ├── model/
│   │   └── Models.kt                 ← Pure Kotlin domain models + enums
│   └── usecase/
│       ├── SummaryUseCases.kt        ← Monthly / Yearly / AllTime summary
│       └── ValidationUseCases.kt     ← Tag / Attachment / Transaction validation
│
├── presentation/
│   ├── navigation/
│   │   └── NavGraph.kt               ← Animated NavHost + sealed Screen routes
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   └── Theme.kt                  ← Material3 dynamic + static themes
│   ├── components/
│   │   └── CommonComponents.kt       ← Reusable Compose widgets
│   └── ui/
│       ├── MainActivity.kt
│       ├── MainViewModel.kt
│       ├── auth/
│       │   └── AuthScreen.kt         ← Google Sign-In UI
│       ├── dashboard/
│       │   ├── DashboardViewModel.kt
│       │   └── DashboardScreen.kt
│       ├── addtransaction/
│       │   ├── AddTransactionViewModel.kt
│       │   └── AddTransactionScreen.kt
│       ├── transactions/
│       │   ├── TransactionsViewModel.kt
│       │   ├── TransactionsScreen.kt
│       │   └── TransactionDetailSheet.kt
│       ├── categories/
│       │   ├── CategoriesViewModel.kt
│       │   └── CategoriesScreen.kt
│       ├── accounts/
│       │   └── AccountsScreen.kt     ← ViewModel + Screen in one file
│       ├── budget/
│       │   └── BudgetScreen.kt       ← ViewModel + Screen in one file
│       └── settings/
│           ├── SettingsViewModel.kt
│           └── SettingsScreen.kt
│
├── util/
│   ├── CsvExporter.kt
│   ├── DriveBackupScheduler.kt
│   ├── DriveBackupManager.kt         ← (stub — superseded)
│   ├── FormatUtils.kt
│   ├── ExtensionFunctions.kt
│   └── NetworkUtils.kt
│
└── worker/
    └── DriveBackupWorker.kt          ← @HiltWorker for Drive backup
```

---

## ⚙️ Setup Instructions

### 1 — Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com) → **Add project**
2. Register an Android app with package name `com.expensetracker`
3. Download `google-services.json` → place it in `app/`
4. Enable **Authentication → Google Sign-In** in Firebase Console

### 2 — Google Drive API

1. In [Google Cloud Console](https://console.cloud.google.com), open the same project
2. Enable the **Google Drive API**
3. Under **OAuth 2.0 Client IDs**, copy the **Web Client ID**
4. Paste it in `AuthManager.kt`:

```kotlin
.requestIdToken("YOUR_WEB_CLIENT_ID")  // ← replace here
```

### 3 — Build & Run

```bash
# Clone the repo
git clone https://github.com/your-org/expense-tracker.git
cd expense-tracker

# Open in Android Studio Hedgehog (or later)
# Let Gradle sync, then run on emulator or device (API 26+)
```

---

## 🎨 UI Highlights

- **Gradient summary card** — purple-indigo gradient with live income/expense pills
- **Material You** — dynamic colour support on Android 12+
- **Animated navigation** — slide + fade transitions between screens
- **Swipe-to-delete** — `SwipeToDismissBox` on transactions with confirmation dialog
- **Bottom sheets** — filter panel + transaction detail sheet for quick access
- **Splash screen** — `core-splashscreen` with branded vector icon

---

## 📌 Notes

- The app requires a real device or emulator with **Google Play Services** for Google Sign-In
- Replace `YOUR_WEB_CLIENT_ID` in `AuthManager.kt` before building
- The Google Drive backup creates a folder **"ExpenseTracker Backups"** in the signed-in user's Drive
- All Room migrations use `fallbackToDestructiveMigration()` (change to proper migrations before production)
- CSV exports land in `Android/data/com.expensetracker/files/exports/` and are shared via the system share sheet

---

## 📄 License

MIT — see `LICENSE` for details.
