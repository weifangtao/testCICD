# Android 与 Flutter 混合架构及双向通信指南

本文档详细介绍了本项目中 **Android 宿主与 Flutter 模块的混合开发架构**，包含引擎预热管理、类型安全的 **MethodChannel** 与 **EventChannel** 双向通信桥梁，以及 Flutter 端 Dart 代码接入规范。

---

## 一、 混合架构设计

### 1. 整体架构图

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        Android Native (Kotlin)                         │
│                                                                        │
│  ┌───────────────────────┐  ┌──────────────────────────────────────┐  │
│  │ FlutterEngineManager  │  │  AppFlutterBridgeInitializer         │  │
│  │ (引擎预热与缓存管理)    │  │  (系统/登录态/扫码等 Handlers 注册)   │  │
│  └───────────┬───────────┘  └──────────────────┬───────────────────┘  │
│              │                                 │                      │
│              ▼                                 ▼                      │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │              FlutterBridge Core (lib:core / lib.flutter)          │ │
│  │  - FlutterMethodBridge       (MethodChannel 调 Native 方法)       │ │
│  │  - FlutterEventStreamBridge  (EventChannel Native 事件实时推送)   │ │
│  └───────────────────────────────┬──────────────────────────────────┘ │
└──────────────────────────────────┼────────────────────────────────────┘
                                   │  MethodChannel & EventChannel
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Flutter Module (Dart)                         │
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │  AppNativeBridge.dart                                            │ │
│  │  - getSystemInfo() / getUserSession() / logout()                 │ │
│  │  - scannerEventStream (Stream<BarcodeEvent>)                     │ │
│  └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 二、 核心组件说明 (Android 端)

| 类名 | 核心职责 | 所在包/位置 |
|---|---|---|
| `FlutterEngineManager` | **引擎单例预热管理**：负责 `FlutterEngineCache` 初始化、预载引擎与内存销毁。 | `lib.flutter` |
| `FlutterMethodBridge` | **RPC 方法桥梁**：接收 Flutter 的 `MethodChannel` 调用，自动在协程（`Dispatchers.IO`）中处理并切回主线程返回结果。 | `lib.flutter` |
| `FlutterEventStreamBridge` | **事件流桥梁**：管理 `EventChannel` 实时推送到 Flutter 的数据流（如 PDA 条码枪扫码事件）。 | `lib.flutter` |
| `FlutterBridgeHandler` | **Method 处理接口**：定义单个 Native 暴露给 Flutter 的业务方法（如 `getSystemInfo`, `getUserSession`）。 | `lib.flutter` |
| `FlutterEventStreamHandler` | **Event 处理接口**：定义 Native 向 Flutter 持续推送数据的 Channel。 | `lib.flutter` |
| `AppFlutterBridgeInitializer` | **应用装配入口**：在 `AppApplication.onCreate()` 中自动注册系统信息、登录态、扫码等核心 Handlers。 | `app.flutter` |

---

## 三、 通信通道协议定义

所有通道统一使用 Domain 前缀：`com.example.testdemo/`

### 1. MethodChannel (RPC 响应式调用)

频道名称：`com.example.testdemo/bridge`

#### ① 获取系统与环境信息 (`getSystemInfo`)
* **Flutter 调用**：
  ```dart
  final result = await AppNativeBridge.getSystemInfo();
  ```
* **Native 返回数据**：
  ```json
  {
    "appName": "testDemo",
    "versionName": "1.0.0",
    "versionCode": 1,
    "environmentKey": "dev",
    "baseUrl": "https://wms-dev.haoqianyi.com/k3_wms/",
    "androidSdkVersion": 34,
    "deviceModel": "Pixel 7"
  }
  ```

#### ② 获取当前登录会话 (`getUserSession`)
* **Flutter 调用**：
  ```dart
  final session = await AppNativeBridge.getUserSession();
  ```
* **Native 返回数据**（未登录返回 Error: `UNAUTHORIZED`）：
  ```json
  {
    "userCode": "alice",
    "userName": "张三",
    "token": "bearer_token_xxx",
    "houseId": "WH001",
    "houseName": "中央仓库",
    "environment": "dev"
  }
  ```

