package com.g433_v5.lab3;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DateTimeException;
import java.time.format.ResolverStyle;

/** Editor handles both insert and update, with strict validation before SQL. */
public class EditActivity extends BaseActivity {
    private DocumentDatabase database;
    private long recordId;
    private EditText employee, date;
    private Spinner type;
    private DateTimeFormatter format;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_edit);
        applyInsets();
        database = new DocumentDatabase(this);
        recordId = getIntent().getLongExtra(getString(R.string.extra_id), -1);
        employee = findViewById(R.id.employee);
        date = findViewById(R.id.date);
        type = findViewById(R.id.type);
        format = DateTimeFormatter.ofPattern(getString(R.string.date_format)).withResolverStyle(ResolverStyle.STRICT);
        ((TextView) findViewById(R.id.editor_title)).setText(recordId < 0 ? R.string.add_title : R.string.edit_title);
        findViewById(R.id.delete).setVisibility(recordId < 0 ? View.GONE : View.VISIBLE);
        // Framework view state restores an unsaved draft after rotation.
        if (state == null) {
            if (recordId < 0) date.setText(LocalDate.now().format(format));
            else {
                try {
                    DocumentEntry entry = database.find(recordId);
                    if (entry == null) { message(R.string.missing); finish(); return; }
                    type.setSelection(entry.type);
                    employee.setText(entry.employee);
                    date.setText(LocalDate.parse(entry.date).format(format));
                } catch (SQLiteException e) { message(R.string.db_error); finish(); return; }
            }
        }
        findViewById(R.id.choose_date).setOnClickListener(v -> chooseDate());
        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
        findViewById(R.id.delete).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title).setMessage(R.string.delete_confirm)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        if (database.delete(recordId)) { message(R.string.removed); finish(); }
                        else { message(R.string.missing); finish(); }
                    } catch (SQLiteException e) { message(R.string.db_error); }
                }).show());
    }

    private LocalDate readDate() {
        LocalDate value = LocalDate.parse(date.getText().toString().trim(), format);
        if (value.getYear() < 1900 || value.getYear() > 2100) throw new IllegalArgumentException();
        return value;
    }

    private void chooseDate() {
        LocalDate initial;
        try { initial = readDate(); } catch (DateTimeException | IllegalArgumentException e) { initial = LocalDate.now(); }
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) ->
                date.setText(LocalDate.of(year, month + 1, day).format(format)),
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        java.util.Calendar min = java.util.Calendar.getInstance();
        min.clear(); min.set(1900, 0, 1);
        java.util.Calendar max = java.util.Calendar.getInstance();
        max.clear(); max.set(2100, 11, 31);
        picker.getDatePicker().setMinDate(min.getTimeInMillis());
        picker.getDatePicker().setMaxDate(max.getTimeInMillis());
        picker.show();
    }

    private void save() {
        employee.setError(null); date.setError(null);
        String name = employee.getText().toString().trim();
        if (name.isEmpty() || name.length() > 120) {
            employee.setError(getString(R.string.name_error)); employee.requestFocus(); return;
        }
        LocalDate value;
        try { value = readDate(); }
        catch (DateTimeException | IllegalArgumentException e) {
            date.setError(getString(R.string.date_error)); date.requestFocus(); return;
        }
        try {
            if (database.save(recordId, type.getSelectedItemPosition(), name, value.toString())) {
                message(R.string.saved); finish();
            } else message(R.string.missing);
        } catch (SQLiteException e) { message(R.string.db_error); }
    }

    @Override protected void onDestroy() { database.close(); super.onDestroy(); }
}
