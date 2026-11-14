package com.vortex.launcher.ui
import android.Manifest
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*
import kotlin.math.hypot
import kotlin.math.max

/** Медленно дрейфующие цветные пятна на тёмном фоне. */
@Composable
fun AuroraBg(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "aur")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Canvas(modifier.fillMaxSize().background(Color(0xFF070A18))) {
        val w = size.width; val h = size.height
        fun blob(c: Color, cx: Float, cy: Float, r: Float) =
            drawCircle(Brush.radialGradient(listOf(c.copy(alpha = .55f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
        blob(Color(0xFF5B3DF5), w * (.15f + .5f * a), h * (.25f + .2f * a), w * .42f)
        blob(Color(0xFF00A8FF), w * (.85f - .45f * a), h * (.75f - .2f * a), w * .38f)
        blob(Color(0xFFEC4899), w * (.5f + .2f * a), h * (.9f - .5f * a), w * .30f)
    }
}

@Composable
fun OnbScaffold(title: String, subtitle: String? = null, bg: @Composable BoxScope.() -> Unit = { AuroraBg() },
                content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        bg()
        Column(Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterVertically)) {
            Stagger(0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
                    subtitle?.let { Text(it, color = Color.White.copy(alpha = .7f), fontSize = 20.sp, textAlign = TextAlign.Center) }
                }
            }
            content()
        }
    }
}

@Composable
fun SplashScreen(onStart: () -> Unit) {
    val r = remember { Animatable(0f) }
    val a = remember { Animatable(0f) }
    var showBtn by remember { mutableStateOf(false) }
    val glow by rememberInfiniteTransition(label = "g").animateFloat(18f, 42f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "gl")
    LaunchedEffect(Unit) {
        r.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)); a.animateTo(1f, tween(1000)); showBtn = true
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AuroraBg(Modifier.graphicsLayer { alpha = a.value })
        Canvas(Modifier.fillMaxSize()) {
            val m = max(1f, hypot(size.width, size.height) / 2 * r.value)
            // круглая «дыра» расширяется из центра
            drawCircle(Brush.radialGradient(listOf(Color(0xFF3A2A8F), Color(0xFF0B1020)), center, m), radius = m)
            drawCircle(Color.White.copy(alpha = (1f - r.value) * .8f), m, style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
        }
        Text("Vortex Launcher", Modifier.align(Alignment.Center).graphicsLayer { alpha = a.value; scaleX = .9f + .1f * a.value; scaleY = scaleX },
            style = TextStyle(color = Color.White, fontSize = 66.sp, fontWeight = FontWeight.ExtraLight,
                letterSpacing = (2f + 10f * (1f - a.value)).sp, shadow = Shadow(Color.White.copy(alpha = .75f), Offset.Zero, glow)))
        AnimatedVisibility(showBtn, Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            enter = fadeIn(tween(500)) + slideInVertically(spring<IntOffset>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { it }) {
            GlassButton("Начнём", onStart, Modifier.autoFocus())
        }
    }
}

@Composable
fun SkinScreen(onPick: (String) -> Unit) {
    OnbScaffold("Выберите интерфейс", "Позже его можно изменить в настройках") {
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Stagger(1) { SkinCard("Vortex", "Классический режим", false, Modifier.autoFocus()) { onPick("APPLE") } }
            Stagger(2) { SkinCard("Vortex Gaming", "Игровой режим", true, Modifier) { onPick("PS5") } }
        }
    }
}

@Composable
private fun SkinCard(title: String, sub: String, gaming: Boolean, m: Modifier, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) 1.07f else 1f, sp(), label = "sk")
    val shape = RoundedCornerShape(26.dp)
    Column(m.graphicsLayer { scaleX = s; scaleY = s }.width(340.dp).clip(shape).background(Color.White.copy(alpha = .12f))
        .border(if (f) 2.dp else 1.dp, Color.White.copy(alpha = if (f) 1f else .3f), shape)
        .onFocusChanged { f = it.isFocused }.clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp))
            .background(if (gaming) Brush.verticalGradient(listOf(PsBg, Color(0xFF0B1A3D))) else Brush.linearGradient(Wallpapers[0]))) {
            if (gaming) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(6) { k -> Box(Modifier.size(if (k == 0) 38.dp else 28.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = if (k == 0) .55f else .25f))) }
                }
                Box(Modifier.align(Alignment.BottomStart).padding(14.dp).size(width = 90.dp, height = 22.dp).clip(CircleShape).background(Color.White.copy(alpha = .75f)))
            } else {
                Text("09:41", Modifier.padding(14.dp), color = Color.White.copy(alpha = .55f), fontSize = 40.sp, fontWeight = FontWeight.ExtraLight)
                Row(Modifier.align(Alignment.BottomCenter).padding(10.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .22f)).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(5) { Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = .8f))) }
                }
            }
        }
        Text(title, color = Color.White, fontSize = 24.sp)
        Text(sub, color = Color.White.copy(alpha = .7f), fontSize = 16.sp)
    }
}

