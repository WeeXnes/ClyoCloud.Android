package dev.weexnes.clyocloudandroid

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weexnes.clyocloudandroid.ui.theme.ClyoCloudAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequest
import androidx.work.ExistingWorkPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Data Models
data class CloudConnection(
    val serverUrl: String,
    val username: String,
    val token: String
)

data class WallpaperItem(
    val name: String,
    val size: Long,
    val type: String,
    val lastModified: Long
)

enum class AppScreen {
    Login,
    Dashboard,
    Editor
}

enum class WallpaperDestination {
    Home,
    Lock,
    Both
}

// SharedPreferences Manager
object PrefsManager {
    private const val PREFS_NAME = "clyo_wallpaper_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_TOKEN = "token"

    fun getCloudConnection(context: Context): CloudConnection? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val user = prefs.getString(KEY_USERNAME, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return CloudConnection(url, user, token)
    }

    fun saveCloudConnection(context: Context, connection: CloudConnection) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SERVER_URL, connection.serverUrl)
            putString(KEY_USERNAME, connection.username)
            putString(KEY_TOKEN, connection.token)
            apply()
        }
    }

    fun clearCloudConnection(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getPlaylist(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("playlist", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun savePlaylist(context: Context, playlist: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        playlist.forEach { arr.put(it) }
        prefs.edit().putString("playlist", arr.toString()).apply()
    }

    fun getCycleEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("cycle_enabled", false)
    }

    fun saveCycleEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("cycle_enabled", enabled).apply()
    }

    fun getCycleInterval(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("cycle_interval", 15)
    }

    fun saveCycleInterval(context: Context, interval: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("cycle_interval", interval).apply()
    }

    fun getCycleShuffle(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("cycle_shuffle", false)
    }

    fun saveCycleShuffle(context: Context, shuffle: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("cycle_shuffle", shuffle).apply()
    }

    fun getCycleSource(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("cycle_source", "Playlist") ?: "Playlist"
    }

    fun saveCycleSource(context: Context, source: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("cycle_source", source).apply()
    }

    fun getCycleDestination(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("cycle_destination", "Both") ?: "Both"
    }

    fun saveCycleDestination(context: Context, destination: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("cycle_destination", destination).apply()
    }

    fun getWallpaperArrangement(context: Context, path: String): WallpaperArrangement? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scale = prefs.getFloat("arr_scale_$path", -1f)
        if (scale == -1f) return null
        val panX = prefs.getFloat("arr_panx_$path", 0f)
        val panY = prefs.getFloat("arr_pany_$path", 0f)
        return WallpaperArrangement(scale, panX, panY)
    }

    fun saveWallpaperArrangement(context: Context, path: String, arrangement: WallpaperArrangement) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("arr_scale_$path", arrangement.scale)
            .putFloat("arr_panx_$path", arrangement.panX)
            .putFloat("arr_pany_$path", arrangement.panY)
            .apply()
    }

    fun getCurrentWallpaper(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("current_wallpaper", "") ?: ""
    }

    fun saveCurrentWallpaper(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("current_wallpaper", path).apply()
    }

    fun getNextWallpaper(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("next_wallpaper", "") ?: ""
    }

    fun saveNextWallpaper(context: Context, path: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("next_wallpaper", path).apply()
    }

    fun getCycleNextExecution(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("cycle_next_execution", 0L)
    }

    fun saveCycleNextExecution(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("cycle_next_execution", timestamp).apply()
    }
}

data class WallpaperArrangement(
    val scale: Float,
    val panX: Float,
    val panY: Float
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClyoCloudAndroidTheme {
                MainAppRouter()
            }
        }
    }
}

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080612), // Dark obsidian
                        Color(0xFF140D26), // Dark amethyst
                        Color(0xFF05030A)  // Core midnight black
                    )
                )
            )
    ) {
        // Glowing backdrops
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = -120f
                    translationY = -120f
                    alpha = 0.18f
                }
                .background(Brush.radialGradient(listOf(Color(0xFF8A2BE2), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    translationX = 180f
                    translationY = 180f
                    alpha = 0.15f
                }
                .background(Brush.radialGradient(listOf(Color(0xFF00E5FF), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = 0.08f
                }
                .background(Brush.radialGradient(listOf(Color(0xFFFF007F), Color.Transparent)))
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderBrush: Brush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f))
    ),
    containerColor: Color = Color.White.copy(alpha = 0.05f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, borderBrush, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Column(content = content) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFFA0A0C0)) },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFFF3F3F7),
            unfocusedTextColor = Color(0xFFF3F3F7),
            focusedBorderColor = Color(0xFFBB86FC),
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedLabelColor = Color(0xFFBB86FC),
            unfocusedLabelColor = Color(0xFFA0A0C0),
            cursorColor = Color(0xFFBB86FC)
        )
    )
}

