# Smartisan Music Revived

English | [中文](./README.md)

[![Release](https://img.shields.io/github/v/release/wowohut/SmartisanMusic-Revived?logo=github)](https://github.com/wowohut/SmartisanMusic-Revived/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-9.2.1-3DDC84?logo=android)](https://developer.android.com/build)
[![API](https://img.shields.io/badge/minSdk-31-3DDC84?logo=android)](https://developer.android.com/about/versions/12)
[![License](https://img.shields.io/badge/License-Custom%20NonCommercial-lightgrey)](./LICENSE)

> “This is for you.”

Smartisan Music has always been one of my favorite apps on Smartisan OS.

Smartisan OS is long gone, and the original music player stayed behind on old Android releases. But that vinyl turntable, tonearm drag, scratch effect, lighting, and little animations are still hard to replace. So I brought it back to modern Android, and added the things a modern music player needs: online streaming, sound effects, and lyrics.

> [!NOTE]
> This project does not provide a public cloud music catalog or media distribution service. Online music features require authorization with your own NetEase Cloud Music account. Membership or restricted content is still subject to NetEase Cloud Music's platform rules.

## Project Status

The current release is in a relatively complete and stable state. The main interface and playback screen have been refined through many rounds of reverse engineering, side-by-side comparison, and real-device tuning. The overall look, page hierarchy, animations, and core interactions are now where I want them.

### Local Playback

- Local music scanning, background playback, favorites, playlists, play counts
- Opening external audio files, basic sound effects, custom artist separators
- Song sorting and filtering, multi-select swipe, alphabetical sidebar

### Online Music (New in 3.0)

- NetEase Cloud Music account login, home recommendations, search
- Playlist, album, artist, radio / podcast browsing
- Liked songs, daily recommendations, account playlist browsing
- Playlist creation, adding/removing songs, deleting playlists
- Online playback URL refresh, online queue restoration, online cover art and lyrics
- In-memory / disk caching for pages, lyrics, and streaming

Online features depend on your own NetEase Cloud Music account authorization. This app does not provide a public music catalog; what you can search, play, and manage depends on your account status and NetEase Cloud Music's rules.

### Visuals & Interaction

- Bottom playback bar, search, dialogs
- Vinyl turntable, tonearm drag, scratch effect
- Lyrics / control area, expandable playback queue with drag-to-reorder

Going forward, work will mostly be stability maintenance, detail polish, performance improvements, and fixing bugs I haven't found yet.

## Screenshots

<p align="center">
  <img src="docs/images/device-shot-01.jpeg" width="200" />
  <img src="docs/images/device-shot-02.jpeg" width="200" />
  <img src="docs/images/device-shot-03.jpeg" width="200" />
  <img src="docs/images/device-shot-04.jpeg" width="200" />
</p>
<p align="center">
  <img src="docs/images/device-shot-05.jpeg" width="200" />
  <img src="docs/images/device-shot-06.jpeg" width="200" />
  <img src="docs/images/device-shot-07.jpeg" width="200" />
  <img src="docs/images/device-shot-08.jpeg" width="200" />
</p>

> Album covers, artist images, brand logos, and music content shown in the screenshots belong to their respective rights holders and are used only to demonstrate the app's interface.

## Download & Feedback

Download the latest APK from [GitHub Releases](https://github.com/wowohut/SmartisanMusic-Revived/releases).

For issues or suggestions, feel free to open an [Issue](https://github.com/wowohut/SmartisanMusic-Revived/issues).

## Project Background

This project started as a pure Jetpack Compose rebuild based on Smartisan OS 6.8.0 (Nut R1). That history is preserved in the `archive/6.8.0-compose` branch.

That version was functional — the playback pipeline and core features worked. But for a true 1:1 recreation of Smartisan Music, it always fell short. The app's details aren't just about placing elements in the right spots; they depend on the old View/XML system's measurements, shadows, selectors, list layering, typography, press states, and animation timing.

The Compose version couldn't capture those details well enough.

So I switched to the 8.1.0 legacy View approach. Wherever possible, this version preserves the original XML, drawables, dimens, selectors, animations, and view structure. Playback, scanning, favorites, queue, settings, and background services are rebuilt with modern Android technology.

Note that the original 8.1.0 release did not include online music, lyrics, sound effects, or custom artist separators. Those were added during this recreation as modern music player capabilities.

## Tech Stack

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

## Project Structure

```text
.
├── app/
│   └── src/main/
│       ├── java/com/smartisanos/music/
│       │   ├── SmartisanMusicApplication.kt  # Application entry point
│       │   ├── MainActivity.kt               # Legacy View main shell entry
│       │   ├── data/                         # Room, DataStore, Repository
│       │   │   └── online/                   # NetEase Cloud Music data layer, auth, cache, parsing
│       │   ├── playback/                     # Media3 service, local library, queue, covers, lyrics
│       │   │   └── PlaybackService.kt        # Background playback service
│       │   └── ui/
│       │       ├── shell/                    # 8.1.0 legacy shell, pages, transitions, dialogs
│       │       │   └── cloud/                # Online music pages, routing, lists, detail pages
│       │       ├── online/                   # NetEase account login and online feature UI
│       │       ├── playback/                 # Playback screen, turntable, scratch, overlays, controls
│       │       └── widgets/                  # Recreated legacy View / shim controls
│       └── res/                              # 8.1.0 migrated assets and modern Android resources
├── docs/
├── reverse/
└── gradle/
```

For more on the legacy shell design, see [docs/legacy-shell-structure.md](./docs/legacy-shell-structure.md).

## Acknowledgments

Thanks to [People-11](https://github.com/People-11/) for the [SmartisanOS_APP_Port](https://github.com/People-11/SmartisanOS_APP_Port/) project. Its `Music_8.1.0.apk` filled in many of the gaps between the original music APK and the Smartisan OS framework, allowing the 8.1.0 version to run on non-Smartisan devices, and providing a reliable visual and interaction baseline for this recreation.

People-11's work is about porting the original Smartisan OS app so the original APK can run as faithfully as possible on other systems. This repository is a recreation: the UI stays close to 8.1.0, but playback, scanning, queue, favorites, settings, data persistence, and background services are rebuilt with modern Android technology, making ongoing maintenance and extensions possible without breaking the original feel.

## Disclaimer

This project is not affiliated with ByteDance. It is an unofficial recreation driven by personal interest.

- Smartisan OS and related visual designs are the intellectual property of their respective rights holders.
- The 8.1.0 APK resource files are included or referenced only for learning, research, and preservation purposes; copyright belongs to the original rights holders.
- Online music features require authorization with your own NetEase Cloud Music account. This project does not provide a public music catalog, media distribution service, or third-party platform membership benefits.
- Original source code is for non-commercial use only.
- This project is provided "AS IS". The author assumes no liability for any direct or indirect damages resulting from the use of this project, including but not limited to account restrictions, data loss, copyright disputes, or service interruptions.

## License

Original source code is licensed under a custom non-commercial license. See [LICENSE](./LICENSE) for details.

---

Recreating it isn't just nostalgia. It's the hope that when you open it, you feel the same joy I do.
