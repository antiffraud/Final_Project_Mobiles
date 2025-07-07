package com.example.projectmobiles.domain.repository

import android.app.Application
import android.database.Cursor
import android.util.Log
import com.example.projectmobiles.data.local.DatabaseHelper

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Example PRAGMA debug you asked about:
        val db = DatabaseHelper(this).readableDatabase
        val cursor: Cursor = db.rawQuery("PRAGMA table_info(bookmark)", null)
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
            Log.d("DB_SCHEMA", "column: $name  type: $type")
        }
        cursor.close()
        db.close()
    }
}