@Composable
fun GlassPillSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) Color(0xFFBB86FC).copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFA0A0C0),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MainAppRouter() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Login) }
    var connection by remember { mutableStateOf<CloudConnection?>(null) }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }

    LaunchedEffect(Unit) {
        val saved = PrefsManager.getCloudConnection(context)
        if (saved != null) {
            connection = saved
            currentScreen = AppScreen.Dashboard
        } else {
            currentScreen = AppScreen.Login
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            AppScreen.Login -> {
                LoginScreen(
                    onLoginSuccess = { conn ->
                        connection = conn
                        PrefsManager.saveCloudConnection(context, conn)
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
            AppScreen.Dashboard -> {
                DashboardScreen(
                    connection = connection,
                    onWallpaperSelect = { wallpaper ->
                        selectedWallpaper = wallpaper
                        currentScreen = AppScreen.Editor
                    },
                    onDisconnect = {
                        PrefsManager.clearCloudConnection(context)
                        connection = null
                        currentScreen = AppScreen.Login
                    }
                )
            }
            AppScreen.Editor -> {
                WallpaperEditorScreen(
                    connection = connection,
                    wallpaper = selectedWallpaper,
                    onBack = {
                        selectedWallpaper = null
                        currentScreen = AppScreen.Dashboard
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (CloudConnection) -> Unit) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var is2faMode by remember { mutableStateOf(false) }
    var tempToken by remember { mutableStateOf("") }
    var code2fa by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Logo / Text
                    Text(
                        text = "CLYO",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFBB86FC),
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "WALLPAPER ENGINE",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03DAC6),
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (!is2faMode) {
                        Text(
                            text = "Connect to Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F3F7)
                        )

                        Text(
                            text = "Enter your server credentials to sync live desktop wallpapers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0A0C0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it; errorMessage = null },
                            label = "Server URL (e.g. http://10.0.2.2:3000)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = null },
                            label = "Username",
                            modifier = Modifier.fillMaxWidth()
                        )

                        GlassTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = "Password",
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all fields"
                                    return@Button
                                }
                                var formattedUrl = serverUrl.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "http://$formattedUrl"
                                }
                                formattedUrl = formattedUrl.removeSuffix("/")

                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        when (val result = loginToCloud(formattedUrl, username.trim(), password)) {
                                            is LoginResponse.Success -> {
                                                onLoginSuccess(CloudConnection(formattedUrl, username.trim(), result.token))
                                            }
                                            is LoginResponse.Requires2Fa -> {
                                                tempToken = result.tempToken
                                                is2faMode = true
                                            }
                                            is LoginResponse.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error connecting: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBB86FC),
                                contentColor = Color(0xFF0F0C1B),
                                disabledContainerColor = Color(0xFFBB86FC).copy(alpha = 0.3f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFF0F0C1B), modifier = Modifier.size(24.dp))
                            } else {
                                Text("Connect Server", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "Two-Factor Auth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F3F7)
                        )

                        Text(
                            text = "Enter the 6-digit verification code from your authenticator app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0A0C0),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassTextField(
                            value = code2fa,
                            onValueChange = { code2fa = it; errorMessage = null },
                            label = "6-Digit Code",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFF5252),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (code2fa.isBlank() || code2fa.length < 6) {
                                    errorMessage = "Please enter a valid 6-digit code"
                                    return@Button
                                }
                                var formattedUrl = serverUrl.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "http://$formattedUrl"
                                }
                                formattedUrl = formattedUrl.removeSuffix("/")

                                coroutineScope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    try {
                                        val token = verify2fa(formattedUrl, tempToken, code2fa.trim())
                                        if (token != null) {
                                            onLoginSuccess(CloudConnection(formattedUrl, username.trim(), token))
                                        } else {
                                            errorMessage = "Invalid 2FA code"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Verification failed: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFBB86FC),
                                contentColor = Color(0xFF0F0C1B),
                                disabledContainerColor = Color(0xFFBB86FC).copy(alpha = 0.3f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color(0xFF0F0C1B), modifier = Modifier.size(24.dp))
                            } else {
                                Text("Verify Code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                is2faMode = false
                                code2fa = ""
                                errorMessage = null
                            },
                            enabled = !isLoading
                        ) {
                            Text("Back to Login", color = Color(0xFF03DAC6))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    connection: CloudConnection?,
    onWallpaperSelect: (WallpaperItem) -> Unit,
    onDisconnect: () -> Unit
) {
    if (connection == null) return
    val context = LocalContext.current
    var wallpapers by remember { mutableStateOf<List<WallpaperItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    // Request POST_NOTIFICATIONS permission for Android 13+ (required for foreground service)
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> /* Permission result handled, service will show notification if granted */ }


    // Playlist and Auto-Cycle States
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var playlist by remember { mutableStateOf(PrefsManager.getPlaylist(context)) }
    var cycleEnabled by remember { mutableStateOf(PrefsManager.getCycleEnabled(context)) }
    var cycleInterval by remember { mutableStateOf(PrefsManager.getCycleInterval(context)) }
    var cycleShuffle by remember { mutableStateOf(PrefsManager.getCycleShuffle(context)) }
    var cycleSource by remember { mutableStateOf(PrefsManager.getCycleSource(context)) }
    var cycleDestination by remember { mutableStateOf(PrefsManager.getCycleDestination(context)) }

    // Worker and Timer States
    var currentWallpaper by remember { mutableStateOf(PrefsManager.getCurrentWallpaper(context)) }
    var nextWallpaper by remember { mutableStateOf(PrefsManager.getNextWallpaper(context)) }
    var nextExecutionTime by remember { mutableStateOf(PrefsManager.getCycleNextExecution(context)) }
    var serviceRunning by remember { mutableStateOf(cycleEnabled) }

    var remainingSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(cycleEnabled) {
        while (true) {
            nextExecutionTime = PrefsManager.getCycleNextExecution(context)
            currentWallpaper = PrefsManager.getCurrentWallpaper(context)
            nextWallpaper = PrefsManager.getNextWallpaper(context)
            serviceRunning = cycleEnabled && nextExecutionTime > 0
            if (serviceRunning && nextExecutionTime > 0) {
                val now = System.currentTimeMillis()
                remainingSeconds = ((nextExecutionTime - now) / 1000).coerceAtLeast(0L)
            } else {
                remainingSeconds = 0L
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    fun loadWallpapers() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                wallpapers = fetchWallpapersList(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load wallpapers"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(connection) {
        loadWallpapers()
    }

    GlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("ClyoCloudAndroid", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                            Text(
                                text = connection.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF03DAC6),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadWallpapers() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = onDisconnect) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Disconnect", tint = Color(0xFFFF5252))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.3f),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                GlassPillSelector(
                    options = listOf("Wallpapers", "Playlist & Cycle"),
                    selectedIndex = tabIndex,
                    onSelect = { tabIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                if (tabIndex == 0) {
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { loadWallpapers() },
                        state = refreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (errorMessage != null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(48.dp))
                                        Text(
                                            text = "Connection Error",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5252)
                                        )
                                        Text(
                                            text = errorMessage!!,
                                            color = Color(0xFFA0A0C0),
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { loadWallpapers() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC), contentColor = Color(0xFF0F0C1B)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Retry", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else if (!isLoading && wallpapers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF03DAC6), modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No Wallpapers Synced",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "To see images here, log in to ClyoCloud via your web browser, right-click on any image, and select 'Select as Wallpaper'.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA0A0C0),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(wallpapers) { wallpaper ->
                                    val inPlaylist = playlist.contains(wallpaper.name)
                                    WallpaperGridItem(
                                        wallpaper = wallpaper,
                                        connection = connection,
                                        isInPlaylist = inPlaylist,
                                        onPlaylistToggle = {
                                            val updated = if (inPlaylist) {
                                                playlist.filter { it != wallpaper.name }
                                            } else {
                                                playlist + wallpaper.name
                                            }
                                            playlist = updated
                                            PrefsManager.savePlaylist(context, updated)
                                        },
                                        onClick = { onWallpaperSelect(wallpaper) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Playlist & Auto-Cycle Tab Content: scrollable page
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Auto-Cycle Wallpapers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                            Text("Change wallpaper on a timer", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0C0))
                                        }
                                        Switch(
                                            checked = cycleEnabled,
                                            onCheckedChange = { checked ->
                                                cycleEnabled = checked
                                                PrefsManager.saveCycleEnabled(context, checked)
                                                if (checked) {
                                                    // Request notification permission for Android 13+
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                }
                                                scheduleWallpaperCycle(context, checked, cycleInterval)
                                                if (checked) {
                                                    // Request battery optimization exemption
                                                    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                                                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                                        try {
                                                            val intent = android.content.Intent(
                                                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                                android.net.Uri.parse("package:${context.packageName}")
                                                            )
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Log.w("ClyoCloudAndroid", "Could not request battery optimization exemption: ${e.message}")
                                                        }
                                                    }
                                                }
                                            },
                                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF0F0C1B),
                                                checkedTrackColor = Color(0xFF03DAC6),
                                                uncheckedThumbColor = Color(0xFFA0A0C0),
                                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    }

                                    // Status Panel (Matrix/Neon Terminal vibe)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("SYSTEM STATUS:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFA0A0C0))
                                                val statusText = if (serviceRunning) "ACTIVE" else "INACTIVE"
                                                val statusColor = if (serviceRunning) Color(0xFF39FF14) else Color(0xFFFF5252)
                                                Text(
                                                    text = statusText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (cycleEnabled) statusColor else Color(0xFFFF5252)
                                                )
                                            }

                                            if (cycleEnabled && remainingSeconds > 0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("NEXT CYCLE IN:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFA0A0C0))
                                                    val mins = remainingSeconds / 60
                                                    val secs = remainingSeconds % 60
                                                    Text(
                                                        text = String.format("%02d:%02d", mins, secs),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF00E5FF)
                                                    )
                                                }
                                            }

                                            if (currentWallpaper.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("CURRENT WALLPAPER:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0C0))
                                                    Text(
                                                        text = currentWallpaper.split('/').last(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        modifier = Modifier.width(180.dp),
                                                        textAlign = TextAlign.End,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            if (cycleEnabled && nextWallpaper.isNotEmpty() && !cycleShuffle) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("UPCOMING:", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A0C0))
                                                    Text(
                                                        text = nextWallpaper.split('/').last(),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        modifier = Modifier.width(180.dp),
                                                        textAlign = TextAlign.End,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Source Picker Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Cycle Source", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        val sources = listOf("Playlist", "All")
                                        Row(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(10.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                                                .padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            sources.forEach { src ->
                                                val isSelected = cycleSource == src
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isSelected) Color(0xFFBB86FC).copy(alpha = 0.2f) else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            cycleSource = src
                                                            PrefsManager.saveCycleSource(context, src)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (src == "All") "All" else "Playlist",
                                                        color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFA0A0C0),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Interval Selector
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Interval", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                            Text("$cycleInterval minutes", fontWeight = FontWeight.Bold, color = Color(0xFF03DAC6))
                                        }
                                        Slider(
                                            value = cycleInterval.toFloat(),
                                            onValueChange = { cycleInterval = it.toInt() },
                                            onValueChangeFinished = {
                                                PrefsManager.saveCycleInterval(context, cycleInterval)
                                                if (cycleEnabled) {
                                                    scheduleWallpaperCycle(context, true, cycleInterval)
                                                }
                                            },
                                            valueRange = 1f..120f,
                                            steps = 119,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF03DAC6),
                                                activeTrackColor = Color(0xFF03DAC6),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Shuffle Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Shuffle Mode", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Switch(
                                            checked = cycleShuffle,
                                            onCheckedChange = { checked ->
                                                cycleShuffle = checked
                                                PrefsManager.saveCycleShuffle(context, checked)
                                            },
                                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF0F0C1B),
                                                checkedTrackColor = Color(0xFFBB86FC),
                                                uncheckedThumbColor = Color(0xFFA0A0C0),
                                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                            )
                                        )
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Apply To (Destination) Picker
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Apply To", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        val dests = listOf("Home", "Lock", "Both")
                                        Row(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(10.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                                                .padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            dests.forEach { dest ->
                                                val isSelected = cycleDestination == dest
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (isSelected) Color(0xFFBB86FC).copy(alpha = 0.2f) else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            cycleDestination = dest
                                                            PrefsManager.saveCycleDestination(context, dest)
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = dest,
                                                        color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFA0A0C0),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                    // Reset Timer Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                forceResetWallpaperCycle(context, cycleInterval)
                                                Toast.makeText(context, "Cycle timer reset. Triggering update immediately!", Toast.LENGTH_SHORT).show()
                                                nextExecutionTime = PrefsManager.getCycleNextExecution(context)
                                            },
                                            enabled = cycleEnabled,
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Color(0xFFBB86FC))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reset & Cycle Now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Playlist/All Wallpapers List header & items
                        if (cycleSource == "Playlist") {
                            item {
                                Text(
                                    text = "Current Playlist",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            if (playlist.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Your playlist is empty. Add wallpapers by clicking the heart button in the Wallpapers tab.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA0A0C0)
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(playlist) { index, itemPath ->
                                    val isCurrent = itemPath == currentWallpaper
                                    val isNext = itemPath == nextWallpaper && !cycleShuffle
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PlaylistItemRow(
                                            path = itemPath,
                                            connection = connection,
                                            isCurrent = isCurrent,
                                            isNext = isNext,
                                            onMoveUp = if (index > 0) {
                                                {
                                                    val mutable = playlist.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    playlist = mutable
                                                    PrefsManager.savePlaylist(context, mutable)
                                                }
                                            } else null,
                                            onMoveDown = if (index < playlist.size - 1) {
                                                {
                                                    val mutable = playlist.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    playlist = mutable
                                                    PrefsManager.savePlaylist(context, mutable)
                                                }
                                            } else null,
                                            onRemove = {
                                                val updated = playlist.filter { it != itemPath }
                                                playlist = updated
                                                PrefsManager.savePlaylist(context, updated)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "All Wallpapers",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            if (wallpapers.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No wallpapers available on the server.",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFA0A0C0)
                                        )
                                    }
                                }
                            } else {
                                items(wallpapers) { wallpaper ->
                                    val isCurrent = wallpaper.name == currentWallpaper
                                    val isNext = wallpaper.name == nextWallpaper && !cycleShuffle
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PlaylistItemRow(
                                            path = wallpaper.name,
                                            connection = connection,
                                            isCurrent = isCurrent,
                                            isNext = isNext,
                                            onMoveUp = null,
                                            onMoveDown = null,
                                            onRemove = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperGridItem(
    wallpaper: WallpaperItem,
    connection: CloudConnection,
    isInPlaylist: Boolean,
    onPlaylistToggle: () -> Unit,
    onClick: () -> Unit
) {
    val cleanName = wallpaper.name.split('/').last()
    var resolution by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(wallpaper.name) {
        resolution = fetchImageResolution(connection.serverUrl, connection.token, wallpaper.name)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncServerImage(
                    serverUrl = connection.serverUrl,
                    token = connection.token,
                    relativePath = wallpaper.name,
                    isThumbnail = true,
                    contentDescription = cleanName,
                    modifier = Modifier.fillMaxSize()
                )

                // Playlist toggle button
                IconButton(
                    onClick = { onPlaylistToggle() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isInPlaylist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Toggle Playlist",
                        tint = if (isInPlaylist) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = cleanName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color.White
                )
                Text(
                    text = formatFileSize(wallpaper.size) + (resolution?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0C0)
                )
            }
        }
    }
}

@Composable
fun PlaylistItemRow(
    path: String,
    connection: CloudConnection,
    isCurrent: Boolean,
    isNext: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: (() -> Unit)?
) {
    val cleanName = path.split('/').last()

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White.copy(alpha = 0.03f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncServerImage(
                    serverUrl = connection.serverUrl,
                    token = connection.token,
                    relativePath = path,
                    isThumbnail = true,
                    contentDescription = cleanName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                        color = Color.White
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF39FF14).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Current",
                                color = Color(0xFF39FF14),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isNext) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00E5FF).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Next",
                                color = Color(0xFF00E5FF),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = path.substringBeforeLast("/", "Root"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0A0C0),
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                    }
                }
                if (onRemove != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}

@Composable
fun AsyncServerImage(
    serverUrl: String,
    token: String,
    relativePath: String,
    isThumbnail: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(relativePath) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(relativePath) { mutableStateOf(true) }

    LaunchedEffect(relativePath, token) {
        isLoading = true
        try {
            val loaded = fetchBitmapFromUrl(serverUrl, token, relativePath, isThumbnail)
            bitmap = loaded?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFFBB86FC))
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF5252).copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperEditorScreen(
    connection: CloudConnection?,
    wallpaper: WallpaperItem?,
    onBack: () -> Unit
) {
    if (connection == null || wallpaper == null) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    BackHandler {
        onBack()
    }

    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSettingWallpaper by remember { mutableStateOf(false) }

    // Customization variables
    var scale by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0.0f) } // -1.0f to 1.0f
    var panY by remember { mutableStateOf(0.0f) } // -1.0f to 1.0f
    var destination by remember { mutableStateOf(WallpaperDestination.Both) }

    LaunchedEffect(wallpaper) {
        isLoading = true
        val savedArr = wallpaper?.let { PrefsManager.getWallpaperArrangement(context, it.name) }
        if (savedArr != null) {
            scale = savedArr.scale
            panX = savedArr.panX
            panY = savedArr.panY
        } else {
            scale = 1.0f
            panX = 0.0f
            panY = 0.0f
        }
        try {
            fullBitmap = fetchBitmapFromUrl(connection.serverUrl, connection.token, wallpaper.name, false)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error downloading image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(scale, panX, panY) {
        if (!isLoading) {
            PrefsManager.saveWallpaperArrangement(context, wallpaper.name, WallpaperArrangement(scale, panX, panY))
            Log.d("WallpaperEditor", "Auto-saved arrangement: scale=$scale, panX=$panX, panY=$panY")
        }
    }

    // Screen aspect ratio for live cropping preview
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    val screenAspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

    GlassBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Configure Wallpaper", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.3f),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFBB86FC))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Downloading high quality image...", color = Color(0xFFA0A0C0), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else if (fullBitmap != null) {
                    // Preview Frame (floating mockup device)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "PREVIEW (SCREEN ASPECT RATIO)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03DAC6),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // phone bezel wrapper
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(screenAspectRatio)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFBB86FC), Color(0xFF00E5FF))
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(3.dp) // bezel padding
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val boxW = maxWidth
                            val boxH = maxHeight

                            val bmpW = fullBitmap!!.width.toFloat()
                            val bmpH = fullBitmap!!.height.toFloat()
                            val bmpRatio = bmpW / bmpH

                            val imgWidth = if (bmpRatio > screenAspectRatio) boxH * bmpRatio else boxW
                            val imgHeight = if (bmpRatio > screenAspectRatio) boxH else boxW / bmpRatio

                            Image(
                                bitmap = fullBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .requiredSize(imgWidth, imgHeight)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale

                                        val boxWPx = boxW.toPx()
                                        val boxHPx = boxH.toPx()
                                        val imgWPx = imgWidth.toPx()
                                        val imgHPx = imgHeight.toPx()

                                        val wz = imgWPx * scale
                                        val hz = imgHPx * scale

                                        val maxTx = ((wz - boxWPx) / 2f).coerceAtLeast(0f)
                                        val maxTy = ((hz - boxHPx) / 2f).coerceAtLeast(0f)

                                        translationX = -panX * maxTx
                                        translationY = -panY * maxTy
                                    }
                            )
                        }
                    }

                    // Floating Glass Control Sheet
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Zoom Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Zoom Adjustment", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(String.format("%.1fx", scale), style = MaterialTheme.typography.bodySmall, color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = scale,
                                    onValueChange = { scale = it },
                                    valueRange = 1.0f..3.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFBB86FC),
                                        activeTrackColor = Color(0xFFBB86FC),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            // Align Horizontal Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Align Horizontally", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(if (panX > 0.05f) "Right" else if (panX < -0.05f) "Left" else "Center", style = MaterialTheme.typography.bodySmall, color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = panX,
                                    onValueChange = { panX = it },
                                    valueRange = -1.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF03DAC6),
                                        activeTrackColor = Color(0xFF03DAC6),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            // Align Vertical Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Align Vertically", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(if (panY > 0.05f) "Bottom" else if (panY < -0.05f) "Top" else "Center", style = MaterialTheme.typography.bodySmall, color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = panY,
                                    onValueChange = { panY = it },
                                    valueRange = -1.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF03DAC6),
                                        activeTrackColor = Color(0xFF03DAC6),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            // Destination Pill Segmented Control
                            Column {
                                Text("Set Destination", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(
                                        WallpaperDestination.Home to "Home",
                                        WallpaperDestination.Lock to "Lock",
                                        WallpaperDestination.Both to "Both"
                                    ).forEach { (dest, title) ->
                                        val isSelected = destination == dest
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = if (isSelected) Color(0xFFBB86FC).copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { destination = dest }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFA0A0C0),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Set Desktop Wallpaper Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSettingWallpaper = true
                                        try {
                                            PrefsManager.saveWallpaperArrangement(
                                                context,
                                                wallpaper.name,
                                                WallpaperArrangement(scale, panX, panY)
                                            )

                                            val applied = applyCroppedWallpaper(
                                                context = context,
                                                source = fullBitmap!!,
                                                scale = scale,
                                                panX = panX,
                                                panY = panY,
                                                screenAspectRatio = screenAspectRatio,
                                                destination = destination
                                            )
                                            withContext(Dispatchers.Main) {
                                                if (applied) {
                                                    Toast.makeText(context, "Wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
                                                    onBack()
                                                } else {
                                                    Toast.makeText(context, "Failed to apply wallpaper.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Error setting wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        } finally {
                                            isSettingWallpaper = false
                                        }
                                    }
                                },
                                enabled = !isSettingWallpaper,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFBB86FC),
                                    contentColor = Color(0xFF0F0C1B),
                                    disabledContainerColor = Color(0xFFBB86FC).copy(alpha = 0.3f)
                                )
                            ) {
                                if (isSettingWallpaper) {
                                    CircularProgressIndicator(color = Color(0xFF0F0C1B), modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Set Desktop Wallpaper", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Failed to load image. Tap back and try again.", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- Crop & Apply Core Logic ---

suspend fun applyCroppedWallpaper(
    context: Context,
    source: Bitmap,
    scale: Float,
    panX: Float,
    panY: Float,
    screenAspectRatio: Float,
    destination: WallpaperDestination
): Boolean = withContext(Dispatchers.Default) {
    try {
        val srcW = source.width
        val srcH = source.height

        // Calculate fitting scale (matching the BoxWithConstraints logic)
        // Treat screen bounds as the relative box frame size
        val boxW = 1080f
        val boxH = 1080f / screenAspectRatio

        val sf = if (srcW.toFloat() / srcH.toFloat() > screenAspectRatio) {
            boxH / srcH.toFloat()
        } else {
            boxW / srcW.toFloat()
        }

        val wz = srcW.toFloat() * sf * scale
        val hz = srcH.toFloat() * sf * scale

        val cropW = boxW / (sf * scale)
        val cropH = boxH / (sf * scale)

        // Starting X coordinate on the original bitmap
        val startXVal = if (wz > boxW) {
            val maxOffsetZ = (wz - boxW) / 2.0f
            val offsetZ = panX * maxOffsetZ
            val centerZ = wz / 2.0f + offsetZ
            val startZ = centerZ - boxW / 2.0f
            startZ / (sf * scale)
        } else {
            0.0f
        }

        // Starting Y coordinate on the original bitmap
        val startYVal = if (hz > boxH) {
            val maxOffsetZ = (hz - boxH) / 2.0f
            val offsetZ = panY * maxOffsetZ
            val centerZ = hz / 2.0f + offsetZ
            val startZ = centerZ - boxH / 2.0f
            startZ / (sf * scale)
        } else {
            0.0f
        }

        val startX = startXVal.toInt().coerceIn(0, srcW - 1)
        val startY = startYVal.toInt().coerceIn(0, srcH - 1)
        val width = cropW.toInt().coerceIn(1, srcW - startX)
        val height = cropH.toInt().coerceIn(1, srcH - startY)

        Log.d("WallpaperCrop", "Cropping bitmap: startX=$startX, startY=$startY, width=$width, height=$height (orig: ${srcW}x${srcH})")

        // Create the cropped Bitmap
        val cropped = Bitmap.createBitmap(source, startX, startY, width, height)

        // Set as wallpaper via WallpaperManager
        val wpm = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val flags = when (destination) {
                WallpaperDestination.Home -> WallpaperManager.FLAG_SYSTEM
                WallpaperDestination.Lock -> WallpaperManager.FLAG_LOCK
                WallpaperDestination.Both -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            wpm.setBitmap(cropped, null, true, flags)
        } else {
            wpm.setBitmap(cropped)
        }
        true
    } catch (e: Exception) {
        Log.e("WallpaperCrop", "Failed to apply wallpaper", e)
        false
    }
}

// --- Networking & Helpers ---

sealed class LoginResponse {
    data class Success(val token: String) : LoginResponse()
    data class Requires2Fa(val tempToken: String) : LoginResponse()
    data class Error(val message: String) : LoginResponse()
}

suspend fun loginToCloud(serverUrl: String, user: String, pass: String): LoginResponse = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/login")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("username", user)
            put("password", pass)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            
            val json = JSONObject(response.toString())
            if (json.has("requires2fa") && json.getBoolean("requires2fa")) {
                LoginResponse.Requires2Fa(json.getString("tempToken"))
            } else {
                LoginResponse.Success(json.getString("token"))
            }
        } else {
            LoginResponse.Error("Invalid username or password")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LoginResponse.Error(e.message ?: "Connection error")
    } finally {
        conn.disconnect()
    }
}

suspend fun verify2fa(serverUrl: String, tempToken: String, code: String): String? = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/2fa/verify-login")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("tempToken", tempToken)
            put("code", code)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()
            JSONObject(response.toString()).getString("token")
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        conn.disconnect()
    }
}


suspend fun fetchWallpapersList(serverUrl: String, token: String): List<WallpaperItem> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/wallpapers")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            throw Exception("Server returned HTTP code ${conn.responseCode}")
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val arr = JSONArray(response.toString())
        val list = mutableListOf<WallpaperItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                WallpaperItem(
                    name = obj.getString("name"),
                    size = obj.getLong("size"),
                    type = obj.getString("type"),
                    lastModified = obj.getLong("lastModified")
                )
            )
        }
        list
    } finally {
        conn.disconnect()
    }
}

