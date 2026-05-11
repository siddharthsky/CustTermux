package com.termux.sky.filehandlers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.termux.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileManagerActivity extends AppCompatActivity {

    private File currentDir = null;
    private FileAdapter adapter;
    private ListView listView;
    private TextView pathText;
    private LinearLayout actionToolbar;

    private File clipboardFile = null;
    private boolean isMoveOperation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        listView = findViewById(R.id.fileListView);
        pathText = findViewById(R.id.currentPath);
        actionToolbar = findViewById(R.id.actionToolbar);

        showRootMenu();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selected = (File) adapter.getItem(position);

            if (currentDir == null) {
                currentDir = selected;
                updateFileList();
            } else if (selected == null) {
                goBack();
            } else if (selected.isDirectory()) {
                currentDir = selected;
                updateFileList();
            } else {
                openFile(selected);
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            File selected = (File) adapter.getItem(position);
            if (selected != null) {
                showFileOptions(selected);
            }
            return true;
        });
    }

    private void showRootMenu() {
        currentDir = null;
        pathText.setText("Select Storage");
        List<File> roots = new ArrayList<>();

        roots.add(Environment.getExternalStorageDirectory());

        roots.add(getFilesDir());

        File[] externalDirs = getExternalFilesDirs(null);
        for (File f : externalDirs) {
            if (f != null && !f.getAbsolutePath().contains(getPackageName())) {
                String path = f.getAbsolutePath().split("/Android")[0];
                roots.add(new File(path));
            }
        }

        adapter = new FileAdapter(this, roots, true);
        listView.setAdapter(adapter);
    }

    private void updateFileList() {
        if (currentDir == null) {
            showRootMenu();
            return;
        }

        pathText.setText(currentDir.getAbsolutePath());
        File[] allFiles = currentDir.listFiles();
        List<File> filteredList = new ArrayList<>();

        filteredList.add(null); // The "Go Back" item

        if (allFiles != null) {
            String appBasePath = getApplicationInfo().dataDir;

            for (File file : allFiles) {
                if (currentDir.getAbsolutePath().equals(appBasePath)) {
                    if (file.getName().equals("files")) {
                        filteredList.add(file);
                    }
                } else {
                    filteredList.add(file);
                }
            }

            filteredList.sort((a, b) -> {
                if (a == null || b == null) return 0;
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
        }

        adapter = new FileAdapter(this, filteredList, false);
        listView.setAdapter(adapter);
        updateToolbar();
    }

    private void showFileOptions(File file) {
        String[] options = {"Copy", "Move", "Delete", "Rename"};
        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Copy
                        clipboardFile = file;
                        isMoveOperation = false;
                        break;
                    case 1: // Move
                        clipboardFile = file;
                        isMoveOperation = true;
                        break;
                    case 2: // Delete
                        confirmDelete(file);
                        break;
                }
                updateToolbar();
            }).show();
    }

    private void updateToolbar() {
        if (clipboardFile != null) {
            actionToolbar.setVisibility(View.VISIBLE);
            findViewById(R.id.btnPaste).setOnClickListener(v -> performPaste());
            findViewById(R.id.btnCancel).setOnClickListener(v -> {
                clipboardFile = null;
                updateToolbar();
            });
        } else {
            actionToolbar.setVisibility(View.GONE);
        }
    }

    private void performPaste() {
        if (clipboardFile == null || currentDir == null) return;

        File dest = new File(currentDir, clipboardFile.getName());
        try {
            if (isMoveOperation) {
                if (clipboardFile.renameTo(dest)) {
                    Toast.makeText(this, "Moved successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                copyFile(clipboardFile, dest);
                Toast.makeText(this, "Copied successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        clipboardFile = null;
        updateFileList();
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileChannel in = new FileInputStream(source).getChannel();
             FileChannel out = new FileOutputStream(dest).getChannel()) {
            out.transferFrom(in, 0, in.size());
        }
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
            .setMessage("Delete " + file.getName() + "?")
            .setPositiveButton("Delete", (d, w) -> {
                if (file.delete()) {
                    updateFileList();
                } else {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(
                this,
                "com.termux.fileprovider",
                file
            );
            String mimeType = getMimeType(file);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "FileProvider not configured correctly.", Toast.LENGTH_LONG).show();
            Log.e("FileManager", "FileProvider Error: ", e);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file type.", Toast.LENGTH_SHORT).show();
            Log.e("FileManager", "Open File Error: ", e);
        }
    }

    private String getMimeType(File file) {
        String mimeType = "*/*";
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        if (extension != null && !extension.isEmpty()) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (type != null) {
                mimeType = type;
            }
        }
        return mimeType;
    }

    private void goBack() {
        if (currentDir == null) {
            super.onBackPressed();
            return;
        }

        String currentPath = currentDir.getAbsolutePath();
        String deviceRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        String appFilesRoot = getFilesDir().getAbsolutePath();

        if (currentPath.equals(deviceRoot) || currentPath.equals(appFilesRoot)) {
            currentDir = null;
            updateFileList();
        } else {
            currentDir = currentDir.getParentFile();
            updateFileList();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    // --- ADAPTER ---
    private static class FileAdapter extends ArrayAdapter<File> {
        private boolean isRootView;
        private final LayoutInflater inflater;

        public FileAdapter(Context context, List<File> files, boolean isRootView) {
            super(context, android.R.layout.simple_list_item_1, files);
            this.isRootView = isRootView;
            this.inflater = LayoutInflater.from(context); // Initialize the inflater
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            View view = convertView;
            if (view == null) {
                view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView tv = view.findViewById(android.R.id.text1);
            File file = getItem(position);

            if (isRootView) {
                tv.setText("💾 " + (file != null ? file.getAbsolutePath() : "Unknown"));
            } else if (file == null) {
                tv.setText("⬅️ .. (Go Back)");
            } else {
                tv.setText(file.isDirectory() ? "📁 " + file.getName() : "📄 " + file.getName());
            }

            tv.setTextColor(Color.WHITE);
            return view;
        }
    }
}
