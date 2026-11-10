package com.g433_v5.lab3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** RecyclerView reuses holders instead of creating a view for every record. */
final class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.Holder> {
    private List<DocumentEntry> entries = new ArrayList<>();
    private final Consumer<DocumentEntry> edit;
    private final Consumer<DocumentEntry> delete;

    DocumentAdapter(Consumer<DocumentEntry> edit, Consumer<DocumentEntry> delete) {
        this.edit = edit;
        this.delete = delete;
    }

    void submit(List<DocumentEntry> data) {
        entries = data;
        notifyDataSetChanged();
    }

    @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false));
    }

    @Override public void onBindViewHolder(Holder holder, int position) {
        DocumentEntry entry = entries.get(position);
        holder.type.setText(holder.itemView.getResources().getStringArray(R.array.document_types)[entry.type]);
        holder.employee.setText(entry.employee);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(holder.itemView.getContext().getString(R.string.date_format));
        holder.date.setText(holder.itemView.getContext().getString(R.string.date_display, LocalDate.parse(entry.date).format(format)));
        holder.itemView.setOnClickListener(v -> edit.accept(entry));
        holder.remove.setOnClickListener(v -> delete.accept(entry));
    }

    @Override public int getItemCount() { return entries.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView type, employee, date;
        final View remove;
        Holder(View item) {
            super(item);
            type = item.findViewById(R.id.row_type);
            employee = item.findViewById(R.id.row_employee);
            date = item.findViewById(R.id.row_date);
            remove = item.findViewById(R.id.row_delete);
        }
    }
}