#### ③ 触发 Native 退出登录 (`logout`)
* **Flutter 调用**：
  ```dart
  await AppNativeBridge.logout();
  ```

---

### 2. EventChannel (Native 实时事件推送)

频道名称：`com.example.testdemo/scanner_events`

#### 条码枪/PDA 扫码广播事件推送
* **Native 发送**：
  ```kotlin
  AppFlutterBridgeInitializer.scannerStreamHandler.dispatchBarcodeScan("6901234567890", "EAN_13")
  ```
* **Flutter 订阅**：
  ```json
  {
    "barcode": "6901234567890",
    "scanType": "EAN_13",
    "timestamp": 1710000000000
  }
  ```

---

## 四、 Flutter 端 (Dart) 代码接入模板

在 Flutter 项目的 `lib/bridge/app_native_bridge.dart` 中粘贴以下代码即可直接调用：

```dart
import 'package:flutter/services.dart';

/// App 与 Android Native 通信桥梁
class AppNativeBridge {
  static const MethodChannel _methodChannel =
      MethodChannel('com.example.testdemo/bridge');

  static const EventChannel _scannerEventChannel =
      EventChannel('com.example.testdemo/scanner_events');

  /// 获取系统与应用环境信息
  static Future<Map<String, dynamic>> getSystemInfo() async {
    try {
      final result = await _methodChannel.invokeMethod('getSystemInfo');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      throw '获取系统信息失败: ${e.message}';
    }
  }

  /// 获取当前登录会话
  static Future<Map<String, dynamic>> getUserSession() async {
    try {
      final result = await _methodChannel.invokeMethod('getUserSession');
      return Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      if (e.code == 'UNAUTHORIZED') {
        throw '未登录';
      }
      throw '获取登录态失败: ${e.message}';
    }
  }

  /// 触发 Native 退出登录
  static Future<void> logout() async {
    try {
      await _methodChannel.invokeMethod('logout');
    } on PlatformException catch (e) {
      throw '退出登录失败: ${e.message}';
    }
  }

  /// 监听 Native 扫码广播事件流
  static Stream<Map<String, dynamic>> get scannerEvents {
    return _scannerEventChannel.receiveBroadcastStream().map((event) {
      return Map<String, dynamic>.from(event);
    });
  }
}
```

---

## 五、 如何在项目中引入 Flutter AAR / Module

当你的团队创建了 Flutter Module 并打出 AAR 后，按以下步骤挂载进 Android 工程：

1. **设置 `settings.gradle.kts`**：
   ```kotlin
   // 挂载本地 Flutter Module 或 AAR 仓库
   setBinding(new Binding([gradle: this]))
   evaluate(new File(
     settingsDir.parentFile,
     'my_flutter_module/.android/include_flutter.groovy'
   ))
   ```

2. **在 `app/build.gradle.kts` 中添加依赖**：
   ```kotlin
   dependencies {
       implementation(project(":flutter"))
   }
   ```

3. **引擎挂载 (`FlutterEngineManager.kt`)**：
   将 `prewarmEngine` 函数中的逻辑连接至 `io.flutter.embedding.engine.FlutterEngine` 和 `FlutterEngineCache` 即可：
   ```kotlin
   val flutterEngine = FlutterEngine(context).apply {
       dartExecutor.executeDartEntrypoint(
           DartExecutor.DartEntrypoint.createDefault()
       )
   }
   FlutterEngineCache.getInstance().put(engineId, flutterEngine)
   ```

---

## 六、 最佳实践与注意事项

1. **线程安全**：
   * `FlutterMethodBridge` 保证所有业务代码在协程 `Dispatchers.IO` 中异步执行，**绝不阻塞 Android 主线程**。
   * 回调结果自动切回 `Dispatchers.Main` 投递给 Flutter Engine。
2. **内存泄露防护**：
   * `EventChannel` 在 Flutter 页面销毁时会触发 `onCancel()`，Native 侧会自动移除引用。
3. **引擎预热**：
   * 在 `AppApplication.onCreate()` 中提早预热引擎，后续打开 FlutterActivity 即可实现**秒开无黑屏**。
