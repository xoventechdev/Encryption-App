package dev.xoventech.encryptionapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText secretKeyInput, dataInput;
    private Button encryptButton, decryptButton, viewSavedButton;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private boolean useExternalStorage = false; // Toggle for internal/external storage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        secretKeyInput = findViewById(R.id.secretKeyInput);
        dataInput = findViewById(R.id.dataInput);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);
        viewSavedButton = findViewById(R.id.viewSavedButton);

        encryptButton.setOnClickListener(v -> processData("Encrypt"));
        decryptButton.setOnClickListener(v -> processData("Decrypt"));
        viewSavedButton.setOnClickListener(v -> startActivity(new Intent(this, SavedResponsesActivity.class)));

        setupExitConfirmation();
    }

    private void setupExitConfirmation() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // When back is pressed, show our custom dialog
                showExitDialog();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showExitDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit? We'd love to hear your feedback!")

                // "Rate App" button
                .setNeutralButton("Rate App", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                    } catch (ActivityNotFoundException e) {
                        // If Play Store is not installed, open in browser
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                    }
                    finishAffinity(); // Close all activities
                    System.exit(0);                })

                // "Cancel" button
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Just dismiss the dialog, do nothing
                    dialog.dismiss();
                })

                // "Exit" button
                .setPositiveButton("Exit", (dialog, which) -> {
                    // Close the app
                    finishAffinity(); // Close all activities
                    System.exit(0);
                })
                .show();
    }

    private void processData(String type) {
        String secretKey = secretKeyInput.getText().toString().trim();
        String data = dataInput.getText().toString().trim();

        if (secretKey.isEmpty() || data.isEmpty()) {
            Toast.makeText(this, "Enter both secret key and data", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String result;
            if (type.equals("Encrypt")) {
                result = CryptoUtils.encrypt(data, secretKey);
            } else {
                result = CryptoUtils.decrypt(data, secretKey);
            }

            showResultDialog(result, type);
        } catch (Exception e) {
            Toast.makeText(this, type + " error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showResultDialog(String result, String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_result, null);
        builder.setView(dialogView);

        TextView resultTitle = dialogView.findViewById(R.id.resultTitle);
        EditText resultText = dialogView.findViewById(R.id.resultText);
        Button copyButton = dialogView.findViewById(R.id.copyButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeButton);

        resultTitle.setText(type + " Result");
        resultText.setText(result);

        AlertDialog dialog = builder.create();

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(type + " Result", result);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        saveButton.setOnClickListener(v -> {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = type.toLowerCase() + "_" + timeStamp + ".txt";
            if (useExternalStorage) {
                if (checkStoragePermissions()) {
                    saveFile(fileName, result, type);
                } else {
                    requestStoragePermissions(() -> saveFile(fileName, result, type));
                }
            } else {
                saveFile(fileName, result, type);
            }
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveFile(String fileName, String content, String type) {
        try {
            FileUtils.saveFile(this, fileName, content, type, useExternalStorage);
            Toast.makeText(this, "Saved as " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("aaaa", e.getMessage());
        }
    }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-12
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Below Android 6, no runtime permissions needed
    }

    private void requestStoragePermissions(Runnable onGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},
                    STORAGE_PERMISSION_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
        // Store callback to execute after permission result
        this.onPermissionGrantedCallback = onGranted;
    }

    private Runnable onPermissionGrantedCallback;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted && onPermissionGrantedCallback != null) {
                onPermissionGrantedCallback.run();
            } else {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}