package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {
    private Context context;
    private List<String> list;
    private OnFilterClickListener listener;
    private int selectedPosition = 0;
    private int layoutResId = R.layout.item_filter;

    public interface OnFilterClickListener {
        void onFilterClick(String category);
    }

    public FilterAdapter(Context context, List<String> list) {
        this.context = context;
        this.list = list;
    }

    public FilterAdapter(Context context, List<String> list, OnFilterClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    public FilterAdapter(Context context, List<String> list, int layoutResId, OnFilterClickListener listener) {
        this.context = context;
        this.list = list;
        this.layoutResId = layoutResId;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedCategory(String category) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(category)) {
                setSelectedPosition(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = list.get(position);
        holder.tvFilterName.setText(item);

        if (selectedPosition == position) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.orange_primary));
            holder.cardView.setStrokeWidth(0);
            holder.tvFilterName.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setStrokeColor(ContextCompat.getColorStateList(context, R.color.divider));
            holder.tvFilterName.setTextColor(ContextCompat.getColor(context, R.color.gray_text)); // Changed to gray_text for unselected
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                int oldPosition = selectedPosition;
                selectedPosition = currentPos;
                notifyItemChanged(oldPosition);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    listener.onFilterClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFilterName;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFilterName = itemView.findViewById(R.id.tvFilterName);
            cardView = (MaterialCardView) itemView;
        }
    }
}