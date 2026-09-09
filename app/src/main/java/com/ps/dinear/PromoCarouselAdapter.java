package com.ps.dinear;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PromoCarouselAdapter extends RecyclerView.Adapter<PromoCarouselAdapter.ViewHolder> {

    public interface OnPromoClickListener {
        void onPromoClick(PromoItem item);
    }

    private final List<PromoItem> promoList;
    private final OnPromoClickListener listener;

    public PromoCarouselAdapter(List<PromoItem> promoList, OnPromoClickListener listener) {
        this.promoList = promoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promo_banner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PromoItem item = promoList.get(position % promoList.size());
        
        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(item.imageRes)
                .centerCrop()
                .into(holder.ivImage);
                
        holder.tvBadge.setText(item.badge);
        holder.tvTitle.setText(item.title);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPromoClick(item);
        });
    }

    @Override
    public int getItemCount() {
        // Infinite scrolling trick
        return Integer.MAX_VALUE;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvBadge, tvTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPromoImage);
            tvBadge = itemView.findViewById(R.id.tvPromoBadge);
            tvTitle = itemView.findViewById(R.id.tvPromoTitle);
        }
    }

    public static class PromoItem {
        public String id;
        public String badge;
        public String title;
        public int imageRes;
        public String actionQuery;

        public PromoItem(String id, String badge, String title, int imageRes, String actionQuery) {
            this.id = id;
            this.badge = badge;
            this.title = title;
            this.imageRes = imageRes;
            this.actionQuery = actionQuery;
        }
    }
}
