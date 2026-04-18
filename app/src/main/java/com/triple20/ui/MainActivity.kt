package com.triple20.ui

import android.Manifest
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
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triple20.model.RestTips
import com.triple20.model.TimerState
import com.triple20.service.TimerService
import com.triple20.ui.theme.Triple20Theme

class MainActivity : ComponentActivity() {

    private var timerService: TimerService? = null
    private var timerState by mutableStateOf(TimerState())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            android.util.Log.d("MainActivity", "onServiceConnected")
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            timerService?.onTimerStateChanged = { state ->
                android.util.Log.d("MainActivity", "State changed: $state")
                timerState = state
            }
            timerState = timerService?.getCurrentState() ?: TimerState()
            android.util.Log.d("MainActivity", "Initial state: $timerState")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            android.util.Log.d("MainActivity", "onServiceDisconnected")
            timerService = null
        }
    }

    // 悬浮窗权限请求
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 权限授予结果处理
    }

    // 通知权限请求（Android 13+）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化提示语持久化
        RestTips.init(applicationContext)

        // 检查并请求悬浮窗权限
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }

        // 检查并请求通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
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
                    var showTipsEditor by remember { mutableStateOf(false) }

                    if (showTipsEditor) {
                        TipsEditScreen(
                            onExit = { showTipsEditor = false }
                        )
                    } else {
                        TimerScreen(
                            timerState = timerState,
                            onStartClick = { startTimer() },
                            onPauseClick = { pauseTimer() },
                            onTimeChanged = { minutes, seconds -> updateTime(minutes, seconds) },
                            onEditTips = { showTipsEditor = true }
                        )
                    }
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startTimer() {
        timerService?.let {
            val intent = Intent(this, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_MINUTES, it.getCurrentState().minutes)
                putExtra(TimerService.EXTRA_SECONDS, it.getCurrentState().seconds)
            }
            startService(intent)
        }
    }

    private fun pauseTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
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
    onTimeChanged: (Int, Int) -> Unit,
    onEditTips: () -> Unit = {}
) {
    // 可变状态用于滚动编辑
    var minutes by remember { mutableIntStateOf(timerState.minutes) }
    var seconds by remember { mutableIntStateOf(timerState.seconds) }

    // 监听timerState变化，始终更新本地状态
    LaunchedEffect(timerState) {
        minutes = timerState.minutes
        seconds = timerState.seconds
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        // 设置按钮（右上角）
        IconButton(
            onClick = onEditTips,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "编辑提示语",
                modifier = Modifier.size(32.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsEditScreen(onExit: () -> Unit) {
    val tips = remember { mutableStateListOf(*RestTips.getTips().toTypedArray()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text("编辑提示语") },
            navigationIcon = {
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "退出"
                    )
                }
            }
        )

        // 提示语列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tips.size) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tips[index],
                        onValueChange = { newText ->
                            tips[index] = newText
                            RestTips.saveTips(tips.toList())
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            tips.removeAt(index)
                            RestTips.saveTips(tips.toList())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 添加按钮
        FilledTonalButton(
            onClick = {
                tips.add("")
                RestTips.saveTips(tips.toList())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加提示语")
        }
    }
}
