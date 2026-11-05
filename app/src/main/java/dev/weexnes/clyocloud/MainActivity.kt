package dev.weexnes.clyocloud

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.ui.AppRoot
import dev.weexnes.clyocloud.ui.theme.ClyoCloudTheme
import dev.weexnes.clyocloud.ui.theme.ThemePrefs
import dev.weexnes.clyocloud.wallpaper.PhotoWidgetCycle
import dev.weexnes.clyocloud.wallpaper.WallpaperCycle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePrefs.load(this)
        Session.load(this)

        // Bring back timers that were lost, e.g. after a force-stop
        WallpaperCycle.ensureScheduled(this)
        PhotoWidgetCycle.ensureScheduled(this)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            val dark by ThemePrefs.isDark
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            ClyoCloudTheme(dark = dark) {
                AppRoot()
            }
        }
    }
}
