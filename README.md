# 锤子音乐复刻版 (Smartisan Music Revived Fork)

## 原作者 [Mangi-11](https://github.com/Mangi-11/) 

原项目保留锤子音乐8.1.0原版界面资源与交互，剥离原系统私有依赖，用现代安卓技术全量重构播放、存储等底层逻辑，独立打包运行；同时新增网易云在线曲库、在线歌词缓存等功能，完整保留黑胶、搓碟等经典特色交互。本项目旨在保留原项目所有优秀的交互和UI设计，并针对小米设备进行针对性优化，增加超级岛相关适配：

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

- 感谢 [Mangi-11](https://github.com/Mangi-11/) 的 [SmartisanMusic Revived](https://github.com/Mangi-11/SmartisanMusic-Revived) 对 [People-11](https://github.com/People-11/) 的 [SmartisanOS_APP_Port](https://github.com/People-11/SmartisanOS_APP_Port) 移植和逆向复活工作。
- 感谢 [limczhh](https://github.com/limczhh/) 的 [HyperLyric](https://github.com/limczhh/HyperLyric) 为 LSPosed 模块和无root下的实况通知、超级岛提供实现思路参考。

---

## 免责声明

本项目与字节跳动无关，仅为个人兴趣驱动的非官方复刻。

- Smartisan OS 及相关视觉设计的知识产权归原权利人所有。
- 8.1.0 APK 资源文件仅供学习研究，版权归原权利人所有。
- 云音乐能力依赖用户自己的网易云音乐账号授权，本项目不提供公共曲库、媒体分发服务或第三方平台会员权益。
- 原创代码仅限非商业用途。
- 本项目按「原样」（AS IS）提供，作者不对因使用本项目而产生的任何直接或间接损失承担责任。
- 本项目为本人初次尝试制作公开为目的的安卓软件，出现问题为正常现象，如发现bug，各位大佬请多多指教。

## 许可证

本项目原创代码采用和原项目相同的自定义非商业许可证（Custom NonCommercial License）。详见 [LICENSE](./LICENSE)。
