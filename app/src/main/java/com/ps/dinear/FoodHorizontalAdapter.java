package com.ps.dinear;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ps.dinear.menu.FoodDetailsActivity;

import java.util.ArrayList;
import java.util.List;

public class FoodHorizontalAdapter extends RecyclerView.Adapter<FoodHorizontalAdapter.ViewHolder> {

    private List<MenuItem> items = new ArrayList<>();
    private final Context context;

    public FoodHorizontalAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<MenuItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_card_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("Rs. " + (int)item.getPrice());

        if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0) {
            int discountPercent = (int) ((1 - (item.getDiscountPrice() / item.getPrice())) * 100);
            if (discountPercent > 0) {
                holder.tvBadge.setText(discountPercent + "% OFF");
                holder.tvBadge.setVisibility(View.VISIBLE);
                holder.tvPrice.setText("Rs. " + (int)item.getDiscountPrice().doubleValue());
            } else {
                holder.tvBadge.setVisibility(View.GONE);
            }
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        String fullImageUrl = RetrofitClient.getFullUrl(context, item.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .placeholder(R.drawable.bg_skeleton)
                .centerCrop()
                .into(holder.ivFood);
                
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FoodDetailsActivity.class);
            intent.putExtra("selectedItem", item);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 10); // Show max 10 in horizontal lists
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFood;
        TextView tvName, tvPrice, tvBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFood = itemView.findViewById(R.id.ivFoodHome);
            tvName = itemView.findViewById(R.id.tvFoodNameHome);
            tvPrice = itemView.findViewById(R.id.tvFoodPriceHome);
            tvBadge = itemView.findViewById(R.id.tvDiscountBadge);
        }
    }
}
