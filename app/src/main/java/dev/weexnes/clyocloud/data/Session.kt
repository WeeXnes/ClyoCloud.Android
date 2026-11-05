package dev.weexnes.clyocloud.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * The signed-in server. Uses the same preference file and keys as earlier versions,
 * so updating the app keeps people signed in.
 */
object Session {
    private const val PREFS = "clyo_cloud_prefs"
    private const val KEY_SERVER = "server_url"
    private const val KEY_USER = "username"
    private const val KEY_TOKEN = "token"

    /** Observed by the root composable. */
    val current = mutableStateOf<CloudConnection?>(null)

    fun load(context: Context): CloudConnection? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val url = p.getString(KEY_SERVER, null)
        val user = p.getString(KEY_USER, null)
        val token = p.getString(KEY_TOKEN, null)
        val conn = if (url.isNullOrBlank() || user.isNullOrBlank() || token.isNullOrBlank()) null else CloudConnection(url, user, token)
        current.value = conn
        return conn
    }

    /** For code running outside the UI (workers, widgets). */
    fun get(context: Context): CloudConnection? = current.value ?: load(context)

    fun save(context: Context, connection: CloudConnection) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SERVER, connection.serverUrl)
            .putString(KEY_USER, connection.username)
            .putString(KEY_TOKEN, connection.token)
            .apply()
        current.value = connection
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_SERVER).remove(KEY_USER).remove(KEY_TOKEN)
            .apply()
        current.value = null
    }

    fun api(context: Context): ClyoApi? = get(context)?.let { ClyoApi(it) }
}

/** Small UI preferences (view mode, sort order, autoplay). */
object UiPrefs {
    private const val PREFS = "clyo_cloud_prefs"
    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun gridView(c: Context) = p(c).getBoolean("is_grid_view", false)
    fun setGridView(c: Context, v: Boolean) = p(c).edit().putBoolean("is_grid_view", v).apply()

    fun sort(c: Context) = p(c).getString("sort_option", "name") ?: "name"
    fun setSort(c: Context, v: String) = p(c).edit().putString("sort_option", v).apply()

    fun autoplayPreviews(c: Context) = p(c).getBoolean("autoplay_previews", true)
    fun setAutoplayPreviews(c: Context, v: Boolean) = p(c).edit().putBoolean("autoplay_previews", v).apply()

    fun shortsMuted(c: Context) = p(c).getBoolean("shorts_muted", true)
    fun setShortsMuted(c: Context, v: Boolean) = p(c).edit().putBoolean("shorts_muted", v).apply()
}
