package com.ps.dinear;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.ps.dinear.data.model.Restaurant;

import java.util.List;
import java.util.Locale;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {
    private Context context;
    private List<Restaurant> list;
    private OnRestaurantClickListener listener;
    private int layoutResId;
    private boolean locationPermissionDenied = false;

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
        default void onDetailClick(Restaurant restaurant) {}
    }

    public RestaurantAdapter(Context context, List<Restaurant> list, OnRestaurantClickListener listener) {
        this(context, list, R.layout.item_restaurant, listener);
    }

    public RestaurantAdapter(Context context, List<Restaurant> list, @LayoutRes int layoutResId, OnRestaurantClickListener listener) {
        this.context = context;
        this.list = list;
        this.layoutResId = layoutResId;
        this.listener = listener;
    }

    public void updateList(List<Restaurant> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public void setLocationPermissionDenied(boolean denied) {
        this.locationPermissionDenied = denied;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Restaurant restaurant = list.get(position);
        holder.tvName.setText(restaurant.getName());

        if (restaurant.getRating() > 0) {
            holder.tvRating.setText(String.format(Locale.US, "%.1f", restaurant.getRating()));
            if (holder.llRating != null) holder.llRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setText("N/A");
            // Optionally hide or show "New"
        }
        
        // Cleaner cuisine and price info
        String info = restaurant.getCuisine();
        if (restaurant.getPriceRange() != null && !restaurant.getPriceRange().isEmpty()) {
            info += " • " + restaurant.getPriceRange();
        }
        holder.tvCuisine.setText(info);
        
        holder.tvTime.setText(restaurant.getDeliveryTime());

        if (holder.tvDeliveryChargeInfo != null) {
            if (restaurant.getDeliveryCharge() == 0) {
                holder.tvDeliveryChargeInfo.setText("FREE DELIVERY");
                holder.tvDeliveryChargeInfo.setTextColor(ContextCompat.getColor(context, R.color.green_delivery));
                if (holder.vDotDelivery != null) holder.vDotDelivery.setVisibility(View.VISIBLE);
                holder.tvDeliveryChargeInfo.setVisibility(View.VISIBLE);
            } else {
                holder.tvDeliveryChargeInfo.setText("DELIVERY");
                holder.tvDeliveryChargeInfo.setTextColor(ContextCompat.getColor(context, R.color.paid_delivery));
                if (holder.vDotDelivery != null) holder.vDotDelivery.setVisibility(View.VISIBLE);
                holder.tvDeliveryChargeInfo.setVisibility(View.VISIBLE);
            }
        }
        
        if (holder.llDistance != null) {
            if (restaurant.getDistance() > 0) {
                holder.llDistance.setVisibility(View.VISIBLE);
                holder.tvDistance.setText(String.format(Locale.US, "%.1f km", restaurant.getDistance()));
                if (holder.ivDistanceIcon != null) holder.ivDistanceIcon.setImageResource(R.drawable.icon_walk);
            } else if (locationPermissionDenied) {
                holder.llDistance.setVisibility(View.VISIBLE);
                holder.tvDistance.setText("No GPS");
                if (holder.ivDistanceIcon != null) holder.ivDistanceIcon.setImageResource(R.drawable.icon_nolocation);
            } else {
                holder.llDistance.setVisibility(View.GONE);
            }
        }

        String fullImageUrl = RetrofitClient.getFullUrl(context, restaurant.getImageUrl());
        Glide.with(context)
                .load(fullImageUrl)
                .placeholder(R.drawable.bg_skeleton)
                .error(R.drawable.burger)
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                .centerCrop()
                .into(holder.ivRestaurant);

        // Favorite Logic (Premium Glow Effect)
        boolean isFav = FavoritesManager.getInstance().isRestaurantFavorite(restaurant.getId());
        if (isFav) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.cardView.setStrokeWidth(4);
            holder.cardView.setStrokeColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.orange_primary)));
            holder.ivFavorite.setVisibility(View.VISIBLE);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
            holder.ivFavorite.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onRestaurantClick(restaurant));
        
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(v -> listener.onDetailClick(restaurant));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivRestaurant, ivFavorite, ivDistanceIcon;
        TextView tvName, tvRating, tvCuisine, tvTime, tvDistance, tvDeliveryChargeInfo;
        View llDistance, llRating, vDotDelivery;
        MaterialButton btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivRestaurant = itemView.findViewById(R.id.ivRestaurant);
            ivFavorite = itemView.findViewById(R.id.ivFavoriteRestItem);
            tvName = itemView.findViewById(R.id.tvRestaurantName);
            tvRating = itemView.findViewById(R.id.tvRating);
            llRating = itemView.findViewById(R.id.llRating);
            tvCuisine = itemView.findViewById(R.id.tvCuisine);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDeliveryChargeInfo = itemView.findViewById(R.id.tvDeliveryChargeInfo);
            vDotDelivery = itemView.findViewById(R.id.vDotDelivery);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            ivDistanceIcon = itemView.findViewById(R.id.ivDistanceIcon);
            llDistance = itemView.findViewById(R.id.llDistance);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}