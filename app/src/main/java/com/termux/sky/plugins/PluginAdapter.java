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
import androidx.appcompat.widget.PopupMenu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.VH> {

    private static final int MENU_LOGIN = 1;
    private static final int MENU_UPDATE = 2;
    private static final int MENU_SUPPORT = 3;
    private static final int MENU_DELETE = 4;

    public interface OnDeleteClick {
        void onDelete(Plugin plugin, int position);
    }

    public interface OnLoginClick {
        void onLogin(Plugin plugin, int position);
    }
    public interface OnUpdateClick {
        void onUpdate(Plugin plugin, int position);
    }

    public interface OnWatchClick {
        void onWatch(Plugin plugin, int position);
    }

    public interface OnSupportClick {
        void onSupport(Plugin plugin, int position);
    }

    List<Plugin> list;
    Context ctx;
    OnDeleteClick deleteListener;
    OnLoginClick loginListener;
    OnUpdateClick updateListener;
    OnWatchClick watchListener;
    OnSupportClick supportListener;

    public PluginAdapter(Context ctx, List<Plugin> list, OnLoginClick login_listener, OnUpdateClick update_listener, OnWatchClick watch_listener, OnSupportClick support_listener, OnDeleteClick listener) {
        this.ctx = ctx;
        this.list = list;
        this.loginListener = login_listener;
        this.updateListener = update_listener;
        this.watchListener = watch_listener;
        this.supportListener = support_listener;
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
        h.playlist.setText(getDisplayName(current.playlist));

        final boolean isTool = (current.tool != null) ? current.tool : false;

        h.status.setText("Checking...");

        new Thread(() -> {
            if (!isTool && (current == null || current.playlist == null)) {
                ((Activity) ctx).runOnUiThread(() -> {
                    int adapterPos = h.getAdapterPosition();
                    if (adapterPos != RecyclerView.NO_POSITION) {
                        h.status.setText("Stopped");
                    }
                });
                return;
            }

            boolean run = isTool || PluginUtils.isRunning(
                (current.server_check_url != null && !current.server_check_url.isEmpty())
                    ? current.server_check_url
                    : current.playlist,
                isTool
            );

            ((Activity) ctx).runOnUiThread(() -> {
                int adapterPos = h.getAdapterPosition();

                if (adapterPos != RecyclerView.NO_POSITION) {
                    Plugin itemAtPos = list.get(adapterPos);

                    if (itemAtPos != null) {
                        boolean isSameItem = false;

                        if (isTool) {
                            isSameItem = itemAtPos.title != null && itemAtPos.title.equals(current.title);
                        } else {
                            isSameItem = itemAtPos.playlist != null && itemAtPos.playlist.equals(current.playlist);
                        }


                        if (isSameItem) {
                            h.status.setText(run ? "Running" : "Stopped");
                        }
                    }
                }
            });
        }).start();

        h.playlist.setOnClickListener(v -> {
            if (current.playlist != null &&
                (current.playlist.startsWith("content://") || current.playlist.startsWith("file://"))) {
                Toast.makeText(ctx, "Local file path cannot be copied", Toast.LENGTH_SHORT).show();
                return;
            }

            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("url", current.playlist));
            Toast.makeText(ctx, "Playlist URL Copied", Toast.LENGTH_SHORT).show();
        });

        if (current.login_url == null || current.login_url.trim().isEmpty()) {
            h.login.setVisibility(View.GONE);
            h.login.setOnClickListener(null);
        } else {
            h.login.setVisibility(View.VISIBLE);
            h.login.setOnClickListener(v -> {
                if (loginListener != null) {
                    loginListener.onLogin(current, h.getAdapterPosition());
                }
            });
        }


        if (isTool) {
            h.watchBtnText.setText("OPEN");
        }
        h.watch.setVisibility(View.VISIBLE);
        h.watch.setOnClickListener(v -> {
            if (watchListener != null) {
                watchListener.onWatch(current, h.getAdapterPosition());
            }
        });

        h.settingsBtn.setOnClickListener(v -> {

            Context wrapper = new ContextThemeWrapper(ctx, R.style.CustomPopupMenuTheme);
            PopupMenu popup = new PopupMenu(wrapper, h.settingsBtn);

            try {
                Field field = popup.getClass().getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popup);
                Class<?> cls = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceIcons = cls.getMethod("setForceShowIcon", boolean.class);
                setForceIcons.invoke(menuPopupHelper, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (current.login_url != null && !current.login_url.trim().isEmpty()) {
                popup.getMenu().add(Menu.NONE, MENU_LOGIN, 0, "Login")
                    .setIcon(R.drawable.tx_passkey);
            }

            if (Boolean.TRUE.equals(current.updatable)) {
                popup.getMenu().add(Menu.NONE, MENU_UPDATE, 1, "Update")
                    .setIcon(R.drawable.tx_reload);
            }

            boolean hasSupport = (current.support_url != null && !current.support_url.trim().isEmpty())
                || (current.repo != null && !current.repo.trim().isEmpty());

            if (hasSupport) {
                popup.getMenu().add(Menu.NONE, MENU_SUPPORT, 2, "Support")
                    .setIcon(R.drawable.tx_support);
            }

            popup.getMenu().add(Menu.NONE, MENU_DELETE, 3, "Delete Plugin")
                .setIcon(R.drawable.tx_del);

            popup.setOnMenuItemClickListener(item -> {
                int position = h.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return false;

                switch (item.getItemId()) {
                    case MENU_LOGIN:
                        if (loginListener != null) loginListener.onLogin(current, position);
                        return true;
                    case MENU_UPDATE:
                        if (updateListener != null) updateListener.onUpdate(current, position);
                        return true;
                    case MENU_SUPPORT:
                        if (supportListener != null) supportListener.onSupport(current, position);
                        return true;
                    case MENU_DELETE:
                        if (deleteListener != null) deleteListener.onDelete(current, position);
                        return true;
                    default:
                        return false;
                }
            });

            popup.show();
        });
    }

    private String getDisplayName(String url) {
        if (url == null) return "";

        if (url.startsWith("content://") || url.startsWith("file://")) {
            try {
                android.net.Uri uri = android.net.Uri.parse(url);
                String lastSegment = uri.getLastPathSegment();

                if (lastSegment != null && lastSegment.contains(":")) {
                    return lastSegment.substring(lastSegment.lastIndexOf(":") + 1);
                }
                return lastSegment != null ? lastSegment : "Local File";
            } catch (Exception e) {
                return "Local File";
            }
        }

        return url;
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {

        TextView name, status, playlist, watchBtnText;
        ImageButton login, settingsBtn;
        LinearLayout watch;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.pluginName);
            status = v.findViewById(R.id.pluginStatus);
            playlist = v.findViewById(R.id.pluginPlaylist);
            watch = v.findViewById(R.id.watchBtn);
            watchBtnText = v.findViewById(R.id.watchBtnText);

            login = v.findViewById(R.id.loginBtn);
            settingsBtn = v.findViewById(R.id.settingsBtn);
        }
    }
}
