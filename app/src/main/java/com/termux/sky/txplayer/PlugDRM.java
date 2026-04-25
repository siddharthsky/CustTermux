package com.termux.sky.txplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.termux.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlugDRM extends AppCompatActivity {

    RecyclerView recyclerView;
    ProgressBar progressBar;
    EditText searchBar;
    ChannelAdapter adapter;
    List<ChannelModel> fullList = new ArrayList<>();

    ImageButton btnReload; // Define the reload button
    String port;
    String playlistData;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channels_activity);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchBar = findViewById(R.id.searchChannel);
        btnReload = findViewById(R.id.btnReload);

        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(this::showPopupMenu);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        playlistData = getIntent().getStringExtra("playlist_url");
        int portInt = getIntent().getIntExtra("port", 0);
        port = String.valueOf(portInt);

        loadData(false);

        btnReload.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            loadData(true);
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showPopupMenu(View view) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.channel_menu, popup.getMenu());

        // Update menu text based on current state
        popup.getMenu().findItem(R.id.menu_layout_toggle)
            .setTitle("Clear Favorite");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_layout_toggle) {
                loadData(true);
                Toast.makeText(this, "Clearing...", Toast.LENGTH_SHORT).show();
                Log.d("PlugDRM","Cleared fav.");
                return true;
            } else if (id == R.id.menu_refresh) {
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
                loadData(true);
                return true;
            }
            return false;
        });
        popup.show();
    }


    private void loadData(boolean forceRefresh) {
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            List<ChannelModel> channels;

            // Check if data exists in SharedPreferences and we aren't forcing a refresh
            if (!forceRefresh && M3UParser.existsInPrefs(this, port)) {
                channels = M3UParser.getFromPrefs(this, port);
            } else {
                // Parse manually
                String content = playlistData;
                if (playlistData != null && playlistData.startsWith("http")) {
                    content = downloadUrl(playlistData);
                }
                channels = M3UParser.parse(content);

                // Save to prefs for next time
                M3UParser.saveToPrefs(this, port, channels);
            }

            this.fullList = channels;

            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (channels.isEmpty()) {
                    Toast.makeText(this, "No data found!", Toast.LENGTH_SHORT).show();
                }

                // Update UI
                setupAdapter(channels);
//                if (adapter == null) {
//                    adapter = new ChannelAdapter(channels, this::playVideo, this::toggleFavorite);
//                    recyclerView.setAdapter(adapter);
//                } else {
//                    adapter.updateList(channels);
//                }
            });
        });
    }

    private void processPlaylist(String data) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            String content = data;
            if (data.startsWith("http")) {
                content = downloadUrl(data);
            }

            List<ChannelModel> channels = M3UParser.parse(content);
            this.fullList = channels;

            // Extract Port
            String port = "default";
            Pattern p = Pattern.compile(":(\\d+)");
            Matcher m = p.matcher(data);
            if (m.find()) port = m.group(1);

            // Save all tags (Language, Type, DRM)
            M3UParser.saveToPrefs(this, port, channels);

            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (channels.isEmpty()) {
                    Toast.makeText(this, "No channels found!", Toast.LENGTH_SHORT).show();
                }
                adapter = new ChannelAdapter(channels, this::playVideo, this::toggleFavorite);
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void filter(String text) {
        List<ChannelModel> filteredList = new ArrayList<>();
        for (ChannelModel item : fullList) {
            if (item.name.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (adapter != null) adapter.updateList(filteredList);
    }

    private void playVideo(ChannelModel channel) {
        Intent intent = new Intent(this, ExoPlayerActivityDRM.class);
        intent.putExtra("url", channel.url);
        intent.putExtra("name", channel.name);
        intent.putExtra("logo_url", channel.logo);
        intent.putExtra("license_key", channel.licenseKey);
        intent.putExtra("license_type", channel.licenseType);
        intent.putExtra("user_agent", channel.userAgent);
        intent.putExtra("manifest_type", channel.manifestType);
        intent.putExtra("plugin_port", port);
        startActivity(intent);
    }

    private String downloadUrl(String urlString) {
        try {
            java.util.Scanner s = new java.util.Scanner(new java.net.URL(urlString).openStream(), "UTF-8").useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } catch (Exception e) { return ""; }
    }

    private void setupAdapter(List<ChannelModel> channels) {
        // Sort so favorites appear at the top
        sortList(channels);

        if (adapter == null) {
            adapter = new ChannelAdapter(channels, this::playVideo, this::toggleFavorite);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(channels);
        }
    }

    private void toggleFavorite(ChannelModel channel) {
        channel.isFavorite = !channel.isFavorite;

        // Save state to SharedPreferences
        M3UParser.saveToPrefs(this, port, fullList);

        // Re-sort and notify UI
        sortList(fullList);
        adapter.updateList(fullList);

        String msg = channel.isFavorite ? "Added to Favorites" : "Removed from Favorites";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void sortList(List<ChannelModel> list) {
        list.sort((o1, o2) -> {
            if (o1.isFavorite != o2.isFavorite) {
                return o1.isFavorite ? -1 : 1;
            }
            return 0;
        });
    }
}
