# 锤子音乐复刻版 (Smartisan Music Revived)

> 一个基于 Smartisan Music 8.1.0 资源与现代 Android 技术栈重写的非官方复刻播放器。

本仓库对应产物：`SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 与原项目相比做了什么修改

原项目（Smartisan Music 8.1.0 官方 APK / People-11 的移植版本）是一个为 Smartisan OS 设计的系统音乐播放器，依赖 Smartisan OS 私有框架，无法在普通 Android 设备上长期稳定运行，且没有云音乐、歌词显示、播放音效等现代能力。

本项目在保留 8.1.0 视觉资源（XML、drawable、dimens、selector、anim）和交互形态的前提下，做了以下核心改动：

- **壳与逻辑分离**：UI 沿用 8.1.0 legacy View 资源复刻；播放、扫描、队列、收藏、设置、数据持久化和后台服务全部使用现代 Android 技术栈重写。
- **包名替换**：`applicationId` 改为 `app.smartisanmusic.revived`，作为独立应用安装，不再依赖 Smartisan OS 系统签名或框架。
- **播放链路重写**：基于 Media3 `1.10.1` 实现后台播放服务、本地媒体库、播放队列和播放状态管理。
- **数据存储重写**：使用 Room `2.8.4` + DataStore Preferences `1.2.1` 替代原项目依赖系统数据库的内容提供者。
- **图片加载重写**：使用 Coil `3.5.0` 处理本地封面与在线封面。
- **构建与 SDK 升级**：AGP `9.2.1`、Kotlin `2.4.0`、`minSdk 31` / `targetSdk 36` / `compileSdk 37`。

---

## 实现了哪些功能

### 本地播放

- 本地媒体扫描、后台播放、收藏、播放列表、播放统计
- 外部音频打开、简易音效、自定义艺术家分隔符
- 歌曲排序筛选、多选滑动、字母快捷栏

### 云音乐（网易云音乐）

- 网易云音乐账号登录、首页推荐、搜索
- 歌单、专辑、艺术家、电台 / 播客浏览
- 喜欢列表、每日推荐、账号歌单浏览
- 歌单创建、添加歌曲、移除歌曲、删除歌单
- 在线播放 URL 刷新、在线队列恢复、在线封面和在线歌词
- 页面 / 歌词 / 流媒体缓存

> 云音乐能力依赖用户自己的网易云音乐账号授权，本项目不提供公共云端曲库或媒体分发服务。

### 视觉与交互

- 底部播放条、搜索、弹窗
- 黑胶唱盘、唱针拖拽、搓碟
- 歌词 / 控制区、播放队列展开和队列拖拽排序

### 歌词通知与跨进程歌词共享

- **HyperOS 超级岛焦点通知**：在小米 / HyperOS 设备的超级岛中实时显示歌词，支持专辑封面、歌名、艺人信息和逐行歌词
- **Android 实况通知（Live Update）**：Android 16+ 使用 `ProgressStyle` 注册系统实况通知，带歌曲进度条；低版本回退 `BigTextStyle` 持续通知
- **多模式切换**：可独立开关超级岛歌词和实况通知，也可启用 LSPosed hook 模式交由独立模块渲染
- **歌词轮询 ~15Hz**：仅在显示内容变化时触发 notify，性能友好
- **封面取色**：从专辑封面提取 Vibrant/Muted 主色，自动适配亮暗主题
- **跨进程歌词共享**：通过 ContentProvider（authority `com.smartisanos.music.lyric`）将歌词快照暴露给 LSPosed 模块（SystemUI 进程），含自定义签名权限保护
- **LyricStateHolder**：跨进程歌词状态快照，播放器侧持续更新，LSPosed hook 侧读取

配合 LSPosed 模块 [`LyricsIsland-LSPosed-For-SmartisanMusic-Revived`](../LyricsIsland-LSPosed-For-SmartisanMusic-Revived/) 可在超级岛中实现逐字高亮、渐变进度、羽化边缘、走马灯等高级歌词渲染。

---

## 编译产物

- **音乐 App Release APK**：`SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`
- **LSPosed 模块 Release APK**：`LyricsIsland-LSPosed-For-SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`

两个子项目互不共享源码，各自独立编译。Release 构建当前未配置发布签名，产物为 `*-unsigned.apk`，如需安装请使用自己的签名密钥进行签名。

```bash
# 音乐 App
cd SmartisanMusic-Revived
./gradlew :app:assembleRelease

# LSPosed 模块
cd LyricsIsland-LSPosed-For-SmartisanMusic-Revived
./gradlew :app:assembleRelease
```

---

## 技术栈

| 类别 | 技术 |
| ---- | ---- |
| 构建 | Android Gradle Plugin `9.2.1` |
| 语言 | Kotlin `2.4.0` |
| UI | legacy View 壳 + Jetpack Compose 桥接 |
| 播放 | Media3 `1.10.1` |
| 存储 | Room `2.8.4` + DataStore Preferences `1.2.1` |
| 在线 | 网易云音乐账号能力 + 页面 / 歌词 / 流媒体缓存 |
| 图片 | Coil `3.5.0` |
| SDK | `minSdk 31` / `targetSdk 36` / `compileSdk 37` |

---

## 致谢

- 感谢 [People-11](https://github.com/People-11/) 的 [SmartisanOS_APP_Port](https://github.com/People-11/SmartisanOS_APP_Port/) 移植工作，其 `Music_8.1.0.apk` 为本项目提供了视觉与交互基准。
- 感谢 [limczhh](https://github.com/limczhh/) 的 [HyperLyric](https://github.com/limczhh/HyperLyric) 为 LSPosed 模块提供实现思路参考。
- 感谢 [LSPosed](https://github.com/LSPosed/LSPosed) Xposed 框架。

---

## 免责声明

本项目与字节跳动无关，仅为个人兴趣驱动的非官方复刻。

- Smartisan OS 及相关视觉设计的知识产权归原权利人所有。
- 8.1.0 APK 资源文件仅供学习研究，版权归原权利人所有。
- 云音乐能力依赖用户自己的网易云音乐账号授权，本项目不提供公共曲库、媒体分发服务或第三方平台会员权益。
- 原创代码仅限非商业用途。
- 本项目按「原样」（AS IS）提供，作者不对因使用本项目而产生的任何直接或间接损失承担责任。

## 许可证

本项目原创代码采用自定义非商业许可证（Custom NonCommercial License）。详见 [LICENSE](./LICENSE)。
