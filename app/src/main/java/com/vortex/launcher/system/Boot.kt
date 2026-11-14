package com.vortex.launcher.system
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vortex.launcher.MainActivity
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.Profile
import com.vortex.launcher.data.SettingsStore
import com.vortex.launcher.ui.LocalPrefs
import com.vortex.launcher.ui.ProfilePickerScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Нужен для MediaSessionManager.getActiveSessions (пользователь выдаёт «Доступ к уведомлениям»). */
class MediaListener : android.service.notification.NotificationListenerService()

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var store: SettingsStore
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED) return
        val pr = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = store.flow.first()
                val home = c.packageManager.resolveActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
                )?.activityInfo?.packageName
                if (s.onboarded && s.askProfile && home == c.packageName)
                    c.startActivity(Intent(c, ProfilePickerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } finally { pr.finish() }
        }
    }
}

@AndroidEntryPoint
class ProfilePickerActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ps by vm.profiles.collectAsState()
            val st by vm.settings.collectAsState()
            CompositionLocalProvider(LocalPrefs provides (st ?: com.vortex.launcher.data.Settings())) {
                ProfilePickerScreen(ps, {
                    vm.selectProfile(it)
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                }, { n, i -> vm.saveProfile(Profile(name = n, iconUri = i)) })
            }
        }
    }
}
