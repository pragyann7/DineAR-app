package com.ps.dinear.menu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.ps.dinear.CartManager;
import com.ps.dinear.FavoritesManager;
import com.ps.dinear.MenuItem;
import com.ps.dinear.R;
import com.ps.dinear.RetrofitClient;

import java.util.List;

public class MenuFoodAdapter extends RecyclerView.Adapter<MenuFoodAdapter.ViewHolder> {
    private Context context;
    private List<MenuItem> list;
    private OnFoodClickListener listener;
    private int restaurantId;
    private String restaurantName;
    private double deliveryCharge = 50.00;

    public interface OnFoodClickListener {
        void onFoodClick(MenuItem item);
    }

    public MenuFoodAdapter(Context context, List<MenuItem> list, OnFoodClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }
    
    public void setRestaurantId(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public void setDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public void updateList(List<MenuItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
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
        
        if (item.getDiscountPrice() != null && item.getDiscountPrice() > 0) {
            holder.tvPrice.setText(item.getCurrency() + " " + (int)item.getDiscountPrice().doubleValue());
        } else {
            holder.tvPrice.setText(item.getCurrency() + " " + (int)item.getPrice());
        }

        holder.tvDescription.setText(item.getDescription() != null ? item.getDescription() : "");
        holder.tvTag1.setText(item.getTag1() != null ? item.getTag1() : "");
        holder.tvTag2.setText(item.getTag2() != null ? item.getTag2() : "");

        String fullImageUrl = RetrofitClient.getFullUrl(context, item.getImageUrl());
        Glide.with(context).load(fullImageUrl).into(holder.ivFood);

        // Favorite Logic (Premium Glow Effect)
        boolean isFav = FavoritesManager.getInstance().isFoodFavorite(item.getId());
        if (isFav) {
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.orange_fav_bg));
            holder.cardView.setStrokeWidth(3);
            holder.ivFavorite.setVisibility(View.VISIBLE);
        } else {
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.white));
            holder.cardView.setStrokeWidth(0);
            holder.ivFavorite.setVisibility(View.GONE);
        }

        holder.btnAddToCart.setOnClickListener(v -> {
            if (restaurantId != -1) {
                CartManager.getInstance().addItem(item, restaurantId, restaurantName, deliveryCharge);
                Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onFoodClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivFood, ivFavorite, btnAddToCart;
        TextView tvName, tvPrice, tvDescription, tvTag1, tvTag2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivFood = itemView.findViewById(R.id.ivFood);
            ivFavorite = itemView.findViewById(R.id.ivFavoriteMenuFood);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCartMenu);
            tvName = itemView.findViewById(R.id.tvFoodName);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
            tvTag1 = itemView.findViewById(R.id.tvTag1);
            tvTag2 = itemView.findViewById(R.id.tvTag2);
        }
    }
}