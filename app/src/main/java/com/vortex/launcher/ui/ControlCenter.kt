package com.vortex.launcher.ui
import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*
import com.vortex.launcher.system.PowerActions
import kotlinx.coroutines.delay

private enum class Page { MAIN, POWER, RECENTS }

/** Центр управления: крупные плитки, как на референсе (Power, Wi-Fi, Recent Apps, Audio, Sleep Timer + круглые кнопки). */
@Composable
fun ControlCenter(vm: MainViewModel, st: Settings, apps: List<AppInfo>, nav: NavController, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val launch = rememberLaunch()
    var page by remember { mutableStateOf(Page.MAIN) }
    var recents by remember { mutableStateOf(listOf<AppInfo>()) }
    BackHandler(true) { if (page != Page.MAIN) page = Page.MAIN else onClose() }
    LaunchedEffect(Unit) { recents = recentApps(ctx, apps) }
    val time by rememberTime(st.clock24)
    val prof by vm.active.collectAsState()
    val sleepEnd by vm.sleepEnd.collectAsState()
    val wifi by produceState("…") { while (true) { value = wifiStatus(ctx); delay(3000) } }
    val minsLeft by produceState(0, sleepEnd) {
        while (true) { value = if (sleepEnd == 0L) 0 else ((sleepEnd - System.currentTimeMillis()) / 60000 + 1).toInt().coerceAtLeast(0); delay(15000) }
    }
    val p = prof
    val wall = if (st.skin == "PS5") PsWall else LocalWall.current
    CompositionLocalProvider(LocalWall provides wall) {
        GlassPanel(modifier.width(560.dp).fillMaxHeight(), RoundedCornerShape(40.dp), Color(0xFF0C1A16).copy(alpha = .55f)) {
            Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(time, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBtn(I.HOME, onClose, size = 48.dp)
                        IconBtn(I.TOGGLES, { onClose(); nav.navigate("settings") }, size = 48.dp)
                        FocusTile({ onClose(); nav.navigate("profiles") }, Modifier.size(48.dp), CircleShape, Color.Transparent, Color.White.copy(alpha = .3f), 1.1f) { _ ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (p != null) Avatar(p.name, p.iconUri, 40.dp) else Ico(I.USER) }
                        }
                    }
                }
                AnimatedContent(page, Modifier.weight(1f), transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally(tween<IntOffset>(250)) { it / 8 }) togetherWith fadeOut(tween(150))
                }, label = "ccp") { pg ->
                    when (pg) {
                        Page.MAIN -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(Modifier.fillMaxWidth().weight(1.6f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                FocusTile({ page = Page.POWER }, Modifier.weight(1f).fillMaxHeight().autoFocus(), RoundedCornerShape(34.dp)) { f ->
                                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Ico(I.POWER, size = 84.dp, tint = ink(f))
                                        Spacer(Modifier.height(12.dp))
                                        Text("Питание", color = ink(f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    CCRow(I.WIFI, "Wi-Fi", wifi, { ctx.startActivity(Intent(AndroidSettings.ACTION_WIFI_SETTINGS)) }, Modifier.weight(1f))
                                    CCRow(I.RECENT, "Недавние", "Закрыть приложения", { page = Page.RECENTS }, Modifier.weight(1f))
                                }
                            }
                            Row(Modifier.fillMaxWidth().weight(.8f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                CCRow(I.VOLUME, "Звук", "Вывод и громкость", { ctx.startActivity(Intent(AndroidSettings.ACTION_SOUND_SETTINGS)) }, Modifier.weight(1f))
                                CCRow(I.TIMER, "Таймер сна", if (sleepEnd == 0L) "Выключен" else "$minsLeft мин", { vm.cycleSleep() }, Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                CircleBtn(I.SEARCH) { runCatching { ctx.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                                CircleBtn(I.GAME) { vm.setSkin(if (st.skin == "PS5") "APPLE" else "PS5"); onClose() }
                                CircleBtn(I.IMAGE) { onClose(); nav.navigate("wallpaperSettings") }
                                CircleBtn(I.APPS) { onClose(); nav.navigate("apps") }
                            }
                        }
                        Page.POWER -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Питание", color = Color.White, fontSize = 28.sp)
                            CCRow(I.POWER, "Выключить", "Выключение ТВ", {
                                if (!PowerActions.shutdown(ctx)) Toast.makeText(ctx, "Нужны права системного приложения", Toast.LENGTH_LONG).show()
                            }, Modifier.weight(1f).fillMaxWidth().autoFocus())
                            CCRow(I.RESTART, "Перезагрузить", "Нужен Device Owner", {
                                if (!PowerActions.reboot(ctx)) Toast.makeText(ctx, "Нужен Device Owner (см. README)", Toast.LENGTH_LONG).show()
                            }, Modifier.weight(1f).fillMaxWidth())
                            CCRow(I.CHEVRON, "Назад", "", { page = Page.MAIN }, Modifier.weight(.6f).fillMaxWidth())
                        }
                        Page.RECENTS -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Недавние приложения", color = Color.White, fontSize = 28.sp)
                            if (recents.isEmpty()) {
                                Text("Нет данных. Выдайте доступ к статистике использования.", color = Color.White.copy(alpha = .7f), fontSize = 18.sp)
                                CCRow(I.GEAR, "Выдать доступ", "Статистика использования", {
                                    ctx.startActivity(Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS))
                                }, Modifier.height(84.dp).fillMaxWidth().autoFocus())
                            }
                            recents.forEachIndexed { idx, a ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FocusTile({ launch(a.pkg, null) }, (if (idx == 0) Modifier.autoFocus() else Modifier).weight(1f).height(68.dp), RoundedCornerShape(24.dp)) { f ->
                                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Image(a.icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                                            Text(a.label, color = ink(f), fontSize = 19.sp, maxLines = 1)
                                        }
                                    }
                                    IconBtn(I.CLOSE, {
                                        ctx.getSystemService(ActivityManager::class.java).killBackgroundProcesses(a.pkg)
                                        recents = recents - a
                                    }, size = 56.dp)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            CCRow(I.CHEVRON, "Назад", "", { page = Page.MAIN }, Modifier.height(72.dp).fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CCRow(icon: I, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusTile(onClick, modifier, RoundedCornerShape(30.dp)) { f ->
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (f) Color.Black.copy(alpha = .08f) else Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Ico(icon, size = 26.dp, tint = ink(f))
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, color = ink(f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitle.isNotBlank()) Text(subtitle, color = ink(f).copy(alpha = .7f), fontSize = 15.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CircleBtn(i: I, onClick: () -> Unit) {
    FocusTile(onClick, Modifier.size(84.dp), CircleShape, focusScale = 1.08f) { f ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ico(i, size = 36.dp, tint = ink(f)) }
    }
}

private fun wifiStatus(ctx: Context): String = runCatching {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    when {
        caps == null -> "Нет сети"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            @Suppress("DEPRECATION")
            val ssid = ctx.getSystemService(WifiManager::class.java).connectionInfo?.ssid?.trim('"')
            if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") "Подключено" else ssid
        }
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Подключено"
    }
}.getOrDefault("Нет данных")

/** getRecentTasks сторонним приложениям недоступен → UsageStats (нужен спец. доступ). */
private fun recentApps(ctx: Context, apps: List<AppInfo>): List<AppInfo> = runCatching {
    val now = System.currentTimeMillis()
    ctx.getSystemService(UsageStatsManager::class.java).queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 86_400_000L, now)
        .sortedByDescending { it.lastTimeUsed }.mapNotNull { u -> apps.find { it.pkg == u.packageName } }.distinct().take(5)
}.getOrDefault(emptyList())
