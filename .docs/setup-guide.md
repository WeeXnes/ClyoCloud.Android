# Setup & Build Guide - ClyoCloudAndroid

This guide assists you in configuring your development environment to compile and debug the ClyoCloudAndroid client app.

---

## Prerequisites

Before building, verify the following are configured on your developer workstation:
*   **Android Studio**: Version Koala (2024.1.1) or newer.
*   **Java Development Kit (JDK)**: JDK 17 or higher (configured inside Android Studio Gradle settings).
*   **Android SDK**: API Level 34 (Android 14) installed via SDK Manager.

---

## Getting Started

### 1. Project Import
1.  Launch **Android Studio**.
2.  Choose **Open** and select the `/home/weexnes/ClyoWallpaper/ClyoCloudAndroid` directory.
3.  Wait for Android Studio to index the files and download the required Gradle dependencies.

### 2. Run Debug Build
To run the project on a physical device or emulator:
1.  Connect your Android device with **USB Debugging** enabled, or start an emulator.
2.  Select `app` from the run configuration selector in the top toolbar.
3.  Click the green **Run** icon (or press `Shift + F10`).

### 3. Gradle Command Reference
Run these from the project root directory:

*   **Build Debug APK**:
    ```bash
    ./gradlew assembleDebug
    ```
*   **Run Linter & Tests**:
    ```bash
    ./gradlew test
    ```
*   **Clean Build Cache**:
    ```bash
    ./gradlew clean
    ```

---

## Connection Configuration

When starting the application for the first time, you will be prompted for:
1.  **Server URL**: The address of your running self-hosted ClyoCloud server (e.g. `https://mycloud.mydomain.com` or `http://10.0.2.2:3000` inside standard emulator setups).
2.  **Credentials**: Username and Password.
3.  **2FA Code (Optional)**: If 2FA is active, input the TOTP code to complete login.
