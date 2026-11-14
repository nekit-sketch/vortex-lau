package com.vortex.launcher.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class I { WIFI, POWER, SEARCH, HOME, GEAR, USER, GAME, IMAGE, TIMER, VOLUME, APPS, RECENT, DOTS, CLOSE,
    PLAY, PAUSE, PREV, NEXT, CLOUD, RESTART, TOGGLES, CHEVRON, CHECK, PLUS, TROPHY }

/** Белые векторные иконки, нарисованные в Canvas (сетка 24x24). Эмодзи не используются. */
@Composable
fun Ico(i: I, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Color.White) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        val st = Stroke(2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(a: Float, b: Float, c: Float, d: Float) = drawLine(tint, p(a, b), p(c, d), 2f * u, StrokeCap.Round)
        fun arc(cx: Float, cy: Float, r: Float, s: Float, sw: Float) =
            drawArc(tint, s, sw, false, p(cx - r, cy - r), Size(2 * r * u, 2 * r * u), style = st)
        fun ring(cx: Float, cy: Float, r: Float) = drawCircle(tint, r * u, p(cx, cy), style = st)
        fun dot(cx: Float, cy: Float, r: Float) = drawCircle(tint, r * u, p(cx, cy))
        fun box(x: Float, y: Float, w: Float, h: Float, r: Float, fill: Boolean = false) =
            drawRoundRect(tint, p(x, y), Size(w * u, h * u), CornerRadius(r * u), style = if (fill) Fill else st)
        fun Path.m(x: Float, y: Float) = moveTo(x * u, y * u)
        fun Path.l(x: Float, y: Float) = lineTo(x * u, y * u)
        fun path(fill: Boolean = false, build: Path.() -> Unit) {
            val pa = Path().apply(build)
            drawPath(pa, tint, style = if (fill) Fill else st)
        }
        when (i) {
            I.WIFI -> { arc(12f, 19f, 5f, -135f, 90f); arc(12f, 19f, 9f, -135f, 90f); arc(12f, 19f, 13f, -135f, 90f); dot(12f, 19.5f, 1.5f) }
            I.POWER -> { arc(12f, 13f, 7.5f, -50f, 280f); line(12f, 3.5f, 12f, 12f) }
            I.SEARCH -> { ring(10.5f, 10.5f, 6.5f); line(15.5f, 15.5f, 20.5f, 20.5f) }
            I.HOME -> path { m(3.5f, 11.5f); l(12f, 4f); l(20.5f, 11.5f); m(5.5f, 10f); l(5.5f, 19.5f); l(18.5f, 19.5f); l(18.5f, 10f); m(10f, 19.5f); l(10f, 14.5f); l(14f, 14.5f); l(14f, 19.5f) }
            I.GEAR -> {
                ring(12f, 12f, 3f); ring(12f, 12f, 7f)
                for (k in 0 until 8) {
                    val a = k * PI / 4; val c = cos(a).toFloat(); val s = sin(a).toFloat()
                    line(12f + 7f * c, 12f + 7f * s, 12f + 9.8f * c, 12f + 9.8f * s)
                }
            }
            I.USER -> { ring(12f, 8f, 4f); arc(12f, 21f, 8f, 180f, 180f) }
            I.GAME -> { box(2.5f, 6.5f, 19f, 11f, 5.5f); line(7f, 9.5f, 7f, 14.5f); line(4.5f, 12f, 9.5f, 12f); dot(15.5f, 10.8f, 1.1f); dot(18f, 13.2f, 1.1f) }
            I.IMAGE -> { box(3f, 4f, 18f, 16f, 3f); ring(9f, 9.5f, 1.6f); path { m(3.5f, 17f); l(9f, 12.5f); l(13f, 16f); l(16f, 13.5f); l(20.5f, 17.5f) } }
            I.TIMER -> { ring(12f, 13.5f, 7.5f); line(12f, 13.5f, 15f, 10.5f); line(9.5f, 3f, 14.5f, 3f); line(12f, 3f, 12f, 6f) }
            I.VOLUME -> { path { m(4f, 9.5f); l(8f, 9.5f); l(13f, 5f); l(13f, 19f); l(8f, 14.5f); l(4f, 14.5f); close() }; arc(13f, 12f, 4.5f, -50f, 100f); arc(13f, 12f, 8f, -50f, 100f) }
            I.APPS -> { box(4f, 4f, 6f, 6f, 1.6f); box(14f, 4f, 6f, 6f, 1.6f); box(4f, 14f, 6f, 6f, 1.6f); box(14f, 14f, 6f, 6f, 1.6f) }
            I.RECENT -> { box(8f, 3.5f, 12.5f, 12.5f, 2.6f); box(3.5f, 8f, 12.5f, 12.5f, 2.6f) }
            I.DOTS -> { dot(5f, 12f, 1.9f); dot(12f, 12f, 1.9f); dot(19f, 12f, 1.9f) }
            I.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            I.PLAY -> path(true) { m(8f, 5.5f); l(8f, 18.5f); l(19f, 12f); close() }
            I.PAUSE -> { box(7f, 5.5f, 3.6f, 13f, 1.2f, true); box(13.4f, 5.5f, 3.6f, 13f, 1.2f, true) }
            I.PREV -> { path(true) { m(18f, 6f); l(18f, 18f); l(9f, 12f); close() }; line(6.5f, 6f, 6.5f, 18f) }
            I.NEXT -> { path(true) { m(6f, 6f); l(6f, 18f); l(15f, 12f); close() }; line(17.5f, 6f, 17.5f, 18f) }
            I.CLOUD -> { dot(8.5f, 14f, 4f); dot(14f, 11.5f, 5f); dot(18f, 15f, 3.4f); box(8.5f, 14f, 9.5f, 4.4f, 0f, true) }
            I.RESTART -> { arc(12f, 12f, 7.5f, -40f, 290f); path { m(17.9f, 3f); l(17.75f, 7.2f); l(13.6f, 6.6f) } }
            I.TOGGLES -> { box(3f, 4.5f, 18f, 6f, 3f); dot(16.5f, 7.5f, 1.7f); box(3f, 13.5f, 18f, 6f, 3f); dot(7.5f, 16.5f, 1.7f) }
            I.CHEVRON -> path { m(9f, 5f); l(16f, 12f); l(9f, 19f) }
            I.CHECK -> path { m(5f, 12.5f); l(10f, 17.5f); l(19f, 7f) }
            I.PLUS -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
            I.TROPHY -> { path { m(7f, 4f); l(17f, 4f); l(17f, 9f); l(12f, 14f); l(7f, 9f); close() }; line(12f, 14f, 12f, 18.5f); line(8f, 20f, 16f, 20f); arc(5f, 7.5f, 2.5f, 90f, 180f); arc(19f, 7.5f, 2.5f, -90f, 180f) }
        }
    }
}
