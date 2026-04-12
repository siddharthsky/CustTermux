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

    public interface OnDeleteClick {
        void onDelete(Plugin plugin, int position);
    }

    List<Plugin> list;
    Context ctx;
    OnDeleteClick deleteListener;

    public PluginAdapter(Context ctx, List<Plugin> list, OnDeleteClick listener) {
        this.ctx = ctx;
        this.list = list;
        this.deleteListener = listener;
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

        new Thread(() -> {
            boolean run = PluginUtils.isRunning(p.playlist);

            ((Activity)ctx).runOnUiThread(() -> {
                h.status.setText(run ? "Running" : "Stopped");
                h.toggle.setText(run ? "Stop" : "Start");
            });

        }).start();

        h.playlist.setOnClickListener(v -> {
            ClipboardManager cm =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

            cm.setPrimaryClip(ClipData.newPlainText("url", p.playlist));
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
        });

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

        h.delete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(p, h.getAdapterPosition());
            }
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
