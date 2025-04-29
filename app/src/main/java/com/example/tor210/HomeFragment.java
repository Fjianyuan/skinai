package com.example.tor210;

import com.example.tor210.api.ApiConfig;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.tor210.home.Tip;
import com.example.tor210.home.TipAdapter;
import com.example.tor210.home.TipDetailFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.DefaultRetryPolicy;

/**
 * 首页Fragment，展示护肤小贴士
 */
public class HomeFragment extends Fragment {
    // 使用ApiConfig类中定义的BASE_URL常量
    private static final String TIPS_API_URL = ApiConfig.BASE_URL + "/tips";
    
    private RecyclerView recyclerTips;
    private List<Tip> tipList = new ArrayList<>(); 
    private TipAdapter adapter;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // 初始化控件
        recyclerTips = view.findViewById(R.id.recycler_posts);
        
        // 隐藏搜索栏和发布按钮
        View searchBar = view.findViewById(R.id.search_bar);
        View btnCreate = view.findViewById(R.id.btn_create);
        searchBar.setVisibility(View.GONE);
        btnCreate.setVisibility(View.GONE);
        
        // 初始化Volley请求队列
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // 初始化RecyclerView
        initTipsRecyclerView();
        
        // 从服务器获取小贴士数据
        fetchTipsFromServer();
        
