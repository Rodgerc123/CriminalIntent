package com.bignerdranch.android.criminalintent.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v1 → v2: add requiresPolice (INTEGER NOT NULL DEFAULT 0)
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE crimes ADD COLUMN requiresPolice INTEGER NOT NULL DEFAULT 0"
        )
    }
}