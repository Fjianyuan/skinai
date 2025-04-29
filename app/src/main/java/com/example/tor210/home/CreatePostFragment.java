package com.example.tor210.home;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tor210.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 发布帖子Fragment
 */
public class CreatePostFragment extends Fragment implements ImageSourceDialogFragment.ImageSourceListener {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int CAPTURE_IMAGE = 101;
    private static final int REQUEST_CAMERA_PERMISSION = 102;
    private static final int REQUEST_STORAGE_PERMISSION = 103;
    private static final String PREF_MY_POSTS = "my_posts";
    
    // UI组件
    private EditText editTitle;
    private EditText editContent;
    private RecyclerView recyclerImages;
    private MaterialButton btnPublish;
    private ChipGroup chipGroupCategories;
    
    // 数据
    private List<Uri> selectedImages = new ArrayList<>();
    private PostImageAdapter imageAdapter;
    private String selectedCategory = "";
    private boolean isPrivate = false;
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_post, container, false);
        initViews(view);
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 首先初始化所有控件
        initViews(view);
        
        // 然后设置点击事件
        setupClickListeners();
        
        // 最后设置文本变化监听
        setupTextWatchers();
        
        // 更新发布按钮状态
        updatePublishButtonState();
    }
    
    /**
     * 初始化视图组件
     */
    private void initViews(View view) {
        // 顶部工具栏
        view.findViewById(R.id.btn_close).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        
        btnPublish = view.findViewById(R.id.btn_publish);
        btnPublish.setEnabled(false); // 初始禁用
        
        // 编辑区域
        editTitle = view.findViewById(R.id.edit_title);
        editContent = view.findViewById(R.id.edit_content);
        
        // 图片区域
        recyclerImages = view.findViewById(R.id.recycler_images);
        recyclerImages.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        imageAdapter = new PostImageAdapter();
        recyclerImages.setAdapter(imageAdapter);
        
        // 分类选择
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        
        // 隐私设置 - 使用正确的Switch类型
        android.widget.Switch switchPrivate = view.findViewById(R.id.switch_private);
        switchPrivate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPrivate = isChecked;
        });
    }
    
    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 发布按钮
        btnPublish.setOnClickListener(v -> {
            Toast.makeText(getContext(), "正在处理发布...", Toast.LENGTH_SHORT).show();
            
            // 检查发布条件
            boolean titleOk = !editTitle.getText().toString().trim().isEmpty();
            boolean contentOk = !editContent.getText().toString().trim().isEmpty();
            boolean imagesOk = selectedImages.size() > 0;
            boolean categoryOk = !selectedCategory.isEmpty();
            
            if (!titleOk) {
                Toast.makeText(getContext(), "请输入标题", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!contentOk) {
                Toast.makeText(getContext(), "请输入内容", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!imagesOk) {
                Toast.makeText(getContext(), "请选择至少一张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!categoryOk) {
                Toast.makeText(getContext(), "请选择一个分类", Toast.LENGTH_SHORT).show();
                selectDefaultCategory(); // 自动选择默认分类
                return;
            }
            
            publishPost();
        });
        
        // 图片适配器点击事件
        imageAdapter.setOnAddClickListener(() -> {
            showImageSourceDialog();
        });
        
        // 分类选择
        initCategoryChips();
    }
    
    /**
     * 显示图片来源选择对话框
     */
    private void showImageSourceDialog() {
        ImageSourceDialogFragment dialog = new ImageSourceDialogFragment();
        dialog.setTargetFragment(this, 0);
        dialog.show(getParentFragmentManager(), "ImageSourceDialog");
    }
    
    /**
     * 实现ImageSourceListener接口的方法
     */
    @Override
    public void onImageSourceSelected(int source) {
        if (source == ImageSourceDialogFragment.SOURCE_CAMERA) {
            checkCameraPermissionAndOpenCamera();
        } else if (source == ImageSourceDialogFragment.SOURCE_GALLERY) {
            checkStoragePermissionAndOpenGallery();
        }
    }
    
    /**
     * 检查相机权限并打开相机
     */
    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }
    
    /**
     * 打开相机
     */
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // 创建保存图片的文件
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 如果文件创建成功，继续
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.tor210.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE);
            }
        }
    }
    
    /**
     * 创建图片文件
     */
    private File createImageFile() throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );
        
        // 保存文件路径，以便后续使用
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
    /**
     * 检查存储权限并打开图库
     */
    private void checkStoragePermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }
    
    /**
     * 打开图库
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getContext(), "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            Uri imageUri = null;
            
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // 从图库选择
                imageUri = data.getData();
            } else if (requestCode == CAPTURE_IMAGE) {
                // 从相机拍照
                imageUri = currentPhotoUri;
            }
            
            if (imageUri != null) {
                // 添加图片到列表
                selectedImages.add(imageUri);
                imageAdapter.notifyDataSetChanged();
                updatePublishButtonState();
                
                // 提示用户已添加图片
                Toast.makeText(getContext(), "图片已添加", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 初始化分类Chips
     */
    private void initCategoryChips() {
        // 确保chipGroupCategories不为空
        if (chipGroupCategories == null) {
            Toast.makeText(getContext(), "分类组件初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 清空之前的选择
        selectedCategory = "";
        
        // 处理所有的Chip
        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            View child = chipGroupCategories.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                
                // 重置状态
                chip.setChecked(false);
                
                // 设置新的点击事件
                chip.setOnClickListener(v -> {
                    // 取消之前选择的Chip
                    for (int j = 0; j < chipGroupCategories.getChildCount(); j++) {
                        View otherChild = chipGroupCategories.getChildAt(j);
                        if (otherChild instanceof Chip) {
                            ((Chip) otherChild).setChecked(otherChild == v);
                        }
                    }
                    
                    // 设置选中的分类
                    selectedCategory = chip.getText().toString();
                    Toast.makeText(getContext(), "已选择分类: " + selectedCategory, Toast.LENGTH_SHORT).show();
                    
                    // 更新发布按钮状态
                    updatePublishButtonState();
                });
            }
        }
    }
    
    /**
     * 自动选择默认分类
     */
    private void selectDefaultCategory() {
        if (chipGroupCategories != null && chipGroupCategories.getChildCount() > 0) {
            View child = chipGroupCategories.getChildAt(0);
            if (child instanceof Chip) {
                child.performClick();
            }
        }
    }
    
    /**
     * 设置文本变化监听
     */
    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                updatePublishButtonState();
            }
        };
        
        editTitle.addTextChangedListener(textWatcher);
        editContent.addTextChangedListener(textWatcher);
    }
    
    /**
     * 更新发布按钮状态
     */
    private void updatePublishButtonState() {
        boolean titleFilled = !editTitle.getText().toString().trim().isEmpty();
        boolean contentFilled = !editContent.getText().toString().trim().isEmpty();
        boolean imageSelected = selectedImages.size() > 0;
        boolean categorySelected = !selectedCategory.isEmpty();
        
        boolean canPublish = titleFilled && contentFilled && imageSelected && categorySelected;
        
        // 输出当前状态
        if (getContext() != null) {
            String status = String.format("标题: %s, 内容: %s, 图片: %s, 分类: %s => 可发布: %s",
                    titleFilled ? "✓" : "✗",
                    contentFilled ? "✓" : "✗",
                    imageSelected ? "✓" : "✗",
                    categorySelected ? "✓" : "✗",
                    canPublish ? "是" : "否");
            
            if (!canPublish) {
                Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
            }
        }
        
        btnPublish.setEnabled(canPublish);
        btnPublish.setAlpha(canPublish ? 1.0f : 0.5f);
    }
    
    /**
     * 发布帖子
     */
    private void publishPost() {
        try {
            String title = editTitle.getText().toString().trim();
            String content = editContent.getText().toString().trim();
            
            if (title.isEmpty()) {
                Toast.makeText(getContext(), "请输入标题", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (content.isEmpty()) {
                Toast.makeText(getContext(), "请输入内容", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedImages.isEmpty()) {
                Toast.makeText(getContext(), "请至少选择一张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (selectedCategory.isEmpty()) {
                Toast.makeText(getContext(), "请选择一个分类", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建图片URL数组
            String[] imageUrls = new String[selectedImages.size()];
            for (int i = 0; i < selectedImages.size(); i++) {
                imageUrls[i] = selectedImages.get(i).toString();
            }
            
            // 创建帖子对象
            Post post = new Post(
                    System.currentTimeMillis(),
                    selectedImages.get(0).toString(), // 使用第一张图片作为封面
                    title,
                    "当前用户", // 暂时写死，后续可以从用户系统获取
                    "https://example.com/avatar.jpg", // 暂时写死，后续可以从用户系统获取
                    0,
                    0,
                    content,
                    System.currentTimeMillis(),
                    imageUrls,
                    isPrivate
            );
            
            // 保存到本地
            boolean saved = savePostToLocalStorage(post);
            
            if (saved) {
                // 显示成功提示
                Toast.makeText(getContext(), "发布成功", Toast.LENGTH_SHORT).show();
                
                // 返回上一页
                if (getActivity() != null) {
                    getParentFragmentManager().popBackStack();
                }
            } else {
                Toast.makeText(getContext(), "发布失败，请重试", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // 捕获任何异常并显示
            Toast.makeText(getContext(), "发布过程中出现错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    /**
     * 保存帖子到本地存储
     * @return 是否保存成功
     */
    private boolean savePostToLocalStorage(Post post) {
        try {
            if (getContext() == null) return false;
            
            SharedPreferences preferences = getContext().getSharedPreferences("app_preferences", Activity.MODE_PRIVATE);
            Gson gson = new Gson();
            
            // 获取现有帖子列表
            String postsJson = preferences.getString(PREF_MY_POSTS, "");
            List<Post> posts = new ArrayList<>();
            
            if (!postsJson.isEmpty()) {
                Type type = new TypeToken<ArrayList<Post>>() {}.getType();
                posts = gson.fromJson(postsJson, type);
            }
            
            // 添加新帖子
            posts.add(0, post); // 添加到列表最前面
            
            // 保存更新后的列表
            String updatedPostsJson = gson.toJson(posts);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_MY_POSTS, updatedPostsJson);
            boolean result = editor.commit(); // 使用commit()而不是apply()，以便获取保存结果
            
            if (result) {
                Toast.makeText(getContext(), "帖子已保存到本地", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "帖子保存失败", Toast.LENGTH_SHORT).show();
            }
            
            return result;
        } catch (Exception e) {
            Toast.makeText(getContext(), "保存过程中出现错误: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 发布帖子的图片适配器
     */
    private class PostImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        
        private static final int TYPE_ADD = 0;
        private static final int TYPE_IMAGE = 1;
        
        private OnAddClickListener onAddClickListener;
        
        public void setOnAddClickListener(OnAddClickListener listener) {
            this.onAddClickListener = listener;
        }
        
        @Override
        public int getItemViewType(int position) {
            if (position == selectedImages.size()) {
                return TYPE_ADD;
            } else {
                return TYPE_IMAGE;
            }
        }
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_ADD) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_image, parent, false);
                return new AddViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_image, parent, false);
                return new ImageViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof AddViewHolder) {
                AddViewHolder addHolder = (AddViewHolder) holder;
                addHolder.itemView.setOnClickListener(v -> {
                    if (onAddClickListener != null) {
                        onAddClickListener.onAddClick();
                    }
                });
            } else if (holder instanceof ImageViewHolder) {
                ImageViewHolder imageHolder = (ImageViewHolder) holder;
                Uri imageUri = selectedImages.get(position);
                
                Glide.with(requireContext())
                        .load(imageUri)
                        .centerCrop()
                        .into(imageHolder.imageView);
                
                imageHolder.btnDelete.setOnClickListener(v -> {
                    selectedImages.remove(position);
                    notifyDataSetChanged();
                    updatePublishButtonState();
                });
            }
        }
        
        @Override
        public int getItemCount() {
            // 图片数量 + 添加按钮
            return selectedImages.size() + 1;
        }
        
        class AddViewHolder extends RecyclerView.ViewHolder {
            public AddViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
        
        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            View btnDelete;
            
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_post_image);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    interface OnAddClickListener {
        void onAddClick();
    }
} 