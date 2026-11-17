# ClyoCloudAndroid

<p align="center">
  <img src="../clyo.png" alt="ClyoCloudAndroid Logo" width="120" height="120" style="border-radius: 20%;" />
</p>

<p align="center">
  <strong>Android companion application for ClyoCloud, built with Jetpack Compose.</strong>
</p>

---

ClyoCloudAndroid brings your self-hosted ClyoCloud storage, media library and wallpapers to your phone. Since 1.3.0 it also includes everything from the former **ClyoWallpaper** app, so one app does both. The interface follows the same black, monochrome design as the web UI.

---

## Features

*   **Sign-in**: Server address, username and password, two-factor codes, or scanning the login QR code from the website.
*   **Files**: Browse with breadcrumbs, search by name or tag, sort, list or grid, upload in the background, new folders, and multi-select to download as ZIP, star, tag, rename, move or delete.
*   **Details**: Notes, tags (from your tag library or new ones), public links, wallpaper and photo-widget flags, and folder thumbnails.
*   **Media**: The video and slideshow library with search, and a full-screen Shorts feed.
*   **Viewer**: Swipe through images and videos, pinch to zoom.
*   **Wallpapers** (formerly ClyoWallpaper):
    *   Library of everything marked as wallpaper, with a playlist.
    *   Framing editor (pan and zoom per image, for home, lock or both).
    *   Automatic changes every 15 minutes to 1 day, shuffled or in order, from all wallpapers or the playlist, optionally only on Wi-Fi. Runs on WorkManager, so it keeps going while the phone sleeps and after restarts, without a permanent notification.
    *   **Create wallpaper**: extend and upscale any photo to 2880×1800 with the server's ComfyUI pipeline (scene preset, upscale model, custom prompts, live progress).
    *   Home-screen widgets: current wallpaper with a countdown (tap to change), and a rotating photo widget.
*   **Camera backup**: New photos and videos upload automatically shortly after they're taken (and hourly as a safety net). Choose albums, include videos or not, Wi-Fi only, only while charging, a target folder with Year/Month subfolders, and whether to start with new photos only or everything. Nothing is uploaded twice, and files with the same name never overwrite each other.
*   **Save to ClyoCloud**: Appears in every app's share menu. Pick a folder (recent ones first, or create one) and the files upload in the background; shared text is saved as a note.
*   **Settings**: Dark or Light theme, camera backup, recycle bin, image cache, sign out.

---

## Tech Stack and Architecture

*   Kotlin, Jetpack Compose and Material 3 (restyled with the ClyoCloud design tokens), Geist and Geist Mono fonts bundled.
*   `HttpURLConnection` API client (`data/Api.kt`), downsampling image loader with memory and disk cache (`data/ImageLoader.kt`).
*   WorkManager jobs for the wallpaper cycle and photo widget (`wallpaper/`), a foreground service for uploads, the system download manager for downloads.

```
app/src/main/java/dev/weexnes/clyocloud/
├── MainActivity.kt          entry point
├── data/                    API client, models, session, image loading
├── ui/                      app state, navigation, theme, components, screens
└── wallpaper/               wallpaper cycle, applying/framing, widgets
```

---

## Getting Started

### Prerequisites

*   **Android Studio**, or the Android SDK command-line tools with platform 35
*   **JDK 21** for Gradle (17 to 25 work; newer JDKs are not supported by Gradle 9.1)
*   **A phone with Android 15 or newer** (minSdk 35)
*   **ClyoCloud Server** 2.21.0 or newer (for wallpaper generation and wallpaper sync), reachable from the phone

### Pre-built APKs

For automatically built APKs from the OneDev workflow, download the latest artifacts directly:
*   [Latest APK Artifacts](https://weexnes.dev/project/ClyoCloudAndroid#artifacts)

### Build and Install from Source

1.  **Clone the Repository** (if not already done):
    ```bash
    git clone <repository-url>
    cd ClyoCloudAndroid
    ```

2.  **Open in Android Studio**:
    *   Select **File > Open** and choose the `ClyoCloudAndroid` directory.
    *   Allow Gradle sync to download all dependencies.

3.  **Build via CLI** (Optional):
    ```bash
    # Build debug APK
    ./gradlew assembleDebug
    ```

4.  **Run on Device/Emulator**:
    *   Select your target device in Android Studio and click **Run**.
    *   Enter your self-hosted ClyoCloud URL.
    *   Log in using your ClyoCloud credentials.

## Documentation

Detailed architectural overviews, security specifications, and setups can be found in the project documentation:
*   [Introduction & Features Overview](https://weexnes.dev/project/ClyoCloudAndroid?path=introduction.md)
*   [Architecture & Design Details](https://weexnes.dev/project/ClyoCloudAndroid?path=architecture.md)
*   [Setup & Build Guide](https://weexnes.dev/project/ClyoCloudAndroid?path=setup-guide.md)