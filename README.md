我要新建一个安卓项目，尽可能简单，做一个20-20-20的提醒软件，功能如下
- 每隔二十分钟提醒休息20秒，可能要用悬浮窗实现
- 开始前10秒发出消息提醒
- 保持后台运行，息屏时暂停

## 技术架构

技术选型

- 语言：Kotlin
- UI框架：Jetpack Compose
- 架构：简单分层
- 最小SDK：Android 8.0 (API 26)

功能清单

1. 定时器核心

- 20分钟工作倒计时
- 倒计时结束前10秒预告（无震动+无声音，仅一条通知）
- 20秒休息全屏遮罩
- 息屏时暂停计时，亮屏恢复

2. 全屏休息界面

- 全屏遮罩（MATCH_PARENT × MATCH_PARENT）
- 20秒倒计时显示成一条进度条加载
- 进度条上面是一段文字，目前设为：起身倒杯水吧
- 禁止返回键退出
- 倒计时结束自动关闭

3. 后台服务

- 普通Service（非前台）
- BroadcastReceiver 监听屏幕状态
- 用户引导：设置允许后台运行

4. 主界面

- 倒计时显示，如 20:00
- 开始/暂停按钮
- 在暂停期间，倒计时上下滚动谁就可以修改谁，比如把 20 改成 00，把 00 改成 10，倒计时 10s 用来测试
- 开始后倒计时开始变成 19:59
- 暂停后时间不变

权限需求

- SYSTEM_ALERT_WINDOW（悬浮窗）
- POST_NOTIFICATIONS（10秒预告通知）

目录结构

app/
├── service/
│   ├── TimerService.kt          # 定时器核心服务
│   └── ScreenStateReceiver.kt   # 屏幕状态监听
├── ui/
│   ├── MainActivity.kt          # 主界面
│   ├── FullRestActivity.kt      # 全屏休息页（特殊主题）
│   └── theme/                   # Compose主题
├── overlay/
│   └── RestOverlayView.kt       # 全屏遮罩View
└── model/
    └── TimerState.kt            # 定时器状态数据类

核心流程

启动 → 20分钟计时 → 10秒预告 → 全屏休息20秒 → 循环
                ↓
            息屏暂停 → 亮屏恢复

