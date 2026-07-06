plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 🟢 THÊM PLUGIN GOOGLE SERVICES VÀO ĐÂY:
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.to_do_list"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.to_do_list"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 🟢 ĐỒNG BỘ MỘT BOM DUY NHẤT ĐỂ QUẢN LÝ PHIÊN BẢN FIREBASE
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database") // Gọi thư viện DB chuẩn
    implementation("com.google.firebase:firebase-auth")
    // Đăng nhập bằng gmail
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    // Thêm Firebase Auth
    implementation("com.google.firebase:firebase-auth")

    // Các thư viện hệ thống cơ bản quản lý tự động qua Version Catalog (libs.xxx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Thư viện kiểm thử (Testing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Thư viện bổ sung giao diện hiện đại và tải ảnh Coil
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    // API AI
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
}