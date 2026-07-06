package com.example.to_do_list

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "important_tasks_channel"
    private const val NOTIFICATION_ID = 999
    fun cancelPinnedNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Công việc quan trọng"
            val descriptionText = "Hiển thị các công việc ghim trên thanh thông báo"
            val importance = NotificationManager.IMPORTANCE_LOW // Để không gây tiếng chuông phiền phức
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    // Cập nhật hiển thị danh sách công việc quan trọng ghim lên thanh thông báo
    fun updatePinnedNotification(context: Context, importantTasks: List<Task>) {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isNotificationEnabled = sharedPreferences.getBoolean("pin_notification", true)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Nếu người dùng tắt cài đặt hoặc không có task quan trọng nào, tiến hành xóa thông báo ghim
        if (!isNotificationEnabled || importantTasks.isEmpty()) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        // Tạo nội dung gộp các việc quan trọng
        val contentText = importantTasks.joinToString(separator = "\n") { task ->
            if (task.time.isNotEmpty() && task.time != "Không hẹn giờ") {
                "• ${task.title} (${task.time})"
            } else {
                "• ${task.title}"
            }
        }

        // Bấm vào thông báo sẽ mở lại ứng dụng chính
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Công việc quan trọng hôm nay")
            .setContentText("Bạn có ${importantTasks.size} việc cần chú ý")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}