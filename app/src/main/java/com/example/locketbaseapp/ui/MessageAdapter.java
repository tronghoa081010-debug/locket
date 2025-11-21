package com.example.locketbaseapp.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.locketbaseapp.R;
import com.example.locketbaseapp.model.Message;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isSent = message.senderId.equals(currentUserId);

        holder.tvMessage.setText(message.text);

        // Layout params
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageContainer.getLayoutParams();

        if (isSent) {
            // Tin nhắn của mình (bên phải, màu xám nhạt)
            params.gravity = Gravity.END;
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
            holder.tvMessage.setTextColor(0xFF000000);
        } else {
            // Tin nhắn của bạn (bên trái, màu xám đậm)
            params.gravity = Gravity.START;
            holder.messageContainer.setBackgroundResource(R.drawable.bg_message_received);
            holder.tvMessage.setTextColor(0xFFFFFFFF);
        }

        holder.messageContainer.setLayoutParams(params);

        // Hiển thị "Read" cho tin nhắn cuối cùng của mình
        if (isSent && position == messages.size() - 1 && message.isRead) {
            holder.tvReadStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvReadStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        TextView tvMessage, tvReadStatus;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
        }
    }
}