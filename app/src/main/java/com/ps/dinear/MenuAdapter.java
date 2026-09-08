package com.ps.dinear;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.ps.dinear.data.model.Restaurant;
import com.ps.dinear.menu.FoodDetailsActivity;

import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

    private List<MenuItem> list = new ArrayList<>();
    private List<Restaurant> restaurants = new ArrayList<>();
    private Context context;

    public MenuAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<MenuItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        MenuItem item = list.get(position);

        holder.name.setText(item.getName());
        holder.price.setText("Rs. " + (int)item.getPrice());

        String resName = item.getRestaurantName();
        if ((resName == null || resName.isEmpty()) && item.getRestaurantId() != null && restaurants != null) {
            for (Restaurant r : restaurants) {
                if (r.getId() == item.getRestaurantId().intValue()) {
                    resName = r.getName();
                    break;
                }
            }
        }

        if (resName != null && !resName.isEmpty()) {
            holder.tvRestaurantName.setText(resName);
            holder.tvRestaurantName.setVisibility(View.VISIBLE);
        } else {
            holder.tvRestaurantName.setVisibility(View.GONE);
        }

        String fullImageUrl = RetrofitClient.getFullUrl(context, item.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .into(holder.image);

        holder.btnViewAR.setOnClickListener(v -> {
            Intent intent = new Intent(context, ARActivity.class);
            intent.putExtra("selectedItem", item);
            if (item.getRestaurantId() != null) {
                intent.putExtra("restaurantId", item.getRestaurantId());
            }
            context.startActivity(intent);
        });

        // Favorite Logic (Premium Glow Effect)
        boolean isFav = FavoritesManager.getInstance().isFoodFavorite(item.getId());
        if (isFav) {
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.orange_fav_bg));
            holder.cardView.setStrokeWidth(3);
            holder.ivFavoriteFood.setVisibility(View.VISIBLE);
        } else {
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.white));
            holder.cardView.setStrokeWidth(0);
            holder.ivFavoriteFood.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FoodDetailsActivity.class);
            intent.putExtra("selectedItem", item);
            
            Restaurant foundRes = null;
            if (item.getRestaurantId() != null && restaurants != null) {
                for (Restaurant r : restaurants) {
                    if (r.getId() == item.getRestaurantId().intValue()) {
                        foundRes = r;
                        break;
                    }
                }
            }
            
            if (foundRes != null) {
                intent.putExtra("restaurantId", foundRes.getId());
                intent.putExtra("restaurantSlug", foundRes.getSlug());
                intent.putExtra("restaurantName", foundRes.getName());
                intent.putExtra("cuisine", foundRes.getCuisine());
                intent.putExtra("rating", foundRes.getRating());
                intent.putExtra("description", foundRes.getDescription());
                intent.putExtra("deliveryTime", foundRes.getDeliveryTime());
                intent.putExtra("imageUrl", foundRes.getImageUrl());
                intent.putExtra("bannerImage", foundRes.getBannerImage());
                intent.putExtra("address", foundRes.getAddress());
                intent.putExtra("latitude", foundRes.getLatitude());
                intent.putExtra("longitude", foundRes.getLongitude());
                intent.putExtra("deliveryCharge", foundRes.getDeliveryCharge());
            } else if (item.getRestaurantId() != null) {
                intent.putExtra("restaurantId", item.getRestaurantId());
                intent.putExtra("restaurantName", item.getRestaurantName());
                intent.putExtra("restaurantSlug", item.getRestaurantSlug());
            }
            
            context.startActivity(intent);
        });
    }
    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView name, price, tvRestaurantName;
        View btnViewAR;
        ImageView image, ivFavoriteFood;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            name = itemView.findViewById(R.id.name);
            price = itemView.findViewById(R.id.price);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            btnViewAR = itemView.findViewById(R.id.btnViewAR);

            image = itemView.findViewById(R.id.image);
            ivFavoriteFood = itemView.findViewById(R.id.ivFavoriteFood);
        }
    }
}
