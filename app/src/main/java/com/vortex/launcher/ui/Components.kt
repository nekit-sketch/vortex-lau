package com.vortex.launcher.ui
import android.app.ActivityOptions
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.SoundEffectConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import com.vortex.launcher.R
import com.vortex.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max

val LocalPrefs = compositionLocalOf { Settings() }
val DarkInk = Color(0xFF0E1A16)
fun ink(f: Boolean) = if (f) DarkInk else Color.White
val Accents = listOf("Голубой" to Color(0xFF00A8FF), "Белый" to Color(0xFFFFFFFF), "Розовый" to Color(0xFFFF4D8D),
    "Зелёный" to Color(0xFF2ED573), "Оранжевый" to Color(0xFFFF9F1A))
val ClockColors = listOf(Color.White, Color(0xFF7DD3FC), Color(0xFFFCA5A5), Color(0xFFA7F3D0))
val ClockFonts = listOf(FontFamily.SansSerif, FontFamily.Serif, FontFamily.Monospace)
val Wallpapers = listOf(
    listOf(Color(0xFF141E30), Color(0xFF35577D), Color(0xFF6A8CAF)), listOf(Color(0xFF2B1055), Color(0xFF7597DE)),
    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)), listOf(Color(0xFFFF512F), Color(0xFFDD2476)),
    listOf(Color(0xFF134E5E), Color(0xFF71B280)), listOf(Color(0xFF000428), Color(0xFF004E92))
)
val PsBg = Color(0xFF05070D)

@Composable
fun <T> sp(): AnimationSpec<T> =
    if (LocalPrefs.current.anim) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium) else snap()

fun Modifier.autoFocus(): Modifier = composed {
    val r = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { r.requestFocus() } }
    focusRequester(r)
}

@Composable
fun Stagger(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val anim = LocalPrefs.current.anim
    var v by remember { mutableStateOf(!anim) }
    LaunchedEffect(Unit) { delay(index * 90L + 60); v = true }
    AnimatedVisibility(v, modifier,
        enter = fadeIn(tween(450)) + slideInVertically(spring<IntOffset>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) { it / 4 } + scaleIn(tween(450), initialScale = .92f)) {
        content()
    }
}

/** Плитка с фокус-анимацией (liquid glass). */
@Composable
fun FocusTile(
    onClick: () -> Unit, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(26.dp),
    idle: Color = Color.Black.copy(alpha = .28f), focusFill: Color = Color.White.copy(alpha = .94f),
    focusScale: Float = 1.04f, onFocus: (Boolean) -> Unit = {}, content: @Composable BoxScope.(Boolean) -> Unit
) {
    val P = LocalPrefs.current
    val view = LocalView.current
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) focusScale else 1f, sp(), label = "ft")
    val bg by animateColorAsState(if (f) focusFill else idle, tween(180), label = "ftc")
    Box(
        modifier.graphicsLayer { scaleX = s; scaleY = s }
            .clip(shape).background(bg)
            .border(1.dp, Color.White.copy(alpha = if (f) .95f else .22f), shape)
            .onFocusChanged { f = it.isFocused; onFocus(f); if (f && P.sounds) view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN) }
            .clickable(onClick = onClick),
        content = { content(f) }
    )
}

