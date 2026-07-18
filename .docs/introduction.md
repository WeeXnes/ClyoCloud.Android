# ClyoCloudAndroid Introduction

Welcome to the developer documentation for the **ClyoCloudAndroid** client app.

## What is ClyoCloudAndroid?

ClyoCloudAndroid is a native companion application for **ClyoCloud**, built specifically for Android devices using Kotlin and Jetpack Compose. It allows users to interact with their self-hosted storage instance through an optimized mobile-first experience.

### System Flow
*   **Android App**: Interacts with the user via a glassmorphic dashboard built using the Jetpack Compose UI framework.
*   **Network Layer**: Performs standard, lightweight REST API calls over HTTPS to the Nuxt-based Nitro server.
*   **Authentication**: Saves a secure cookie-like session token locally within `SharedPreferences`.
*   **Streaming**: Retrieves raw image/video files directly and feeds them to Compose media canvases or standard Android `VideoView` controllers.

---

## Key Features

*   **Secure Authentication & 2FA State Machine**: Connects securely to self-hosted instances. Supports standard credentials and temporary token TOTP 2FA verification.
*   **Glassmorphic Design System**: Uses a custom, dark theme featuring glowing radial gradient backdrops and unified custom vectors to ensure maximum aesthetic appeal.
*   **File Exploration**: Browse files and directories in standard Grid or List views.
*   **Media Playback**: Supports video streaming utilizing range-aware API endpoints and automatic slideshow scaling for folders.
*   **Metadata Synchronization**: Allows modifying descriptions, assigning tags, tagging favorites, and toggling public access status on the server.
