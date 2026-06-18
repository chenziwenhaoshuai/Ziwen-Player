# 爱壹帆 TV APK

这是一个 Android TV 原生菜单应用，把 `https://www.yfvod.com/` 当作数据源解析，展示分类、海报列表、详情页和剧集播放菜单。播放时优先捕获 `.m3u8` 并用 ExoPlayer 原生播放，捕获失败时回退到网页播放器。

## 功能

- Android TV / 电视盒子 Launcher 入口
- 原生分类侧栏和海报网格
- 原生详情页和剧集播放菜单
- 解析站点 HTML 提取影片、海报、简介、剧集
- 参考 `crack-iyf` 的运行时捕获思路，拦截 WebView 内 `.m3u8`
- 捕获到 `.m3u8` 后使用 Media3 ExoPlayer 播放
- 捕获失败时回退网页播放器

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

## 修改网站地址

默认首页在 [MainActivity.java](app/src/main/java/com/yfvod/tv/MainActivity.java) 的 `HOME_URL` 常量里。
