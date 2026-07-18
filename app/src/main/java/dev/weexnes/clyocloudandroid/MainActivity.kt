package dev.weexnes.clyocloudandroid

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import dev.weexnes.clyocloudandroid.ui.theme.ClyoCloudAndroidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Custom Vector Icons for clean build without extra material-icons-extended dependency
val FolderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10f, 4f)
        lineTo(4f, 4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
        lineTo(20f, 20f)
        curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
        lineTo(22f, 6f)
        curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
        lineTo(12f, 4f)
        close()
    }.build()

val FileIcon: ImageVector
    get() = ImageVector.Builder(
        name = "File",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(14f, 2f)
        lineTo(6f, 2f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        lineTo(4f, 20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        lineTo(18f, 22f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        lineTo(20f, 8f)
        lineTo(14f, 2f)
        close()
        moveTo(13f, 9f)
        lineTo(13f, 3.5f)
        lineTo(18.5f, 9f)
        lineTo(13f, 9f)
        close()
    }.build()

val ShareIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Share",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(18f, 16.08f)
        curveTo(17.24f, 16.08f, 16.56f, 16.38f, 16.04f, 16.85f)
        lineTo(8.91f, 12.7f)
        curveTo(8.96f, 12.47f, 9f, 12.24f, 9f, 12f)
        curveTo(9f, 11.76f, 8.96f, 11.53f, 8.91f, 11.3f)
        lineTo(15.96f, 7.19f)
        curveTo(16.5f, 7.69f, 17.21f, 8f, 18f, 8f)
        curveTo(19.66f, 8f, 21f, 6.66f, 21f, 5f)
        curveTo(21f, 3.34f, 19.66f, 2f, 18f, 2f)
        curveTo(16.34f, 2f, 15f, 3.34f, 15f, 5f)
        curveTo(15f, 5.24f, 15.04f, 5.47f, 15.09f, 5.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveTo(4.34f, 9f, 3f, 10.34f, 3f, 12f)
        curveTo(3f, 13.66f, 4.34f, 15f, 6f, 15f)
        curveTo(6.79f, 15f, 7.5f, 14.69f, 8.04f, 14.19f)
        lineTo(15.16f, 18.35f)
        curveTo(15.11f, 18.56f, 15.08f, 18.77f, 15.08f, 19f)
        curveTo(15.08f, 20.61f, 16.39f, 21.92f, 18f, 21.92f)
        curveTo(19.61f, 21.92f, 20.92f, 20.61f, 20.92f, 19f)
        curveTo(20.92f, 17.39f, 19.61f, 16.08f, 18f, 16.08f)
        close()
    }.build()

val DownloadIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Download",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
        lineTo(19f, 20f)
        curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
        curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
        close()
        moveTo(17f, 13f)
        lineTo(12f, 18f)
        lineTo(7f, 13f)
        lineTo(10f, 13f)
        lineTo(10f, 9f)
        lineTo(14f, 9f)
        lineTo(14f, 13f)
        lineTo(17f, 13f)
        close()
    }.build()

val StarIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineTo(16.54f, 13.97f)
        lineTo(22f, 9.24f)
        lineTo(14.81f, 8.63f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineTo(7.46f, 13.97f)
        lineTo(5.82f, 21f)
        lineTo(12f, 17.27f)
        close()
    }.build()

val StarBorderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "StarBorder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(22f, 9.24f)
        lineTo(14.81f, 8.62f)
        lineTo(12f, 2f)
        lineTo(9.19f, 8.63f)
        lineTo(2f, 9.24f)
        lineTo(7.46f, 13.97f)
        lineTo(5.82f, 21f)
        lineTo(12f, 17.27f)
        lineTo(18.18f, 21f)
        lineTo(16.54f, 13.97f)
        lineTo(22f, 9.24f)
        close()
        moveTo(12f, 15.4f)
        lineTo(8.24f, 17.67f)
        lineTo(9.24f, 13.39f)
        lineTo(5.92f, 10.51f)
        lineTo(10.3f, 10.13f)
        lineTo(12f, 6.1f)
        lineTo(13.7f, 10.13f)
        lineTo(18.08f, 10.51f)
        lineTo(14.76f, 13.39f)
        lineTo(15.76f, 17.67f)
        lineTo(12f, 15.4f)
        close()
    }.build()

val SearchIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Search",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(15.5f, 14f)
        lineTo(14.71f, 14f)
        lineTo(14.43f, 13.73f)
        curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        lineTo(14f, 15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
        close()
    }.build()

val PlayIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        lineTo(19f, 12f)
        close()
    }.build()

val CloseIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }.build()

val SettingsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19.14f, 12.94f)
        curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
        curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
        lineTo(21.1f, 9.53f)
        curveTo(21.28f, 9.39f, 21.33f, 9.14f, 21.21f, 8.94f)
        lineTo(19.35f, 5.72f)
        curveTo(19.23f, 5.51f, 18.98f, 5.43f, 18.77f, 5.51f)
        lineTo(16.46f, 6.44f)
        curveTo(15.98f, 6.07f, 15.46f, 5.76f, 14.89f, 5.53f)
        lineTo(14.54f, 3.07f)
        curveTo(14.5f, 2.85f, 14.31f, 2.69f, 14.09f, 2.69f)
        lineTo(10.37f, 2.69f)
        curveTo(10.15f, 2.69f, 9.96f, 2.85f, 9.92f, 3.07f)
        lineTo(9.57f, 5.53f)
        curveTo(9f, 5.76f, 8.48f, 6.07f, 8f, 6.44f)
        lineTo(5.69f, 5.51f)
        curveTo(5.48f, 5.43f, 5.23f, 5.51f, 5.11f, 5.72f)
        lineTo(3.25f, 8.94f)
        curveTo(3.13f, 9.14f, 3.18f, 9.39f, 3.36f, 9.53f)
        lineTo(5.32f, 11.06f)
        curveTo(5.28f, 11.37f, 5.26f, 11.68f, 5.26f, 12f)
        curveTo(5.26f, 12.32f, 5.28f, 12.63f, 5.32f, 12.94f)
        lineTo(3.36f, 14.47f)
        curveTo(3.18f, 14.61f, 3.13f, 14.86f, 3.25f, 15.06f)
        lineTo(5.11f, 18.28f)
        curveTo(5.23f, 18.49f, 5.48f, 18.57f, 5.69f, 18.49f)
        lineTo(8f, 17.56f)
        curveTo(8.48f, 17.93f, 9f, 18.24f, 9.57f, 18.47f)
        lineTo(9.92f, 20.93f)
        curveTo(9.96f, 21.15f, 10.15f, 21.31f, 10.37f, 21.31f)
        lineTo(14.09f, 21.31f)
        curveTo(14.31f, 21.31f, 14.5f, 21.15f, 14.54f, 20.93f)
        lineTo(14.89f, 18.47f)
        curveTo(15.46f, 18.24f, 15.98f, 17.93f, 16.46f, 17.56f)
        lineTo(18.77f, 18.49f)
        curveTo(18.98f, 18.57f, 19.23f, 18.49f, 19.35f, 18.28f)
        lineTo(21.21f, 15.06f)
        curveTo(21.33f, 14.86f, 21.28f, 14.61f, 21.1f, 14.47f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12.23f, 15.62f)
        curveTo(10.23f, 15.62f, 8.61f, 14f, 8.61f, 12f)
        curveTo(8.61f, 10f, 10.23f, 8.38f, 12.23f, 8.38f)
        curveTo(14.23f, 8.38f, 15.85f, 10f, 15.85f, 12f)
        curveTo(15.85f, 14f, 14.23f, 15.62f, 12.23f, 15.62f)
        close()
    }.build()

val ArrowBackIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(20f, 11f)
        lineTo(7.83f, 11f)
        lineTo(13.41f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        lineTo(20f, 13f)
        close()
    }.build()

val AddIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Add",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(19f, 13f)
        lineTo(13f, 13f)
        lineTo(13f, 19f)
        lineTo(11f, 19f)
        lineTo(11f, 13f)
        lineTo(5f, 13f)
        lineTo(5f, 11f)
        lineTo(11f, 11f)
        lineTo(11f, 5f)
        lineTo(13f, 5f)
        lineTo(13f, 11f)
        lineTo(19f, 11f)
        close()
    }.build()

val ExitIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Exit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(10.09f, 15.59f)
        lineTo(11.5f, 17f)
        lineTo(16.5f, 12f)
        lineTo(11.5f, 7f)
        lineTo(10.09f, 8.41f)
        lineTo(12.67f, 11f)
        lineTo(3f, 11f)
        lineTo(3f, 13f)
        lineTo(12.67f, 13f)
        close()
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
        lineTo(3f, 9f)
        lineTo(5f, 9f)
        lineTo(5f, 5f)
        lineTo(19f, 5f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        lineTo(5f, 15f)
        lineTo(3f, 15f)
        lineTo(3f, 19f)
        curveTo(3.89f, 21f, 5f, 21f, 5f, 21f)
        lineTo(19f, 21f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        lineTo(21f, 5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
    }.build()

// Data Models
data class CloudConnection(
    val serverUrl: String,
    val username: String,
    val token: String
)

data class TagItem(
    val name: String,
    val color: String
)

data class CloudFile(
    val name: String,
    val size: Long,
    val type: String,
    val lastModified: Long,
    val isFolder: Boolean,
    var isPublic: Boolean,
    var isFavourite: Boolean,
    val description: String?,
    val tags: List<TagItem>
)

data class MediaItem(
    val type: String, // "video" or "slideshow"
    val name: String,
    val fullPath: String,
    val images: List<String>,
    val size: Long,
    val lastModified: Long,
    val isPublic: Boolean,
    val isFavourite: Boolean,
    val description: String?,
    val tags: List<TagItem>
)

enum class AppScreen {
    Login,
    Dashboard
}

enum class DashboardTab {
    Files,
    Media,
    Settings
}

// SharedPreferences Manager
object PrefsManager {
    private const val PREFS_NAME = "clyo_cloud_prefs"
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
}

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
fun MainAppRouter() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.Login) }
    var connection by remember { mutableStateOf<CloudConnection?>(null) }

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
                    onDisconnect = {
                        PrefsManager.clearCloudConnection(context)
                        connection = null
                        currentScreen = AppScreen.Login
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
                    Text(
                        text = "CLYO",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFBB86FC),
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "CLOUD BROWSER",
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
                            text = "Enter your server credentials to browse files and view media.",
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

// Main Dashboard Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    connection: CloudConnection?,
    onDisconnect: () -> Unit
) {
    if (connection == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(DashboardTab.Files) }
    var searchQuery by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    // Files Tab state
    var currentPath by remember { mutableStateOf("") }
    val pathStack = remember { mutableStateListOf<String>() }
    var filesList by remember { mutableStateOf<List<CloudFile>>(emptyList()) }
    var isFilesLoading by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Media Tab state
    var mediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isMediaLoading by remember { mutableStateOf(false) }

    // Global Predefined Tags state
    var predefinedTags by remember { mutableStateOf<List<TagItem>>(emptyList()) }

    // Tag editing dialog state
    var showTagEditDialog by remember { mutableStateOf(false) }
    var fileForTagEdit by remember { mutableStateOf<CloudFile?>(null) }

    // Immersive viewer state
    var activeViewerFile by remember { mutableStateOf<CloudFile?>(null) }
    var activeViewerMedia by remember { mutableStateOf<MediaItem?>(null) }

    // Fetch predefined tags once
    LaunchedEffect(Unit) {
        predefinedTags = fetchPredefinedTags(connection.serverUrl, connection.token)
    }

    // Navigation Backpress handling
    BackHandler(enabled = pathStack.isNotEmpty() || activeViewerFile != null || activeViewerMedia != null) {
        if (activeViewerFile != null) {
            activeViewerFile = null
        } else if (activeViewerMedia != null) {
            activeViewerMedia = null
        } else if (pathStack.isNotEmpty()) {
            pathStack.removeAt(pathStack.size - 1)
            currentPath = pathStack.joinToString("/")
        }
    }

    // Refresh Files List when path changes
    LaunchedEffect(currentPath) {
        isFilesLoading = true
        try {
            filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading files: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isFilesLoading = false
        }
    }

    // Refresh Media List when entering Media tab
    LaunchedEffect(currentTab) {
        if (currentTab == DashboardTab.Media) {
            isMediaLoading = true
            try {
                mediaList = fetchMediaList(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading media: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isMediaLoading = false
            }
        }
    }

    val triggerFileRefresh = {
        scope.launch {
            isFilesLoading = true
            try {
                filesList = fetchFilesList(connection.serverUrl, connection.token, currentPath)
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Refresh error", e)
            } finally {
                isFilesLoading = false
            }
        }
    }

    val triggerMediaRefresh = {
        scope.launch {
            isMediaLoading = true
            try {
                mediaList = fetchMediaList(connection.serverUrl, connection.token)
            } catch (e: Exception) {
                Log.e("ClyoCloudAndroid", "Refresh error", e)
            } finally {
                isMediaLoading = false
            }
        }
    }

    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header / Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = when (currentTab) {
                            DashboardTab.Files -> "Cloud Storage"
                            DashboardTab.Media -> "Media Library"
                            DashboardTab.Settings -> "Connection"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = connection.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A0C0)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentTab != DashboardTab.Settings) {
                        IconButton(onClick = { showFavoritesOnly = !showFavoritesOnly }) {
                            Icon(
                                imageVector = if (showFavoritesOnly) StarIcon else StarBorderIcon,
                                contentDescription = "Favorites Only",
                                tint = if (showFavoritesOnly) Color(0xFFFFD700) else Color(0xFFA0A0C0)
                            )
                        }
                    }

                    if (currentTab == DashboardTab.Files) {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(
                                imageVector = AddIcon,
                                contentDescription = "Create Folder",
                                tint = Color(0xFF03DAC6)
                            )
                        }
                    }
                }
            }

            // Search Bar (Files & Media)
            if (currentTab != DashboardTab.Settings) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            Icon(SearchIcon, contentDescription = "Search", tint = Color(0xFFA0A0C0), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search files...", color = Color(0xFFA0A0C0).copy(alpha = 0.6f), fontSize = 14.sp)
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(CloseIcon, contentDescription = "Clear", tint = Color(0xFFA0A0C0))
                                }
                            }
                        }
                    }
                }
            }

            // Main Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    DashboardTab.Files -> {
                        FilesTabContent(
                            serverUrl = connection.serverUrl,
                            token = connection.token,
                            currentPath = currentPath,
                            pathStack = pathStack,
                            files = filesList,
                            isLoading = isFilesLoading,
                            searchQuery = searchQuery,
                            showFavoritesOnly = showFavoritesOnly,
                            onPathChanged = { newPath -> currentPath = newPath },
                            onFileClick = { file -> activeViewerFile = file },
                            onToggleFavorite = { file, isFav ->
                                scope.launch {
                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = isFav)) {
                                        triggerFileRefresh()
                                    }
                                }
                            },
                            onTogglePublic = { file, isPub ->
                                scope.launch {
                                    val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isPublic = isPub)) {
                                        triggerFileRefresh()
                                        if (isPub) {
                                            val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                            shareText(context, publicUrl, "Share public link")
                                        }
                                    }
                                }
                            },
                            onCopyLink = { file ->
                                val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ClyoCloud Link", publicUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDownload = { file ->
                                val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                scope.launch {
                                    Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                                    val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullFilePath, file.name)
                                    if (success) {
                                        Toast.makeText(context, "Download complete: ${file.name}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = { file ->
                                val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                scope.launch {
                                    if (deleteFileOrFolder(connection.serverUrl, connection.token, fullFilePath)) {
                                        Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                        triggerFileRefresh()
                                    } else {
                                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEditTags = { file ->
                                fileForTagEdit = file
                                showTagEditDialog = true
                            }
                        )
                    }
                    DashboardTab.Media -> {
                        MediaTabContent(
                            serverUrl = connection.serverUrl,
                            token = connection.token,
                            mediaList = mediaList,
                            isLoading = isMediaLoading,
                            searchQuery = searchQuery,
                            showFavoritesOnly = showFavoritesOnly,
                            onMediaClick = { media -> activeViewerMedia = media }
                        )
                    }
                    DashboardTab.Settings -> {
                        SettingsTabContent(
                            connection = connection,
                            onDisconnect = onDisconnect
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            NavigationBar(
                containerColor = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == DashboardTab.Files,
                    onClick = { currentTab = DashboardTab.Files },
                    icon = { Icon(FolderIcon, contentDescription = "Files", modifier = Modifier.size(22.dp)) },
                    label = { Text("Files") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFBB86FC),
                        selectedTextColor = Color(0xFFBB86FC),
                        unselectedIconColor = Color(0xFFA0A0C0),
                        unselectedTextColor = Color(0xFFA0A0C0),
                        indicatorColor = Color(0xFFBB86FC).copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.Media,
                    onClick = { currentTab = DashboardTab.Media },
                    icon = { Icon(PlayIcon, contentDescription = "Media", modifier = Modifier.size(22.dp)) },
                    label = { Text("Media") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFBB86FC),
                        selectedTextColor = Color(0xFFBB86FC),
                        unselectedIconColor = Color(0xFFA0A0C0),
                        unselectedTextColor = Color(0xFFA0A0C0),
                        indicatorColor = Color(0xFFBB86FC).copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == DashboardTab.Settings,
                    onClick = { currentTab = DashboardTab.Settings },
                    icon = { Icon(SettingsIcon, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFBB86FC),
                        selectedTextColor = Color(0xFFBB86FC),
                        unselectedIconColor = Color(0xFFA0A0C0),
                        unselectedTextColor = Color(0xFFA0A0C0),
                        indicatorColor = Color(0xFFBB86FC).copy(alpha = 0.1f)
                    )
                )
            }
        }

        // Folder Creation Dialog
        if (showCreateFolderDialog) {
            var newFolderName by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showCreateFolderDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Create Folder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        GlassTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = "Folder name",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCreateFolderDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        scope.launch {
                                            if (createFolder(connection.serverUrl, connection.token, newFolderName.trim(), currentPath)) {
                                                showCreateFolderDialog = false
                                                triggerFileRefresh()
                                            } else {
                                                Toast.makeText(context, "Failed to create folder", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                            ) {
                                Text("Create", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Tag Edit Dialog
        if (showTagEditDialog && fileForTagEdit != null) {
            val file = fileForTagEdit!!
            val selectedTags = remember { mutableStateListOf<TagItem>().apply { addAll(file.tags) } }
            Dialog(onDismissRequest = { showTagEditDialog = false }) {
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Edit Tags: ${file.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(predefinedTags) { tag ->
                                val isChecked = selectedTags.any { it.name == tag.name }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) {
                                                selectedTags.removeAll { it.name == tag.name }
                                            } else {
                                                selectedTags.add(tag)
                                            }
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(android.graphics.Color.parseColor(tag.color)), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(tag.name, color = Color.White, modifier = Modifier.weight(1f))
                                    if (isChecked) {
                                        Icon(CheckIcon, contentDescription = "Selected", tint = Color(0xFF03DAC6), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTagEditDialog = false }) {
                                Text("Cancel", color = Color(0xFFA0A0C0))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, tags = selectedTags.toList())) {
                                            showTagEditDialog = false
                                            triggerFileRefresh()
                                        } else {
                                            Toast.makeText(context, "Failed to update tags", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                            ) {
                                Text("Save", color = Color(0xFF0F0C1B))
                            }
                        }
                    }
                }
            }
        }

        // Immersive viewer for files
        if (activeViewerFile != null) {
            val file = activeViewerFile!!
            val fullFilePath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
            ImmersiveMediaViewer(
                serverUrl = connection.serverUrl,
                token = connection.token,
                filePath = fullFilePath,
                fileName = file.name,
                isPublic = file.isPublic,
                isFavourite = file.isFavourite,
                tags = file.tags,
                onClose = { activeViewerFile = null },
                onToggleFavorite = { isFav ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isFavourite = isFav)) {
                            file.isFavourite = isFav
                            triggerFileRefresh()
                        }
                    }
                },
                onTogglePublic = { isPub ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, fullFilePath, isPublic = isPub)) {
                            file.isPublic = isPub
                            triggerFileRefresh()
                            if (isPub) {
                                val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                                shareText(context, publicUrl, "Share public link")
                            }
                        }
                    }
                },
                onDownload = {
                    scope.launch {
                        Toast.makeText(context, "Downloading ${file.name}...", Toast.LENGTH_SHORT).show()
                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, fullFilePath, file.name)
                        if (success) {
                            Toast.makeText(context, "Download complete", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShare = {
                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(fullFilePath)}"
                    shareText(context, publicUrl, "Share link")
                }
            )
        }

        // Immersive viewer for Media Tab
        if (activeViewerMedia != null) {
            val media = activeViewerMedia!!
            val viewerPath = media.fullPath
            val viewerName = media.name
            ImmersiveMediaViewer(
                serverUrl = connection.serverUrl,
                token = connection.token,
                filePath = viewerPath,
                fileName = viewerName,
                isPublic = media.isPublic,
                isFavourite = media.isFavourite,
                tags = media.tags,
                onClose = { activeViewerMedia = null },
                onToggleFavorite = { isFav ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, viewerPath, isFavourite = isFav)) {
                            triggerMediaRefresh()
                        }
                    }
                },
                onTogglePublic = { isPub ->
                    scope.launch {
                        if (updateFileMeta(connection.serverUrl, connection.token, viewerPath, isPublic = isPub)) {
                            triggerMediaRefresh()
                            if (isPub) {
                                val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(viewerPath)}"
                                shareText(context, publicUrl, "Share public link")
                            }
                        }
                    }
                },
                onDownload = {
                    scope.launch {
                        Toast.makeText(context, "Downloading $viewerName...", Toast.LENGTH_SHORT).show()
                        val success = downloadFileToDevice(context, connection.serverUrl, connection.token, viewerPath, viewerName)
                        if (success) {
                            Toast.makeText(context, "Download complete", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShare = {
                    val publicUrl = "${connection.serverUrl}/p/${connection.username}/${Uri.encode(viewerPath)}"
                    shareText(context, publicUrl, "Share link")
                }
            )
        }
    }
}

