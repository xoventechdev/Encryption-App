package dev.xoventech.encryptionapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedResponseAdapter extends RecyclerView.Adapter<SavedResponseAdapter.ViewHolder> {
    private List<FileUtils.FileMetadata> metadataList;
    private Context context;
    private boolean useExternalStorage;

    public SavedResponseAdapter(Context context, List<FileUtils.FileMetadata> metadataList, boolean useExternalStorage) {
        this.context = context;
        this.metadataList = metadataList;
        this.useExternalStorage = useExternalStorage;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_saved_response, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileUtils.FileMetadata metadata = metadataList.get(position);
        holder.fileNameText.setText(metadata.fileName);
        holder.dateText.setText("Date: " + metadata.date);
        holder.typeText.setText("Type: " + metadata.type);

        holder.viewButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("View Content");
            builder.setMessage(metadata.content);
            builder.setPositiveButton("Copy", (dialog, which) -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Saved Content", metadata.content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("OK", null);
            builder.show();
        });

        holder.renameButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Rename File");
            EditText input = new EditText(context);
            input.setText(metadata.fileName);
            builder.setView(input);
            builder.setPositiveButton("Rename", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    try {
                        FileUtils.renameFile(context, metadata.fileName, newName, useExternalStorage);
                        metadata.fileName = newName;
                        notifyDataSetChanged();
                        Toast.makeText(context, "File renamed", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        holder.deleteButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete File");
            builder.setMessage("Are you sure you want to delete " + metadata.fileName + "?");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                try {
                    FileUtils.deleteFile(context, metadata.fileName, useExternalStorage);
                    metadataList.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return metadataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, dateText, typeText;
        Button viewButton, renameButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            dateText = itemView.findViewById(R.id.dateText);
            typeText = itemView.findViewById(R.id.typeText);
            viewButton = itemView.findViewById(R.id.viewButton);
            renameButton = itemView.findViewById(R.id.renameButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}