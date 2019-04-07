package com.kansou.tiberian.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class DatabaseHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, "Tiberian_DB", null, 1) {
    companion object {
        private var instance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(ctx.getApplicationContext())
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        db.createTable("keys", true,
                "id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                "account" to TEXT,
                "issuer" to TEXT,
                "secret" to TEXT,
                "algorithm" to TEXT,
                "period" to TEXT,
                "type" to TEXT,
                "uri" to TEXT,
                "creationDate" to TEXT)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable("keys", true)
    }
}

// Access property for Context
val Context.database: DatabaseHelper
    get() = DatabaseHelper.getInstance(getApplicationContext())