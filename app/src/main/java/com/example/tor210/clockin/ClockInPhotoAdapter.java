package com.example.tor210.clockin;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;

import java.util.ArrayList;
import java.util.List;

public class ClockInPhotoAdapter extends RecyclerView.Adapter<ClockInPhotoAdapter.PhotoViewHolder> {

    private List<Uri> photoUris = new ArrayList<>();
    private OnPhotoDeleteListener listener;

    public interface OnPhotoDeleteListener {
        void onPhotoDeleted(int position);
    }

    public ClockInPhotoAdapter(OnPhotoDeleteListener listener) {
        this.listener = listener;
    }

    public void addPhoto(Uri uri) {
        photoUris.add(uri);
        notifyItemInserted(photoUris.size() - 1);
    }

    public List<Uri> getPhotoUris() {
        return new ArrayList<>(photoUris);
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clock_in_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);
        
        Glide.with(holder.itemView.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.photoImageView);
                
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    photoUris.remove(adapterPosition);
                    notifyItemRemoved(adapterPosition);
                    listener.onPhotoDeleted(adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        ImageView deleteButton;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.iv_photo);
            deleteButton = itemView.findViewById(R.id.iv_delete);
        }
    }
} 