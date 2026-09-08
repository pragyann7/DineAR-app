package com.ps.dinear;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ps.dinear.data.model.UserAddress;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    private Context context;
    private List<UserAddress> list;
    private AddressListener listener;

    public interface AddressListener {
        void onAddressSelected(UserAddress address);
        void onAddressDelete(UserAddress address);
    }

    public AddressAdapter(Context context, List<UserAddress> list, AddressListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserAddress address = list.get(position);
        holder.tvTitle.setText(address.getTitle());
        holder.tvLine.setText(address.getAddressLine() + ", " + address.getCity());
        
        holder.tvDefault.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> listener.onAddressSelected(address));
        holder.btnDelete.setOnClickListener(v -> listener.onAddressDelete(address));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLine, tvDefault;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAddressTitle);
            tvLine = itemView.findViewById(R.id.tvAddressLine);
            tvDefault = itemView.findViewById(R.id.tvDefaultBadge);
            btnDelete = itemView.findViewById(R.id.btnDeleteAddress);
        }
    }
}
