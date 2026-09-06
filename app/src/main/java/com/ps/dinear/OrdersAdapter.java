package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.Order;
import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
    private Context context;
    private List<Order> list;

    public OrdersAdapter(Context context, List<Order> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = list.get(position);
        
        if (order.getRestaurantDetails() != null) {
            holder.tvResName.setText(order.getRestaurantDetails().getName());
        } else {
            holder.tvResName.setText("Order #" + order.getId());
        }
        
        holder.tvStatus.setText(order.getStatus());
        
        String date = order.getCreatedAt();
        if (date != null && date.contains("T")) {
            holder.tvDate.setText(date.split("T")[0]);
        } else {
            holder.tvDate.setText(date != null ? date : "N/A");
        }
        
        holder.tvTotal.setText("Rs. " + (int)order.getTotalPrice());

        StringBuilder summary = new StringBuilder();
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                Order.OrderItem item = order.getItems().get(i);
                if (item.getFoodItemDetails() != null) {
                    summary.append(item.getQuantity()).append(" x ").append(item.getFoodItemDetails().getName());
                    if (i < order.getItems().size() - 1) summary.append(", ");
                }
            }
        }
        holder.tvItems.setText(summary.toString());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvResName, tvStatus, tvDate, tvTotal, tvItems;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResName = itemView.findViewById(R.id.tvOrderResName);
            tvStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvDate = itemView.findViewById(R.id.tvOrderDate);
            tvTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvItems = itemView.findViewById(R.id.tvOrderItemsSummary);
        }
    }
}
