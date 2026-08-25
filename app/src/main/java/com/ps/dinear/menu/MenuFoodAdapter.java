package com.ps.dinear.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

import java.util.List;

public class MenuFoodAdapter extends RecyclerView.Adapter<MenuFoodAdapter.ViewHolder> {
    private Context context;
    private List<MenuItem> list;
    private OnFoodClickListener listener;

    public interface OnFoodClickListener {
        void onFoodClick(MenuItem item);
    }

    public MenuFoodAdapter(Context context, List<MenuItem> list, OnFoodClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = list.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("$" + item.getPrice() + ".00");
        holder.tvDescription.setText(item.getDescription() != null ? item.getDescription() : "");
        holder.tvTag1.setText(item.getTag1() != null ? item.getTag1() : "");
        holder.tvTag2.setText(item.getTag2() != null ? item.getTag2() : "");

        String fullImageUrl = RetrofitClient.getFullUrl(item.getImageUrl());
        Glide.with(context).load(fullImageUrl).into(holder.ivFood);

        holder.itemView.setOnClickListener(v -> listener.onFoodClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFood;
        TextView tvName, tvPrice, tvDescription, tvTag1, tvTag2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFood = itemView.findViewById(R.id.ivFood);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvTag1 = itemView.findViewById(R.id.tvTag1);
            tvTag2 = itemView.findViewById(R.id.tvTag2);
        }
    }
}