package com.example.tor210.clock;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tor210.R;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 照片适配器，用于在打卡页面显示上传的皮肤照片
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Uri> photoUris;
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onDeleteClick(int position);
    }

    public PhotoAdapter(List<Uri> photoUris, OnPhotoClickListener listener) {
        this.photoUris = photoUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri photoUri = photoUris.get(position);
        
        // 使用Picasso加载图片
        Picasso.get()
                .load(photoUri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .fit()
                .centerCrop()
                .into(holder.imageView);
        
        // 设置删除按钮点击事件
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public void removePhoto(int position) {
        if (position >= 0 && position < photoUris.size()) {
            photoUris.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addPhoto(Uri photoUri) {
        photoUris.add(photoUri);
        notifyItemInserted(photoUris.size() - 1);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView deleteButton;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivPhoto);
            deleteButton = itemView.findViewById(R.id.ivDelete);
        }
    }
} 