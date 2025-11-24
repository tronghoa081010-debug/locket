package com.example.locketbaseapp.ui;

import android.graphics.Typeface;
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
import java.util.Map;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MESSAGE = 0;
    private static final int TYPE_TIMESTAMP = 1;
    private static final long ONE_HOUR_MS = 60 * 60 * 1000;

    private List<Message> messages;
    private String currentUserId;
    private OnMessageLongClickListener longClickListener;

    // Interface for long-press callback
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message, View view);
    }

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        setHasStableIds(true);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }
    
    @Override
    public long getItemId(int position) {
        Message message = messages.get(position);
        if (message.messageId != null) {
            return message.messageId.hashCode();
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (shouldShowTimestamp(position)) {
            return TYPE_TIMESTAMP;
        }
        return TYPE_MESSAGE;
    }

    private boolean shouldShowTimestamp(int position) {
        // ✅ LUÔN HIỂN THỊ TIMESTAMP CHO TIN NHẮN ĐẦU TIÊN
        if (position == 0) {
            return true;  // Luôn hiển thị cho tin nhắn đầu tiên
        }

        Message current = messages.get(position);
        Message previous = messages.get(position - 1);

        // Nếu một trong 2 null → Không hiện timestamp
        if (current.timestamp == null || previous.timestamp == null) {
            return false;
        }

        // Hiển thị timestamp nếu khoảng cách > 1 giờ
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
            ((MessageViewHolder) holder).bind(message, currentUserId, longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvStatus, tvReactions, tvTimer;
        LinearLayout messageContainer;
        ImageView ivMessageImage;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReactions = itemView.findViewById(R.id.tvReactions);
            tvTimer = itemView.findViewById(R.id.tvTimer);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(Message message, String currentUserId, OnMessageLongClickListener listener) {
            boolean isSentByMe = message.senderId.equals(currentUserId);
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) messageContainer.getLayoutParams();

            // RECALLED MESSAGE UI
            if (message.isRecalled()) {
                String recalledText = isSentByMe 
                    ? "🔄 Bạn đã thu hồi tin nhắn"
                    : "🔄 Người dùng đã thu hồi tin nhắn";
                
                tvMessage.setText(recalledText);
                tvMessage.setTypeface(null, Typeface.ITALIC);
                tvMessage.setTextColor(0xFF999999);
                tvStatus.setVisibility(View.GONE);
                tvReactions.setVisibility(View.GONE);
                tvTimer.setVisibility(View.GONE);
                ivMessageImage.setVisibility(View.GONE);
                tvMessage.setVisibility(View.VISIBLE);
                
                if (isSentByMe) {
                    params.gravity = Gravity.END;
                    messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
                } else {
                    params.gravity = Gravity.START;
                    messageContainer.setBackgroundResource(R.drawable.bg_message_received);
                }
                messageContainer.setLayoutParams(params);
                messageContainer.setOnLongClickListener(null);
                
            } else {
                // NORMAL MESSAGE UI
                
                // STICKER OR TEXT DISPLAY
                if (message.imageUrl != null && !message.imageUrl.isEmpty()) {
                    // Hiển thị sticker
                    ivMessageImage.setVisibility(View.VISIBLE);
                    tvMessage.setVisibility(View.GONE);
                    
                    Glide.with(itemView.getContext())
                            .load(message.imageUrl)
                            .placeholder(R.drawable.ic_person_circle)
                            .into(ivMessageImage);
                } else {
                    // Hiển thị text
                    ivMessageImage.setVisibility(View.GONE);
                    tvMessage.setVisibility(View.VISIBLE);
                    tvMessage.setText(message.text);
                    tvMessage.setTypeface(null, Typeface.NORMAL);
                }
                
                if (isSentByMe) {
                    params.gravity = Gravity.END;
                    messageContainer.setBackgroundResource(R.drawable.bg_message_sent);
                    tvMessage.setTextColor(0xFFFFFFFF);

                    String status = message.getStatus(currentUserId);
                    if (!status.isEmpty()) {
                        tvStatus.setText(status);
                        tvStatus.setVisibility(View.VISIBLE);

                        if (status.equals("Đã xem")) {
                            tvStatus.setTextColor(0xFF4CAF50);
                        } else {
                            tvStatus.setTextColor(0xFFAAAAAA);
                        }
                    } else {
                        tvStatus.setVisibility(View.GONE);
                    }

                } else {
                    params.gravity = Gravity.START;
                    messageContainer.setBackgroundResource(R.drawable.bg_message_received);
                    tvMessage.setTextColor(0xFFFFFFFF);
                    tvStatus.setVisibility(View.GONE);
                }

                messageContainer.setLayoutParams(params);

                // REACTIONS DISPLAY
                if (message.reactions != null && !message.reactions.isEmpty()) {
                    StringBuilder reactionsText = new StringBuilder();
                    for (Map.Entry<String, List<String>> entry : message.reactions.entrySet()) {
                        String emoji = entry.getKey();
                        int count = entry.getValue().size();
                        reactionsText.append(emoji).append(" ").append(count).append("  ");
                    }
                    tvReactions.setText(reactionsText.toString().trim());
                    tvReactions.setVisibility(View.VISIBLE);
                } else {
                    tvReactions.setVisibility(View.GONE);
                }

                // SELF-DESTRUCT TIMER DISPLAY
                if (message.selfDestructDuration != null && message.selfDestructDuration > 0) {
                    if (message.expiresAt != null && message.expiresAt > 0) {
                        // Timer đã bắt đầu → Hiển thị countdown
                        long remainingMs = message.expiresAt - System.currentTimeMillis();
                        if (remainingMs > 0) {
                            String timerText = formatTimer(remainingMs);
                            tvTimer.setText("⏱️ " + timerText);
                            tvTimer.setVisibility(View.VISIBLE);
                        } else {
                            tvTimer.setVisibility(View.GONE);
                        }
                    } else {
                        // Timer chưa bắt đầu (người nhận chưa xem)
                        // Hiển thị label tĩnh
                        String durationText = formatDuration(message.selfDestructDuration);
                        tvTimer.setText("⏱️ " + durationText);
                        tvTimer.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvTimer.setVisibility(View.GONE);
                }
                
                // LONG-PRESS LISTENER
                if (listener != null) {
                    messageContainer.setOnLongClickListener(v -> {
                        listener.onMessageLongClick(message, v);
                        return true;
                    });
                }
            }
        }
    }

    static class TimestampViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestamp;

        TimestampViewHolder(View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        void bind(Message message) {
            if (message.timestamp != null) {
                tvTimestamp.setText(formatTimestamp(message.timestamp.toDate()));
            } else {
                tvTimestamp.setText("Vừa xong");
            }
        }

        private String formatTimestamp(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd 'tháng' M HH:mm 'CH'", new Locale("vi", "VN"));
            return sdf.format(date);
        }
    }

    private static String formatTimer(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else {
            return (seconds / 3600) + "h";
        }
    }
    
    private static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "m";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + "h";
        } else {
            long days = seconds / 86400;
            return days + "d";
        }
    }
}