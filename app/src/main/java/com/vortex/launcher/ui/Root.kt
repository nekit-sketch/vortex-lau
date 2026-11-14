package com.vortex.launcher.ui
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vortex.launcher.MainViewModel
import com.vortex.launcher.data.*

@Composable
private fun stateOf(vm: MainViewModel): Settings = vm.settings.collectAsState().value ?: Settings()

@Composable
fun VortexRoot(vm: MainViewModel) {
    // Скин и все настройки приходят из DataStore: после выключения ТВ запускается тот же режим
    val first = vm.settings.collectAsState().value ?: return
    val st = first
    val start = remember { if (first.onboarded) "home" else "splash" }   // фиксируем, иначе граф пересоздаётся
    val nav = rememberNavController()
    LaunchedEffect(Unit) { vm.homePress.collect { nav.popBackStack("home", false) } }
    CompositionLocalProvider(LocalPrefs provides st) {
        WallProvider(st.wallpaper) {
            NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize().background(Color.Black),
                enterTransition = { slideInHorizontally(tween<IntOffset>(300)) { it / 5 } + fadeIn(tween(300)) },
                exitTransition = { slideOutHorizontally(tween<IntOffset>(300)) { -it / 5 } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween<IntOffset>(300)) { -it / 5 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutHorizontally(tween<IntOffset>(300)) { it / 5 } + fadeOut(tween(300)) }) {
                composable("splash") { SplashScreen { nav.navigate("skin") } }
                composable("skin") { SkinScreen { s -> vm.edit { it[K.SKIN] = s }; nav.navigate("wifi") } }
                composable("wifi") { WifiScreen { nav.navigate("wallpaper") } }
                composable("wallpaper") {
                    WallpaperPicker(stateOf(vm).wallpaper, { w -> vm.edit { it[K.WALL] = w } }, "Далее") { nav.navigate("extras") }
                }
                composable("extras") { ExtrasScreen(stateOf(vm), vm) { nav.navigate("default") } }
                composable("default") { DefaultHomeScreen { nav.navigate("profile") } }
                composable("profile") {
                    val ps by vm.profiles.collectAsState()
                    ProfilePickerScreen(ps, { p ->
                        vm.selectProfile(p, fromOnboarding = true); vm.edit { it[K.ONB] = true }
                        nav.navigate("home") { popUpTo(0) }
                    }, { n, i -> vm.saveProfile(Profile(name = n, iconUri = i)) })
                }
                composable("home") { HomeRoute(vm, stateOf(vm), nav) }
                composable("apps") { AllAppsScreen(vm, stateOf(vm)) }
                composable("settings") { SettingsScreen(vm, stateOf(vm), nav) }
                composable("wallpaperSettings") {
                    WallpaperPicker(stateOf(vm).wallpaper, { w -> vm.setWallpaper(w) }, "Готово") { nav.popBackStack() }
                }
                composable("dock") { DockManager(vm) }
                composable("profiles") { ProfileManager(vm) }
            }
        }
    }
}

@Composable
fun HomeRoute(vm: MainViewModel, st: Settings, nav: NavController) {
    BackHandler(true) {}
    val apps by vm.apps.collectAsState()
    val prof by vm.active.collectAsState()
    val tick by vm.resumeTick.collectAsState()
    var cc by remember { mutableStateOf(false) }
    val resume = remember { Animatable(1f) }
    // анимация возврата на главный экран после закрытия приложения
    LaunchedEffect(tick) {
        if (tick > 0 && st.anim) { resume.snapTo(0f); resume.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
    }
    val blurR by animateDpAsState(if (cc && st.blur) 20.dp else 0.dp, tween(350), label = "ccblur")
    val dock = remember(apps, prof?.dockApps) {
        prof?.dockApps.orEmpty().split(",").filter { it.isNotBlank() }.mapNotNull { p -> apps.find { it.pkg == p } }.take(10).ifEmpty { apps.take(8) }
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()
            .graphicsLayer { val v = .94f + .06f * resume.value; scaleX = v; scaleY = v; alpha = (.4f + .6f * resume.value).coerceIn(0f, 1f) }
            .blur(blurR)
            .then(if (cc) Modifier.focusProperties { canFocus = false } else Modifier)) {
            SkinMorph(st.skin) { skin ->
                if (skin == "PS5") Ps5HomeScreen(st, vm, apps, nav) { cc = true }
                else AppleHomeScreen(st, vm, dock, nav) { cc = true }
            }
        }
        AnimatedVisibility(cc, enter = fadeIn(tween(300)), exit = fadeOut(tween(250))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)))
        }
        AnimatedVisibility(cc, Modifier.fillMaxSize(),
            enter = slideInHorizontally(spring<IntOffset>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) { it } + fadeIn(tween(250)),
            exit = slideOutHorizontally(tween<IntOffset>(250)) { it } + fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().padding(36.dp), contentAlignment = Alignment.CenterEnd) {
                ControlCenter(vm, st, apps, nav, { cc = false })
            }
        }
    }
}
