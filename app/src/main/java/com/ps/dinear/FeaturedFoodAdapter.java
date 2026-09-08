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

public class FeaturedFoodAdapter extends RecyclerView.Adapter<FeaturedFoodAdapter.ViewHolder> {

    private List<MenuItem> items = new ArrayList<>();
    private Context context;

    public FeaturedFoodAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<MenuItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_featured_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);
        
        holder.tvName.setText(item.getName());
        
        if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0) {
            holder.tvPrice.setText(item.getCurrency() + " " + (int)item.getDiscountPrice().doubleValue());
        } else {
            holder.tvPrice.setText(item.getCurrency() + " " + (int)item.getPrice());
        }
        
        String fullImageUrl = RetrofitClient.getFullUrl(context, item.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivFood);
                
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FoodDetailsActivity.class);
            intent.putExtra("selectedItem", item);
            if (item.getRestaurantId() != null) {
                intent.putExtra("restaurantId", item.getRestaurantId());
                intent.putExtra("restaurantName", item.getRestaurantName());
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
        TextView tvName, tvPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFood = itemView.findViewById(R.id.ivFood);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
        }
    }
}