@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: I? = null) {
    FocusTile(onClick, modifier, RoundedCornerShape(24.dp), Color.White.copy(alpha = .15f), Color.White.copy(alpha = .32f), 1.05f) { _ ->
        Row(Modifier.padding(horizontal = 28.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon?.let { Ico(it, size = 22.dp) }
            Text(text, color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
fun IconBtn(i: I, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    FocusTile(onClick, modifier.size(size), CircleShape, focusScale = 1.1f) { f ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ico(i, size = size * .5f, tint = ink(f)) }
    }
}

val AvatarColors = listOf(Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEF4444))
@Composable
fun Avatar(name: String, icon: String?, size: Dp) {
    val c = AvatarColors[(icon?.toIntOrNull() ?: 0).mod(AvatarColors.size)]
    Box(Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(c, c.copy(alpha = .6f)))), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = Color.White, fontSize = (size.value * .46f).sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- запуск приложений с анимацией ----------
fun launchApp(ctx: Context, view: View, pkg: String, r: Rect?) {
    val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
    ctx.getSharedPreferences("launches", 0).let { it.edit().putInt(pkg, it.getInt(pkg, 0) + 1).apply() }
    val o = if (r != null && r.width > 0f) ActivityOptions.makeScaleUpAnimation(view, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    else ActivityOptions.makeCustomAnimation(ctx, R.anim.app_open_enter, R.anim.app_open_exit)
    runCatching { ctx.startActivity(i, o.toBundle()) }
}
@Composable
fun rememberLaunch(): (String, Rect?) -> Unit {
    val c = LocalContext.current; val v = LocalView.current
    return remember(c, v) { { pkg: String, r: Rect? -> launchApp(c, v, pkg, r) } }
}

@Composable
fun AppIcon(app: AppInfo, size: Dp, onClick: (Rect?) -> Unit, modifier: Modifier = Modifier, label: Boolean = false,
            focusScale: Float = 1.18f, onFocus: (Boolean) -> Unit = {}) {
    val P = LocalPrefs.current
    val view = LocalView.current
    var f by remember { mutableStateOf(false) }
    var rect by remember { mutableStateOf<Rect?>(null) }
    val sc by animateFloatAsState(if (f) focusScale else 1f, sp(), label = "ai")
    val lift by animateDpAsState(if (f) (-6).dp else 0.dp, sp(), label = "ail")
    val shape = RoundedCornerShape(size * .22f)
    Column(
        modifier.graphicsLayer { scaleX = sc; scaleY = sc; translationY = lift.toPx() }
            .onGloballyPositioned { rect = it.boundsInWindow() }
            .onFocusChanged { f = it.isFocused; onFocus(f); if (f && P.sounds) view.playSoundEffect(SoundEffectConstants.NAVIGATION_RIGHT) }
            .clickable { onClick(rect) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(size).clip(shape).border(if (f) 3.dp else 0.dp, Color.White, shape)) {
            Image(app.icon, null, Modifier.fillMaxSize())
        }
        if (label) Text(app.label, color = Color.White, fontSize = 15.sp, maxLines = 1, modifier = Modifier.width(size + 16.dp).padding(top = 6.dp))
    }
}

// ---------- обои и «стекло» ----------
class Wall(val colors: List<Color>?, val soft: ImageBitmap?)
val LocalWall = compositionLocalOf { Wall(Wallpapers[0], null) }
val PsWall = Wall(listOf(PsBg, Color(0xFF0B1226)), null)

suspend fun loadBitmap(ctx: Context, w: String, sample: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        ctx.contentResolver.openInputStream(Uri.parse(w))!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })!!.asImageBitmap()
        }
    }.getOrNull()
}

@Composable
fun WallProvider(w: String, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val soft by produceState<ImageBitmap?>(null, w) { value = if (w.startsWith("grad:")) null else loadBitmap(ctx, w, 16) }
    val wall = remember(w, soft) {
        if (w.startsWith("grad:")) Wall(Wallpapers[(w.removePrefix("grad:").toIntOrNull() ?: 0).coerceIn(0, Wallpapers.lastIndex)], null) else Wall(null, soft)
    }
    CompositionLocalProvider(LocalWall provides wall) { content() }
}

