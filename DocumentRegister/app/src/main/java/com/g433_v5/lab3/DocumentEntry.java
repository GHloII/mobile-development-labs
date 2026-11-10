package com.g433_v5.lab3;

/** Immutable record transferred from SQLite to the list adapter. */
final class DocumentEntry {
    final long id;
    final int type;
    final String employee;
    final String date;

    DocumentEntry(long id, int type, String employee, String date) {
        this.id = id;
        this.type = type;
        this.employee = employee;
        this.date = date;
    }
}
