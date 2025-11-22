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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_TIMESTAMP = 1;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000; // 1 giờ

    private List<Message> messages;
    private String currentUserId;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        // Kiểm tra xem có cần hiện timestamp không
        if (shouldShowTimestamp(position)) {
            return TYPE_TIMESTAMP;
        }
        return TYPE_MESSAGE;
    }

    // ← LOGIC: Hiện timestamp nếu cách tin nhắn trước > 1 giờ
    private boolean shouldShowTimestamp(int position) {
        if (position == 0) return true; // Luôn hiện timestamp cho tin nhắn đầu tiên

        Message current = messages.get(position);
        Message previous = messages.get(position - 1);

        if (current.timestamp == null || previous.timestamp == null) {
            return false;
        }

        long diff = current.timestamp.toDate().getTime() - previous.timestamp.toDate().getTime();
        return diff > ONE_HOUR_MS;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_TIMESTAMP) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_timestamp, parent, false);
            return new TimestampViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof TimestampViewHolder) {
            ((TimestampViewHolder) holder).bind(message);
        } else if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).bind(message, currentUserId);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ← VIEWHOLDER CHO TIN NHẮN
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvStatus;
        LinearLayout messageContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(Message message, String currentUserId) {
            tvMessage.setText(message.text);

            boolean isSentByMe = message.senderId.equals(currentUserId);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();

            if (isSentByMe) {
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
                tvMessage.setTextColor(0xFFFFFFFF);

                // ← HIỂN THỊ STATUS CHO TIN NHẮN ĐÃ GỬI
                String status = message.getStatus(currentUserId);
                if (!status.isEmpty()) {
                    tvStatus.setText(status);
                    tvStatus.setVisibility(View.VISIBLE);

                    // Màu sắc theo trạng thái
                    if (status.equals("Đã xem")) {
                        tvStatus.setTextColor(0xFF4CAF50); // Xanh lá
                    } else {
                        tvStatus.setTextColor(0xFFAAAAAA); // Xám
                    }
                } else {
                    tvStatus.setVisibility(View.GONE);
                }

            } else {
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.bg_message_received);
                tvMessage.setTextColor(0xFFFFFFFF);
                tvStatus.setVisibility(View.GONE); // Không hiện status cho tin nhận
            }

            messageContainer.setLayoutParams(params);
        }
    }

    // ← VIEWHOLDER CHO TIMESTAMP
    static class TimestampViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestamp;

        TimestampViewHolder(View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        void bind(Message message) {
            if (message.timestamp != null) {
                tvTimestamp.setText(formatTimestamp(message.timestamp.toDate()));
            }
        }

        private String formatTimestamp(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd 'tháng' M HH:mm 'CH'", new Locale("vi", "VN"));
            return sdf.format(date);
        }
    }
}