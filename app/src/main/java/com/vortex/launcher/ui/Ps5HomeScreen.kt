package com.vortex.launcher.ui
import android.content.Intent
import android.view.SoundEffectConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.Text
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*

/** Игровой режим в стиле PS5: вкладки, карусель, баннер выбранного приложения, кнопка «Играть», блок статистики. */
@Composable
fun Ps5HomeScreen(st: Settings, vm: MainViewModel, apps: List<AppInfo>, nav: NavController, onCC: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val launch = rememberLaunch()
    val cfg = LocalConfiguration.current
    val time by rememberTime(st.clock24)
    val prof by vm.active.collectAsState()
    val accent = Accents[st.accent.coerceIn(0, Accents.lastIndex)].second
    var tab by remember { mutableIntStateOf(0) }
    var foc by remember { mutableIntStateOf(0) }
    val list = remember(apps, tab) { if (tab == 0) apps else apps.filter { it.category != 0 }.ifEmpty { apps } }
    val cur = list.getOrNull(foc.coerceIn(0, (list.size - 1).coerceAtLeast(0)))
    val p = prof
    LaunchedEffect(tab) { foc = 0 }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PsBg, Color(0xFF0A1024))))) {
        // фоновый «арт» выбранного приложения
        Crossfade(cur, animationSpec = tween(550), label = "bg") { a ->
            if (a != null) Box(Modifier.fillMaxSize()) {
                Image(a.icon, null, Modifier.align(Alignment.CenterEnd).offset(x = 120.dp).requiredSize(cfg.screenHeightDp.dp * 1.15f),
                    contentScale = ContentScale.Crop, alpha = .55f)
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PsBg, .42f to PsBg.copy(alpha = .86f), .8f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to PsBg.copy(alpha = .75f), .3f to Color.Transparent, .75f to Color.Transparent, 1f to PsBg.copy(alpha = .7f))))

        Column(Modifier.fillMaxSize()) {
            // верхняя панель
            Row(Modifier.fillMaxWidth().padding(start = 64.dp, end = 64.dp, top = 30.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    PsTab("Приложения", tab == 0, accent) { tab = 0 }
                    PsTab("Мультимедиа", tab == 1, accent) { tab = 1 }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBtn(I.SEARCH, { runCatching { ctx.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } })
                    IconBtn(I.GEAR, { nav.navigate("settings") })
                    FocusTile(onCC, Modifier.size(52.dp), CircleShape, Color.Transparent, Color.White.copy(alpha = .25f), 1.1f) { _ ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (p != null) Avatar(p.name, p.iconUri, 40.dp) else Ico(I.USER, size = 26.dp) }
                    }
                    Text(time, Modifier.padding(start = 12.dp), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                }
            }
            Spacer(Modifier.height(18.dp))

            // карусель; выбранный элемент закреплён слева
            key(tab) {
                val ls = rememberLazyListState()
                LaunchedEffect(foc) { runCatching { ls.animateScrollToItem(foc) } }
                LazyRow(Modifier.fillMaxWidth().height(176.dp), state = ls, contentPadding = PaddingValues(start = 64.dp, end = 64.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(list) { i, a ->
                        var f by remember { mutableStateOf(false) }
                        val sz by animateDpAsState(if (f) 156.dp else 112.dp, sp(), label = "pt")
                        val shape = RoundedCornerShape(sz * .16f)
                        Box(Modifier.size(sz).then(if (i == 0 && tab == 0) Modifier.autoFocus() else Modifier).clip(shape)
                            .border(if (f) 3.dp else 0.dp, Color.White.copy(alpha = .9f), shape)
                            .onFocusChanged { f = it.isFocused; if (f) { foc = i; if (st.sounds) view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT) } }
                            .clickable { launch(a.pkg, null) }) {
                            Image(a.icon, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    item {
                        FocusTile({ nav.navigate("apps") }, Modifier.size(112.dp), RoundedCornerShape(18.dp), Color.White.copy(alpha = .08f), Color.White.copy(alpha = .25f), 1.1f) { _ ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ico(I.APPS, size = 44.dp) }
                        }
                    }
                }
            }
            if (cur != null) Text(cur.label, Modifier.padding(start = 64.dp + 156.dp + 16.dp, top = 2.dp), color = Color.White.copy(alpha = .92f), fontSize = 22.sp, maxLines = 1)
        }

        // баннер/название и кнопки внизу слева
        Column(Modifier.align(Alignment.BottomStart).padding(start = 80.dp, bottom = 64.dp).width(760.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedContent(cur, transitionSpec = {
                (fadeIn(tween(380)) + slideInVertically(tween<IntOffset>(380)) { it / 8 }) togetherWith fadeOut(tween(150))
            }, label = "hero") { a ->
                if (a != null) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(a.label, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(when (a.category) { 2 -> "Игра"; 1 -> "Медиа и развлечения"; else -> "Приложение" }, color = Color.White.copy(alpha = .65f), fontSize = 22.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusTile({ cur?.let { launch(it.pkg, null) } }, Modifier.width(380.dp).height(64.dp), CircleShape,
                    Color.White.copy(alpha = .12f), Color.White.copy(alpha = .94f), 1.04f) { f ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (cur?.category == 2) "Играть" else "Открыть", color = ink(f), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconBtn(I.DOTS, { nav.navigate("apps") }, size = 64.dp)
            }
        }

        // блок статистики (вместо трофеев): запуски и тип
        if (cur != null) {
            val launches = ctx.getSharedPreferences("launches", 0).getInt(cur.pkg, 0)
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 64.dp, bottom = 72.dp).clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = .45f)).padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Ico(I.TROPHY, size = 40.dp, tint = Color.White.copy(alpha = .75f))
                Column { Text("Запусков", color = Color.White.copy(alpha = .65f), fontSize = 16.sp); Text("$launches", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light) }
                Column { Text("Тип", color = Color.White.copy(alpha = .65f), fontSize = 16.sp)
                    Text(when (cur.category) { 2 -> "Игра"; 1 -> "Медиа"; else -> "Прочее" }, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light) }
            }
        }
    }
}

@Composable
private fun PsTab(text: String, selected: Boolean, accent: Color, onSelect: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.clip(shape).onFocusChanged { f = it.isFocused; if (f) onSelect() }.clickable(onClick = onSelect).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = if (selected) Color.White else Color.White.copy(alpha = .5f), fontSize = 30.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Box(Modifier.padding(top = 4.dp).height(3.dp).width(if (selected || f) 40.dp else 0.dp).clip(CircleShape).background(if (f) Color.White else accent))
    }
}
