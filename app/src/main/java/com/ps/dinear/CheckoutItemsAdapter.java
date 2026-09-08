package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.CartItem;
import java.util.List;

public class CheckoutItemsAdapter extends RecyclerView.Adapter<CheckoutItemsAdapter.ViewHolder> {
    private Context context;
    private List<CartItem> list;

    public CheckoutItemsAdapter(Context context, List<CartItem> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_checkout_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = list.get(position);
        MenuItem food = item.getMenuItem();

        holder.tvName.setText(food.getName());
        holder.tvQuantity.setText(item.getQuantity() + "x");
        double price = food.getDiscountPrice() != null ? food.getDiscountPrice() : food.getPrice();
        holder.tvPrice.setText("Rs. " + (int)(price * item.getQuantity()));

        if (food.getDescription() != null && !food.getDescription().isEmpty()) {
            holder.tvDesc.setText(food.getDescription());
            holder.tvDesc.setVisibility(View.VISIBLE);
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvDesc = itemView.findViewById(R.id.tvItemDesc);
        }
    }
}
