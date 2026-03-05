package com.triple20.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triple20.model.TimerState
import com.triple20.service.TimerService
import com.triple20.ui.theme.Triple20Theme

class MainActivity : ComponentActivity() {

    private var timerService: TimerService? = null
    private var timerState by mutableStateOf(TimerState())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            timerService?.onTimerStateChanged = { state ->
                timerState = state
            }
            timerState = timerService?.getCurrentState() ?: TimerState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
        }
    }

    // 悬浮窗权限请求
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 权限授予结果处理
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查并请求悬浮窗权限
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }

        // 启动服务
        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            Triple20Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TimerScreen(
                        timerState = timerState,
                        onStartClick = { startTimer() },
                        onPauseClick = { pauseTimer() },
                        onTimeChanged = { minutes, seconds -> updateTime(minutes, seconds) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun startTimer() {
        timerService?.let {
            val intent = Intent(TimerService.ACTION_START).apply {
                putExtra(TimerService.EXTRA_MINUTES, it.getCurrentState().minutes)
                putExtra(TimerService.EXTRA_SECONDS, it.getCurrentState().seconds)
            }
            startService(intent)
        }
    }

    private fun pauseTimer() {
        val intent = Intent(TimerService.ACTION_PAUSE)
        startService(intent)
    }

    private fun updateTime(minutes: Int, seconds: Int) {
        timerService?.updateTime(minutes, seconds)
        timerState = timerService?.getCurrentState() ?: timerState
    }
}

@Composable
fun TimerScreen(
    timerState: TimerState,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onTimeChanged: (Int, Int) -> Unit
) {
    // 可变状态用于滚动
    var minutes by remember { mutableIntStateOf(timerState.minutes) }
    var seconds by remember { mutableIntStateOf(timerState.seconds) }

    // 监听timerState变化更新本地状态
    LaunchedEffect(timerState) {
        if (timerState.isPaused || !timerState.isRunning) {
            minutes = timerState.minutes
            seconds = timerState.seconds
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 时间显示
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 分钟
            ScrollableNumber(
                value = minutes,
                range = 0..99,
                enabled = timerState.isPaused || !timerState.isRunning,
                onValueChange = { newMinutes ->
                    minutes = newMinutes
                    onTimeChanged(minutes, seconds)
                }
            )

            // 冒号
            Text(
                text = ":",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // 秒钟
            ScrollableNumber(
                value = seconds,
                range = 0..59,
                enabled = timerState.isPaused || !timerState.isRunning,
                onValueChange = { newSeconds ->
                    seconds = newSeconds
                    onTimeChanged(minutes, seconds)
                }
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        // 开始/暂停按钮
        Button(
            onClick = {
                if (timerState.isRunning && !timerState.isPaused) {
                    onPauseClick()
                } else {
                    onStartClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (timerState.isRunning && !timerState.isPaused) {
                    "暂停"
                } else {
                    "开始"
                },
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun ScrollableNumber(
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    var currentValue by remember { mutableIntStateOf(value) }

    LaunchedEffect(value) {
        currentValue = value
    }

    Box(
        modifier = Modifier.size(120.dp, 160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%02d", currentValue),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                val threshold = 100f
                                if (dragAmount > threshold) {
                                    // 向上拖动，数值减小
                                    val newValue = (currentValue - 1).coerceIn(range)
                                    if (newValue != currentValue) {
                                        currentValue = newValue
                                        onValueChange(newValue)
                                    }
                                } else if (dragAmount < -threshold) {
                                    // 向下拖动，数值增大
                                    val newValue = (currentValue + 1).coerceIn(range)
                                    if (newValue != currentValue) {
                                        currentValue = newValue
                                        onValueChange(newValue)
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        )
    }
}
