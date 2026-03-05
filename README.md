# Triple20 - 20-20-20 护眼提醒

![Version](https://img.shields.io/badge/version-1.0.0-green)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

基于 20-20-20 法则的护眼提醒应用，帮助你养成良好的用眼习惯。

## 📖 什么是 20-20-20 法则？

20-20-20 法则是美国眼科医生推荐的护眼方法：
- 每用眼 **20 分钟**
- 向 **20 英尺**（约 6 米）外的地方看
- 至少 **20 秒**

这样可以有效缓解眼部疲劳，预防近视和干眼症。

## ✨ 功能特性

### ⏱️ 智能定时器
- **20 分钟工作倒计时** - 默认工作时间，可自定义调整
- **10 秒预告通知** - 休息前 10 秒发出通知（无震动无声音）
- **20 秒休息提醒** - 全屏遮罩显示休息提示

### 💬 随机休息提示
- **45 条精心挑选的休息建议**
- 包括眼部运动、身体拉伸、姿势调整等
- 每次随机显示，保持新鲜感

### 🎨 护眼配色
- **豆沙绿背景** (#C7EDCC) - 经典护眼色，柔和不刺眼
- **深绿色文字** (#006400) - 对比度高但不过于刺眼
- 减少蓝光刺激，缓解眼部疲劳

### 🛡️ 智能暂停
- **息屏自动暂停** - 屏幕关闭时暂停计时
- **亮屏保持暂停** - 需要手动恢复，不打扰工作
- **后台持续运行** - Service 保证稳定运行

### 🎯 便捷交互
- **滚动调整时间** - 暂停时上下滚动可调整分钟/秒数
- **简单直观** - 只有一个开始/暂停按钮
- **防误触** - 休息时禁止返回键退出

## 📥 下载安装

### 方式一：直接安装 APK

1. 下载最新的 Release APK：[app-release-unsigned.apk](app/build/outputs/apk/release/app-release-unsigned.apk)
2. 在手机上打开文件并安装
3. 授予悬浮窗权限和通知权限
4. 开始使用！

### 方式二：从源码构建

```bash
# 克隆仓库
git clone https://github.com/yourusername/Triple20.git
cd Triple20

# 编译 Release APK
./gradlew assembleRelease

# APK 位置
ls app/build/outputs/apk/release/
```

## 🔧 权限说明

| 权限 | 用途 | 必需性 |
|------|------|--------|
| `SYSTEM_ALERT_WINDOW` | 显示全屏休息遮罩 | 必需 |
| `POST_NOTIFICATIONS` | 显示 10 秒预告通知 | 必需 |

## 🚀 使用方法

### 基本使用

1. **启动应用** - 首次启动会请求权限
2. **点击开始** - 开始 20 分钟倒计时
3. **收到通知** - 10 秒后会收到休息预告
4. **全屏休息** - 20 秒全屏遮罩显示休息提示
5. **自动循环** - 休息结束后自动开始新一轮

### 高级技巧

- **快速测试**：暂停时滚动调整时间为 10 秒，快速体验流程
- **自定义时间**：根据个人习惯调整工作和休息时长
- **暂停修改**：暂停时可以滚动调整时间

## 🛠️ 技术架构

### 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构**: 简单分层（Service + UI）
- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 14 (API 34)

### 项目结构

```
app/
├── service/
│   ├── TimerService.kt          # 定时器核心服务
│   └── ScreenStateReceiver.kt   # 屏幕状态监听
├── ui/
│   ├── MainActivity.kt          # 主界面
│   ├── FullRestActivity.kt      # 全屏休息页
│   └── theme/                   # Compose 主题
├── overlay/
│   └── RestOverlayView.kt       # 全屏遮罩 View
└── model/
    ├── TimerState.kt            # 定时器状态
    └── RestTips.kt              # 休息提示语库
```

### 核心流程

```
启动 → 20分钟计时 → 10秒预告 → 全屏休息20秒 → 循环
                ↓
            息屏暂停 → 亮屏恢复
```

### 构建优化

- ✅ 代码混淆（R8/ProGuard）
- ✅ 资源压缩
- ✅ 国内镜像源（阿里云 + 腾讯云）
- ✅ Release APK 仅 1.5 MB（相比 Debug 减少 82%）

## 📝 版本历史

### v1.0.0 (2026-03-05)

**功能特性**
- ✅ 20 分钟工作倒计时 + 10 秒预告通知
- ✅ 20 秒全屏休息遮罩（随机提示语 + 护眼配色）
- ✅ 息屏暂停计时，亮屏保持暂停状态
- ✅ 滚动修改时间功能
- ✅ 45 条随机休息提示语
- ✅ 护眼配色方案（豆沙绿 + 深绿色）

**技术实现**
- Kotlin + Jetpack Compose
- 普通 Service + BroadcastReceiver
- WindowManager 悬浮窗
- Android 13+ 通知权限适配
- 国内镜像源优化

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

Dao

## 🙏 致谢

- 休息提示语灵感来源于 [Stretchly](https://github.com/hovancik/stretchly) 项目
- 感谢所有贡献者

---

**保护眼睛，从现在开始！** 👀✨
