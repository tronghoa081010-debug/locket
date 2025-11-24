package com.example.locketbaseapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.locketbaseapp.R;
import com.example.locketbaseapp.model.Sticker;
import java.util.List;

public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.StickerViewHolder> {
    private List<Sticker> stickerList;
    private OnStickerClickListener listener;

    public interface OnStickerClickListener {
        void onStickerClick(Sticker sticker);
    }

    public StickerAdapter(List<Sticker> stickerList, OnStickerClickListener listener) {
        this.stickerList = stickerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sticker, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerViewHolder holder, int position) {
        Sticker sticker = stickerList.get(position);
        
        Glide.with(holder.itemView.getContext())
                .load(sticker.url)
                .placeholder(R.drawable.ic_person_circle) // Placeholder khi loading
                .into(holder.ivSticker);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStickerClick(sticker);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stickerList.size();
    }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSticker;

        public StickerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSticker = itemView.findViewById(R.id.ivSticker);
        }
    }
}
