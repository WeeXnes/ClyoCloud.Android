package dev.weexnes.clyocloud.wallpaper

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

enum class WallpaperTarget(val label: String) { Both("Home and lock"), Home("Home screen"), Lock("Lock screen") }

enum class CycleSource(val label: String) { All("All wallpapers"), Playlist("Playlist") }

/** How a wallpaper is framed on the screen: zoom and pan (-1..1 from the centre). */
data class Framing(val scale: Float = 1f, val panX: Float = 0f, val panY: Float = 0f)

/** Every setting of the wallpaper cycle and the photo widget, in one preference file. */
object WallpaperPrefs {
    private const val PREFS = "clyo_wallpaper_prefs"
    private fun p(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Wallpaper cycle ──
    fun cycleEnabled(c: Context) = p(c).getBoolean("cycle_enabled", false)
    fun setCycleEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("cycle_enabled", v).apply()

    /** Minutes between changes; WorkManager can't run more often than every 15 minutes. */
    fun intervalMinutes(c: Context) = p(c).getInt("cycle_interval", 60).coerceAtLeast(MIN_INTERVAL_MINUTES)
    fun setIntervalMinutes(c: Context, v: Int) = p(c).edit().putInt("cycle_interval", v.coerceAtLeast(MIN_INTERVAL_MINUTES)).apply()

    fun shuffle(c: Context) = p(c).getBoolean("cycle_shuffle", true)
    fun setShuffle(c: Context, v: Boolean) = p(c).edit().putBoolean("cycle_shuffle", v).apply()

    fun source(c: Context) = runCatching { CycleSource.valueOf(p(c).getString("cycle_source", "All")!!) }.getOrDefault(CycleSource.All)
    fun setSource(c: Context, v: CycleSource) = p(c).edit().putString("cycle_source", v.name).apply()

    fun target(c: Context) = runCatching { WallpaperTarget.valueOf(p(c).getString("cycle_target", "Both")!!) }.getOrDefault(WallpaperTarget.Both)
    fun setTarget(c: Context, v: WallpaperTarget) = p(c).edit().putString("cycle_target", v.name).apply()

    /** Change only on Wi-Fi / unmetered networks (big images). */
    fun unmeteredOnly(c: Context) = p(c).getBoolean("cycle_unmetered_only", false)
    fun setUnmeteredOnly(c: Context, v: Boolean) = p(c).edit().putBoolean("cycle_unmetered_only", v).apply()

    fun playlist(c: Context): List<String> = readList(p(c).getString("playlist", "[]"))
    fun setPlaylist(c: Context, v: List<String>) = p(c).edit().putString("playlist", JSONArray(v.distinct()).toString()).apply()
    fun togglePlaylist(c: Context, path: String): Boolean {
        val list = playlist(c).toMutableList()
        val added = if (path in list) { list.remove(path); false } else { list.add(path); true }
        setPlaylist(c, list)
        return added
    }

    /** Position in the ordered list (sequential mode). */
    fun cursor(c: Context) = p(c).getInt("cycle_index", 0)
    fun setCursor(c: Context, v: Int) = p(c).edit().putInt("cycle_index", v).apply()

    /** Recently shown paths, so shuffle doesn't repeat until most of the list has been seen. */
    fun recent(c: Context): List<String> = readList(p(c).getString("cycle_recent", "[]"))
    fun setRecent(c: Context, v: List<String>) = p(c).edit().putString("cycle_recent", JSONArray(v).toString()).apply()

    fun current(c: Context): String? = p(c).getString("current_wallpaper", null)
    fun lastChanged(c: Context) = p(c).getLong("cycle_last_changed", 0L)
    fun recordApplied(c: Context, path: String) = p(c).edit()
        .putString("current_wallpaper", path)
        .putLong("cycle_last_changed", System.currentTimeMillis())
        .remove("cycle_last_error")
        .apply()

    /** When the next automatic change is due (0 = none scheduled). */
    fun nextRun(c: Context) = p(c).getLong("cycle_next_execution", 0L)
    fun setNextRun(c: Context, v: Long) = p(c).edit().putLong("cycle_next_execution", v).apply()

    fun lastError(c: Context): String? = p(c).getString("cycle_last_error", null)
    fun setLastError(c: Context, v: String?) = p(c).edit().putString("cycle_last_error", v).apply()

    fun framing(c: Context, path: String): Framing? {
        val raw = p(c).getString("framing:$path", null) ?: return null
        return runCatching { JSONObject(raw).let { Framing(it.getDouble("s").toFloat(), it.getDouble("x").toFloat(), it.getDouble("y").toFloat()) } }.getOrNull()
    }
    fun setFraming(c: Context, path: String, f: Framing) =
        p(c).edit().putString("framing:$path", JSONObject().put("s", f.scale.toDouble()).put("x", f.panX.toDouble()).put("y", f.panY.toDouble()).toString()).apply()

    // ── Photo widget ──
    fun widgetEnabled(c: Context) = p(c).getBoolean("widget_cycle_enabled", false)
    fun setWidgetEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("widget_cycle_enabled", v).apply()

    fun widgetIntervalMinutes(c: Context) = p(c).getInt("widget_cycle_interval", 60).coerceAtLeast(MIN_INTERVAL_MINUTES)
    fun setWidgetIntervalMinutes(c: Context, v: Int) = p(c).edit().putInt("widget_cycle_interval", v.coerceAtLeast(MIN_INTERVAL_MINUTES)).apply()

    fun widgetCursor(c: Context) = p(c).getInt("widget_index", 0)
    fun setWidgetCursor(c: Context, v: Int) = p(c).edit().putInt("widget_index", v).apply()

    fun widgetCurrent(c: Context): String? = p(c).getString("widget_current_image", null)
    fun setWidgetCurrent(c: Context, v: String?) = p(c).edit().putString("widget_current_image", v).apply()

    fun widgetNextRun(c: Context) = p(c).getLong("widget_next_execution", 0L)
    fun setWidgetNextRun(c: Context, v: Long) = p(c).edit().putLong("widget_next_execution", v).apply()

    fun register(c: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).registerOnSharedPreferenceChangeListener(listener)
    fun unregister(c: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) = p(c).unregisterOnSharedPreferenceChangeListener(listener)

    private fun readList(raw: String?): List<String> = runCatching {
        val arr = JSONArray(raw ?: "[]")
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    const val MIN_INTERVAL_MINUTES = 15
}
