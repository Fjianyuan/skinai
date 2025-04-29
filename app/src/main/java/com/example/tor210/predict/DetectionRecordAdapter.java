package com.example.tor210.predict;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 检测记录列表适配器
 */
public class DetectionRecordAdapter extends RecyclerView.Adapter<DetectionRecordAdapter.RecordViewHolder> {

    private Context context;
    private List<DetectionRecord> recordList;
    private OnItemClickListener onItemClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public DetectionRecordAdapter(Context context, List<DetectionRecord> recordList) {
        this.context = context;
        this.recordList = recordList;
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_detection_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        DetectionRecord record = recordList.get(position);
        
        // 设置记录日期
        String dateStr = dateFormat.format(record.getRecordTime());
        holder.tvRecordDate.setText(dateStr);
        
        // 设置痘痘数量
        holder.tvAcneCount.setText("痘痘数量: " + record.getAcneCount());
        
        // 设置严重程度
        holder.tvSeverity.setText("严重程度: " + record.getSeverity());
        
        // 根据严重程度设置指示器颜色
        int indicatorResId;
        switch (record.getSeverity().toLowerCase()) {
            case "轻度":
                indicatorResId = R.drawable.severity_indicator_green;
                break;
            case "中度":
                indicatorResId = R.drawable.severity_indicator_yellow;
                break;
            case "重度":
                indicatorResId = R.drawable.severity_indicator_orange;
                break;
            case "严重":
                indicatorResId = R.drawable.severity_indicator_red;
                break;
            default:
                indicatorResId = R.drawable.severity_indicator_green;
                break;
        }
        holder.severityIndicator.setBackgroundResource(indicatorResId);
        
        // 判断是否包含AI诊断数据
        String recommendation = record.getRecommendation();
        if (recommendation != null && !recommendation.isEmpty() && recommendation.trim().startsWith("{")) {
            // 包含AI诊断数据，显示AI标签
            if (holder.tvAiTag != null) {
                holder.tvAiTag.setVisibility(View.VISIBLE);
            }
        } else {
            // 不包含AI诊断数据，隐藏AI标签
            if (holder.tvAiTag != null) {
                holder.tvAiTag.setVisibility(View.GONE);
            }
        }
        
        // 加载结果图片
        Glide.with(context)
                .load(record.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .centerCrop()
                .into(holder.imageResult);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(record, position);
            }
        });
        
        // 设置删除按钮点击事件
        holder.btnDelete.setOnClickListener(v -> {
            // 显示确认对话框
            new AlertDialog.Builder(context)
                    .setTitle("删除记录")
                    .setMessage("确定要删除这条检测记录吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        if (onDeleteClickListener != null) {
                            onDeleteClickListener.onDeleteClick(record, position);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return recordList != null ? recordList.size() : 0;
    }

    public void updateData(List<DetectionRecord> newRecords) {
        this.recordList = newRecords;
        notifyDataSetChanged();
    }
    
    /**
     * 删除指定位置的记录
     */
    public void removeItem(int position) {
        if (position >= 0 && position < recordList.size()) {
            recordList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, recordList.size());
        }
    }

    /**
     * 设置项目点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    /**
     * 设置删除按钮点击监听器
     */
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(DetectionRecord record, int position);
    }
    
    /**
     * 删除按钮点击监听器接口
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(DetectionRecord record, int position);
    }

    /**
     * 记录视图持有者
     */
    static class RecordViewHolder extends RecyclerView.ViewHolder {
        ImageView imageResult;
        TextView tvRecordDate;
        TextView tvAcneCount;
        TextView tvSeverity;
        View severityIndicator;
        ImageButton btnDelete;
        TextView tvAiTag;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            imageResult = itemView.findViewById(R.id.image_result);
            tvRecordDate = itemView.findViewById(R.id.tv_record_date);
            tvAcneCount = itemView.findViewById(R.id.tv_acne_count);
            tvSeverity = itemView.findViewById(R.id.tv_severity);
            severityIndicator = itemView.findViewById(R.id.severity_indicator);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvAiTag = itemView.findViewById(R.id.tv_ai_tag);
        }
    }
} 