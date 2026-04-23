package com.termux.sky.plugins;

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

    public interface OnLoginClick {
        void onLogin(Plugin plugin, int position);
    }

    public interface OnWatchClick {
        void onWatch(Plugin plugin, int position);
    }

    List<Plugin> list;
    Context ctx;
    OnDeleteClick deleteListener;
    OnLoginClick loginListener;
    OnWatchClick watchListener;

    public PluginAdapter(Context ctx, List<Plugin> list, OnLoginClick login_listener, OnWatchClick watch_listener, OnDeleteClick listener) {
        this.ctx = ctx;
        this.list = list;
        this.loginListener = login_listener;
        this.watchListener = watch_listener;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(ctx)
            .inflate(R.layout.plugin_manager_items, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {

        Plugin current = list.get(pos);

        h.name.setText(current.title);
        h.playlist.setText(current.playlist);

        new Thread(() -> {

            String checkUrl = (current.server_check_url != null && !current.server_check_url.isEmpty())
                ? current.server_check_url
                : current.playlist;

            boolean run = PluginUtils.isRunning(checkUrl);

            ((Activity) ctx).runOnUiThread(() -> {
                if (h.getAdapterPosition() != RecyclerView.NO_POSITION &&
                    list.get(h.getAdapterPosition()) == current) {

                    h.status.setText(run ? "Running" : "Stopped");
//                    h.toggle.setText(run ? "Stop" : "Start");
                }
            });

        }).start();

        h.playlist.setOnClickListener(v -> {
            ClipboardManager cm =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);

            cm.setPrimaryClip(ClipData.newPlainText("url", current.playlist));
            Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
        });

//        h.toggle.setOnClickListener(v -> {
//            new Thread(() -> {
//                boolean run = PluginUtils.isRunning(current.playlist);
//
//                if (!run) {
//                    PluginUtils.run(current.start);
//                } else {
//                    PluginUtils.run("pkill -f " + current.port);
//                }
//            }).start();
//        });

        if (current.login_url == null || current.login_url.trim().isEmpty()) {
            h.login.setVisibility(View.GONE);
        } else {
            h.login.setVisibility(View.VISIBLE);

            h.login.setOnClickListener(v -> {
                if (loginListener != null) {
                    loginListener.onLogin(current, h.getAdapterPosition());
                }
            });
        }

//        if (current.watch_url == null || current.watch_url.trim().isEmpty()) {
//            h.watch.setVisibility(View.GONE);
//        } else {
//            h.watch.setVisibility(View.VISIBLE);
//
//            h.watch.setOnClickListener(v -> {
//                if (watchListener != null) {
//                    watchListener.onWatch(current, h.getAdapterPosition());
//                }
//            });
//        }

        h.watch.setOnClickListener(v -> {
            if (watchListener != null) {
                watchListener.onWatch(current, h.getAdapterPosition());
            }
        });

        h.delete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(current, h.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {

        TextView name, status, playlist;
        ImageButton login, delete;
        LinearLayout watch;


        VH(View v) {
            super(v);
            name = v.findViewById(R.id.pluginName);
            status = v.findViewById(R.id.pluginStatus);
            playlist = v.findViewById(R.id.pluginPlaylist);
            login = v.findViewById(R.id.loginBtn);
            watch = v.findViewById(R.id.watchBtn);
            delete = v.findViewById(R.id.deleteBtn);
        }
    }
}
