package com.expensetracker.di

// All utility classes (AuthManager, CsvExporter, DriveBackupScheduler,
// UserPreferencesRepository) are annotated with @Singleton + @Inject constructor,
// so Hilt provides them automatically — no @Module bindings needed here.
