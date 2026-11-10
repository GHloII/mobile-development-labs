package com.g433_v5.lab3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** SQLite storage. ISO dates have the same chronological and lexical order. */
final class DocumentDatabase extends SQLiteOpenHelper {
    private final Context context;
    private final String table, id, type, employee, date;

    DocumentDatabase(Context context) {
        super(context, context.getString(R.string.db_name), null, 1);
        this.context = context.getApplicationContext();
        table = context.getString(R.string.db_table);
        id = context.getString(R.string.col_id);
        type = context.getString(R.string.col_type);
        employee = context.getString(R.string.col_employee);
        date = context.getString(R.string.col_date);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(context.getString(R.string.sql_create, table, id, type, employee, date));
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 has no migration yet; never silently delete user data.
        throw new IllegalStateException(context.getString(R.string.migration_error));
    }

    List<DocumentEntry> load() {
        List<DocumentEntry> result = new ArrayList<>();
        String order = context.getString(R.string.sql_order, date, id);
        try (Cursor cursor = getReadableDatabase().query(table, null, null, null, null, null, order)) {
            while (cursor.moveToNext()) result.add(read(cursor));
        }
        return result;
    }

    DocumentEntry find(long key) {
        try (Cursor cursor = getReadableDatabase().query(table, null,
                context.getString(R.string.sql_id_where, id),
                new String[]{Long.toString(key)}, null, null, null)) {
            return cursor.moveToFirst() ? read(cursor) : null;
        }
    }

    private DocumentEntry read(Cursor cursor) {
        return new DocumentEntry(cursor.getLong(cursor.getColumnIndexOrThrow(id)),
                cursor.getInt(cursor.getColumnIndexOrThrow(type)),
                cursor.getString(cursor.getColumnIndexOrThrow(employee)),
                cursor.getString(cursor.getColumnIndexOrThrow(date)));
    }

    boolean save(long key, int kind, String name, String isoDate) {
        ContentValues values = new ContentValues();
        values.put(type, kind);
        values.put(employee, name);
        values.put(date, isoDate);
        SQLiteDatabase db = getWritableDatabase();
        // Placeholders and ContentValues keep names containing quotes safe.
        if (key < 0) return db.insertOrThrow(table, null, values) != -1;
        return db.update(table, values, context.getString(R.string.sql_id_where, id),
                new String[]{Long.toString(key)}) == 1;
    }

    boolean delete(long key) {
        return getWritableDatabase().delete(table, context.getString(R.string.sql_id_where, id),
                new String[]{Long.toString(key)}) == 1;
    }

    void clear() { getWritableDatabase().delete(table, null, null); }
}
