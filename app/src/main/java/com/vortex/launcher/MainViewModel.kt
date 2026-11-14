package com.vortex.launcher
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.MutablePreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vortex.launcher.data.*
import com.vortex.launcher.system.PowerActions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val store: SettingsStore,
    private val dao: ProfileDao
) : ViewModel() {
    val settings: StateFlow<Settings?> = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val profiles: StateFlow<List<Profile>> = dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val active: StateFlow<Profile?> = combine(profiles, settings) { ps, s ->
        ps.find { it.id == s?.activeProfile } ?: ps.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val homePress = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resumeTick = MutableStateFlow(0)
    val weather = MutableStateFlow<Weather?>(null)
    val sleepEnd = MutableStateFlow(0L)
    private var sleepMin = 0
    private var sleepJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.count() == 0) dao.insert(Profile(name = "Основной", iconUri = "0"))
            apps.value = loadApps()
        }
        viewModelScope.launch(Dispatchers.IO) {
            settings.filterNotNull().map { it.city to it.fahrenheit }.distinctUntilChanged().collectLatest { (c, f) ->
                while (true) { WeatherApi.fetch(c, f)?.let { weather.value = it }; delay(30 * 60_000L) }
            }
        }
    }

    fun onResumed() { resumeTick.value += 1 }

    // NonCancellable: запись должна завершиться, даже если Activity закрылась (ProfilePickerActivity)
    private fun bg(b: suspend () -> Unit) { viewModelScope.launch(NonCancellable) { b() } }
    fun edit(b: (MutablePreferences) -> Unit) = bg { store.update(b) }

    private suspend fun activeProfile(): Profile? {
        val ps = dao.allOnce(); val id = store.flow.first().activeProfile
        return ps.find { it.id == id } ?: ps.firstOrNull()
    }

    /** Скин пишется и в DataStore (глобально), и в активный профиль — переживает выключение ТВ. */
    fun setSkin(s: String) = bg {
        val p = activeProfile()
        store.update { it[K.SKIN] = s; if (p != null && (it[K.ACTIVE] ?: 0L) == 0L) it[K.ACTIVE] = p.id }
        p?.let { dao.update(it.copy(skinType = s)) }
    }
    fun setWallpaper(w: String) = bg {
        val p = activeProfile()
        store.update { it[K.WALL] = w; if (p != null && (it[K.ACTIVE] ?: 0L) == 0L) it[K.ACTIVE] = p.id }
        p?.let { dao.update(it.copy(wallpaperUri = w)) }
    }
    fun selectProfile(p: Profile, fromOnboarding: Boolean = false) = bg {
        val cur = store.flow.first()
        when {
            fromOnboarding -> {
                dao.update(p.copy(skinType = cur.skin, wallpaperUri = cur.wallpaper))
                store.update { it[K.ACTIVE] = p.id }
            }
            // тот же профиль (например, выбор при загрузке ТВ) — ничего не сбрасываем
            cur.activeProfile == p.id || cur.activeProfile == 0L -> store.update { it[K.ACTIVE] = p.id }
            else -> store.update {
                it[K.ACTIVE] = p.id; it[K.SKIN] = p.skinType; p.wallpaperUri?.let { w -> it[K.WALL] = w }
            }
        }
    }
    fun saveProfile(p: Profile) = bg { if (p.id == 0L) dao.insert(p) else dao.update(p) }
    fun deleteProfile(p: Profile) = bg { if (profiles.value.size > 1) dao.delete(p) }
    fun setDock(pkgs: List<String>) = bg { activeProfile()?.let { dao.update(it.copy(dockApps = pkgs.joinToString(","))) } }

    fun cycleSleep() {
        val opts = listOf(0, 15, 30, 60, 120)
        sleepMin = opts[(opts.indexOf(sleepMin) + 1) % opts.size]
        sleepJob?.cancel()
        if (sleepMin == 0) { sleepEnd.value = 0L; return }
        val m = sleepMin
        sleepEnd.value = System.currentTimeMillis() + m * 60_000L
        sleepJob = viewModelScope.launch {
            delay(m * 60_000L)
            sleepEnd.value = 0L; sleepMin = 0
            if (!PowerActions.sleep(ctx)) Toast.makeText(ctx, "Нужен доступ администратора устройства (Настройки → Система)", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadApps(): List<AppInfo> {
        val pm = ctx.packageManager
        val l = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0) +
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        return l.filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val cat = when (it.activityInfo.applicationInfo.category) {
                    ApplicationInfo.CATEGORY_GAME -> 2
                    ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> 1
                    else -> 0
                }
                AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(192, 192).asImageBitmap(), cat)
            }
            .sortedBy { it.label.lowercase() }
    }
}
