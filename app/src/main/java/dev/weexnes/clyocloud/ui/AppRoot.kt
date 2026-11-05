package dev.weexnes.clyocloud.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.weexnes.clyocloud.data.AuthApi
import dev.weexnes.clyocloud.data.ClyoApi
import dev.weexnes.clyocloud.data.Session
import dev.weexnes.clyocloud.ui.components.Hairline
import dev.weexnes.clyocloud.ui.components.RadiusInput
import dev.weexnes.clyocloud.ui.screens.FavoritesScreen
import dev.weexnes.clyocloud.ui.screens.FilesScreen
import dev.weexnes.clyocloud.ui.screens.GenerateWallpaperSheet
import dev.weexnes.clyocloud.ui.screens.LoginScreen
import dev.weexnes.clyocloud.ui.screens.MediaScreen
import dev.weexnes.clyocloud.ui.screens.RecycleBinScreen
import dev.weexnes.clyocloud.ui.screens.SettingsScreen
import dev.weexnes.clyocloud.ui.screens.ViewerScreen
import dev.weexnes.clyocloud.ui.screens.WallpaperEditorScreen
import dev.weexnes.clyocloud.ui.screens.WallpapersScreen
import dev.weexnes.clyocloud.ui.theme.Clyo
import dev.weexnes.clyocloud.ui.theme.ClyoType

@Composable
fun AppRoot() {
    val connection by Session.current
    val c = Clyo.colors
    Box(Modifier.fillMaxSize().background(c.bg)) {
        val conn = connection
        if (conn == null) {
            LoginScreen()
        } else {
            // A new AppState per account, so nothing leaks between sign-ins
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val state = remember(conn) { AppState(context, scope, ClyoApi(conn)) }

            // A token the server no longer accepts (revoked device, expired) signs out
            LaunchedEffect(conn) {
                if (AuthApi.verifyToken(conn.serverUrl, conn.token) == false) {
                    Session.clear(context)
                }
            }
            CompositionLocalProvider(LocalAppState provides state) {
                MainScaffold(state)
            }
        }
    }
}

@Composable
private fun MainScaffold(state: AppState) {
    val c = Clyo.colors
    val tab by state.tab
    val overlays by state.overlays
    val generateFor by state.generateFor

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.Files -> FilesScreen()
                    Tab.Media -> MediaScreen()
                    Tab.Wallpapers -> WallpapersScreen()
                    Tab.Favorites -> FavoritesScreen()
                    Tab.Settings -> SettingsScreen()
                }
            }
            BottomNav(tab) { state.tab.value = it }
        }

        overlays.forEachIndexed { index, overlay ->
            Box(Modifier.fillMaxSize().background(c.bg)) {
                when (overlay) {
                    is Overlay.Viewer -> ViewerScreen(overlay.files, overlay.startIndex, onClose = { state.pop() })
                    is Overlay.WallpaperEditor -> WallpaperEditorScreen(overlay.path, onClose = { state.pop() })
                    Overlay.RecycleBin -> RecycleBinScreen(onClose = { state.pop() })
                }
            }
            if (index == overlays.lastIndex) BackHandler { state.pop() }
        }

        generateFor?.let { file ->
            GenerateWallpaperSheet(file, onDismiss = { state.generateFor.value = null })
        }

        SnackbarHost(
            state.snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp, start = 16.dp, end = 16.dp),
        ) { data ->
            Snackbar(
                shape = RadiusInput,
                containerColor = c.invert,
                contentColor = c.onInvert,
            ) { Text(data.visuals.message, style = ClyoType.small) }
        }
    }
}

/** Plain icons and labels; the active tab is text-coloured, the rest grey. No filled pills. */
@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val c = Clyo.colors
    Column(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding()) {
        Hairline()
        Row(Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Tab.entries.forEach { t ->
                val active = t == current
                val color = if (active) c.text else c.text3
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        when (t) {
                            Tab.Files -> Icons.Outlined.Folder
                            Tab.Media -> Icons.Outlined.PlayCircle
                            Tab.Wallpapers -> Icons.Outlined.Wallpaper
                            Tab.Favorites -> Icons.Outlined.StarOutline
                            Tab.Settings -> Icons.Outlined.Settings
                        },
                        contentDescription = t.label,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(t.label, style = ClyoType.micro.copy(letterSpacing = ClyoType.small.letterSpacing), color = color, maxLines = 1)
                }
            }
        }
    }
}
