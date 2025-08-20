package dev.xoventech.encryptionapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Saved Content", metadata.content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("OK", null);
            builder.show();
        });

        holder.editButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra("file_metadata", metadata);
            if (context instanceof SavedResponsesActivity) {
                ((SavedResponsesActivity) context).startActivityForResult(intent, 1);
            }
        });

        holder.renameButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Rename File");
            EditText input = new EditText(context);
            input.setText(metadata.fileName);
            input.setTextColor(0xFFFFFFFF);
            input.setHintTextColor(0xFFB0B0B0);
            input.setBackgroundResource(R.drawable.edit_text_background);
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

    public void updateFile(String fileName) {
        List<FileUtils.FileMetadata> updatedMetadataList = FileUtils.loadMetadata(context);
        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i).fileName.equals(fileName)) {
                for (FileUtils.FileMetadata updatedMetadata : updatedMetadataList) {
                    if (updatedMetadata.fileName.equals(fileName)) {
                        metadataList.set(i, updatedMetadata);
                        break;
                    }
                }
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return metadataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, dateText, typeText;
        Button viewButton, editButton, renameButton, deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            dateText = itemView.findViewById(R.id.dateText);
            typeText = itemView.findViewById(R.id.typeText);
            viewButton = itemView.findViewById(R.id.viewButton);
            editButton = itemView.findViewById(R.id.editButton);
            renameButton = itemView.findViewById(R.id.renameButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}