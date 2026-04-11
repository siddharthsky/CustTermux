package com.termux.sky.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

import java.util.List;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.VH> {

    List<Plugin> list;
    Context ctx;

    public PluginAdapter(Context ctx, List<Plugin> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(ctx)
            .inflate(R.layout.item_plugin, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {

        Plugin p = list.get(pos);

        h.name.setText(p.title);
        h.playlist.setText(p.playlist);

        // 🔥 status check
        new Thread(() -> {
            boolean run = PluginUtils.isRunning(p.playlist);

            ((Activity)ctx).runOnUiThread(() -> {
                h.status.setText(run ? "Running" : "Stopped");
                h.toggle.setText(run ? "Stop" : "Start");
            });

        }).start();

        // 🔥 copy playlist
        h.playlist.setOnClickListener(v -> {
            ClipboardManager cm =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

            cm.setPrimaryClip(ClipData.newPlainText("url", p.playlist));
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
        });

        // 🔥 start/stop
        h.toggle.setOnClickListener(v -> {
            new Thread(() -> {
                boolean run = PluginUtils.isRunning(p.playlist);

                if (!run) {
                    PluginUtils.run(p.start);
                } else {
                    PluginUtils.run("pkill -f " + p.port);
                }
            }).start();
        });

        // 🔥 delete
        h.delete.setOnClickListener(v -> {
            list.remove(pos);
            PluginStorage.save(ctx, list);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {

        TextView name, status, playlist;
        Button toggle;
        Button delete;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.pluginName);
            status = v.findViewById(R.id.pluginStatus);
            playlist = v.findViewById(R.id.pluginPlaylist);
            toggle = v.findViewById(R.id.toggleBtn);
            delete = v.findViewById(R.id.deleteBtn);
        }
    }
}
