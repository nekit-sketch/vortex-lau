package com.vortex.launcher.ui
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*
import com.vortex.launcher.system.MediaListener
import kotlinx.coroutines.delay

data class MediaUi(val ctl: MediaController, val title: String, val playing: Boolean)

@Composable
fun rememberMedia(): State<MediaUi?> {
    val ctx = LocalContext.current
    return produceState<MediaUi?>(null) {
        while (true) {
            value = runCatching {
                val c = ctx.getSystemService(MediaSessionManager::class.java)
                    .getActiveSessions(ComponentName(ctx, MediaListener::class.java)).firstOrNull()
                c?.let { MediaUi(it, it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Воспроизведение", it.playbackState?.state == PlaybackState.STATE_PLAYING) }
            }.getOrNull()
            delay(1500)
        }
    }
}

/** Классический режим: большие часы, приветствие/погода, остров справа сверху, док-капсула снизу. */
@Composable
fun AppleHomeScreen(st: Settings, vm: MainViewModel, dock: List<AppInfo>, nav: NavController, onCC: () -> Unit) {
    val launch = rememberLaunch()
    val time by rememberTime(st.clock24)
    val weather by vm.weather.collectAsState()
    val prof by vm.active.collectAsState()
    val cfg = LocalConfiguration.current
    var label by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val al = when (st.clockPos) { 1 -> Alignment.CenterHorizontally; 2 -> Alignment.End; else -> Alignment.Start }
    val p = prof
    val w = weather
    val iconSize = listOf(60.dp, 76.dp, 92.dp)[st.dockSize.coerceIn(0, 2)]
    val itemW = iconSize + 18.dp
    val dockW = (itemW * (dock.size + 1) + 32.dp).coerceAtMost(cfg.screenWidthDp.dp - 120.dp)
    Box(Modifier.fillMaxSize()) {
        WallpaperBg(st.wallpaper)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .22f), .4f to Color.Transparent, 1f to Color.Black.copy(alpha = .5f))))

        Column(Modifier.fillMaxWidth().padding(start = 72.dp, end = 72.dp, top = 40.dp), horizontalAlignment = al) {
            AnimatedVisibility(shown, enter = fadeIn(tween(800)) + slideInVertically(tween<IntOffset>(800)) { -it / 5 }) {
                Column(horizontalAlignment = al) {
                    if (st.greeting) {
                        val name = if (st.showProfile && p != null) ", ${p.name}" else ""
                        Text(greeting() + name, color = Color.White.copy(alpha = .92f), fontSize = 24.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(dateText(), color = Color.White.copy(alpha = .78f), fontSize = 20.sp)
                        if (st.weather && w != null) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .7f)))
                            Ico(I.CLOUD, size = 22.dp, tint = Color.White.copy(alpha = .78f))
                            Text("${w.temp}° ${w.text}", color = Color.White.copy(alpha = .78f), fontSize = 20.sp)
                        }
                    }
                    if (st.bigClock) Text(time, Modifier.offset(y = (-14).dp), color = ClockColors[st.clockColor % ClockColors.size].copy(alpha = .6f),
                        fontSize = 190.sp, fontWeight = FontWeight.ExtraLight, fontFamily = ClockFonts[st.clockFont % ClockFonts.size])
                    else ClockPill(st, Modifier.padding(top = 12.dp))
                }
            }
        }

        Island(Modifier.align(Alignment.TopEnd).padding(36.dp), p, onCC)

        if (st.widgets) MiniPlayer(Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 52.dp))

        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (st.dockLabels) Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                Crossfade(label, label = "dl") { l -> if (l != null) Text(l, color = Color.White, fontSize = 20.sp) }
            }
            AnimatedVisibility(shown, enter = slideInVertically(spring<IntOffset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { it } + fadeIn(tween(600))) {
                GlassPanel(shape = RoundedCornerShape(iconSize * .5f), tint = Color.White.copy(alpha = .14f)) {
                    LazyRow(Modifier.width(dockW), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        itemsIndexed(dock) { i, a ->
                            AppIcon(a, iconSize, { r -> launch(a.pkg, r) }, if (i == 0) Modifier.autoFocus() else Modifier,
                                onFocus = { f -> if (f) label = a.label })
                        }
                        item {
                            FocusTile({ nav.navigate("apps") }, Modifier.size(iconSize), RoundedCornerShape(iconSize * .22f),
                                Color.White.copy(alpha = .16f), onFocus = { f -> if (f) label = "Все приложения" }) { f ->
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ico(I.APPS, size = iconSize * .5f, tint = ink(f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Мини-островок: Wi-Fi, профиль и кнопка Центра управления; при фокусе расширяется. */
@Composable
fun Island(modifier: Modifier, profile: Profile?, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) 1.06f else 1f, sp(), label = "isl")
    GlassPanel(modifier.graphicsLayer { scaleX = sc; scaleY = sc }.animateContentSize(spring(Spring.DampingRatioMediumBouncy))
        .onFocusChanged { f = it.isFocused }.clickable(onClick = onClick),
        shape = CircleShape, tint = Color.Black.copy(alpha = .28f), border = Color.White.copy(alpha = if (f) .95f else .25f)) {
        Row(Modifier.padding(horizontal = if (f) 22.dp else 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Ico(I.WIFI, size = 22.dp)
            if (profile != null) Avatar(profile.name, profile.iconUri, 28.dp)
            Ico(I.TOGGLES, size = 22.dp)
            if (f) Text("Центр управления", color = Color.White, fontSize = 16.sp, maxLines = 1)
        }
    }
}

@Composable
fun MiniPlayer(modifier: Modifier) {
    val m by rememberMedia()
    val ui = m ?: return
    GlassPanel(modifier, CircleShape, Color.Black.copy(alpha = .3f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui.title, Modifier.widthIn(max = 240.dp).padding(start = 12.dp), color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconBtn(I.PREV, { ui.ctl.transportControls.skipToPrevious() }, size = 44.dp)
            IconBtn(if (ui.playing) I.PAUSE else I.PLAY, { if (ui.playing) ui.ctl.transportControls.pause() else ui.ctl.transportControls.play() }, size = 44.dp)
            IconBtn(I.NEXT, { ui.ctl.transportControls.skipToNext() }, size = 44.dp)
        }
    }
}