@Composable
fun WifiScreen(onNext: () -> Unit) {
    val ctx = LocalContext.current
    var nets by remember { mutableStateOf(listOf<String>()) }
    fun scan() {
        nets = runCatching {
            ctx.getSystemService(WifiManager::class.java).scanResults.map { it.SSID }.filter { it.isNotBlank() }.distinct()
        }.getOrDefault(emptyList())
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { scan() }
    LaunchedEffect(Unit) { scan(); perm.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
    val openWifi = { ctx.startActivity(Intent(AndroidSettings.ACTION_WIFI_SETTINGS)); Unit }
    OnbScaffold("Wi-Fi", "Выберите сеть — подключение откроется в системных настройках") {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (nets.isEmpty()) item { Stagger(1) { GlassButton("Открыть настройки Wi-Fi", openWifi, icon = I.WIFI) } }
            itemsIndexed(nets) { i, n -> Stagger(i.coerceAtMost(8) + 1) { GlassButton(n, openWifi, icon = I.WIFI) } }
        }
        GlassButton("Пропустить", onNext, Modifier.autoFocus())
    }
}

@Composable
fun WallpaperPicker(current: String, onPick: (String) -> Unit, doneText: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onPick(uri.toString())
        }
    }
    OnbScaffold("Обои", bg = {
        Crossfade(current, animationSpec = tween(500), label = "wp") { WallpaperBg(it) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)))
    }) {
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.width(680.dp).height(250.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            itemsIndexed(Wallpapers) { i, g ->
                var f by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(16.dp)
                val sel = current == "grad:$i"
                val sc by animateFloatAsState(if (f) 1.06f else 1f, sp(), label = "wsc")
                Stagger(i) {
                    Box(Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.height(104.dp).clip(shape).background(Brush.linearGradient(g))
                        .border(if (f || sel) 3.dp else 1.dp, Color.White.copy(alpha = if (f || sel) 1f else .3f), shape)
                        .onFocusChanged { f = it.isFocused }.clickable { onPick("grad:$i") }) {
                        if (sel) Ico(I.CHECK, Modifier.align(Alignment.TopEnd).padding(8.dp), 22.dp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassButton("Загрузить свои", { pickFile.launch(arrayOf("image/*")) }, icon = I.IMAGE)
            GlassButton(doneText, onDone, Modifier.autoFocus())
        }
    }
}

@Composable
fun ExtrasScreen(st: Settings, vm: MainViewModel, onNext: () -> Unit) {
    OnbScaffold("Дополнительно") {
        Stagger(1) { GlassButton("Формат часов: ${if (st.clock24) "24 ч" else "12 ч"}", { vm.edit { it[K.C24] = !st.clock24 } }, Modifier.autoFocus(), I.TIMER) }
        Stagger(2) { GlassButton("Погода: ${if (st.weather) "включена" else "выключена"}", { vm.edit { it[K.WEATHER] = !st.weather } }, icon = I.CLOUD) }
        Stagger(3) { GlassButton("Виджеты: ${if (st.widgets) "включены" else "выключены"}", { vm.edit { it[K.WIDGETS] = !st.widgets } }, icon = I.APPS) }
        Stagger(4) { GlassButton("Далее", onNext, icon = I.CHEVRON) }
    }
}

@Composable
fun DefaultHomeScreen(onNext: () -> Unit) {
    val ctx = LocalContext.current
    OnbScaffold("Сделать Vortex основным?", "Лаунчер будет открываться по кнопке Home") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stagger(1) { GlassButton("Поставить", { ctx.startActivity(Intent(AndroidSettings.ACTION_HOME_SETTINGS)); onNext() }, Modifier.autoFocus(), I.HOME) }
            Stagger(2) { GlassButton("Пропустить", onNext) }
        }
    }
}

@Composable
fun ProfilePickerScreen(profiles: List<Profile>, onSelect: (Profile) -> Unit, onCreate: (String, String) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    OnbScaffold("Кто смотрит?") {
        if (creating) ProfileEditor(null, { n, i -> onCreate(n, i); creating = false }, { creating = false })
        else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                itemsIndexed(profiles) { i, p ->
                    Stagger(i + 1) {
                        FocusTile({ onSelect(p) }, (if (i == 0) Modifier.autoFocus() else Modifier).width(170.dp), RoundedCornerShape(28.dp)) { f ->
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Avatar(p.name, p.iconUri, 88.dp)
                                Text(p.name, color = ink(f), fontSize = 20.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Stagger(profiles.size + 1) { GlassButton("Создать профиль", { creating = true }, icon = I.PLUS) }
        }
    }
}
