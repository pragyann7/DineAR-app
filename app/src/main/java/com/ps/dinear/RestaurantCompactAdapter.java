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
import com.ps.dinear.data.model.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantCompactAdapter extends RecyclerView.Adapter<RestaurantCompactAdapter.ViewHolder> {

    private List<Restaurant> items = new ArrayList<>();
    private final Context context;

    public RestaurantCompactAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<Restaurant> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant_compact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = items.get(position);
        
        holder.tvName.setText(restaurant.getName());
        holder.tvRating.setText(String.valueOf(restaurant.getRating()));
        holder.tvTime.setText(restaurant.getDeliveryTime());
        holder.tvCuisine.setText(restaurant.getCuisine());

        String fullImageUrl = RetrofitClient.getFullUrl(context, restaurant.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .placeholder(R.drawable.bg_skeleton)
                .centerCrop()
                .into(holder.ivImage);
                
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RestaurantDetailsActivity.class);
            intent.putExtra("restaurantId", restaurant.getId());
            intent.putExtra("restaurantSlug", restaurant.getSlug());
            intent.putExtra("restaurantName", restaurant.getName());
            intent.putExtra("cuisine", restaurant.getCuisine());
            intent.putExtra("rating", restaurant.getRating());
            intent.putExtra("description", restaurant.getDescription());
            intent.putExtra("deliveryTime", restaurant.getDeliveryTime());
            intent.putExtra("imageUrl", restaurant.getImageUrl());
            intent.putExtra("bannerImage", restaurant.getBannerImage());
            intent.putExtra("address", restaurant.getAddress());
            intent.putExtra("isFeatured", restaurant.isFeatured());
            intent.putExtra("deliveryCharge", restaurant.getDeliveryCharge());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 8);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvRating, tvTime, tvCuisine;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivRestCompact);
            tvName = itemView.findViewById(R.id.tvRestNameCompact);
            tvRating = itemView.findViewById(R.id.tvRatingCompact);
            tvTime = itemView.findViewById(R.id.tvTimeCompact);
            tvCuisine = itemView.findViewById(R.id.tvCuisineCompact);
        }
    }
}