        return view;
    }
    
    /**
     * 初始化护肤小贴士瀑布流列表
     */
    private void initTipsRecyclerView() {
        // 设置布局管理器为交错网格布局，类似于瀑布流
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerTips.setLayoutManager(layoutManager);
        
        // 设置适配器
        adapter = new TipAdapter(getContext(), tipList);
        adapter.setOnItemClickListener((tip, position) -> {
            // 点击贴士，导航到贴士详情页
            fetchTipDetail(tip.getId());
        });
        recyclerTips.setAdapter(adapter);
    }
    
    /**
     * 从服务器获取小贴士列表
     */
    private void fetchTipsFromServer() {
        // 记录请求的URL
        android.util.Log.d("HomeFragment", "请求小贴士数据: " + TIPS_API_URL);
        
        // 创建Volley请求队列（如果为null）
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(requireContext());
        }
        
        // 创建GET请求
        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET,
            TIPS_API_URL,
            null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    android.util.Log.d("HomeFragment", "获取小贴士成功，数量: " + response.length());
                    android.util.Log.d("HomeFragment", "服务器返回数据: " + response.toString());
                    
                    tipList.clear();
                    
                    if (response.length() == 0) {
                        android.util.Log.w("HomeFragment", "服务器返回的小贴士列表为空，使用本地数据");
                        createTipsData();
                    } else {
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject tipObj = response.getJSONObject(i);
                                
                                // 记录每个小贴士对象的原始JSON
                                android.util.Log.d("HomeFragment", "小贴士对象 #" + i + ": " + tipObj.toString());
                                
                                long id = tipObj.optLong("id", i + 1);
                                String title = tipObj.optString("title", "未知标题");
                                String content = tipObj.optString("content", ""); // 使用optString替代getString
                                String imageUrl = tipObj.optString("image_url", ""); // 注意：匹配服务器返回的字段名
                                
                                // 尝试其他可能的字段名称
                                if (content.isEmpty() && tipObj.has("description")) {
                                    content = tipObj.optString("description", "");
                                }
                                if (imageUrl.isEmpty() && tipObj.has("imageUrl")) {
                                    imageUrl = tipObj.optString("imageUrl", "");
                                }
                                
                                android.util.Log.d("HomeFragment", "解析后的小贴士 #" + id + ": " + title + ", 内容长度: " + content.length() + ", 图片URL: " + imageUrl);
                                
                                // 服务器图片URL转换为完整URL
                                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                                    imageUrl = ApiConfig.BASE_URL + imageUrl;
                                    android.util.Log.d("HomeFragment", "完整图片URL: " + imageUrl);
                                }
                                
                                tipList.add(new Tip(id, imageUrl, title, content));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                android.util.Log.e("HomeFragment", "解析小贴士JSON异常: " + e.getMessage());
                            }
                        }
                    }
                    
                    // 检查处理后的列表是否为空
                    if (tipList.isEmpty()) {
                        android.util.Log.w("HomeFragment", "处理后的小贴士列表为空，使用本地数据");
                        createTipsData();
                    }
                    
                    // 更新适配器
                    adapter.notifyDataSetChanged();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Context context = getContext();
                    String errorMessage = "获取小贴士失败";
                    
                    // 详细记录错误信息
                    if (error.networkResponse != null) {
                        errorMessage += ", 状态码: " + error.networkResponse.statusCode;
                        android.util.Log.e("HomeFragment", errorMessage);
                    } else {
                        errorMessage += ", 原因: " + (error.getMessage() != null ? error.getMessage() : "未知错误");
                        android.util.Log.e("HomeFragment", errorMessage + "\n" + error.toString());
                    }
                    
                    if (context != null) {
                        Toast.makeText(context, errorMessage + "，使用本地数据", Toast.LENGTH_LONG).show();
                    }
                    
                    // 加载本地数据作为后备
                    createTipsData();
                    adapter.notifyDataSetChanged();
                }
            }
        );
        
        // 设置请求超时时间
        request.setRetryPolicy(new DefaultRetryPolicy(
            10000, // 超时时间10秒
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        // 添加请求到队列
        requestQueue.add(request);
    }
    
    /**
     * 获取小贴士详情
     */
    private void fetchTipDetail(long tipId) {
        String url = TIPS_API_URL + "/" + tipId;
        
        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        if (response.length() > 0) {
                            JSONObject tipObj = response.getJSONObject(0);
                            
                            long id = tipObj.getLong("id");
                            String title = tipObj.getString("title");
                            String content = tipObj.getString("content");
                            String imageUrl = tipObj.optString("imageUrl", "");
                            
                            // 服务器图片URL转换为完整URL
                            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                                imageUrl = ApiConfig.BASE_URL + imageUrl;
                            }
                            
                            // 创建小贴士对象
                            Tip tip = new Tip(id, imageUrl, title, content);
                            
                            // 导航到详情页
                            navigateToDetailFragment(tip);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // 获取失败时，使用本地数据
                        showTipDetailWithLocalData(tipId);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "获取小贴士详情失败，使用本地数据", Toast.LENGTH_LONG).show();
                    }
                    // 获取失败时，使用本地数据
                    showTipDetailWithLocalData(tipId);
                }
            }
        );
        
        // 添加请求到队列
        requestQueue.add(request);
    }
    
    /**
     * 使用本地数据显示详情页
     */
    private void showTipDetailWithLocalData(long tipId) {
        TipDetailFragment detailFragment = TipDetailFragment.newInstance(tipId);
        navigateToFragment(detailFragment);
    }
    
    /**
     * 导航到详情页并传递小贴士对象
     */
    private void navigateToDetailFragment(Tip tip) {
        Bundle args = new Bundle();
        args.putLong("tip_id", tip.getId());
        args.putString("tip_title", tip.getTitle());
        args.putString("tip_content", tip.getContent());
        args.putString("tip_image_url", tip.getImageUrl());
        
        TipDetailFragment detailFragment = new TipDetailFragment();
        detailFragment.setArguments(args);
        
        navigateToFragment(detailFragment);
    }
    
    private void navigateToFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    /**
     * 创建护肤小贴士模拟数据（本地后备数据）
     */
    private void createTipsData() {
        tipList.clear();
        // 使用本地drawable资源作为图片源，而不是外部URL
        tipList.add(new Tip(1, "drawable://" + R.drawable.placeholder_image, "痘痘肌肤早餐", "正确的早餐选择可以帮助控制痘痘：\n\n1. 避免高糖高脂食物，如甜甜圈、油条等\n2. 选择全麦面包、燕麦等低GI食物\n3. 添加富含抗氧化物的水果，如蓝莓\n4. 可以适量摄入坚果，补充必要的脂肪酸\n5. 喝足够的水，保持肌肤水分\n\n建议早餐食物：全麦吐司+煮蛋+蓝莓+杏仁"));
        tipList.add(new Tip(2, "drawable://" + R.drawable.placeholder_image, "反复爆痘？可能是皮肤有炎症了！", "很多人认为痘痘只是皮肤问题，但实际上反复爆痘可能是因为皮肤存在炎症。\n\n炎症原因：\n1. 细菌感染：皮肤表面的痤疮丙酸杆菌大量繁殖\n2. 油脂分泌过多：荷尔蒙变化导致\n3. 角质堆积：毛孔堵塞，无法正常排除废物\n4. 免疫反应：身体对细菌的防御反应\n\n如何缓解：\n- 使用含有水杨酸的洁面产品\n- 使用含有烟酰胺的护理产品\n- 保持面部清洁\n- 避免挤压痘痘\n- 保持规律生活作息"));
        tipList.add(new Tip(3, "drawable://" + R.drawable.placeholder_image, "痘痘类型自测", "了解自己的痘痘类型对症下药：\n\n1. 粉刺：毛囊口处于堵塞状态，形成黑头或白头\n   解决方法：使用水杨酸产品，定期去角质\n\n2. 丘疹：皮脂排出受阻，微生物大量繁殖\n   解决方法：控制油脂，使用抗菌成分\n\n3. 脓包：炎症加重，形成内含脓液的囊包\n   解决方法：看皮肤科医生，可能需要药物治疗\n\n4. 结节囊肿：最严重的痘痘类型\n   解决方法：必须就医，可能需要口服药物治疗"));
        tipList.add(new Tip(4, "drawable://" + R.drawable.placeholder_image, "长痘痘的内外原因", "外部刺激：\n- 熬夜导致荷尔蒙分泌紊乱\n- 过度清洁反而导致皮肤分泌更多油脂\n- 防晒不足导致紫外线伤害\n- 环境温度升高导致油脂分泌增加\n\n内部因素：\n- 内分泌失调导致荷尔蒙变化\n- 情绪焦虑影响皮肤健康\n- 高糖高脂饮食增加痘痘风险\n- 遗传因素也会影响油脂腺活跃程度\n\n建议：保持规律作息、均衡饮食、心情舒畅，痘痘会明显减少"));
        tipList.add(new Tip(5, "drawable://" + R.drawable.placeholder_image, "身体缺啥脸长啥", "面部长痘位置与身体健康的关系：\n\n1. 额头痘痘：可能是消化系统问题，注意饮食规律\n\n2. T区痘痘：与肝脏和胃部健康相关，减少辛辣油腻食物\n\n3. 下巴痘痘：可能是内分泌失调，注意作息规律\n\n4. 脸颊痘痘：可能与肺部健康有关，避免吸烟和污染环境\n\n改善方法：\n- 保持规律作息\n- 适量运动\n- 均衡饮食\n- 减少压力"));
        tipList.add(new Tip(6, "drawable://" + R.drawable.placeholder_image, "痘痘肌必吃蔬菜", "对抗痘痘的必吃蔬菜：\n\n1. 胡萝卜：富含维生素A，帮助皮肤修复\n2. 西兰花：含有抗氧化物质，减少炎症\n3. 菠菜：补充铁质和维生素，改善肤色\n4. 黄瓜：水分充足，有助于排毒\n5. 芹菜：利尿消肿，帮助排出毒素\n6. 番茄：含有番茄红素，减少皮肤炎症\n7. 甘蓝：抗氧化，改善皮肤质地\n8. 南瓜：富含锌元素，控制油脂分泌\n9. 紫甘蓝：含有花青素，提高皮肤抵抗力"));
        tipList.add(new Tip(7, "drawable://" + R.drawable.placeholder_image, "万痘可消 痘痘肌护肤宝典", "痘痘肌肤护理需要注意的要点：\n\n1. 清洁温和为先：使用弱酸性洁面产品，不要过度清洁\n\n2. 保湿很重要：控油≠不保湿，选择清爽质地的保湿产品\n\n3. 抗菌消炎：含有水杨酸、烟酰胺的产品有助于控制炎症\n\n4. 防晒必不可少：选择无油配方的防晒霜\n\n5. 少用粉底：减少彩妆使用频率，让皮肤休息\n\n6. 不要挤压痘痘：会导致二次感染和留疤\n\n7. 定期去角质：每周1-2次温和去角质\n\n8. 及时就医：严重的痘痘问题应该寻求医生帮助"));
        tipList.add(new Tip(8, "drawable://" + R.drawable.placeholder_image, "养成不长痘体质", "\"不长痘\"体质需要综合调理：\n\n1. 饮食调整：\n   - 控制高糖高脂食物摄入\n   - 多吃蔬果，补充维生素\n   - 适量饮水，每天2000ml左右\n   - 少吃乳制品和高升糖指数食物\n\n2. 生活习惯：\n   - 规律作息，早睡早起\n   - 适量运动，促进新陈代谢\n   - 减少压力，避免焦虑\n   - 保持心情愉悦\n   - 勤换枕巾和毛巾\n\n3. 皮肤护理：\n   - 温和清洁\n   - 适当保湿\n   - 规律去角质\n   - 全年防晒"));
    }
} 