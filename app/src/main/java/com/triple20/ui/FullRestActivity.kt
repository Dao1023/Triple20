package com.triple20.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.triple20.R

/**
 * 全屏休息界面
 * 显示20秒倒计时进度条和提示文字
 */
class FullRestActivity : AppCompatActivity() {

    private lateinit var messageTextView: TextView
    private lateinit var progressBar: View
    private var remainingSeconds = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeFullScreen()
        setContentView(R.layout.activity_full_rest)

        initViews()
        startCountdown()
    }

    private fun makeFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    private fun initViews() {
        messageTextView = findViewById(R.id.messageTextView)
        progressBar = findViewById(R.id.progressBar)

        // 设置背景色
        window.decorView.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.black)
        )
    }

    private fun startCountdown() {
        messageTextView.text = getString(R.string.rest_message)
        updateProgress()

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    updateProgress()
                    handler.postDelayed(this, 1000)
                } else {
                    finish()
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }

    private fun updateProgress() {
        val progress = (remainingSeconds.toFloat() / 20) * 100
        progressBar.layoutParams.height = (progress * resources.displayMetrics.density).toInt()
        progressBar.requestLayout()
    }

    override fun onBackPressed() {
        // 禁止返回键退出
    }
}
