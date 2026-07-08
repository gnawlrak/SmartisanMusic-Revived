# Smartisan Music Revived

> An unofficial recreation of Smartisan Music 8.1.0, rebuilt with modern Android technology while preserving the original visual assets.

Build artifact for this project: `SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`

---

## What changed from the original project

The original project (Smartisan Music 8.1.0 official APK / People-11's port) is a system music player designed for Smartisan OS. It relies on private Smartisan OS frameworks and cannot run stably on stock Android devices. It also lacks modern features like cloud music, lyrics display, and sound effects.

This project keeps the 8.1.0 visual resources (XML layouts, drawables, dimens, selectors, animations) and interaction style, but makes the following core changes:

- **UI shell vs. logic separation**: The UI is recreated using the 8.1.0 legacy View shell; playback, media scanning, queue, favorites, settings, data persistence, and background services are all rewritten with modern Android technology.
- **Package name changed**: `applicationId` is set to `app.smartisanmusic.revived` so it can be installed as a standalone app without depending on the Smartisan OS system signature or framework.
- **Playback pipeline rewritten**: Based on Media3 `1.10.1`, implementing background playback service, local media library, playback queue, and playback state management.
- **Data storage rewritten**: Uses Room `2.8.4` + DataStore Preferences `1.2.1` instead of the original system-database-dependent content providers.
- **Image loading rewritten**: Uses Coil `3.5.0` for local and online cover art.
- **Build and SDK upgraded**: AGP `9.2.1`, Kotlin `2.4.0`, `minSdk 31` / `targetSdk 36` / `compileSdk 37`.

---

## What features are implemented

### Local playback

- Local media scanning, background playback, favorites, playlists, play counts
- Opening external audio files, basic sound effects, custom artist separators
- Song sorting and filtering, multi-select swipe, alphabetical sidebar

### Cloud music (NetEase Cloud Music)

- NetEase Cloud Music account login, home recommendations, search
- Playlist, album, artist, radio / podcast browsing
- Liked songs, daily recommendations, account playlist browsing
- Playlist creation, adding/removing songs, deleting playlists
- Online playback URL refresh, online queue restoration, online cover art and lyrics
- In-memory / disk caching for pages, lyrics, and streaming

> Online music features require authorization with your own NetEase Cloud Music account. This project does not provide a public cloud music catalog or media distribution service.

### Visuals and interaction

- Bottom playback bar, search, dialogs
- Vinyl turntable, tonearm drag, scratch effect
- Lyrics / control area, expandable playback queue with drag-to-reorder

### Lyrics notification and cross-process lyrics sharing

- **HyperOS Super Island focus notification**: Real-time lyrics display on Xiaomi / HyperOS devices' super island, with album art, song title, artist info, and per-line lyrics
- **Android Live Update**: Android 16+ uses `ProgressStyle` for system live update notifications with song progress bar; falls back to `BigTextStyle` on older versions
- **Multi-mode switching**: Independently toggle super island lyrics, live update, or LSPosed hook mode for the standalone module
- **~15Hz lyrics polling**: `notify()` only triggers when display content changes, battery-friendly
- **Album art color extraction**: Extracts Vibrant/Muted colors from album art for adaptive light/dark theming
- **Cross-process lyrics sharing**: Exposes lyrics snapshots via ContentProvider (authority `com.smartisanos.music.lyric`) to the LSPosed module (SystemUI process), protected by a custom signature permission
- **LyricStateHolder**: Cross-process lyric state snapshot, continuously updated by the player, read by the LSPosed hook

Works with the LSPosed module [`LyricsIsland-LSPosed-For-SmartisanMusic-Revived`](../LyricsIsland-LSPosed-For-SmartisanMusic-Revived/) for word-level highlighting, gradient progress, feathered edges, and marquee scrolling in the super island.

---

## Build artifacts

- **Music App Release APK**: `SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`
- **LSPosed Module Release APK**: `LyricsIsland-LSPosed-For-SmartisanMusic-Revived/app/build/outputs/apk/release/app-release-unsigned.apk`

The two subprojects do not share source code and can be compiled independently. The release build currently has no release signing configuration, so the output is `*-unsigned.apk`. To install, sign it with your own release key.

```bash
# Music App
cd SmartisanMusic-Revived
./gradlew :app:assembleRelease

# LSPosed Module
cd LyricsIsland-LSPosed-For-SmartisanMusic-Revived
./gradlew :app:assembleRelease
```

---

## Tech stack

| Category | Technology |
| --- | --- |
| Build | Android Gradle Plugin `9.2.1` |
| Language | Kotlin `2.4.0` |
| UI | Legacy View shell + Jetpack Compose bridge |
| Playback | Media3 `1.10.1` |
| Storage | Room `2.8.4` + DataStore Preferences `1.2.1` |
| Online | NetEase Cloud Music account integration + page / lyric / streaming cache |
| Images | Coil `3.5.0` |
| SDK | `minSdk 31` / `targetSdk 36` / `compileSdk 37` |

---

## Acknowledgments

- Thanks to [People-11](https://github.com/People-11/) for the [SmartisanOS_APP_Port](https://github.com/People-11/SmartisanOS_APP_Port/) project; its `Music_8.1.0.apk` provided the visual and interaction baseline for this recreation.
- Thanks to [limczhh](https://github.com/limczhh/) for [HyperLyric](https://github.com/limczhh/HyperLyric), which inspired the LSPosed module implementation.
- Thanks to [LSPosed](https://github.com/LSPosed/LSPosed) for the Xposed framework.

---

## Disclaimer

This project is not affiliated with ByteDance. It is an unofficial recreation driven by personal interest.

- Smartisan OS and related visual designs are the intellectual property of their respective rights holders.
- The 8.1.0 APK resource files are included or referenced only for learning, research, and preservation purposes; copyright belongs to the original rights holders.
- Online music features require authorization with your own NetEase Cloud Music account. This project does not provide a public music catalog, media distribution service, or third-party platform membership benefits.
- Original source code is for non-commercial use only.
- This project is provided "AS IS". The author assumes no liability for any direct or indirect damages resulting from the use of this project.

## License

Original source code is licensed under a custom non-commercial license. See [LICENSE](./LICENSE) for details.
