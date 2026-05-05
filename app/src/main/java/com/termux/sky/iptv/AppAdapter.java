package com.termux.sky.iptv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.termux.R;

public class AppAdapter extends ListAdapter<AppModel, AppAdapter.ViewHolder> {

    Context context;
    OnAppClick listener;

    public interface OnAppClick {
        void onClick(AppModel app);
    }

    public AppAdapter(OnAppClick listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        View view = LayoutInflater.from(context)
            .inflate(R.layout.iptv_manager_items, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppModel app = getItem(position);

        holder.name.setText(app.appName);

        if ("hana_player".equals(app.packageName)) {
            holder.icon.setImageResource(R.drawable.op);
//            holder.icon.setColorFilter(android.graphics.Color.parseColor("#FADADD"));
            holder.icon.clearColorFilter();
        } else {
            try {
                holder.icon.setImageDrawable(
                    context.getPackageManager().getApplicationIcon(app.packageName)
                );
                holder.icon.clearColorFilter();
            } catch (Exception e) {
                holder.icon.setImageDrawable(null);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(app));
    }

    // ✅ DiffUtil
    static final DiffUtil.ItemCallback<AppModel> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<AppModel>() {
            @Override
            public boolean areItemsTheSame(AppModel oldItem, AppModel newItem) {
                return oldItem.packageName.equals(newItem.packageName);
            }

            @Override
            public boolean areContentsTheSame(AppModel oldItem, AppModel newItem) {
                return oldItem.appName.equals(newItem.appName);
            }
        };
}
