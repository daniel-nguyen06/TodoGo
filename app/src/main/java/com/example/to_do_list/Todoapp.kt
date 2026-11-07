package com.example.to_do_list

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

/**
 * Khởi tạo Firebase App Check để bảo vệ lệnh gọi Gemini (Firebase AI Logic).
 * Nhớ khai báo trong AndroidManifest: <application android:name=".TodoApp" ...>
 */
class TodoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            // Chế độ debug: dùng debug token (đăng ký ở Firebase Console > App Check > Manage debug tokens)
            Firebase.appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        }
        // Bản phát hành (release): thêm dependency "com.google.firebase:firebase-appcheck-playintegrity"
        // rồi bật đoạn dưới đây:
        // else {
        //     Firebase.appCheck.installAppCheckProviderFactory(
        //         com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
        //     )
        // }
    }
}