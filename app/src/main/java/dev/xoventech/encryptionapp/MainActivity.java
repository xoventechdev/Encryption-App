package dev.xoventech.encryptionapp;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private EditText  packageNameInput, versionCodeInput, forceUpdateMessageInput, contentInput;
    private Spinner forceUpdateSpinner, adNetworkSpinner;
    private Button encryptButton, decryptButton, viewSavedButton, importDecryptButton;
    private FileUtils.FileMetadata editingMetadata;
    private String secretKeyInput = "ali@2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        packageNameInput = findViewById(R.id.packageNameInput);
        versionCodeInput = findViewById(R.id.versionCodeInput);
        forceUpdateSpinner = findViewById(R.id.forceUpdateSpinner);
        adNetworkSpinner = findViewById(R.id.adNetworkSpinner);
        forceUpdateMessageInput = findViewById(R.id.forceUpdateMessageInput);
        contentInput = findViewById(R.id.contentInput);
        encryptButton = findViewById(R.id.encryptButton);
        decryptButton = findViewById(R.id.decryptButton);
        viewSavedButton = findViewById(R.id.viewSavedButton);
        importDecryptButton = findViewById(R.id.importDecryptButton);

        // Setup Spinners
        ArrayAdapter<String> forceUpdateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"true", "false"});
        forceUpdateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        forceUpdateSpinner.setAdapter(forceUpdateAdapter);

        ArrayAdapter<String> adNetworkAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"admob", "adNetwork"});
        adNetworkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adNetworkSpinner.setAdapter(adNetworkAdapter);

        // Handle intent from SavedResponsesActivity for editing
        Intent intent = getIntent();
        if (intent.hasExtra("file_metadata")) {
            editingMetadata = (FileUtils.FileMetadata) intent.getSerializableExtra("file_metadata");
            if (editingMetadata.type.equals("Encrypt")) {
                if (!secretKeyInput.contains("")) {
                    showSecretKeyDialog(editingMetadata);
                } else {
                    loadMetadata(editingMetadata, secretKeyInput);
                }
            } else {
                loadMetadata(editingMetadata, null);
            }
        }

        encryptButton.setOnClickListener(v -> processData("Encrypt"));
        decryptButton.setOnClickListener(v -> processData("Decrypt"));
        viewSavedButton.setOnClickListener(v -> startActivity(new Intent(this, SavedResponsesActivity.class)));
        importDecryptButton.setOnClickListener(v -> showImportDecryptDialog());

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
                .setMessage("Are you sure you want to exit?")

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


    private void showSecretKeyDialog(FileUtils.FileMetadata metadata) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_decrypt, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.title);
        EditText encryptedInput = dialogView.findViewById(R.id.encryptedInput);
        Button decryptButton = dialogView.findViewById(R.id.decryptButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeButton);

        title.setText("Enter Secret Key for Decryption");
        encryptedInput.setHint("Enter Secret Key");
        encryptedInput.setText(secretKeyInput);
        decryptButton.setText("Load");

        AlertDialog dialog = builder.create();

        decryptButton.setOnClickListener(v -> {
            String secretKey = encryptedInput.getText().toString().trim();
            if (secretKey.isEmpty()) {
                Toast.makeText(this, "Enter secret key", Toast.LENGTH_SHORT).show();
                return;
            }
            loadMetadata(metadata, secretKey);
            dialog.dismiss();
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadMetadata(FileUtils.FileMetadata metadata, String secretKey) {
        try {
            String decrypted = metadata.type.equals("Encrypt") ? CryptoUtils.decrypt(metadata.content, secretKey) : metadata.content;
            JSONObject json = new JSONObject(decrypted);
//            secretKeyInput.setText(secretKey != null ? secretKey : "");
            populateFields(json);
        } catch (Exception e) {
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processData(String type) {
        String secretKey = secretKeyInput;
        String packageName = packageNameInput.getText().toString().trim();
        String versionCode = versionCodeInput.getText().toString().trim();
        String forceUpdate = forceUpdateSpinner.getSelectedItem().toString();
        String adNetwork = adNetworkSpinner.getSelectedItem().toString();
        String forceUpdateMessage = forceUpdateMessageInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        if (secretKey.isEmpty() || packageName.isEmpty() || versionCode.isEmpty() || forceUpdateMessage.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("packageName", packageName);
            json.put("versionCode", Integer.parseInt(versionCode));
            json.put("forceUpdate", Boolean.parseBoolean(forceUpdate));
            json.put("adNetwork", adNetwork);
            json.put("forceUpdateMessage", forceUpdateMessage);
            json.put("content", content);

            String data = json.toString();
            String result;
            if (type.equals("Encrypt")) {
                result = CryptoUtils.encrypt(data, secretKey);
                showResultDialog(result, type);
            } else {
                result = CryptoUtils.decrypt(data, secretKey);
                populateFields(new JSONObject(result));
            }
        } catch (Exception e) {
            Toast.makeText(this, type + " error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showImportDecryptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_import_decrypt, null);
        builder.setView(dialogView);

        TextView title = dialogView.findViewById(R.id.title);
        EditText encryptedInput = dialogView.findViewById(R.id.encryptedInput);
        Button decryptButton = dialogView.findViewById(R.id.decryptButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeButton);

        title.setText("Import Encrypted Data");

        AlertDialog dialog = builder.create();

        decryptButton.setOnClickListener(v -> {
            String encryptedData = encryptedInput.getText().toString().trim();
            String secretKey = secretKeyInput;

            if (encryptedData.isEmpty() || secretKey.isEmpty()) {
                Toast.makeText(this, "Enter both encrypted data and secret key", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String decrypted = CryptoUtils.decrypt(encryptedData, secretKey);
                JSONObject json = new JSONObject(decrypted);
                populateFields(json);
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Decrypt error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void populateFields(JSONObject json) throws Exception {
        packageNameInput.setText(json.getString("packageName"));
        versionCodeInput.setText(String.valueOf(json.getInt("versionCode")));
        forceUpdateSpinner.setSelection(json.getBoolean("forceUpdate") ? 0 : 1);
        adNetworkSpinner.setSelection(json.getString("adNetwork").equals("admob") ? 0 : 1);
        forceUpdateMessageInput.setText(json.getString("forceUpdateMessage"));
        contentInput.setText(json.getString("content"));
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
            try {
                String fileName;
                String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                if (editingMetadata != null) {
                    fileName = editingMetadata.fileName; // Use existing file name for edit
                } else {
                    fileName = type.toLowerCase() + "_" + timeStamp + ".txt"; // New file
                }
                FileUtils.saveFile(this, fileName, result, type, false);
                Toast.makeText(this, "Saved as " + fileName, Toast.LENGTH_SHORT).show();
                if (editingMetadata != null) {
                    // Notify SavedResponsesActivity to refresh
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated_file_name", fileName);
                    setResult(RESULT_OK, resultIntent);
                }
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}