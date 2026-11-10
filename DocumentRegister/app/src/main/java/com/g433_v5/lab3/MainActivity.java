package com.g433_v5.lab3;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/** List controller: refresh from SQLite whenever the editor returns. */
public class MainActivity extends BaseActivity {
    private DocumentDatabase database;
    private DocumentAdapter adapter;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        applyInsets();
        database = new DocumentDatabase(this);
        adapter = new DocumentAdapter(this::edit, this::confirmDelete);
        RecyclerView list = findViewById(R.id.documents);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.add).setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class)));
        findViewById(R.id.clear).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.clear_title).setMessage(R.string.clear_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> {
                    try { database.clear(); refresh(); } catch (SQLiteException e) { message(R.string.db_error); }
                }).show());
        findViewById(R.id.authors).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.authors_title).setMessage(R.string.authors_text)
                .setPositiveButton(R.string.close, null).show());
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        try {
            List<DocumentEntry> data = database.load();
            adapter.submit(data);
            ((TextView) findViewById(R.id.count)).setText(getString(R.string.count, data.size()));
            findViewById(R.id.empty).setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            findViewById(R.id.clear).setEnabled(!data.isEmpty());
        } catch (SQLiteException e) { message(R.string.db_error); }
    }

    private void edit(DocumentEntry entry) {
        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra(getString(R.string.extra_id), entry.id);
        startActivity(intent);
    }

    private void confirmDelete(DocumentEntry entry) {
        String kind = getResources().getStringArray(R.array.document_types)[entry.type];
        new AlertDialog.Builder(this).setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, kind, entry.employee))
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, (dialog, which) -> {
                    try { database.delete(entry.id); refresh(); } catch (SQLiteException e) { message(R.string.db_error); }
                }).show();
    }

    @Override protected void onDestroy() { database.close(); super.onDestroy(); }
}
