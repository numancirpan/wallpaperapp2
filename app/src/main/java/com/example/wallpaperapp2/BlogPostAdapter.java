package com.example.wallpaperapp2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class BlogPostAdapter extends RecyclerView.Adapter<BlogPostAdapter.ViewHolder> {

    public interface OnPostActionListener {
        void onEdit(BlogPost post);
        void onDelete(BlogPost post);
    }

    private List<BlogPost> posts;
    private final OnPostActionListener actionListener;

    public BlogPostAdapter(List<BlogPost> posts, OnPostActionListener actionListener) {
        this.posts = posts;
        this.actionListener = actionListener;
    }

    public void updateList(List<BlogPost> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blog_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlogPost post = posts.get(position);

        Glide.with(holder.itemView.getContext())
                .load(post.imageUrl)
                .centerCrop()
                .into(holder.imageBlogPost);

        holder.txtBlogComment.setText(post.comment);
        String dateText = post.createdAt > 0
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(post.createdAt))
                : "";
        String category = post.aiCategory == null || post.aiCategory.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.not_available)
                : CategoryDisplayMapper.toDisplayName(holder.itemView.getContext(), post.aiCategory);
        holder.txtBlogMeta.setText(post.photographer + " • " + category + " • " + dateText);
        holder.btnEditPost.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(post);
        });
        holder.btnDeletePost.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(post);
        });
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageBlogPost;
        TextView txtBlogComment;
        TextView txtBlogMeta;
        MaterialButton btnEditPost;
        MaterialButton btnDeletePost;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBlogPost = itemView.findViewById(R.id.imageBlogPost);
            txtBlogComment = itemView.findViewById(R.id.txtBlogComment);
            txtBlogMeta = itemView.findViewById(R.id.txtBlogMeta);
            btnEditPost = itemView.findViewById(R.id.btnEditPost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        }
    }
}
