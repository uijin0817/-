package com.example.smiti;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private OnPostClickListener listener;
    private Context context;

    public interface OnPostClickListener {
        void onPostSelected(String postId);
    }

    public PostAdapter(List<Post> postList, OnPostClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        context = parent.getContext();
        return new PostViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post currentPost = postList.get(position);
        holder.titleTextView.setText(currentPost.getTitle());
        // holder.authorTextView.setText(currentPost.getAuthor()); // 필요하다면 작성자 표시

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostSelected(currentPost.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void updateList(List<Post> newList) {
        postList = newList;
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        // TextView authorTextView; // 필요하다면 작성자 표시

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView); // 수정된 부분
            // authorTextView = itemView.findViewById(R.id.postAuthorTextView); // 필요하다면 작성자 표시
        }
    }
}