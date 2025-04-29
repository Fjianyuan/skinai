package com.example.tor210.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.tor210.R;

/**
 * 护肤小贴士详情Fragment
 */
public class TipDetailFragment extends Fragment {

    private static final String ARG_TIP_ID = "tip_id";
    private static final String ARG_TIP_TITLE = "tip_title";
    private static final String ARG_TIP_CONTENT = "tip_content";
    private static final String ARG_TIP_IMAGE_URL = "tip_image_url";
    
    private long tipId;
    private String tipTitle;
    private String tipContent;
    private String tipImageUrl;
    
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvContent;
    private Tip tip;

    /**
     * 创建Fragment实例（本地数据模式）
     */
    public static TipDetailFragment newInstance(long tipId) {
        TipDetailFragment fragment = new TipDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TIP_ID, tipId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tipId = getArguments().getLong(ARG_TIP_ID);
            
            // 检查是否有从外部传入的完整数据
            if (getArguments().containsKey(ARG_TIP_TITLE)) {
                tipTitle = getArguments().getString(ARG_TIP_TITLE);
                tipContent = getArguments().getString(ARG_TIP_CONTENT);
                tipImageUrl = getArguments().getString(ARG_TIP_IMAGE_URL);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tip_detail, container, false);
        
        // 初始化视图
        ivCover = view.findViewById(R.id.iv_cover);
        tvTitle = view.findViewById(R.id.tv_tip_title);
        tvContent = view.findViewById(R.id.tv_tip_content);
        
        // 设置返回按钮点击事件
        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        
        // 判断是使用外部数据还是加载本地数据
        if (tipTitle != null && tipContent != null) {
            displayExternalData();
        } else {
            loadTipData();
        }
        
        return view;
    }
    
    /**
     * 显示从外部接收的数据
     */
    private void displayExternalData() {
        tvTitle.setText(tipTitle);
        tvContent.setText(tipContent);
        
        // 加载图片
        if (tipImageUrl != null && !tipImageUrl.isEmpty()) {
            // 日志记录图片URL
            String baseUrl = "http://10.0.2.2:5000"; // 模拟器中访问本机地址
            String fullImageUrl = "";
            
            // 检查URL格式
            if (tipImageUrl.startsWith("/")) {
                // 相对路径，需要添加基础URL
                fullImageUrl = baseUrl + tipImageUrl;
            } else {
                // 完整URL或资源ID
                fullImageUrl = tipImageUrl;
            }
            android.util.Log.d("TipDetailFragment", "加载图片: " + fullImageUrl);
            
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivCover);
        } else {
            // 没有图片URL时显示占位图
            Glide.with(this)
                    .load(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivCover);
        }
    }
    
    /**
     * 加载本地贴士数据
     */
    private void loadTipData() {
        // 模拟从本地资源加载数据，实际项目中应该从后端获取
        tip = getTipById(tipId);
        
        if (tip != null) {
            tvTitle.setText(tip.getTitle());
            tvContent.setText(tip.getContent());
            
            // 加载封面图片
            Glide.with(this)
                    .load(R.drawable.placeholder_image)  // 实际项目中应该使用tip.getImageUrl()
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivCover);
        }
    }
    
    /**
     * 根据ID获取贴士数据，实际项目中应该从数据库或后端获取
     */
    private Tip getTipById(long id) {
        // 这里仅做演示，实际项目中应该从数据库或后端获取
        switch ((int)id) {
            case 1:
                return new Tip(1, "", "痘痘肌肤早餐选择", "正确的早餐选择可以帮助控制痘痘：\n\n1. 避免高糖高脂食物，如甜甜圈、油条等\n2. 选择全麦面包、燕麦等低GI食物\n3. 添加富含抗氧化物的水果，如蓝莓\n4. 可以适量摄入坚果，补充必要的脂肪酸\n5. 喝足够的水，保持肌肤水分\n\n建议早餐食物：全麦吐司+煮蛋+蓝莓+杏仁");
            case 2:
                return new Tip(2, "", "反复\"爆痘\"？可能是皮肤有炎症了！", "很多人认为痘痘只是皮肤问题，但实际上反复爆痘可能是因为皮肤存在炎症。\n\n炎症原因：\n1. 细菌感染：皮肤表面的痤疮丙酸杆菌大量繁殖\n2. 皮脂分泌过多：荷尔蒙变化导致\n3. 角质堆积：毛孔堵塞，无法正常排除废物\n4. 免疫反应：身体对细菌的防御反应\n\n如何缓解：\n- 使用含有水杨酸的洁面产品\n- 使用含有烟酰胺的修复产品\n- 保持面部清洁\n- 避免挤压痘痘\n- 保持规律生活作息");
            case 3:
                return new Tip(3, "", "痘痘类型自测", "了解自己的痘痘类型对症下药：\n\n1. 粉刺：毛囊口处于堵塞状态，形成黑头或白头\n   解决方法：使用水杨酸产品，定期去角质\n\n2. 丘疹：皮脂排出受阻，微生物大量繁殖\n   解决方法：控制油脂，使用抗菌成分\n\n3. 脓包：炎症加重，形成内含脓液的脓包\n   解决方法：看皮肤科医生，可能需要药物治疗\n\n4. 结节囊肿：最严重的痘痘类型\n   解决方法：必须就医，可能需要口服药物治疗");
            case 4:
                return new Tip(4, "", "长痘痘的内外原因", "外部刺激：\n- 熬夜导致荷尔蒙分泌紊乱\n- 过度清洁反而导致皮肤分泌更多油脂\n- 防晒不足导致紫外线伤害\n- 环境温度升高导致皮脂分泌增加\n\n内部因素：\n- 内分泌失调导致荷尔蒙变化\n- 情绪焦虑影响皮肤健康\n- 高糖高脂饮食增加痘痘风险\n- 遗传因素也会影响皮脂腺活跃程度\n\n建议：保持规律作息、均衡饮食、心情舒畅，痘痘会明显减少");
            case 5:
                return new Tip(5, "", "痘痘肌护肤课堂", "痘痘肌肤护理需要注意的要点：\n\n1. 清洁温和为先：使用弱酸性洁面产品，不要过度清洁\n\n2. 保湿很重要：控油≠不保湿，选择清爽质地的保湿产品\n\n3. 抗菌消炎：含有水杨酸、烟酰胺的产品有助于控制炎症\n\n4. 防晒必不可少：选择无油配方的防晒霜\n\n5. 少用粉底：减少彩妆使用频率，让皮肤休息\n\n6. 不要挤压痘痘：会导致二次感染和留疤\n\n7. 定期去角质：每周1-2次温和去角质\n\n8. 及时就医：严重的痘痘问题应该寻求医生帮助");
            default:
                return null;
        }
    }
} 