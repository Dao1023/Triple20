package com.triple20.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 屏幕状态监听器
 * 监听息屏和亮屏事件，控制定时器暂停和恢复
 */
class ScreenStateReceiver(
    private val onScreenOff: () -> Unit,
    private val onScreenOn: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // 息屏时暂停
                onScreenOff()
            }
            Intent.ACTION_SCREEN_ON -> {
                // 亮屏时恢复
                onScreenOn()
            }
        }
    }
}
