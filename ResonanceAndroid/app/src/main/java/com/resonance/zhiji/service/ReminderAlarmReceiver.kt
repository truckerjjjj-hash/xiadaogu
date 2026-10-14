package com.resonance.zhiji.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 第九章 9.4：原生强提醒通知接收器（由 Android AlarmManager 触发，不依赖云端大模型）
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "知己行动提醒"
        val intensity = intent.getStringExtra("INTENSITY") ?: "NORMAL"
        val channelId = "resonance_action_reminder_v2"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "知己行动与强提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于执行经过用户授权确认的本地任务与强提醒"
            }
            manager.createNotificationChannel(channel)
        }

        val titlePrefix = if (intensity == "STRONG") "【强提醒】" else "【行动提醒】"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$titlePrefix$taskTitle")
            .setContentText("点击返回知己标记完成、推迟 15 分钟或记录执行阻碍")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(taskTitle.hashCode(), notification)
    }
}
