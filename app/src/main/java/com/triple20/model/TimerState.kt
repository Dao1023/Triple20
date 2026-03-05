package com.triple20.model

/**
 * 定时器状态数据类
 */
data class TimerState(
    val minutes: Int = 20,        // 分钟
    val seconds: Int = 0,         // 秒钟
    val isRunning: Boolean = false, // 是否正在运行
    val isPaused: Boolean = false,  // 是否暂停
    val totalSeconds: Int = 20 * 60 // 总秒数（用于计算进度）
) {
    /**
     * 获取剩余总秒数
     */
    val remainingSeconds: Int
        get() = minutes * 60 + seconds

    /**
     * 格式化显示时间，如 20:00
     */
    val formattedTime: String
        get() = String.format("%02d:%02d", minutes, seconds)

    /**
     * 是否为零（时间到）
     */
    val isZero: Boolean
        get() = minutes == 0 && seconds == 0

    /**
     * 复制并减少一秒
     */
    fun tick(): TimerState {
        if (isZero) return this

        val total = remainingSeconds - 1
        return copy(
            minutes = total / 60,
            seconds = total % 60
        )
    }

    /**
     * 重置到初始状态
     */
    fun reset(): TimerState {
        return copy(
            minutes = totalSeconds / 60,
            seconds = totalSeconds % 60,
            isRunning = false,
            isPaused = false
        )
    }
}
