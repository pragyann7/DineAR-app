package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class QuickPickAdapter extends RecyclerView.Adapter<QuickPickAdapter.ViewHolder> {

    public interface OnQuickPickClickListener {
        void onQuickPickClick(String query);
    }

    private final Context context;
    private final List<QuickPickItem> items;
    private final OnQuickPickClickListener listener;

    public QuickPickAdapter(Context context, OnQuickPickClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
        populateItems();
    }

    private void populateItems() {
        items.add(new QuickPickItem("Momo", R.drawable.burger)); // Use appropriate icons if available
        items.add(new QuickPickItem("Pizza", R.drawable.burger));
        items.add(new QuickPickItem("Burger", R.drawable.burger));
        items.add(new QuickPickItem("Healthy", R.drawable.bg_skeleton));
        items.add(new QuickPickItem("Bakery", R.drawable.bg_skeleton));
        items.add(new QuickPickItem("Cafe", R.drawable.bg_skeleton));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quick_pick, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickPickItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.ivImage.setImageResource(item.iconRes);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onQuickPickClick(item.name);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivQuickPick);
            tvName = itemView.findViewById(R.id.tvQuickPickName);
        }
    }

    private static class QuickPickItem {
        String name;
        int iconRes;

        QuickPickItem(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }
}
