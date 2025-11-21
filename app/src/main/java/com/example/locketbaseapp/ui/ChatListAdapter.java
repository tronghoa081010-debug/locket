package com.example.locketbaseapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.locketbaseapp.R;
import com.example.locketbaseapp.model.Chat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private List<Chat> chats;
    private Context context;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatListAdapter(List<Chat> chats, Context context, OnChatClickListener listener) {
        this.chats = chats;
        this.context = context;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Chat chat = chats.get(position);

        // Hiển thị tên
        holder.tvName.setText(chat.friendName);

        // Hiển thị avatar
        if (chat.friendPhoto != null && !chat.friendPhoto.isEmpty()) {
            Glide.with(context)
                    .load(chat.friendPhoto)
                    .circleCrop()
                    .into(holder.ivAvatar);
            holder.tvInitial.setVisibility(View.GONE);
        } else {
            // Hiển thị chữ cái đầu
            holder.ivAvatar.setImageDrawable(null);
            holder.tvInitial.setVisibility(View.VISIBLE);
            String initial = chat.friendName != null && !chat.friendName.isEmpty()
                    ? chat.friendName.substring(0, 1).toUpperCase()
                    : "?";
            holder.tvInitial.setText(initial);
        }

        // Hiển thị tin nhắn cuối hoặc "Chưa có câu trả lời nào!"
        if (chat.lastMessage != null && !chat.lastMessage.isEmpty()) {
            holder.tvLastMessage.setText(chat.lastMessage);
            holder.tvLastMessage.setTextColor(0xFFAAAAAA);
        } else {
            holder.tvLastMessage.setText("Chưa có câu trả lời nào!");
            holder.tvLastMessage.setTextColor(0xFF888888);
        }

        // Hiển thị thời gian (chỉ nếu có tin nhắn)
        if (chat.lastMessageTime != null) {
            holder.tvTime.setText(formatTime(chat.lastMessageTime.toDate()));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    private String formatTime(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long days = diff / (1000 * 60 * 60 * 24);

        if (days < 1) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(date);
        } else if (days < 7) {
            return days + " ngày trước";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + " tuần trước";
        } else if (days < 365) {
            SimpleDateFormat sdf = new SimpleDateFormat("d 'tháng' M", new Locale("vi", "VN"));
            return sdf.format(date);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMessage, tvTime, tvInitial;

        ViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvInitial = itemView.findViewById(R.id.tvInitial);
        }
    }
}