@Composable
fun WallpaperBg(w: String) {
    if (w.startsWith("grad:")) {
        val g = Wallpapers[(w.removePrefix("grad:").toIntOrNull() ?: 0).coerceIn(0, Wallpapers.lastIndex)]
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(g)))
    } else {
        val ctx = LocalContext.current
        val bmp by produceState<ImageBitmap?>(null, w) { value = loadBitmap(ctx, w, 2) }
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
            ?: Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

private fun DrawScope.drawSoft(w: Wall, W: Float, H: Float) {
    if (w.colors != null) drawRect(Brush.linearGradient(w.colors, Offset.Zero, Offset(W, H)), size = Size(W, H))
    else w.soft?.let { b ->
        val sc = max(W / b.width, H / b.height)
        val sw = (W / sc).toInt(); val sh = (H / sc).toInt()
        drawImage(b, IntOffset((b.width - sw) / 2, (b.height - sh) / 2), IntSize(sw, sh), IntOffset.Zero, IntSize(W.toInt(), H.toInt()), filterQuality = FilterQuality.High)
    }
}

/** Liquid glass: размытая копия обоев (уменьшенный битмап/градиент) под плашкой + белая подложка и обводка 1dp. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.White.copy(alpha = .16f), border: Color = Color.White.copy(alpha = .24f),
    content: @Composable BoxScope.() -> Unit
) {
    val wall = LocalWall.current
    val on = LocalPrefs.current.blur
    val view = LocalView.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.clip(shape).onGloballyPositioned { pos = it.positionInWindow() }
            .drawBehind {
                if (on) {
                    val W = view.rootView.width.toFloat(); val H = view.rootView.height.toFloat()
                    translate(-pos.x, -pos.y) { drawSoft(wall, W, H) }
                }
                drawRect(tint)
            }
            .border(1.dp, border, shape),
        content = content
    )
}

@Composable
fun rememberTime(is24: Boolean): State<String> = produceState("", is24) {
    val fmt = SimpleDateFormat(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
    while (true) { value = fmt.format(Date()); delay(1000) }
}
fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { h in 5..10 -> "Доброе утро"; h in 11..16 -> "Добрый день"; h in 17..22 -> "Добрый вечер"; else -> "Спокойной ночи" }
}
fun dateText(): String = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())

@Composable
fun ClockPill(st: Settings, modifier: Modifier = Modifier) {
    val t by rememberTime(st.clock24)
    GlassPanel(modifier, RoundedCornerShape(28.dp)) {
        Text(t, Modifier.padding(horizontal = 28.dp, vertical = 6.dp), color = ClockColors[st.clockColor % ClockColors.size],
            fontSize = 56.sp, fontWeight = FontWeight.Thin, fontFamily = ClockFonts[st.clockFont % ClockFonts.size])
    }
}

/** Морфинг скинов: новый скин «раскрывается» кругом из центра поверх старого (который уходит с масштабом и затуханием). */
@Composable
fun SkinMorph(skin: String, content: @Composable (String) -> Unit) {
    val anim = LocalPrefs.current.anim
    var shown by remember { mutableStateOf(skin) }
    var prev by remember { mutableStateOf<String?>(null) }
    val t = remember { Animatable(1f) }
    LaunchedEffect(skin) {
        if (skin != shown) {
            prev = shown; shown = skin
            t.snapTo(0f)
            t.animateTo(1f, tween(if (anim) 950 else 1, easing = CubicBezierEasing(.2f, .8f, .2f, 1f)))
            prev = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        prev?.let { p ->
            Box(Modifier.fillMaxSize().focusProperties { canFocus = false }
                .graphicsLayer { val k = t.value; scaleX = 1f + .08f * k; scaleY = scaleX; alpha = 1f - .6f * k }) { content(p) }
        }
        Box(Modifier.fillMaxSize().then(
            if (prev != null) Modifier.drawWithContent {
                val r = hypot(size.width, size.height) / 2f * t.value * 1.05f
                val path = Path().apply { addOval(Rect(center, r)) }
                clipPath(path) { this@drawWithContent.drawContent() }
                drawCircle(Color.White.copy(alpha = (1f - t.value) * .7f), r, style = Stroke(6f))
            } else Modifier
        )) { content(shown) }
    }
}

@Composable
fun ProfileEditor(initial: Profile?, onSave: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.iconUri?.toIntOrNull() ?: 0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BasicTextField(name, { name = it }, textStyle = TextStyle(color = Color.White, fontSize = 28.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.autoFocus().width(380.dp).border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(16.dp)).padding(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AvatarColors.indices.forEach { idx ->
                FocusTile({ icon = idx }, Modifier.size(72.dp), CircleShape, Color.Transparent, Color.White.copy(alpha = .25f), 1.1f) { _ ->
                    Box(Modifier.fillMaxSize().border(if (icon == idx) 3.dp else 0.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Avatar(name.ifBlank { "A" }, idx.toString(), 54.dp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassButton("Сохранить", { if (name.isNotBlank()) onSave(name.trim(), icon.toString()) })
            GlassButton("Отмена", onCancel)
        }
    }
}

@Composable
fun TextPrompt(title: String, initial: String, onOk: (String) -> Unit, onCancel: () -> Unit) {
    var v by remember { mutableStateOf(initial) }
    BackHandler(true, onCancel)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .75f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(title, color = Color.White, fontSize = 28.sp)
            BasicTextField(v, { v = it }, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 28.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.autoFocus().width(420.dp).border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(16.dp)).padding(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassButton("Сохранить", { if (v.isNotBlank()) onOk(v.trim()) }); GlassButton("Отмена", onCancel)
            }
        }
    }
}

@Composable
fun VolumeOverlay(level: Int, stamp: Long) {
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(stamp) { if (stamp > 0) { vis = true; delay(1600); vis = false } }
    Box(Modifier.fillMaxSize().padding(end = 32.dp), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(vis,
            enter = slideInHorizontally(tween<IntOffset>(250)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween<IntOffset>(250)) { it } + fadeOut(tween(250))) {
            Box(Modifier.width(72.dp).height(300.dp).clip(RoundedCornerShape(36.dp)).background(Color.White.copy(alpha = .25f))) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(level / 25f).background(Color.White))
                Ico(I.VOLUME, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), 28.dp, Color.Black.copy(alpha = .55f))
            }
        }
    }
}
