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
import com.ps.dinear.data.model.CartItem;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private Context context;
    private List<CartItem> list;
    private CartUpdateListener listener;

    public interface CartUpdateListener {
        void onQuantityChanged(int foodItemId, int newQuantity);
        void onItemRemoved(int foodItemId);
    }

    public CartAdapter(Context context, List<CartItem> list, CartUpdateListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = list.get(position);
        MenuItem food = item.getMenuItem();

        holder.tvName.setText(food.getName());
        double price = food.getDiscountPrice() != null ? food.getDiscountPrice() : food.getPrice();
        holder.tvPrice.setText("Rs. " + (int)price);
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        String fullImageUrl = RetrofitClient.getFullUrl(context, food.getImageUrl());
        Glide.with(context).load(fullImageUrl).into(holder.ivFood);

        holder.btnPlus.setOnClickListener(v -> listener.onQuantityChanged(food.getId(), item.getQuantity() + 1));
        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                listener.onQuantityChanged(food.getId(), item.getQuantity() - 1);
            }
        });
        holder.btnRemove.setOnClickListener(v -> listener.onItemRemoved(food.getId()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFood, btnPlus, btnMinus, btnRemove;
        TextView tvName, tvPrice, tvQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFood = itemView.findViewById(R.id.ivCartItem);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
        }
    }
}
