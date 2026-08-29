package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.ViewHolder> {
    private final Context context;
    private final List<Integer> avatars;
    private final OnAvatarClickListener listener;
    private int selectedPosition = -1;

    public interface OnAvatarClickListener {
        void onAvatarClick(int avatarResId);
    }

    public AvatarAdapter(Context context, List<Integer> avatars, int currentAvatarResId, OnAvatarClickListener listener) {
        this.context = context;
        this.avatars = avatars;
        this.listener = listener;
        
        for (int i = 0; i < avatars.size(); i++) {
            if (avatars.get(i) == currentAvatarResId) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int avatarResId = avatars.get(position);
        holder.ivAvatar.setImageResource(avatarResId);

        if (selectedPosition == position) {
            holder.ivAvatar.setStrokeWidth(6.0f); 
        } else {
            holder.ivAvatar.setStrokeWidth(0.0f);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onAvatarClick(avatarResId);
        });
    }

    @Override
    public int getItemCount() {
        return avatars.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatarItem);
        }
    }
}