// Files Tab content Composable
@Composable
fun FilesTabContent(
    serverUrl: String,
    token: String,
    currentPath: String,
    pathStack: List<String>,
    files: List<CloudFile>,
    isLoading: Boolean,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    onPathChanged: (String) -> Unit,
    onFileClick: (CloudFile) -> Unit,
    onToggleFavorite: (CloudFile, Boolean) -> Unit,
    onTogglePublic: (CloudFile, Boolean) -> Unit,
    onCopyLink: (CloudFile) -> Unit,
    onDownload: (CloudFile) -> Unit,
    onDelete: (CloudFile) -> Unit,
    onEditTags: (CloudFile) -> Unit
) {
    val filteredFiles = files.filter { file ->
        val matchesSearch = file.name.contains(searchQuery, ignoreCase = true)
        val matchesFav = !showFavoritesOnly || file.isFavourite
        matchesSearch && matchesFav
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Breadcrumb Trail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Root",
                color = if (currentPath.isEmpty()) Color(0xFFBB86FC) else Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onPathChanged("") }
                    .padding(vertical = 4.dp)
            )

            pathStack.forEachIndexed { index, segment ->
                Text(" / ", color = Color(0xFFA0A0C0))
                Text(
                    text = segment,
                    color = if (index == pathStack.lastIndex) Color(0xFFBB86FC) else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val newPath = pathStack.take(index + 1).joinToString("/")
                            onPathChanged(newPath)
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFBB86FC))
            }
        } else if (filteredFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files found", color = Color(0xFFA0A0C0))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFiles) { file ->
                    var isMenuExpanded by remember { mutableStateOf(false) }
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isFolder) {
                                    val newPath = if (currentPath.isEmpty()) file.name else "$currentPath/${file.name}"
                                    onPathChanged(newPath)
                                } else {
                                    onFileClick(file)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isFolder) FolderIcon else FileIcon,
                                    contentDescription = null,
                                    tint = if (file.isFolder) Color(0xFFBB86FC) else Color(0xFF03DAC6),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (file.isFolder) "Folder" else formatFileSize(file.size),
                                            color = Color(0xFFA0A0C0),
                                            fontSize = 11.sp
                                        )
                                        if (file.isPublic) {
                                            Text(
                                                text = "Shared",
                                                color = Color(0xFF03DAC6),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Toggle Favorite icon directly
                                IconButton(onClick = { onToggleFavorite(file, !file.isFavourite) }) {
                                    Icon(
                                        imageVector = if (file.isFavourite) StarIcon else StarBorderIcon,
                                        contentDescription = "Favorite",
                                        tint = if (file.isFavourite) Color(0xFFFFD700) else Color(0xFFA0A0C0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Quick share/link button
                                if (file.isPublic) {
                                    IconButton(onClick = { onCopyLink(file) }) {
                                        Icon(ShareIcon, contentDescription = "Copy Link", tint = Color(0xFF03DAC6), modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Options menu expander
                                IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                                    Icon(MoreVertIcon, contentDescription = "Actions", tint = Color(0xFFA0A0C0), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Tags Display
                            if (file.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    file.tags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(tag.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Expanded Actions drawer
                            if (isMenuExpanded) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    TextButton(onClick = { onTogglePublic(file, !file.isPublic); isMenuExpanded = false }) {
                                        Text(if (file.isPublic) "Make Private" else "Make Public", color = Color(0xFFBB86FC), fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { onEditTags(file); isMenuExpanded = false }) {
                                        Text("Edit Tags", color = Color(0xFF03DAC6), fontSize = 12.sp)
                                    }
                                    if (!file.isFolder) {
                                        TextButton(onClick = { onDownload(file); isMenuExpanded = false }) {
                                            Text("Download", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                    TextButton(onClick = { onDelete(file); isMenuExpanded = false }) {
                                        Text("Delete", color = Color(0xFFFF5252), fontSize = 12.sp)
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

// Media Tab content Composable
@Composable
fun MediaTabContent(
    serverUrl: String,
    token: String,
    mediaList: List<MediaItem>,
    isLoading: Boolean,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    onMediaClick: (MediaItem) -> Unit
) {
    val filteredMedia = mediaList.filter { media ->
        val matchesSearch = media.name.contains(searchQuery, ignoreCase = true)
        val matchesFav = !showFavoritesOnly || media.isFavourite
        matchesSearch && matchesFav
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFBB86FC))
        }
    } else if (filteredMedia.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No media found", color = Color(0xFFA0A0C0))
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredMedia) { media ->
                // Thumbnail loading path:
                // For video: use fullPath
                // For slideshow: use the first image in list if available
                val thumbnailPath = if (media.type == "slideshow" && media.images.isNotEmpty()) {
                    media.images[0]
                } else {
                    media.fullPath
                }

                GlassCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMediaClick(media) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .background(Color.Black)
                        ) {
                            AsyncServerImage(
                                serverUrl = serverUrl,
                                token = token,
                                relativePath = thumbnailPath,
                                isThumbnail = true,
                                contentDescription = media.name,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Play overlay badge for video
                            if (media.type == "video") {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = PlayIcon,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }

                            // Slideshow indicator badge
                            if (media.type == "slideshow") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color(0xFFBB86FC), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("SLIDESHOW", color = Color(0xFF0F0C1B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = media.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (media.type == "slideshow") "Slideshow" else formatFileSize(media.size),
                                    color = Color(0xFFA0A0C0),
                                    fontSize = 10.sp
                                )
                                if (media.isFavourite) {
                                    Icon(StarIcon, contentDescription = "Favorite", tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Settings Tab Composable
@Composable
fun SettingsTabContent(
    connection: CloudConnection,
    onDisconnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFBB86FC).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        connection.username.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color(0xFFBB86FC)
                    )
                }

                Text(connection.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Server Address", color = Color(0xFFA0A0C0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(connection.serverUrl, color = Color.White, fontSize = 14.sp)
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Button(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(ExitIcon, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                        Text("Disconnect Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Immersive Media Viewer Overlay
@Composable
fun ImmersiveMediaViewer(
    serverUrl: String,
    token: String,
    filePath: String,
    fileName: String,
    isPublic: Boolean,
    isFavourite: Boolean,
    tags: List<TagItem>,
    onClose: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onTogglePublic: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    val isVideo = MimeTypeMap.getFileExtensionFromUrl(fileName).lowercase() in listOf("mp4", "webm", "mov", "mkv")

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        // Immersive Player / Image view
        if (isVideo) {
            val encodedSegments = filePath.split('/').map { Uri.encode(it) }
            val cleanPath = encodedSegments.joinToString("/")
            val streamUrl = "$serverUrl/file/$cleanPath"
            VideoPlayer(
                videoUrl = streamUrl,
                token = token,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )
        } else {
            // Zoomable AsyncServerImage
            var localBitmap by remember(filePath) { mutableStateOf<ImageBitmap?>(null) }
            var isImageLoading by remember(filePath) { mutableStateOf(true) }

            LaunchedEffect(filePath) {
                isImageLoading = true
                try {
                    val bitmapObj = fetchBitmapFromUrl(serverUrl, token, filePath, false)
                    localBitmap = bitmapObj?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isImageLoading = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale > 1f) offset + pan else Offset.Zero
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isImageLoading) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC))
                } else if (localBitmap != null) {
                    Image(
                        bitmap = localBitmap!!,
                        contentDescription = fileName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.Red)
                }
            }
        }

        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(CloseIcon, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Text(
                text = fileName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Row {
                IconButton(onClick = { onToggleFavorite(!isFavourite) }) {
                    Icon(
                        imageVector = if (isFavourite) StarIcon else StarBorderIcon,
                        contentDescription = "Favorite",
                        tint = if (isFavourite) Color(0xFFFFD700) else Color.White
                    )
                }
                IconButton(onClick = onDownload) {
                    Icon(DownloadIcon, contentDescription = "Download", tint = Color.White)
                }
                if (isPublic) {
                    IconButton(onClick = onShare) {
                        Icon(ShareIcon, contentDescription = "Share", tint = Color(0xFF03DAC6))
                    }
                }
            }
        }

        // Bottom Details Card
        GlassCard(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isVideo) "Video File" else "Image File",
                        color = Color(0xFF03DAC6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Public Access", color = Color(0xFFA0A0C0), fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                        androidx.compose.material3.Switch(
                            checked = isPublic,
                            onCheckedChange = { onTogglePublic(it) },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF03DAC6),
                                checkedTrackColor = Color(0xFF03DAC6).copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                if (tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(tag.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text("No tags set", color = Color(0xFFA0A0C0).copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
        }
    }
}

// Custom Native VideoView wrapper with custom headers
@Composable
fun VideoPlayer(
    videoUrl: String,
    token: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)

                val headers = mapOf("Authorization" to "Bearer $token")
                setVideoURI(Uri.parse(videoUrl), headers)

                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        update = { videoView ->
            val headers = mapOf("Authorization" to "Bearer $token")
            videoView.setVideoURI(Uri.parse(videoUrl), headers)
        },
        modifier = modifier
    )
}

// --- Composable helper: Async Server Image ---
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

// Fetch Files list for folder browser
suspend fun fetchFilesList(serverUrl: String, token: String, path: String): List<CloudFile> = withContext(Dispatchers.IO) {
    val encodedPath = Uri.encode(path)
    val url = URL("$serverUrl/api/files?path=$encodedPath")
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
        val list = mutableListOf<CloudFile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val metaObj = obj.optJSONObject("meta")
            val isPublic = metaObj?.optBoolean("isPublic", false) ?: false
            val isFavourite = metaObj?.optBoolean("isFavourite", false) ?: false
            val description = if (metaObj?.has("description") == true) metaObj.getString("description") else null
            val tagsList = mutableListOf<TagItem>()
            val tagsArr = metaObj?.optJSONArray("tags")
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(j)
                    tagsList.add(TagItem(tagObj.getString("name"), tagObj.getString("color")))
                }
            }
            list.add(
                CloudFile(
                    name = obj.getString("name"),
                    size = obj.getLong("size"),
                    type = obj.getString("type"),
                    lastModified = obj.getLong("lastModified"),
                    isFolder = obj.getBoolean("isFolder"),
                    isPublic = isPublic,
                    isFavourite = isFavourite,
                    description = description,
                    tags = tagsList
                )
            )
        }
        list
    } finally {
        conn.disconnect()
    }
}

// Fetch Media list for media tab
suspend fun fetchMediaList(serverUrl: String, token: String): List<MediaItem> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/videos")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext emptyList()
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val list = mutableListOf<MediaItem>()
        val jsonStr = response.toString().trim()
        if (jsonStr.startsWith("{")) {
            val root = JSONObject(jsonStr)
            val itemsArr = root.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    list.add(parseMediaItem(itemsArr.getJSONObject(i)))
                }
            }
        } else if (jsonStr.startsWith("[")) {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(parseMediaItem(arr.getJSONObject(i)))
            }
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        conn.disconnect()
    }
}

fun parseMediaItem(obj: JSONObject): MediaItem {
    val type = obj.optString("type", "video")
    val name = obj.getString("name")
    val fullPath = obj.getString("fullPath")
    val size = obj.optLong("size", 0L)
    val lastModified = obj.optLong("lastModified", 0L)

    val imagesList = mutableListOf<String>()
    val imagesArr = obj.optJSONArray("images")
    if (imagesArr != null) {
        for (i in 0 until imagesArr.length()) {
            imagesList.add(imagesArr.getString(i))
        }
    }

    val metaObj = obj.optJSONObject("meta")
    val isPublic = metaObj?.optBoolean("isPublic", false) ?: false
    val isFavourite = metaObj?.optBoolean("isFavourite", false) ?: false
    val description = if (metaObj?.has("description") == true) metaObj.getString("description") else null

    val tagsList = mutableListOf<TagItem>()
    val tagsArr = metaObj?.optJSONArray("tags")
    if (tagsArr != null) {
        for (i in 0 until tagsArr.length()) {
            val tagObj = tagsArr.getJSONObject(i)
            tagsList.add(TagItem(tagObj.getString("name"), tagObj.getString("color")))
        }
    }

    return MediaItem(
        type = type,
        name = name,
        fullPath = fullPath,
        images = imagesList,
        size = size,
        lastModified = lastModified,
        isPublic = isPublic,
        isFavourite = isFavourite,
        description = description,
        tags = tagsList
    )
}

// Fetch predefined tags from server
suspend fun fetchPredefinedTags(serverUrl: String, token: String): List<TagItem> = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/tags")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 10000
    conn.readTimeout = 10000
    conn.setRequestProperty("Authorization", "Bearer $token")

    try {
        if (conn.responseCode != 200) {
            return@withContext emptyList()
        }
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()

        val arr = JSONArray(response.toString())
        val list = mutableListOf<TagItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(TagItem(obj.getString("name"), obj.getString("color")))
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    } finally {
        conn.disconnect()
    }
}

// Update file metadata (public sharing, favorites, tags)
suspend fun updateFileMeta(
    serverUrl: String,
    token: String,
    filePath: String,
    isPublic: Boolean? = null,
    isFavourite: Boolean? = null,
    tags: List<TagItem>? = null
): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/file-meta")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("fullPath", filePath)
            if (isPublic != null) put("isPublic", isPublic)
            if (isFavourite != null) put("isFavourite", isFavourite)
            if (tags != null) {
                val tagsArr = JSONArray()
                tags.forEach { tag ->
                    tagsArr.put(JSONObject().apply {
                        put("name", tag.name)
                        put("color", tag.color)
                    })
                }
                put("tags", tagsArr)
            }
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Create new folder in cloud
suspend fun createFolder(serverUrl: String, token: String, folderName: String, path: String): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/folder")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("name", folderName)
            put("path", path)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Delete file or folder from cloud
suspend fun deleteFileOrFolder(serverUrl: String, token: String, path: String): Boolean = withContext(Dispatchers.IO) {
    val url = URL("$serverUrl/api/files")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "DELETE"
    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.doOutput = true
    conn.connectTimeout = 10000

    try {
        val body = JSONObject().apply {
            put("path", path)
        }
        val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
        writer.write(body.toString())
        writer.flush()
        writer.close()

        conn.responseCode == 200
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        conn.disconnect()
    }
}

// Download file directly to local Downloads folder using Android 10+ MediaStore API
suspend fun downloadFileToDevice(
    context: Context,
    serverUrl: String,
    token: String,
    filePath: String,
    fileName: String
): Boolean = withContext(Dispatchers.IO) {
    val encodedSegments = filePath.split('/').map { Uri.encode(it) }
    val cleanPath = encodedSegments.joinToString("/")
    val url = URL("$serverUrl/file/$cleanPath")
    var conn: HttpURLConnection? = null
    var inputStream: java.io.InputStream? = null
    try {
        conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("Authorization", "Bearer $token")

        if (conn.responseCode != 200) {
            return@withContext false
        }

        inputStream = conn.inputStream
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        inputStream?.close()
        conn?.disconnect()
    }
}

// Trigger native Android Share Sheet
fun shareText(context: Context, text: String, title: String = "Share Link") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

// Fetch image bitmap helper
suspend fun fetchBitmapFromUrl(serverUrl: String, token: String, path: String, isThumbnail: Boolean): Bitmap? = withContext(Dispatchers.IO) {
    val endpoint = if (isThumbnail) "thumbnail" else "file"
    val encodedSegments = path.split('/').map { Uri.encode(it) }
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
            Log.e("ClyoCloudAndroid", "HTTP error ${conn.responseCode} for: $url")
            null
        }
    } catch (e: Exception) {
        Log.e("ClyoCloudAndroid", "Error loading image: ${e.message}")
        null
    } finally {
        conn.disconnect()
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Custom icons that are missing in standard core library
val MoreVertIcon: ImageVector
    get() = ImageVector.Builder(
        name = "MoreVert",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(12f, 8f)
        curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
        curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
        curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
        curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
        close()
        moveTo(12f, 14f)
        curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
        curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
        curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
        curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
        close()
        moveTo(12f, 20f)
        curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
        curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
        curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
        curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
        close()
    }.build()

val CheckIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.White)) {
        moveTo(9f, 16.17f)
        lineTo(4.83f, 12f)
        lineTo(3.41f, 13.41f)
        lineTo(9f, 19f)
        lineTo(21f, 7f)
        lineTo(19.59f, 5.59f)
        close()
    }.build()
