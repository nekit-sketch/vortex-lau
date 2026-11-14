package com.vortex.launcher.data
import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Entity data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconUri: String? = null,   // индекс цвета аватара "0".."5"
    val skinType: String = "APPLE",
    val wallpaperUri: String? = null,
    val dockApps: String = ""
)
@Dao interface ProfileDao {
    @Query("SELECT * FROM Profile ORDER BY id") fun all(): Flow<List<Profile>>
    @Query("SELECT * FROM Profile ORDER BY id") suspend fun allOnce(): List<Profile>
    @Query("SELECT COUNT(*) FROM Profile") suspend fun count(): Int
    @Insert suspend fun insert(p: Profile): Long
    @Update suspend fun update(p: Profile)
    @Delete suspend fun delete(p: Profile)
}
@Database(entities = [Profile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() { abstract fun profiles(): ProfileDao }

/** category: 0 — обычное, 1 — медиа, 2 — игра */
data class AppInfo(val label: String, val pkg: String, val icon: ImageBitmap, val category: Int)

data class Settings(
    val onboarded: Boolean = false, val skin: String = "APPLE", val clock24: Boolean = true,
    val weather: Boolean = true, val widgets: Boolean = true, val askProfile: Boolean = true,
    val wallpaper: String = "grad:0", val clockPos: Int = 0, val clockColor: Int = 0, val clockFont: Int = 0,
    val city: String = "Москва", val activeProfile: Long = 0,
    val bigClock: Boolean = true, val greeting: Boolean = true, val fahrenheit: Boolean = false,
    val dockSize: Int = 1, val dockLabels: Boolean = true, val blur: Boolean = true, val anim: Boolean = true,
    val sounds: Boolean = true, val accent: Int = 0, val showProfile: Boolean = true
)
object K {
    val ONB = booleanPreferencesKey("onboarded"); val SKIN = stringPreferencesKey("skin")
    val C24 = booleanPreferencesKey("c24"); val WEATHER = booleanPreferencesKey("weather")
    val WIDGETS = booleanPreferencesKey("widgets"); val ASK = booleanPreferencesKey("ask")
    val WALL = stringPreferencesKey("wall"); val CPOS = intPreferencesKey("cpos")
    val CCOL = intPreferencesKey("ccol"); val CFONT = intPreferencesKey("cfont")
    val CITY = stringPreferencesKey("city"); val ACTIVE = longPreferencesKey("active")
    val BIG = booleanPreferencesKey("big"); val GREET = booleanPreferencesKey("greet")
    val FAHR = booleanPreferencesKey("fahr"); val DOCKSIZE = intPreferencesKey("docksize")
    val DOCKLBL = booleanPreferencesKey("docklbl"); val BLUR = booleanPreferencesKey("blur")
    val ANIM = booleanPreferencesKey("anim"); val SOUNDS = booleanPreferencesKey("sounds")
    val ACCENT = intPreferencesKey("accent"); val SHOWPROF = booleanPreferencesKey("showprof")
}
private val Context.ds by preferencesDataStore("vortex")

/** Скин хранится в DataStore (K.SKIN) и восстанавливается при каждом старте. */
@Singleton class SettingsStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    val flow: Flow<Settings> = ctx.ds.data.map { p ->
        val d = Settings()
        Settings(
            onboarded = p[K.ONB] ?: d.onboarded, skin = p[K.SKIN] ?: d.skin, clock24 = p[K.C24] ?: d.clock24,
            weather = p[K.WEATHER] ?: d.weather, widgets = p[K.WIDGETS] ?: d.widgets, askProfile = p[K.ASK] ?: d.askProfile,
            wallpaper = p[K.WALL] ?: d.wallpaper, clockPos = p[K.CPOS] ?: d.clockPos, clockColor = p[K.CCOL] ?: d.clockColor,
            clockFont = p[K.CFONT] ?: d.clockFont, city = p[K.CITY] ?: d.city, activeProfile = p[K.ACTIVE] ?: d.activeProfile,
            bigClock = p[K.BIG] ?: d.bigClock, greeting = p[K.GREET] ?: d.greeting, fahrenheit = p[K.FAHR] ?: d.fahrenheit,
            dockSize = p[K.DOCKSIZE] ?: d.dockSize, dockLabels = p[K.DOCKLBL] ?: d.dockLabels, blur = p[K.BLUR] ?: d.blur,
            anim = p[K.ANIM] ?: d.anim, sounds = p[K.SOUNDS] ?: d.sounds, accent = p[K.ACCENT] ?: d.accent,
            showProfile = p[K.SHOWPROF] ?: d.showProfile
        )
    }
    suspend fun update(b: (MutablePreferences) -> Unit) { ctx.ds.edit { b(it) } }
}
