package com.triple20.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.Manifest
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.triple20.R
import com.triple20.model.TimerState
import com.triple20.overlay.RestOverlayView

/**
 * 定时器核心服务
 * 负责20分钟倒计时和20秒休息提醒
 */
class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.triple20.ACTION_START"
        const val ACTION_PAUSE = "com.triple20.ACTION_PAUSE"
        const val ACTION_RESET = "com.triple20.ACTION_RESET"
        const val ACTION_UPDATE_TIME = "com.triple20.ACTION_UPDATE_TIME"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_SECONDS = "extra_seconds"

        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
        private const val WARNING_SECONDS = 10 // 倒计时结束前10秒提醒
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var timerState = TimerState()
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var timerRunnable: Runnable? = null
    private var restTimerRunnable: Runnable? = null
    private var restOverlayView: RestOverlayView? = null

    // 状态变化回调
    var onTimerStateChanged: ((TimerState) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenReceiver()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, timerState.minutes)
                val seconds = intent.getIntExtra(EXTRA_SECONDS, timerState.seconds)
                startTimer(minutes, seconds)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
        }
        return START_STICKY
    }

    /**
     * 开始计时
     */
    private fun startTimer(minutes: Int, seconds: Int) {
        android.util.Log.d("TimerService", "startTimer called: $minutes:$seconds, callback: ${onTimerStateChanged != null}")

        timerState = timerState.copy(
            minutes = minutes,
            seconds = seconds,
            isRunning = true,
            isPaused = false,
            totalSeconds = minutes * 60 + seconds
        )

        timerRunnable?.let { handler.removeCallbacks(it) }

        timerRunnable = object : Runnable {
            override fun run() {
                android.util.Log.d("TimerService", "Timer tick: $timerState")
                if (!timerState.isRunning || timerState.isPaused) {
                    notifyStateChanged()
                    return
                }

                // 检查是否到10秒预告时间
                if (timerState.remainingSeconds == WARNING_SECONDS) {
                    showWarningNotification()
                }

                // 检查是否时间到
                if (timerState.isZero) {
                    startRestPeriod()
                    return
                }

                // 减少一秒
                timerState = timerState.tick()
                notifyStateChanged()

                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable!!)
        notifyStateChanged()
        android.util.Log.d("TimerService", "startTimer completed, state: $timerState")
    }

    /**
     * 暂停计时
     */
    private fun pauseTimer() {
        timerState = timerState.copy(isPaused = !timerState.isPaused)
        notifyStateChanged()

        if (timerState.isPaused) {
            timerRunnable?.let { handler.removeCallbacks(it) }
        } else {
            // 恢复计时
            handler.post(timerRunnable ?: return)
        }
    }

    /**
     * 重置计时器
     */
    private fun resetTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        restTimerRunnable?.let { handler.removeCallbacks(it) }
        timerState = timerState.reset()
        notifyStateChanged()
    }

    /**
     * 开始休息时间
     */
    private fun startRestPeriod() {
        timerRunnable?.let { handler.removeCallbacks(it) }

        // 显示全屏休息遮罩
        restOverlayView = RestOverlayView(this)
        restOverlayView?.show()

        // 20秒后重置计时器并重新开始
        restTimerRunnable = object : Runnable {
            override fun run() {
                restOverlayView?.hide()
                restOverlayView = null
                resetTimer()
                startTimer(timerState.totalSeconds / 60, timerState.totalSeconds % 60)
            }
        }
        handler.postDelayed(restTimerRunnable!!, 20000)
    }

    /**
     * 显示10秒预告通知
     */
    private fun showWarningNotification() {
        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("TimerService", "No notification permission, skipping notification")
                return
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        android.util.Log.d("TimerService", "Warning notification shown")
    }

    /**
     * 注册屏幕状态监听
     */
    private fun registerScreenReceiver() {
        screenStateReceiver = ScreenStateReceiver(
            onScreenOff = {
                // 息屏时暂停
                if (timerState.isRunning && !timerState.isPaused) {
                    timerState = timerState.copy(isPaused = true)
                    timerRunnable?.let { handler.removeCallbacks(it) }
                    notifyStateChanged()
                }
            },
            onScreenOn = {
                // 亮屏时自动恢复计时
                if (timerState.isRunning && timerState.isPaused) {
                    timerState = timerState.copy(isPaused = false)
                    timerRunnable?.let { handler.post(it) }
                    notifyStateChanged()
                }
            }
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for timer reminders"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 通知状态变化
     */
    private fun notifyStateChanged() {
        onTimerStateChanged?.invoke(timerState)
    }

    /**
     * 更新时间（用于滚动修改时间）
     */
    fun updateTime(minutes: Int, seconds: Int) {
        timerState = timerState.copy(
            minutes = minutes.coerceIn(0, 99),
            seconds = seconds.coerceIn(0, 59),
            totalSeconds = minutes * 60 + seconds
        )
        notifyStateChanged()
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): TimerState = timerState

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
        restTimerRunnable?.let { handler.removeCallbacks(it) }
        screenStateReceiver?.let { unregisterReceiver(it) }
    }
}
