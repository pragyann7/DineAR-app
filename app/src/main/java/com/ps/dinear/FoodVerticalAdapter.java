package com.ps.dinear;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
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

public class FoodVerticalAdapter extends RecyclerView.Adapter<FoodVerticalAdapter.ViewHolder> {

    private final Context context;
    private final List<MenuItem> items;

    public FoodVerticalAdapter(Context context, List<MenuItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvRestaurantName.setText(item.getRestaurantName());

        if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0) {
            holder.tvPrice.setText(item.getCurrency() + " " + (int) item.getDiscountPrice().doubleValue());
            holder.tvOriginalPrice.setText(item.getCurrency() + " " + (int) item.getPrice());
            holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvPrice.setText(item.getCurrency() + " " + (int) item.getPrice());
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        if (holder.flArBadge != null) {
            holder.flArBadge.setVisibility(item.has3d() ? View.VISIBLE : View.GONE);
        }

        String fullImageUrl = RetrofitClient.getFullUrl(context, item.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .placeholder(R.drawable.bg_skeleton)
                .error(R.drawable.burger)
                .centerCrop()
                .into(holder.ivFood);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FoodDetailsActivity.class);
            intent.putExtra("selectedItem", item);
            if (item.getRestaurantId() != null) {
                intent.putExtra("restaurantId", item.getRestaurantId());
                intent.putExtra("restaurantSlug", item.getRestaurantSlug());
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFood;
        TextView tvName, tvRestaurantName, tvPrice, tvOriginalPrice;
        View flArBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFood = itemView.findViewById(R.id.ivFood);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            flArBadge = itemView.findViewById(R.id.flArBadge);
        }
    }
}