suspend fun fetchBitmapFromUrl(serverUrl: String, token: String, path: String, isThumbnail: Boolean): Bitmap? = withContext(Dispatchers.IO) {
    val endpoint = if (isThumbnail) "thumbnail" else "file"
    // Encode components of path using Uri.encode to handle space as %20 (decodeURIComponent compatible)
    val encodedSegments = path.split('/').map { android.net.Uri.encode(it) }
    val cleanPath = encodedSegments.joinToString("/")
    val url = URL("$serverUrl/$endpoint/$cleanPath")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 15000
    conn.readTimeout = 20000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode == 200) {
            BitmapFactory.decodeStream(conn.inputStream)
        } else {
            Log.e("AsyncServerImage", "HTTP error ${conn.responseCode} for: $url")
            null
        }
    } catch (e: Exception) {
        Log.e("AsyncServerImage", "Error loading image: ${e.message}")
        null
    } finally {
        conn.disconnect()
    }
}

suspend fun fetchImageResolution(serverUrl: String, token: String, path: String): String? = withContext(Dispatchers.IO) {
    val encodedSegments = path.split('/').map { android.net.Uri.encode(it) }
    val cleanPath = encodedSegments.joinToString("/")
    val url = URL("$serverUrl/file/$cleanPath")
    var conn: HttpURLConnection? = null
    try {
        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.setRequestProperty("Authorization", "Bearer $token")
        
        if (conn.responseCode == 200) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            conn.inputStream.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                "${options.outWidth}x${options.outHeight}"
            } else {
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    } finally {
        conn?.disconnect()
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun scheduleWallpaperCycle(context: Context, enable: Boolean, intervalMinutes: Int) {
    if (enable) {
        WallpaperCycleService.start(context)
    } else {
        WallpaperCycleService.stop(context)
    }
}

fun forceResetWallpaperCycle(context: Context, intervalMinutes: Int) {
    WallpaperCycleService.forceCycle(context)
}
