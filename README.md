# ClyoCloudAndroid

<p align="center">
  <img src="../clyo.png" alt="ClyoCloudAndroid Logo" width="120" height="120" style="border-radius: 20%;" />
</p>

<p align="center">
  <strong>Android companion application for ClyoCloud, built with Jetpack Compose.</strong>
</p>

---

ClyoCloudAndroid brings your self-hosted ClyoCloud storage and media library directly to your mobile device. The app offers native performance, media playback, and metadata management using a glassmorphic interface.

---

## Features

*   **Secure Connection and 2FA**: Connects to your self-hosted ClyoCloud instance. Supports the two-step TOTP 2FA verification flow.
*   **File Explorer**: Browse folders, view file metadata (size, last modified, tags, descriptions), and perform operations like file uploads and downloads.
*   **Interface**: Dark theme featuring glass cards and custom vector icons.
*   **Media Center**: Streaming video playback using native VideoView and slideshow previews for images.
*   **Metadata Editor**: Edit tags, description details, toggle public share status, and add items to your favorites.
*   **Local Caching**: Persists server connection states and credentials securely using SharedPreferences.

---

## Tech Stack and Architecture

ClyoCloudAndroid is built using standard Android development practices:

```mermaid
graph TD
    User([User]) -->|Interacts| UI[Jetpack Compose UI Layer]
    UI -->|Router / Screens| Screens[Login / Dashboard / Media]
    Screens -->|Read/Write Prefs| Prefs[PrefsManager / SharedPreferences]
    Screens -->|API Actions| Net[Network Layer / HttpURLConnection]
    Net -->|HTTPS Requests| CloudServer[ClyoCloud Backend Instance]
```

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **User Interface** | **Jetpack Compose**, **Material 3** | Declarative, responsive UI featuring custom animations and glassmorphic designs. |
| **Programming Language** | **Kotlin** | Asynchronous execution with Coroutines. |
| **Networking** | **HttpURLConnection** & **JSON Processing** | Lightweight, dependency-free HTTP communication with ClyoCloud REST API endpoints. |
| **Session Persistence** | **SharedPreferences** | Local storage of instance URLs, usernames, and authentication tokens. |

---

## Getting Started

### Prerequisites

*   **Android Studio** (Koala or newer recommended)
*   **Android SDK** (API Level 24+ supported, API 34 target)
*   **ClyoCloud Server**: A running instance of ClyoCloud accessible over the network.

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