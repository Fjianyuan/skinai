plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tor210"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tor210"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // 基础库
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.inappmessaging)
    
    // UI组件
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("de.hdodenhof:circleimageview:3.1.0") // 圆形头像
    
    // 导航组件
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")
    
    // 网络与图片处理
    implementation("com.squareup.okhttp3:okhttp:4.9.3") // HTTP客户端
    implementation("com.android.volley:volley:1.2.1") // 网络请求库
    implementation("com.github.bumptech.glide:glide:4.16.0") // 图片加载库
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8") // 加载图片
    implementation("com.github.yalantis:ucrop:2.2.8") // 图片裁剪
    
    // 工具库
    implementation("com.google.code.gson:gson:2.10.1") // JSON序列化/反序列化
    
    // 测试库
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
