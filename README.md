# 🌙 MoonPlay Music

A Spotify-style Android music player: stream free & legal music, and **download songs to listen offline**.

MoonPlay plays music from [Internet Archive](https://archive.org)'s open audio collections (public-domain and Creative-Commons recordings), so everything inside the app is free and legal — no account, no subscription.

## Features

- 🏠 **Home** with mood chips (Lo-fi, Jazz, Classical Piano, Electronic, Acoustic, Chill) and most-played tracks
- 🔍 **Search** any song title or artist in the Internet Archive audio library
- ▶️ **Player** with play/pause, next/previous, seek bar — Spotify-style dark UI with a moon-purple theme
- ⬇ **Download** any track as an MP3 into `Download/MoonPlay` on the phone, with a Downloads tab that lists them and opens them in the phone's music player
- 🇮🇩 Indonesian UI

## Install

Download **MoonPlayMusic.apk** from this repo (or from Releases) and open it on an Android phone (Android 7.0+). You may need to allow "install from unknown sources" for your browser/file manager — the APK is signed with a debug key for now.

## Build

Two ways:

**A. No Gradle (manual script)** — needs JDK 17 + Android SDK (platform 34, build-tools 34.0.0):

```sh
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./build-apk.sh        # produces MoonPlayMusic.apk in the repo root
```

**B. Gradle / Android Studio** — open the project and build the `app` module (AGP 8.5.2, Gradle 8.7, compileSdk 34). The app has **zero external dependencies**: the UI is a WebView (`app/src/main/assets/index.html`) and `MainActivity` is plain Android, providing the native download bridge (`MoonBridge`) via `DownloadManager`.

## How it works

| Piece | Where |
|---|---|
| Player UI (home, search, player bar, downloads) | `app/src/main/assets/index.html` |
| Native shell + download bridge | `app/src/main/java/com/moonplay/music/MainActivity.java` |
| Music catalog, streaming & files | Internet Archive `advancedsearch` + `metadata` APIs |

## License

MIT
