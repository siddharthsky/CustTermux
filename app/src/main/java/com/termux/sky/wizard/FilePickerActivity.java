package com.termux.sky.wizard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.termux.R;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilePickerActivity extends AppCompatActivity {

    private File currentDir;
    private FileAdapter adapter;
    private ListView listView;
    private TextView pathText;

    public static final String EXTRA_FILTERS = "EXTRA_FILTERS";
    private String[] fileFilters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);

        listView = findViewById(R.id.fileListView);
        pathText = findViewById(R.id.currentPath);

        fileFilters = getIntent().getStringArrayExtra(EXTRA_FILTERS);

        currentDir = new File(Environment.getExternalStorageDirectory().getPath());
        updateFileList();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selected = (File) adapter.getItem(position);

            if (selected == null) { // Go Back
                File parentDir = currentDir.getParentFile();
                if (parentDir != null) {
                    currentDir = parentDir;
                    updateFileList();
                }
            } else if (selected.isDirectory()) {
                currentDir = selected;
                updateFileList();
            } else {
                Intent resultIntent = new Intent();
                resultIntent.setData(Uri.fromFile(selected));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void updateFileList() {
        pathText.setText(currentDir.getAbsolutePath());
        File[] allFiles = currentDir.listFiles();
        List<File> filteredList = new ArrayList<>();

        if (currentDir.getParentFile() != null) {
            filteredList.add(null); // Go Back
        }

        if (allFiles != null) {
            for (File file : allFiles) {
                if (file.isDirectory()) {
                    filteredList.add(file);
                } else {
                    if (fileFilters == null || fileFilters.length == 0) {
                        filteredList.add(file);
                    } else {
                        boolean matches = false;
                        String fileName = file.getName().toLowerCase();
                        for (String filter : fileFilters) {
                            if (fileName.endsWith(filter.toLowerCase())) {
                                matches = true;
                                break;
                            }
                        }
                        if (matches) {
                            filteredList.add(file);
                        }
                    }
                }
            }

            filteredList.sort((a, b) -> {
                if (a == null || b == null) return 0;
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
        }

        adapter = new FileAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private static class FileAdapter extends ArrayAdapter<File> {
        private final LayoutInflater inflater;

        public FileAdapter(Context context, List<File> files) {
            super(context, android.R.layout.simple_list_item_1, files);
            inflater = LayoutInflater.from(context);
        }

        @NonNull
        @SuppressLint("SetTextI18n")
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView tv = view.findViewById(android.R.id.text1);
            File file = getItem(position);

            if (file == null) {
                tv.setText("⬅️ .. (Go Back)");
            } else {
                tv.setText(file.isDirectory() ? "📁 " + file.getName() : "📄 " + file.getName());
            }

            tv.setTextColor(Color.WHITE);
            return view;
        }
    }

    @Override
    public void onBackPressed() {
        if (currentDir.getParentFile() != null && !currentDir.getPath().equals(Environment.getExternalStorageDirectory().getPath())) {
            currentDir = currentDir.getParentFile();
            updateFileList();
        } else {
            super.onBackPressed();
        }
    }
}
