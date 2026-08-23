package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ps.dinear.data.model.Restaurant;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {
    private Context context;
    private List<Restaurant> list;
    private OnRestaurantClickListener listener;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }

    public RestaurantAdapter(Context context, List<Restaurant> list, OnRestaurantClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = list.get(position);
        holder.tvName.setText(restaurant.getName());
        holder.tvRating.setText(String.valueOf(restaurant.getRating()));
        
        // Cleaner cuisine and price info
        String info = restaurant.getCuisine();
        if (restaurant.getPriceRange() != null && !restaurant.getPriceRange().isEmpty()) {
            info += " • " + restaurant.getPriceRange();
        }
        holder.tvCuisine.setText(info);
        
        holder.tvTime.setText(restaurant.getDeliveryTime());
        
        if (holder.tvDistance != null) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(restaurant.getDistance() + " km");
        }

        String fullImageUrl = RetrofitClient.getFullUrl(context, restaurant.getImageUrl());
        Glide.with(context).load(fullImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivRestaurant);

        holder.itemView.setOnClickListener(v -> listener.onRestaurantClick(restaurant));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRestaurant;
        TextView tvName, tvRating, tvCuisine, tvTime, tvDistance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRestaurant = itemView.findViewById(R.id.ivRestaurant);
            tvName = itemView.findViewById(R.id.tvRestaurantName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvCuisine = itemView.findViewById(R.id.tvCuisine);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}