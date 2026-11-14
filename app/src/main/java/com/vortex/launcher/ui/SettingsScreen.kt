package com.vortex.launcher.ui
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*
import com.vortex.launcher.system.PowerActions

private val Cats = listOf("Интерфейс" to I.APPS, "Часы" to I.TIMER, "Док" to I.HOME, "Виджеты и погода" to I.CLOUD,
    "Профили" to I.USER, "Обои" to I.IMAGE, "Система" to I.GEAR)
private val Cities = listOf("Москва", "Санкт-Петербург", "Минск", "Киев", "Алматы", "Берлин", "Лондон")
private fun onOff(b: Boolean) = if (b) "Включено" else "Выключено"

@Composable
private fun STile(title: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusTile(onClick, modifier.fillMaxWidth().heightIn(min = 84.dp), RoundedCornerShape(26.dp)) { f ->
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = ink(f), fontSize = 22.sp, fontWeight = FontWeight.Medium)
            if (value != null) Text(value, color = ink(f).copy(alpha = .65f), fontSize = 16.sp)
        }
    }
}
private fun LazyListScope.tile(title: String, value: String?, mod: Modifier = Modifier, onClick: () -> Unit) = item { STile(title, value, onClick, mod) }

/** Два столбца: слева категории, справа — настройки выбранной категории. */
@Composable
fun SettingsScreen(vm: MainViewModel, st: Settings, nav: NavController) {
    val ctx = LocalContext.current
    var cat by remember { mutableIntStateOf(0) }
    var cityPrompt by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        WallpaperBg(st.wallpaper)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)))
        Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 40.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Настройки", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 14.dp))
                Cats.forEachIndexed { i, (name, icon) ->
                    FocusTile({ cat = i }, (if (i == 0) Modifier.autoFocus() else Modifier).fillMaxWidth().height(68.dp), RoundedCornerShape(24.dp),
                        idle = if (cat == i) Color.White.copy(alpha = .16f) else Color.Transparent, onFocus = { f -> if (f) cat = i }) { f ->
                        Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Ico(icon, size = 26.dp, tint = ink(f)); Text(name, color = ink(f), fontSize = 21.sp)
                        }
                    }
                }
            }
            AnimatedContent(cat, Modifier.weight(1f).fillMaxHeight(), transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "sc") { c ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
                    when (c) {
                        0 -> {
                            tile("Скин", if (st.skin == "PS5") "Vortex Gaming" else "Vortex") { vm.setSkin(if (st.skin == "PS5") "APPLE" else "PS5") }
                            tile("Акцентный цвет (Gaming)", Accents[st.accent.coerceIn(0, Accents.lastIndex)].first) { vm.edit { it[K.ACCENT] = (st.accent + 1) % Accents.size } }
                            tile("Анимации", onOff(st.anim)) { vm.edit { it[K.ANIM] = !st.anim } }
                            tile("Звуки навигации", onOff(st.sounds)) { vm.edit { it[K.SOUNDS] = !st.sounds } }
                            tile("Размытие стекла", onOff(st.blur)) { vm.edit { it[K.BLUR] = !st.blur } }
                        }
                        1 -> {
                            tile("Формат часов", if (st.clock24) "24 часа" else "12 часов") { vm.edit { it[K.C24] = !st.clock24 } }
                            tile("Положение часов", listOf("Слева", "По центру", "Справа")[st.clockPos % 3]) { vm.edit { it[K.CPOS] = (st.clockPos + 1) % 3 } }
                            tile("Цвет часов", "${st.clockColor % ClockColors.size + 1} из ${ClockColors.size}") { vm.edit { it[K.CCOL] = (st.clockColor + 1) % ClockColors.size } }
                            tile("Шрифт часов", listOf("Sans", "Serif", "Mono")[st.clockFont % 3]) { vm.edit { it[K.CFONT] = (st.clockFont + 1) % 3 } }
                            tile("Огромные часы", onOff(st.bigClock)) { vm.edit { it[K.BIG] = !st.bigClock } }
                            tile("Приветствие", onOff(st.greeting)) { vm.edit { it[K.GREET] = !st.greeting } }
                        }
                        2 -> {
                            tile("Управление доком", "Выбрать до 10 приложений") { nav.navigate("dock") }
                            tile("Размер иконок", listOf("Маленький", "Средний", "Большой")[st.dockSize.coerceIn(0, 2)]) { vm.edit { it[K.DOCKSIZE] = (st.dockSize + 1) % 3 } }
                            tile("Название над доком", onOff(st.dockLabels)) { vm.edit { it[K.DOCKLBL] = !st.dockLabels } }
                        }
                        3 -> {
                            tile("Виджеты (плеер)", onOff(st.widgets)) { vm.edit { it[K.WIDGETS] = !st.widgets } }
                            tile("Погода", onOff(st.weather)) { vm.edit { it[K.WEATHER] = !st.weather } }
                            tile("Город", st.city) { cityPrompt = true }
                            tile("Единицы температуры", if (st.fahrenheit) "°F" else "°C") { vm.edit { it[K.FAHR] = !st.fahrenheit } }
                        }
                        4 -> {
                            tile("Управление профилями", "Создать, переименовать, удалить") { nav.navigate("profiles") }
                            tile("Спрашивать профиль при включении ТВ", onOff(st.askProfile)) { vm.edit { it[K.ASK] = !st.askProfile } }
                            tile("Имя профиля на экране", onOff(st.showProfile)) { vm.edit { it[K.SHOWPROF] = !st.showProfile } }
                        }
                        5 -> {
                            tile("Обои", "Встроенные и свои") { nav.navigate("wallpaperSettings") }
                        }
                        else -> {
                            tile("Wi-Fi", "Системные настройки") { ctx.startActivity(Intent(AndroidSettings.ACTION_WIFI_SETTINGS)) }
                            tile("Лаунчер по умолчанию", "Системные настройки Home") { ctx.startActivity(Intent(AndroidSettings.ACTION_HOME_SETTINGS)) }
                            tile("Доступ к уведомлениям", "Нужен для плеера") { ctx.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                            tile("Доступ к статистике", "Нужен для недавних приложений") { ctx.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS)) }
                            tile("Администратор устройства", "Нужен для таймера сна") {
                                ctx.startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, PowerActions.admin(ctx)))
                            }
                            tile("Пройти настройку заново", "Онбординг") { vm.edit { it[K.ONB] = false }; nav.navigate("splash") { popUpTo(0) } }
                        }
                    }
                }
            }
        }
        if (cityPrompt) TextPrompt("Город для погоды", st.city, { v -> vm.edit { it[K.CITY] = v }; cityPrompt = false }, { cityPrompt = false })
    }
}

