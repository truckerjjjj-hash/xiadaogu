package com.resonance.zhiji.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "知己行动提醒"
        val intensity = intent.getStringExtra("INTENSITY") ?: "NORMAL"
        val channelId = "resonance_action_reminder"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "知己行动与强提醒",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (intensity == "STRONG") "【强提醒】$taskTitle" else "行动提醒：$taskTitle")
            .setContentText("点击打开知己 Resonance 标记完成、推迟 15 分钟或调整计划")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(taskTitle.hashCode(), notification)
    }
}
