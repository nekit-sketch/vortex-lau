package com.vortex.launcher
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.vortex.launcher.ui.VolumeOverlay
import com.vortex.launcher.ui.VortexRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var vol by mutableIntStateOf(0)
    private var stamp by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Скин и остальные настройки читаются из DataStore внутри VortexRoot — состояние восстанавливается при каждом старте
        setContent { Box(Modifier.fillMaxSize()) { VortexRoot(vm); VolumeOverlay(vol, stamp) } }
    }
    override fun onResume() { super.onResume(); vm.onResumed() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.homePress.tryEmit(Unit)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            adjust(keyCode == KeyEvent.KEYCODE_VOLUME_UP, event.repeatCount); return true
        }
        return super.onKeyDown(keyCode, event)
    }
    /** Шкала 0..25 (1 шаг = 4%). Удержание: +5 каждые 5 повторов, +10 после 20. */
    private fun adjust(up: Boolean, rep: Int) {
        val step = when { rep == 0 -> 1; rep % 5 != 0 -> 0; rep >= 20 -> 10; else -> 5 }
        if (step == 0) return
        val am = getSystemService(AudioManager::class.java)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = Math.round(am.getStreamVolume(AudioManager.STREAM_MUSIC) * 25f / max)
        val n = (cur + if (up) step else -step).coerceIn(0, 25)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(n * max / 25f), 0)
        vol = n; stamp = System.currentTimeMillis()
    }
}