@Composable
fun DockManager(vm: MainViewModel) {
    val apps by vm.apps.collectAsState()
    val prof by vm.active.collectAsState()
    val pinned = prof?.dockApps.orEmpty().split(",").filter { it.isNotBlank() }
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
        Column(Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Док: выберите до 10 приложений (${pinned.size}/10)", color = Color.White, fontSize = 28.sp)
            LazyVerticalGrid(GridCells.Adaptive(130.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.lazy.grid.itemsIndexed(apps) { i, a ->
                    val on = a.pkg in pinned
                    Box(contentAlignment = Alignment.TopCenter) {
                        AppIcon(a, 72.dp, { vm.setDock(if (on) pinned - a.pkg else if (pinned.size < 10) pinned + a.pkg else pinned) },
                            if (i == 0) Modifier.autoFocus() else Modifier, label = true, focusScale = 1.1f)
                        if (on) Box(Modifier.align(Alignment.TopEnd).padding(end = 14.dp)) { Ico(I.CHECK, size = 26.dp, tint = Color(0xFF2ED573)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileManager(vm: MainViewModel) {
    val ps by vm.profiles.collectAsState()
    var editing by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020)), contentAlignment = Alignment.Center) {
        if (editing != null || creating) {
            ProfileEditor(editing, { n, i ->
                vm.saveProfile(editing?.copy(name = n, iconUri = i) ?: Profile(name = n, iconUri = i)); editing = null; creating = false
            }, { editing = null; creating = false })
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(ps) { i, p ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.width(300.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Avatar(p.name, p.iconUri, 56.dp); Text(p.name, color = Color.White, fontSize = 24.sp)
                    }
                    GlassButton("Изменить", { editing = p }, if (i == 0) Modifier.autoFocus() else Modifier)
                    if (ps.size > 1) GlassButton("Удалить", { vm.deleteProfile(p) })
                }
            }
            item { GlassButton("Создать профиль", { creating = true }, icon = I.PLUS) }
        }
    }
}

@Composable
fun AllAppsScreen(vm: MainViewModel, st: Settings) {
    val apps by vm.apps.collectAsState()
    val launch = rememberLaunch()
    Box(Modifier.fillMaxSize()) {
        if (st.skin == "PS5") Box(Modifier.fillMaxSize().background(PsBg)) else WallpaperBg(st.wallpaper)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)))
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Все приложения", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
            LazyVerticalGrid(GridCells.Adaptive(130.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.foundation.lazy.grid.itemsIndexed(apps) { i, a ->
                    AppIcon(a, 76.dp, { r -> launch(a.pkg, r) }, if (i == 0) Modifier.autoFocus() else Modifier, label = true, focusScale = 1.1f)
                }
            }
        }
    }
}
