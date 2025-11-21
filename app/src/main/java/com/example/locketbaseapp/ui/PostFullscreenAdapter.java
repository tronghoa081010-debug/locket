package com.example.locketbaseapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.locketbaseapp.R;
import com.example.locketbaseapp.model.Post;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PostFullscreenAdapter extends RecyclerView.Adapter<PostFullscreenAdapter.ViewHolder> {

    private List<Post> posts;
    private Context context;
    private FirebaseFirestore db;

    public PostFullscreenAdapter(List<Post> posts, Context context) {
        this.posts = posts;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_fullscreen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Post post = posts.get(position);

        // Load ảnh fullscreen
        Glide.with(context)
                .load(post.imageUrl)
                .centerCrop()
                .into(holder.ivPost);

        // Hiển thị caption (nếu có)
        if (post.caption != null && !post.caption.isEmpty()) {
            holder.tvCaption.setText(post.caption);
            holder.tvCaption.setVisibility(View.VISIBLE);
        } else {
            holder.tvCaption.setVisibility(View.GONE);
        }

        // Hiển thị thời gian (kiểu "2g", "1 ngày trước")
        if (post.timestamp != null) {
            holder.tvTime.setText(getTimeAgo(post.timestamp.toDate()));
        }

        // Load thông tin user
        db.collection("users").document(post.userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String displayName = doc.getString("displayName");
                        String photoUrl = doc.getString("photoUrl");

                        holder.tvUserName.setText(displayName != null ? displayName : "Unknown");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .into(holder.ivUserAvatar);
                        } else {
                            holder.ivUserAvatar.setImageResource(R.drawable.ic_person_circle);
                        }
                    }
                });

        // Reaction buttons
        holder.btnReaction1.setOnClickListener(v -> sendReaction(post, "🔥"));
        holder.btnReaction2.setOnClickListener(v -> sendReaction(post, "😍"));
        holder.btnReaction3.setOnClickListener(v -> sendReaction(post, "💛"));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private String getTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + "p";
        } else if (hours < 24) {
            return hours + "g";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }

    private void sendReaction(Post post, String emoji) {
        // TODO: Gửi reaction như tin nhắn hoặc lưu vào Firestore
        // Có thể mở ChatActivity hoặc gửi quick reaction
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPost, ivUserAvatar, btnMoreReactions;
        TextView tvUserName, tvTime, tvCaption;
        TextView btnReaction1, btnReaction2, btnReaction3;
        EditText etQuickReply;

        ViewHolder(View itemView) {
            super(itemView);
            ivPost = itemView.findViewById(R.id.ivPost);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            btnReaction1 = itemView.findViewById(R.id.btnReaction1);
            btnReaction2 = itemView.findViewById(R.id.btnReaction2);
            btnReaction3 = itemView.findViewById(R.id.btnReaction3);
            btnMoreReactions = itemView.findViewById(R.id.btnMoreReactions);
            etQuickReply = itemView.findViewById(R.id.etQuickReply);
        }
    }
}