package com.triple20.overlay

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.triple20.R
import com.triple20.model.RestTips

/**
 * 全屏休息遮罩
 * 通过WindowManager添加全屏View，显示20秒倒计时
 */
class RestOverlayView(private val context: Context) {

    companion object {
        // 护眼配色方案：豆沙绿背景 + 深绿色文字
        private const val BACKGROUND_COLOR = -0xbd6910 // #C7EDCC 豆沙绿
        private const val TEXT_COLOR = -0xff0001 // #006400 深绿色
        private const val PROGRESS_COLOR = -0xc780de // #2E7D32 中绿色
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: FrameLayout? = null
    private var progressBar: ProgressBar? = null
    private var tipTextView: TextView? = null
    private var remainingSeconds = 20
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    /**
     * 显示全屏遮罩
     */
    fun show() {
        if (overlayView != null) return

        overlayView = createOverlayView()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(overlayView, params)
        startCountdown()
    }

    /**
     * 隐藏全屏遮罩
     */
    fun hide() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
            progressBar = null
            tipTextView = null
            remainingSeconds = 20
        }
    }

    /**
     * 创建遮罩视图
     */
    private fun createOverlayView(): FrameLayout {
        return FrameLayout(context).apply {
            setBackgroundColor(BACKGROUND_COLOR)

            // 提示文字
            tipTextView = TextView(context).apply {
                text = RestTips.getRandomTip()
                setTextColor(TEXT_COLOR)
                textSize = 32f
                setTextAppearance(android.R.style.TextAppearance_Large)
                gravity = Gravity.CENTER
                setPadding(64, 0, 64, 0)
            }
            addView(tipTextView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))

            // 进度条
            progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 20
                progress = 20
                isIndeterminate = false
                indeterminateTintList = android.content.res.ColorStateList.valueOf(PROGRESS_COLOR)
                progressTintList = android.content.res.ColorStateList.valueOf(PROGRESS_COLOR)
            }
            addView(progressBar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ))

            // 拦截返回键
            setOnKeyListener { _, keyCode, event ->
                keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
            }
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    /**
     * 开始倒计时
     */
    private fun startCountdown() {
        updateProgress()

        countdownRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    updateProgress()
                    handler.postDelayed(this, 1000)
                } else {
                    hide()
                }
            }
        }
        handler.postDelayed(countdownRunnable!!, 1000)
    }

    /**
     * 更新进度条
     */
    private fun updateProgress() {
        progressBar?.progress = remainingSeconds
    }
}
