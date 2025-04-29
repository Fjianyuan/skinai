package com.example.tor210.check;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;

import java.util.List;

public class ClockInPhotoAdapter extends RecyclerView.Adapter<ClockInPhotoAdapter.PhotoViewHolder> {

    private List<Uri> photoList;
    private Context context;
    private OnPhotoDeleteListener deleteListener;

    public interface OnPhotoDeleteListener {
        void onPhotoDelete(int position);
    }

    public ClockInPhotoAdapter(Context context, List<Uri> photoList, OnPhotoDeleteListener listener) {
        this.context = context;
        this.photoList = photoList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_clock_in_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri photoUri = photoList.get(position);
        Glide.with(context)
                .load(photoUri)
                .centerCrop()
                .into(holder.ivPhoto);

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onPhotoDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    public void addPhoto(Uri photoUri) {
        photoList.add(photoUri);
        notifyItemInserted(photoList.size() - 1);
    }

    public void removePhoto(int position) {
        if (position >= 0 && position < photoList.size()) {
            photoList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, photoList.size());
        }
    }

    public List<Uri> getPhotoList() {
        return photoList;
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnDelete;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
} 