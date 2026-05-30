# 喝水闹钟

一款帮助你定时喝水的 Android 应用，支持自定义时间段、锁屏语音提醒。

## 功能特性

- 自定义提醒间隔（1分钟起）
- 支持多个提醒时间段（如 8:00-12:00、14:00-20:00）
- 锁屏状态下语音提醒（TTS）
- 前台服务保活，不被系统杀死
- 设置本地持久化存储

## 环境要求

- Node.js >= 22.11.0
- JDK 17+
- Android SDK
- React Native 0.85

## 安装依赖

```bash
npm install
```

## 运行项目

```bash
# 启动开发服务器
npm start

# 编译并安装到手机（新终端执行）
npm run android
```

## 打包 APK

```bash
# 打包 Debug 版本
npm run build:debug

# 打包 Release 版本
npm run build:release
```

打包完成后，APK 文件位于：
```
android/app/build/outputs/apk/release/app-release.apk
```

## 项目结构

```
WaterReminder/
├── src/
│   ├── screens/
│   │   └── HomeScreen.tsx      # 主界面
│   └── NativeNotificationModule.ts  # TurboModule 接口
├── android/
│   └── app/src/main/java/com/waterreminder/
│       ├── MainApplication.kt      # 应用入口
│       ├── MainActivity.kt         # 主 Activity
│       ├── NotificationModule.kt   # 原生通知模块
│       ├── NotificationPackage.kt  # 原生模块注册
│       ├── ReminderService.kt      # 前台提醒服务
│       ├── ReminderActivity.kt     # 锁屏唤醒 Activity
│       └── TtsSingleton.kt         # TTS 语音单例
└── package.json
```

## 技术栈

- React Native 0.85
- TypeScript
- Android 原生 TTS
- 前台服务 (Foreground Service)
- AsyncStorage

## 许可证

MIT
