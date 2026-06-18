
这是一个 Android TV 原生菜单应用。

## 功能

- Android TV / 电视盒子 Launcher 入口
- 原生分类侧栏和海报网格
- 原生详情页和剧集播放菜单
- 解析站点 HTML 提取影片、海报、简介、剧集

## 打包

需要 JDK 17 或更高版本、Android SDK 和 Gradle。当前项目已经带有 Gradle Wrapper，并指向本机 SDK：

```properties
sdk.dir=C\:\\Users\\HP\\AppData\\Local\\Android\\Sdk
```

打 debug 包：

```powershell
.\gradlew.bat assembleDebug
```

生成位置：

```text
app\build\outputs\apk\debug\app-debug.apk
```

安装到已连接电视或盒子：

```powershell
C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

## 声明

本软件仅供开源学习，请勿用于侵权行为，作者不负任何责任。
